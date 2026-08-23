# 酷安净化

基于 libxposed Modern API 102 的全版本酷安去广告模块。

## 功能

- 去除启动/开屏广告和全屏广告。
- 去除首页信息流广告与赞助卡片。
- 去除帖子回复区及评论中的赞助内容。
- 在酷安原生“设置”页首部注入“酷安净化”入口，所有选项直接持久化到酷安 `files/coolapk_purifier_config.json`。
- 可选去除自动评论提示、话题与机型推荐、帖子相关推荐、同话题动态和帖子内推广；酷安 15.x 以下自动禁用这些新选项。
- 配置变更后于下次启动仅解析尚未缓存的已选目标，并分别提示首次适配与适配完成状态。
- 增量覆盖酷安 15/16 的 `FullScreenAdUtils.shouldShowAd` 决策链，保留旧版 Activity 级开屏兜底。
- 通过 DexKit 运行时指纹跨版本解析酷安 Hook 目标，不依赖具体混淆类名/方法名。
- SplashCritical 优先解析：启动时先解析并安装开屏 Hook，再后台完成 Feed/Entity getter 解析。
- 解析顺序：有效缓存 → 强 DexKit 指纹 → 弱 DexKit 指纹 → 历史类名/反射兜底；解析结果跨会话累积合并。
- 多目标覆盖：开屏类 Activity（品牌开屏与广告开屏）全部解析并安装 Hook；Feed 层同时 Hook EntityAdHelper 与 EntityListFragment 中全部 `(List, boolean) -> List` 业务入口（2.0.1 覆盖广度）。
- 两层就绪判定：核心过滤可用（开屏 + 至少一个 Feed Hook + getter 完整）与 Feed 覆盖收敛（两个历史锚点类本轮发现的全部 feed 方法均已 Hook，或 20s deadline 兜底收敛）同时满足才进入 READY；覆盖未收敛期间临时保留单发 ClassLoader 观察器，并由运行时事件与一次性 8s watchdog 提供有界重扫，在 20s deadline 前完成确定性收敛，形成确定性重试路径。
- 会话触发合并：解析会话运行期间到来的 runtime-dex / watchdog 触发不会丢失，合并为恰好一轮后续会话；READY/DEGRADED 为真正冻结终态，迟到的后台会话无法翻转，终态后不再残留全局 loadClass 钩子。
- 解析失败可重试：失败会话会重装 ClassLoader 观察器并强制 DexKit 重建重扫，覆盖加固应用在同一 ClassLoader 上分阶段追加 DEX 的时序。
- 多版本持久缓存（schema 2）：按 stableTargetIdentity 保存，最多 5 个历史版本，完整缓存文件不超过 1 MiB，LRU 淘汰，原子替换写入；多目标条目按方法/类 descriptor 稳定编号，跨会话合并不丢目标。
- 同版本覆盖重装后 identity 不变，直接命中历史缓存；升级/降级到新版本后自动失效重扫。
- Bootstrap 终态低开销：READY 后关闭 DexKit Bridge、Resolver worker、RuntimeDexObserver、watchdog，Bootstrap 日志冻结；Instrumentation 开屏兜底转为被动模式常驻（仅保留开屏判定，2.0.1 行为），保证 READY/DEGRADED 后全屏广告 Activity 仍被拦截。
- 无缓存首次适配时按“默认三项”或“用户新增选项”分别显示一次系统 Toast；适配完成后再提示一次，缓存命中时不重复提示。
- 不包含联网、更新器、后台服务或周期性轮询。

## 兼容性

| 项目 | 要求 |
| --- | --- |
| 目标应用 | 酷安（包名 `com.coolapk.market`），运行时动态适配，不按版本分支 |
| 已实机验证 | 13.1.1 / 15.9.0 / 16.1.2 / 16.5.1 / 16.6.1 |
| Android | 6.0（API 23）及以上 |
| 框架 | 支持 libxposed Modern API 102 的 LSPosed |
| 模块版本 | 2.2.0（versionCode 10） |

模块不针对任何酷安版本做分支，Hook 目标全部由 DexKit 在运行时解析。若新版本功能失效，请先查看目标应用内：

- `files/coolapk_purifier_bootstrap.log`：启动时序与 Bootstrap 终态日志。
- `files/coolapk_purifier_cache_v4.json`：多版本解析缓存（schema 2；旧版 v3 缓存不迁移，升级后首次启动自动重解析）。
- LSPosed 模块日志：Resolver 候选数与 Hook 安装情况。

并提交包含酷安版本号、Android 版本和相关日志的问题报告。

## 安装

1. 从 GitHub Releases 下载并安装 APK。
2. 在 LSPosed 中启用模块，作用域只选择“酷安”。
3. 强制停止酷安后重新打开。

## 构建

需要 JDK 17、Android SDK 35，并可从 Maven Central 获取 `io.github.libxposed:api:102.0.0`。

运行单元测试并生成 debug 测试包：

```powershell
.\gradlew.bat test assembleDebug
```

测试 APK 输出到 `app/build/outputs/apk/debug/`。面向普通用户的已签名版本请从 GitHub Releases 下载。

## 许可证

本项目采用 [MIT License](LICENSE)。

## 免责声明

本项目仅供学习、研究和个人设备使用，与酷安及 LSPosed 项目无隶属或认可关系。使用前请确认符合当地法律及相关服务条款。
