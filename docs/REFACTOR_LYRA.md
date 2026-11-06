# Lyra 重构说明 / Lyra Refactor Notes

本文记录从 NeriPlayer / BlueBlue 到 Lyra 的底层改名决策、保留的兼容标识符与移除范围，
供维护者与后续贡献者查证。
This document records the underlying rename decisions from NeriPlayer / BlueBlue to Lyra,
the preserved compatibility identifiers, and the removed scope, for maintainers and
future contributors.

---

## 1. 背景 / Background

- 应用品牌由 NeriPlayer（界面文案曾用 BlueBlue）统一改为 **Lyra**。
- 包名与 applicationId 由 `moe.ouom.neriplayer` 彻底改为 `com.lyra.player`。
  这是一个**新应用 ID**，不支持从旧版本原地升级安装；旧版本数据不会自动迁移。
- 「一起听（Listen Together）」功能整体移除，含 Android 客户端、Cloudflare Worker
  服务端、协议文档与 CI 步骤。
- 应用图标全部替换为新的 Lyra 图标（黑底圆角方块 + 白色 Lyra 字标）。

## 2. 保留的旧持久化标识符 / Preserved Legacy Identifiers

以下标识符刻意**保留旧值**，不做迁移，以保证已安装设备上的本地数据、授权与远端
同步文件继续可用。修改这些值会破坏既有用户数据，除非同步设计数据迁移。

The following identifiers intentionally keep their old values so existing local data,
credentials, and remote sync files keep working on already-installed devices.
Do not change them without a designed data migration.

| 标识符 / Identifier | 位置 / Location | 说明 / Reason |
| --- | --- | --- |
| `neri_user_data.db` | Room 数据库文件名 | 已存在的本地数据库文件 |
| `auth_store`、`bili_auth_store` | EncryptedSharedPreferences 名称 | 已加密的平台登录凭据 |
| 通知渠道 ID（旧值） | 通知渠道注册 | 系统按渠道 ID 记忆用户通知设置 |
| `lib_neri.so` / `_neri` | JNI 库名与 `System.loadLibrary("_neri")` | 与已打包 native 库保持锁定一致 |
| `Java_com_lyra_player_*` | JNI 导出符号 | 与 Kotlin 新包名同步更新，与库名无关 |
| `CONFIG_KIND = "moe.ouom.neriplayer.config"` | 完整配置导入/导出 | 历史配置文件的 kind 字段 |
| `neriplayer-sync.json` | WebDAV 旧远端文件名 | 仅保留只读兼容；新写入使用 `Lyra-sync.json`，旧文件存在时自动读取并迁移 |
| `backup/neriplayer`（`webdav_base_path_placeholder`） | WebDAV 默认远端路径 | 远端目录结构兼容 |
| `neriplayer-radar-cache-v1:` | 平台缓存 key 前缀 | 已落盘的缓存条目 |
| `neriplayer_playback_widget` | 桌面小组件 SharedPreferences | 已存在的桌面组件状态 |

## 3. 移除的一起听范围 / Removed Listen Together Scope

- Android 客户端 `app/src/main/java/com/lyra/player/listentogether/` 整个包。
- 服务端子模块 `np-submodule/NeriPlayer-LTW`（Cloudflare Workers + Durable Objects）。
- 深链 `neriplayer://listen-together/join` 与对应的路由、邀请、会话 UI。
- 协议、邀请、房间、队列、成员控制相关的全部字符串、设置项与测试。
- CI 中一起听 Worker 的 npm 检查步骤。
- README / CONTRIBUTING（中英共 4 份）中的一起听章节。

## 4. 移除首次启动引导 / Removed First-Launch Onboarding

- 删除 `ui/onboarding/`（9 步向导）、`StartupStage.Onboarding`、`StartupStageFlow`、
  启动转场占位符与 `startup_onboarding_completed` 设置项；启动流程简化为
  `Loading -> Disclaimer -> Main`。
- 免责声明保留，其 48 条文案从原 `strings_onboarding.xml` 迁到
  `strings_disclaimer.xml`。
- 引导页原本是 `POST_NOTIFICATIONS` 的唯一运行时申请点，删除后在设置页「通用」
  新增 `NotificationPermissionSetting` 手动申请入口；本地媒体权限仍由本地扫描按需申请。

## 5. 主要符号重命名 / Key Symbol Renames

| 旧符号 / Old | 新符号 / New |
| --- | --- |
| `moe.ouom.neriplayer` | `com.lyra.player` |
| `NeriPlayerApplication` | `LyraApplication` |
| `ui.NeriApp` | `ui.LyraApp` |
| `ui.component.playback.NeriMiniPlayer` | `ui.component.playback.LyraMiniPlayer` |
| 日志 tag `NeriPlayer` | `LyraPlayer` |
| 深链 scheme `neriplayer://` | `lyra://` |
| APK 文件名 `NeriPlayer-<versionName>[-abi].apk` | `Lyra-<versionName>[-abi].apk` |

测试同步重命名，例如 `LyraAppMainTabTransitionPolicyTest`、
`LyraAppNavigationIsolationPolicyTest`、`LyraAppPlaybackTransitionPolicyTest`。

## 6. 图标替换 / Icon Replacement

- 源图：[`icon/lyra.png`](../icon/lyra.png)（用户提供，1254×1254，黑底圆角方块 + 白色
  Lyra 字标）。字标本身是横幅比例，实测范围 937×457（宽高比约 2.05）。
- **安全区适配**：源图不能按满幅直接铺到 108dp 的 adaptive 画布上——圆形或圆角遮罩会
  切掉首尾字母。因此前景与 monochrome 图层只保留字标（透明背景），并缩放到 66dp 安全
  圆内（在 432px 画布上为 237×116，约占画布宽度的 55%）；legacy mipmap 与
  `ic_lyra` / `ic_lyra_round` 采用同一比例，保证圆形遮罩下字标同样完整。
- 派生资源：mipmap 各密度 webp（方角 + 圆形两套）、`ic_lyra` / `ic_lyra_round`、
  adaptive 的 foreground 与 monochrome 图层、通知小图标与启动画面
  `ic_lyra_splash.xml`。
- 旧的 `icon/` 下 NeriPlayer SVG/XML 图标源文件，以及 Android Studio 模板残留的
  `ic_launcher_foreground.xml` / `ic_launcher_background.xml`（绿色机器人矢量）已全部
  删除；下载页原先把它当封面兜底图，现改用 `ic_lyra_round`。

## 7. 提交清单 / Commit List

| 提交 / Commit | 内容 / Content |
| --- | --- |
| `d09bb92` | 移除一起听（Listen Together）全链路 |
| `caa1288` | 包名与符号重命名 `moe.ouom.neriplayer` -> `com.lyra.player` |
| `976514b` | 修正重命名后 4 个测试文件的期望值 |
| `a24e143` | 品牌文案 BlueBlue -> Lyra |
| `ceea1bd` | 替换应用图标为 Lyra |

文档重写（README、CONTRIBUTING 中英 4 份）与本说明文件在后续提交中完成。
The documentation rewrite and this note land in a follow-up commit.

## 8. 授权与出处 / License and Attribution

- 本仓库沿用 GPLv3；`app/src/main/cpp/` 下原 NeriPlayer 自有 Native 源码的替代授权
  范围保持不变，见 `app/src/main/cpp/README.md`。
- Lyra 由 NeriPlayer 改名而来，原项目：<https://github.com/cwuom/NeriPlayer>。
