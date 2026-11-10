const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { test } = require('node:test')
const vm = require('node:vm')

const preload = readFileSync(resolve(__dirname, '../../main/assets/customsource/preload.js'), 'utf8')

function createHost() {
    const requests = []
    const cancelled = []
    const results = []
    const bridge = {
        scriptInfo: () => '{}',
        log() {},
        ready() {},
        request(id, payload) { requests.push({ id, ...JSON.parse(payload) }) },
        cancelRequest(id) { cancelled.push(id) },
        result(id, payload) { results.push({ id, ...JSON.parse(payload) }) }
    }
    const host = vm.createContext({ window: { LyraSource: bridge }, console: { log() {} } })
    vm.runInContext(preload, host)
    return { host, requests, cancelled, results, bridge }
}

function success(body) {
    return JSON.stringify({ ok: true, statusCode: 200, statusMessage: 'OK', headers: {}, body })
}

test('concurrent requests complete out of order without waiting for slow requests', () => {
    const { host, requests } = createHost()
    const completed = []
    host.lx.request('https://example.com/slow', {}, (_, response, body) => completed.push(['slow', response.statusCode, body]))
    host.lx.request('https://example.com/fast', {}, (_, response, body) => completed.push(['fast', response.statusCode, body]))
    assert.equal(requests.length, 2)
    assert.deepEqual(completed, [])
    host.__neriHttpResult(requests[1].id, success('fast'))
    assert.deepEqual(completed, [['fast', 200, 'fast']])
    host.__neriHttpResult(requests[0].id, success('slow'))
    assert.deepEqual(completed, [['fast', 200, 'fast'], ['slow', 200, 'slow']])
})

test('abort cancels only its request and ignores late or repeated responses', () => {
    const { host, requests, cancelled } = createHost()
    const completed = []
    const abort = host.lx.request('https://example.com/a', {}, () => completed.push('a'))
    host.lx.request('https://example.com/b', {}, () => completed.push('b'))
    abort()
    abort()
    host.__neriHttpResult(requests[0].id, success('late'))
    host.__neriHttpResult(requests[1].id, success('ok'))
    host.__neriHttpResult(requests[1].id, success('duplicate'))
    assert.deepEqual(cancelled, [requests[0].id])
    assert.deepEqual(completed, ['b'])
})

test('network errors retain original error and null response', () => {
    const { host, requests } = createHost()
    let received
    host.lx.request('https://example.com/error', {}, (error, response, body) => {
        received = [error.message, response, body]
    })
    host.__neriHttpResult(requests[0].id, JSON.stringify({ ok: false, error: 'connection reset' }))
    assert.deepEqual(received, ['connection reset', null, null])
})

test('HTTP failures retain status and response body for scripts', () => {
    const { host, requests } = createHost()
    let received
    host.lx.request('https://example.com/forbidden', {}, (error, response, body) => {
        received = [error, response.statusCode, body]
    })
    host.__neriHttpResult(requests[0].id, JSON.stringify({ ok: true, statusCode: 403, body: 'forbidden' }))
    assert.deepEqual(received, [null, 403, 'forbidden'])
})

test('async musicUrl handler resolves through the new bridge protocol', async () => {
    const { host, requests, results } = createHost()
    vm.runInContext(`lx.on(lx.EVENT_NAMES.request, function () {
        return new Promise(function (resolve, reject) {
            lx.request('https://example.com/audio', {}, function (error, response, body) {
                if (error) reject(error)
                else resolve(body)
            })
        })
    })`, host)
    host.__neriInvokeRequest('song-1', JSON.stringify({ source: 'test', action: 'musicUrl', info: {} }))
    assert.equal(results.length, 0)
    host.__neriHttpResult(requests[0].id, success('https://example.com/audio.mp3'))
    await new Promise(resolve => setImmediate(resolve))
    assert.deepEqual(results, [{ id: 'song-1', ok: true, value: 'https://example.com/audio.mp3' }])
})

test('bridge registration failure removes the callback', () => {
    const { host, bridge } = createHost()
    let called = false
    bridge.request = () => { throw new Error('bridge unavailable') }
    assert.throws(() => host.lx.request('https://example.com/fail', {}, () => { called = true }), /bridge unavailable/)
    host.__neriHttpResult('1', success('late'))
    assert.equal(called, false)
})
