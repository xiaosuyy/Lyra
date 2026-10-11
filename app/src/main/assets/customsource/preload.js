"use strict";

/*
 * NeriPlayer 自定义源（音源脚本）运行环境
 *
 * 这里对齐 lx-music 的自定义源 API：
 *   globalThis.lx = { EVENT_NAMES, request, send, on, utils, currentScriptInfo, version, env }
 * 脚本通过 send(EVENT_NAMES.inited, { sources }) 声明音源，
 * 通过 on(EVENT_NAMES.request, handler) 响应 musicUrl 等请求。
 *
 * 与原生（Kotlin）之间通过 window.NeriSource 同步桥接：网络请求、md5 由原生完成。
 */
(function () {
    var bridge = window.NeriSource
    if (!bridge) {
        console.log('NeriSource bridge missing')
        return
    }
    console.log('preload starting, bridge ready')

    var EVENT_NAMES = {
        request: 'request',
        inited: 'inited',
        updateAlert: 'updateAlert'
    }
    var eventNames = [EVENT_NAMES.request, EVENT_NAMES.inited, EVENT_NAMES.updateAlert]
    var events = { request: null }

    function bytesToString(bytes) {
        var result = ''
        var i = 0
        while (i < bytes.length) {
            var byte = bytes[i]
            if (byte < 128) {
                result += String.fromCharCode(byte)
                i++
            } else if (byte >= 192 && byte < 224) {
                result += String.fromCharCode(((byte & 31) << 6) | (bytes[i + 1] & 63))
                i += 2
            } else {
                result += String.fromCharCode(((byte & 15) << 12) | ((bytes[i + 1] & 63) << 6) | (bytes[i + 2] & 63))
                i += 3
            }
        }
        return result
    }

    function stringToBytes(input) {
        var bytes = []
        for (var i = 0; i < input.length; i++) {
            var code = input.charCodeAt(i)
            if (code < 128) {
                bytes.push(code)
            } else if (code < 2048) {
                bytes.push((code >> 6) | 192)
                bytes.push((code & 63) | 128)
            } else {
                bytes.push((code >> 12) | 224)
                bytes.push(((code >> 6) & 63) | 128)
                bytes.push((code & 63) | 128)
            }
        }
        return bytes
    }

    function toBase64(bytes) {
        var binary = ''
        for (var i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i] & 0xff)
        }
        return window.btoa(binary)
    }

    // 与 lx-music 的 utils_str2b64 等价：先把字符串按 UTF-8 编码再 base64
    function str2b64(value) {
        return toBase64(stringToBytes(value))
    }

    function fromBase64(value) {
        var binary = window.atob(value)
        var bytes = new Uint8Array(binary.length)
        for (var i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i)
        }
        return bytes
    }

    var utils = {
        crypto: {
            md5: function (str) {
                if (typeof str !== 'string') throw new Error('param required a string')
                // 与 lx-music 保持一致：对 encodeURIComponent 之后的字符串取 md5
                return bridge.md5(encodeURIComponent(str))
            },
            randomBytes: function (size) {
                var byteArray = new Uint8Array(size)
                for (var i = 0; i < size; i++) {
                    byteArray[i] = Math.floor(Math.random() * 256)
                }
                return byteArray
            },
            aesEncrypt: function () {
                throw new Error('aesEncrypt is not supported')
            },
            rsaEncrypt: function () {
                throw new Error('rsaEncrypt is not supported')
            }
        },
        buffer: {
            from: function (input, encoding) {
                if (typeof input === 'string') {
                    switch (encoding) {
                        case 'base64':
                            return fromBase64(input)
                        case 'hex': {
                            var matched = input.match(/.{1,2}/g) || []
                            return new Uint8Array(matched.map(function (byte) {
                                return parseInt(byte, 16)
                            }))
                        }
                        default:
                            return new Uint8Array(stringToBytes(input))
                    }
                }
                if (Array.isArray(input) || ArrayBuffer.isView(input)) {
                    return new Uint8Array(input)
                }
                throw new Error('Unsupported input type: ' + input + ' encoding: ' + encoding)
            },
            bufToString: function (buf, format) {
                if (Array.isArray(buf) || ArrayBuffer.isView(buf)) {
                    var bytes = Array.from(new Uint8Array(buf.buffer || buf))
                    switch (format) {
                        case 'binary':
                            return buf
                        case 'hex':
                            return bytes.reduce(function (text, byte) {
                                return text + byte.toString(16).padStart(2, '0')
                            }, '')
                        case 'base64':
                            // lx-music: utils_str2b64(bytesToString(buf))
                            return str2b64(bytesToString(bytes))
                        case 'utf8':
                        case 'utf-8':
                        default:
                            return bytesToString(bytes)
                    }
                }
                throw new Error('Input is not a valid buffer: ' + buf + ' format: ' + format)
            }
        }
    }

    function buildResponse(json) {
        var payload = JSON.parse(json)
        if (!payload.ok) {
            return { error: payload.error || 'request failed', response: null, body: null }
        }
        return {
            error: null,
            response: {
                statusCode: payload.statusCode,
                statusMessage: payload.statusMessage,
                headers: payload.headers || {},
                body: payload.body
            },
            body: payload.body
        }
    }

    globalThis.lx = {
        EVENT_NAMES: EVENT_NAMES,
        env: 'mobile',
        version: '2.0.0',
        currentScriptInfo: JSON.parse(bridge.scriptInfo()),
        utils: utils,
        request: function (url, options, callback) {
            var result = buildResponse(bridge.request(JSON.stringify({
                url: String(url),
                options: options || {}
            })))
            // 保持异步语义，和 lx-music 的行为一致
            Promise.resolve().then(function () {
                if (result.error) {
                    callback(new Error(result.error), null, null)
                } else {
                    callback(null, result.response, result.body)
                }
            })
            return function () { /* 原生请求已经完成，无需取消 */ }
        },
        send: function (eventName, data) {
            return new Promise(function (resolve, reject) {
                if (eventNames.indexOf(eventName) === -1) {
                    reject(new Error('The event is not supported: ' + eventName))
                    return
                }
                if (eventName === EVENT_NAMES.inited) {
                    bridge.inited(JSON.stringify(data || {}))
                    resolve()
                    return
                }
                if (eventName === EVENT_NAMES.updateAlert) {
                    resolve()
                    return
                }
                reject(new Error('Unknown event name: ' + eventName))
            })
        },
        on: function (eventName, handler) {
            if (eventNames.indexOf(eventName) === -1) {
                return Promise.reject(new Error('The event is not supported: ' + eventName))
            }
            if (eventName === EVENT_NAMES.request) {
                events.request = handler
                return Promise.resolve()
            }
            return Promise.reject(new Error('The event is not supported: ' + eventName))
        }
    }

    // 供原生调用：执行一次 request 事件（musicUrl 等）
    globalThis.__neriInvokeRequest = function (id, payloadJson) {
        var payload = JSON.parse(payloadJson)
        var done = function (json) {
            bridge.result(id, json)
        }
        if (!events.request) {
            done(JSON.stringify({ ok: false, error: '音源未注册 request 事件' }))
            return
        }
        var result
        try {
            result = events.request.call(globalThis.lx, {
                source: payload.source,
                action: payload.action,
                info: payload.info
            })
        } catch (error) {
            done(JSON.stringify({ ok: false, error: (error && error.message) || String(error) }))
            return
        }
        Promise.resolve(result).then(function (value) {
            done(JSON.stringify({ ok: true, value: value == null ? null : String(value) }))
        }, function (error) {
            done(JSON.stringify({ ok: false, error: (error && error.message) || String(error) }))
        })
    }

    bridge.log('preload ready')
    bridge.ready()
})()
