# 酷安净化

基于 libxposed Modern API 102、面向酷安版本变化进行运行时适配的去广告模块。

## 功能

- 在酷安原生“设置”列表首部注入“酷安净化”入口；所有选项直接持久化到酷安 `files/coolapk_purifier_config.json`。
- 默认去除启动/开屏广告和全屏广告。
- 默认去除首页信息流广告与赞助卡片。
- 默认去除帖子回复区及评论中的赞助内容。
- 可选去除自动评论提示、话题与机型推荐、帖子相关推荐、同话题动态和帖子内推广；酷安 15.x 以下自动禁用这些新选项。
- 详情页推荐采用上游数据链过滤：话题/产品/机型卡从 `Feed.getTargetRow()` 的专用组装入口截断，帖子内推广在 `Feed.getDetailSponsorCard()` 进入 header item 列表前置空；主解析不依赖广告文案或其他可见中文文本。
- 同话题动态按服务端 `entityTemplate=feedRecommendListCard` 在 Feed 数据列表中精确过滤；宿主唯一模板判定方法是数据过滤与布局回退共享的安全硬 gate，证据未验证时保留内容并进入安全降级，不使用标题或其他用户可控文本判定。
- 配置变更后于下次启动仅解析尚未缓存的已选目标，并分别提示首次适配与适配完成状态。
- 增量覆盖酷安 15/16 的 `FullScreenAdUtils.shouldShowAd` 决策链，保留旧版 Activity 级开屏兜底。开屏净化已在酷安 16.5.1 (2607271) 与 16.6.1 各完成 20 次冷启动回归：未复现开屏广告、闪现或崩溃；16.5.1 上决策目标经语义指纹解析到与 16.6.1 不同的混淆名（`rc5.Ԯ` / `kc5.Ԯ`）仍稳定命中，拦截均由决策层前置完成，Activity 级兜底在正式回归中未被动用。
- 核心易混淆业务目标优先由 DexKit 运行时指纹跨版本解析；对已实机验证的设置页、ViewHolder、资源与布局使用语义定位或受控 fallback，并仅按启动时启用项安装相应的高成本兜底 Hook。
- SplashCritical 优先解析：启动时先解析并安装开屏 Hook，再后台完成 Feed/Entity getter 解析。
- 解析顺序：有效缓存 → 强 DexKit 指纹 → 弱 DexKit 指纹 → 历史类名/反射兜底；build cache 只保存 descriptor 元数据，live target 只在当前 ClassLoader generation 内验证、累积和安装。
- 多目标覆盖：开屏类 Activity（品牌开屏与广告开屏）全部解析并安装 Hook；Feed 层同时 Hook EntityAdHelper 与 EntityListFragment 中全部 `(List, boolean) -> List` 业务入口（2.0.1 覆盖广度）。
- 两层就绪判定：核心过滤可用（开屏 + 至少一个 Feed Hook + getter 完整）与 Feed 覆盖收敛（两个历史锚点类本轮发现的全部 feed 方法均已 Hook，或 20s deadline 兜底收敛）同时满足才进入 READY；覆盖未收敛期间临时保留单发 ClassLoader 观察器，并由运行时事件与一次性 8s watchdog 提供有界重扫，在 20s deadline 前完成确定性收敛，形成确定性重试路径。
- 会话触发合并：解析会话运行期间到来的 runtime-dex / watchdog 触发不会丢失，合并为恰好一轮后续会话；READY/DEGRADED 为真正冻结终态，迟到的后台会话无法翻转。终态先逻辑停用全局 loadClass discovery Hook，再尝试 framework unhook；正常路径完全解除，解除失败时残留 Hook 为 inert。
- Resolver 事务隔离：每个 session 在启动时固定捕获 generation + ClassLoader，并独占其 DexKitBridge；loader 中途切换只将旧 session 标记为 superseded，不跨线程关闭 bridge。旧结果不得 apply、写 cache、进入 READY/DEGRADED，bridge 只由所属 worker 退出时关闭。
- Terminal 事务隔离：deadline、cache hit、full scan 与 error 的 READY/DEGRADED 都在同一个 runtimeEpoch 临界区读取当前 generation readiness、missingRequired 和 loader，并原子提交带 generation/loader 的 terminal snapshot；cleanup、日志、Toast 与 watcher retire 在锁外执行，不存在 read G1 / commit G2。
- DexKit Context 隔离：resolver session 直接携带 `Application.attach` 已取得的 appContext，native loader/bridge 主路径不再反射 `ActivityThread.currentApplication()`；Context 尚不可用时安全保持可重试。
- 解析失败可重试：失败会话会重装 ClassLoader 观察器，下一 session 使用新建的 DexKitBridge 重扫，覆盖加固应用在同一 ClassLoader 上分阶段追加 DEX 的时序。
- 多版本持久缓存（schema 2）：stableTargetIdentity 包含包名、目标 `versionCode`、base APK 文件名与大小、稳定排序后的 split APK 文件名与大小，以及通过 `GET_SIGNING_CERTIFICATES` 读取的当前 signer 证书 DER 摘要；证书不可用会明确记录为 unavailable，不冒充真实摘要；最多保存 5 个历史版本，完整缓存文件不超过 1 MiB，LRU 淘汰，原子替换写入；多目标条目按方法/类 descriptor 稳定编号，跨会话合并不丢目标。
- 同版本覆盖重装后 identity 不变，直接命中历史缓存；升级/降级到新版本后自动失效重扫。
- Bootstrap 终态低开销：READY/DEGRADED 后先逻辑停用 discovery Hook，再关闭 Resolver worker、RuntimeDexObserver、watchdog，并尝试卸载临时 Feature ClassLoader Hook；仍在运行的 session 由所属 worker 安全关闭 bridge。cleanup 区分本次/累计 unhook 成功与失败、framework active、logical enabled；部分 unhook 后按 HookSite 只补缺失 overload。正常路径下 Hook 被解除；解除失败时保留句柄但 interceptor 已 inert，不再扫描、安装或改变 readiness。Instrumentation 开屏兜底转为被动模式常驻（仅保留开屏判定，2.0.1 行为）。
- post-READY loader swap 采用低开销边界：generation-aware adaptation 只覆盖 bootstrap/adaptation window；进入 READY/DEGRADED 后不保留常驻 loader monitor。若宿主在终态后替换核心业务 loader，需要正常重启酷安进程以开启新 adaptation window。
- 无缓存首次适配时按“默认三项”或“用户新增选项”分别显示一次系统 Toast；适配完成后再提示一次，缓存命中时不重复提示。
- 不包含联网、更新器、后台服务或周期性轮询。

“安全降级”表示：当某个已开启功能的必要语义目标无法唯一解析、无法通过签名校验或主 Hook 安装失败时，模块拒绝 Hook 不确定目标并在日志中以 `:descriptor`、`:primaryHook` 或 `:semanticEvidence` 区分缺失原因；primary、fallback 与 evidence 安装状态互不冒充。T4 的 evidence 缺失时所有删除路径均关闭；其他具备独立窄范围回退的功能可继续使用回退，但回退不满足 primary READY。它不等同于承诺未知版本的每项净化能力一定生效。

## 兼容性

| 项目 | 要求 |
| --- | --- |
| 目标应用 | 酷安（包名 `com.coolapk.market`），运行时动态适配，不按版本分支 |
| 已实机验证 | 13.1.1 / 15.9.0 / 16.1.2 / 16.5.1 / 16.6.1 |
| Android | 9（API 28）及以上 |
| 框架 | 支持 libxposed Modern API 102 的 LSPosed |
| 模块版本 | 2.2.0（versionCode 10） |

模块不按酷安小版本硬编码业务分支。版本适配由 DexKit 动态解析与稳定语义锚点、资源名及受控 framework fallback 组合完成；其中 fallback 只在对应功能启用时安装，并不被视为绝对稳定接口。已实机验证版本代表测试覆盖，不构成对未来或其他版本的绝对兼容保证。若新版本功能失效，请先查看目标应用内：

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
