# Mode A-ZF 2.2.1 发布加固 — 保留数据修复后的检查点

2026-08-27。用户授权修复 15.9.0 Reply 缓存问题后继续，优先保留登录态。该缺陷已修复并完成 15.9.0 / 16.6.1 双向检查；随后在 16.5.1 降级回归确认默认 Reply 目标不可达，按项目 AGENTS.md 暂停后续操作，等待用户决定。

```text
RELEASE_READY = NO
blockers=[history165ReplyUnavailableNeedsDecision, historyRegressionIncomplete, final1661SmokeIncomplete]
```

这不是已发布版本，也不是完整兼容性验收。没有执行远程写入。

## 1. Static blockers closure table

| 项目 | 改动与证据 | 当前结论 |
| --- | --- | --- |
| R1 Splash coverage | SplashLifecycleGuard 严格匹配 resolved/精确 legacy 类，复用 post-onCreate finish；注册失败不退休 Instrumentation | 代码/测试通过；三版首轮均 guard=true、零 framework；FullScreen 未自然触发 |
| R2 Reply health | FeatureRuntimeHealth 与 core READY 分离；UI 展示本次状态，持久开关不变 | 15.9.0 INSTALLED；16.5.1 / 16.6.1 UNAVAILABLE 明确报告 |
| R3 bounded retry | 最多 4 次 timed、3 次 resume（至少间隔 30s）、总时限 120s，先耗尽的预算停止 | 两个 16.x 首轮均实测 timedAttempts=4、unregister=true、observerActive=false |
| R4 version | 2.2.1 / versionCode 11，构建、签名与安装字节核对 | 通过 |
| R5 history / docs | 实际三版 APK 清单；本报告按检查点范围记录 | 未完成：16.5.1 第二轮/设置页、最终 16.6.1 三轮 smoke 待续 |
| 新发现：Reply cache | verifier 与安装器共用窄范围 class-only Reply 契约 | 15.9.0 修复：8 条持久记录、缓存命中直接安装 3 个 bind Hook |

源码提交：198a2f2（R1–R4）、7c06f95（Reply cache 修复）。之前 A-ZF 检查点 c741d05 / 155a74a 保留。

## 2. Splash before / after

此前 READY 退休 Instrumentation 后，未解析的历史 Splash 类会显示 uncovered。现在通过非 Xposed ActivityLifecycleCallbacks 保留严格匹配覆盖，不采用长期 loose matching，不引入 PRE_BLOCK。

| 类名 | 本轮三个版本的覆盖策略 |
| --- | --- |
| com.coolapk.market.view.splash.SplashAdActivity | 已解析、验证和安装 specific onCreate Hook |
| com.coolapk.market.view.splash.SplashActivity | lifecycle 精确名兜底 |
| com.coolapk.market.view.splash.FullScreenAdActivity | lifecycle 精确名兜底 |
| com.coolapk.market.view.ad.SplashAdActivity | lifecycle 精确名兜底 |
| com.coolapk.market.view.ad.FullScreenAdActivity | lifecycle 精确名兜底 |

三份 APK manifest 均只列出了上述 view.splash.SplashAdActivity；没有声明其他名字不等于 DEX 中不存在，故不冒报 absent。日志相同 simple-name 的别名对应上表不同包名。

正常 READY 后均 frameworkActive=false、frameworkActiveHooks=[]。FullScreenAdActivity 为 **not dynamically triggered**，仅完成策略单测和运行时 guard/coverage 日志核对；没有人为制造广告。

## 3. 15.9.0 缓存缺陷的无数据清除修复

旧行为：已安装三个 Reply bind Hook，但持久化前校验拒绝 feature.replyHolder，日志为 semantic persist rejected ... empty method descriptor，只保存 7 个 core targets。

根因：onSemanticClassDiscovered() 创建 class-only 记录，但 verifier 的空 method 分支只接受 Splash/RelatedData，遗漏 Reply。不是缓存被覆盖，也没有证据表明是宿主数据库降级故障。

新行为：精确匹配 com.coolapk.market.viewholder.MultiFeedReplyViewHolder，要求至少一个非 static、非 abstract、返回 void、单参数 Entity/FeedReply 的声明方法。验证器与安装器使用同一契约，不放宽其它空 descriptor，不猜新版类名。

- 受影响已确认版本：15.9.0。
- 新候选第一轮加载旧 7 条缓存，经有界发现修复为 8 条；后续冷启动 cacheHit entries=8 dexkitScan=false，Reply 直接 INSTALLED。
- 另做模块目标缓存 miss/rebuild，再次保存 Reply，后续 stable hit 通过。
- 16.6.1 紧接着回装并回归：核心 READY/零 framework 正常，旧 Reply 类仍不可达，没有被 verifier 错误接受。
- 仅为 miss 测试将模块自己的 coolapk_purifier_cache_v4.json 备份到 /data/local/tmp/processing/cache_before_159_fixed_miss.json，由模块正常重建；账号、数据库和配置文件未改。

## 4. 实际 APK 清单与当前矩阵

完整路径、包名、versionCode、SHA-256 见 [history_apk_regression_manifest.md](history_apk_regression_manifest.md)。目录只有 15.9.0、16.5.1、16.6.1；没有 13.1.1、16.1.2 APK。下列修复后测试均使用同一 7c06f95 来源的 2.2.1 APK。表中 hash 前缀对应 manifest 全值。

| Coolapk | APK hash 前缀 | READY | Zero FW after READY | Splash | Feed | Reply | Settings | Result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 15.9.0 | 5fc2046309b3 | YES：repair hit、8-target hit、miss、stable hit | 全部 YES | specific + lifecycle | 2 Hook，浏览/评论正常，实际 removed 3，无 crash | INSTALLED；3 bind，持久缓存及重启安装通过 | 入口、8 switches、状态文字、配置 hash 通过 | PASS（默认功能 smoke 范围） |
| 16.5.1 | 8d8ecbdad946 | 首轮 miss YES；第二轮未执行 | 首轮 YES | specific + lifecycle | 2 Hook；正常评论、removed 3，无 crash | UNAVAILABLE；预算停止 | 未完成 | BLOCKED（降级后默认 Reply 不可用，按 AGENTS 暂停） |
| 16.6.1 | 946b98b1dd30 | 修复后双向回归 miss YES | YES | specific + lifecycle | 2 Hook；正常评论、removed 3，无 crash | UNAVAILABLE；预算停止、UI 明示 | 入口、8 switches、状态文字、登录态、配置 hash 通过 | BLOCKED（最终回装及 hit×2/miss×1 未完成） |

15.9.0 Reply 验证覆盖安装、缓存重启和正常评论浏览；未捕获专用 holder 的 sponsor 阳性移除日志，不以页面没有广告冒充该路径阳性证据。三版均有 Feed 业务过滤执行记录。Feed after-filter 源码未改，cursor/page/response 语义依赖已有测试，不声称做过新的网络逐字段对比。

Issue #2 五项可选配置均为 false；未擅自打开，不计入实机功能验收。未重新引入 LayoutInflater/View.setTag fallback。设置页面只检查展示与读取，没有切换用户开关。

## 5. Runtime health samples

15.9.0，21:43:11，8 条缓存命中：

```text
runtime health state=READY coreMissingRequired=[] frameworkActive=false frameworkActiveHooks=[] splashLifecycleGuard=true selectedFeatureStatus={splash:INSTALLED, feedSponsor:INSTALLED, replySponsor:INSTALLED} selectedFeatureProblems=[]
```

16.6.1，21:47:31，修复后双向回归最终状态：

```text
reply retry lifecycle observer stopped unregister=true
reply retry stopped timedAttempts=4 observerActive=false
runtime health state=READY coreMissingRequired=[] frameworkActive=false frameworkActiveHooks=[] splashLifecycleGuard=true selectedFeatureStatus={splash:INSTALLED, feedSponsor:INSTALLED, replySponsor:UNAVAILABLE} selectedFeatureProblems=[replySponsor:retryBudgetExhausted]
```

16.5.1，21:49:42，首轮约 90s 后：

```text
reply direct attempt loadable=unloaded:ClassNotFoundException classLoader=n/a installed=false
reply retry lifecycle observer stopped unregister=true
reply retry stopped timedAttempts=4 observerActive=false
runtime health state=READY coreMissingRequired=[] frameworkActive=false frameworkActiveHooks=[] splashLifecycleGuard=true selectedFeatureStatus={splash:INSTALLED, feedSponsor:INSTALLED, replySponsor:UNAVAILABLE} selectedFeatureProblems=[replySponsor:retryBudgetExhausted]
```

coreMissingRequired=[] 仅表示 core/bootstrap 就绪，不表示所有已选功能生效。两个 16.x 的默认 Reply 专用路径不可用，必须保留显式限制。

## 6. 当前暂停原因与下一步

16.5.1 安装返回 Success，已安装 base.apk hash 与源 APK 完全一致；主进程存活且 crash buffer 为空。Reply 在 Class.forName 阶段即 CNFE，未进入 verifier/installer/persist，没有出现已修复的 empty method descriptor 拒绝。这是已确认的业务目标可达性问题，尚不能断言是哪个 DEX/loader 或替换 holder 导致。

此前对 16.6.1 的已有工件审查：main_useDDI.dex 含旧类名字符串但无可靠匹配类定义/方法目标；另一原始内存工件头表损坏，解析失败不是类不存在的证据。没有把此结论跨版本冒充 16.5.1 静态分析结果。

**没有理由通过清除数据解决 CNFE，也没有执行数据清除。** 用户授权继续后，优先补充可靠业务目标证据；无法可靠定位时，可按任务文档明确 UNAVAILABLE 和兼容限制，再完成 16.5.1 第二轮/设置页、最终 16.6.1 三轮 smoke。未知目标不猜名，不恢复常驻 loadClass Hook，不研究安全 SDK。

当前手机停在 **Coolapk 16.5.1 / Purifier 2.2.1**，按 AGENTS 暂停，未自行回装 16.6.1。15.9.0 与中途 16.6.1 的“我的”页均确认原登录态仍在；16.5.1 未完成账号页 UI 复核，不声称重新验证其登录态。

## 7. Tests / build / staging

执行 gradlew test assembleRelease、gradlew stageReleaseCandidate 成功。Debug / Compatible / Release 各 **228 tests、0 failures、0 errors、0 skipped**：原 201 + 11 Splash lifecycle + 6 runtime health + 4 retry budget + 6 Reply verifier/cache 回归。

新增缓存回归：class-only Reply 接受而其它 key 不放宽、同形状无关类拒绝、Entity/受保护 FeedReply bind 接受、static/abstract/非 void/宽参数/错误参数个数拒绝，以及写盘→重读→验证往返。

- 工件：dist/coolapk-purifier-v2.2.1.apk
- versionName=2.2.1、versionCode=11
- 大小：**1,136,101 bytes**
- APK SHA-256：3541626808F86FA74D022FE8AEB4F19C92A5B872E3B2EBDB672655AB32E01760
- signer certificate SHA-256：12B482270B217A377CF3881382C86EE9F1C3E2B8E4BE25EE5118F327F2858875
- assembled == staged == installed module base.apk SHA-256；apksigner v2 验证通过。
- 原 D3E86BCD... 候选仅为修复前检查点，已被替代，不混入同候选回归矩阵。
- 配置 SHA-256 始终为 8955EAD0BA2586DF1C729D0BC679E84812493B901BB3FF18EF0E7E72F09D35C6，三个 true、五个 false。

## 8. 证据与边界

本地 .tmp_audit/azf_hardening/：

- reply_cache_build_tests.log、reply_cache_stage.log、fixed_candidate.json、fixed_candidate_signer.txt。
- history_159_fixed_{first,hit,miss,stable}_trace.log / *_result.json、history_159_fixed_cache.json、history_159_comments*、history_159_account*、history_159_config*。
- current_fix_crosscheck_install.json / *_trace.log / *_result.json、current_cross_comments*、current_cross_account*、current_cross_config*。
- history_165_install.json、history_165_first_trace.log / *_result.json、history_165_comments*、history_165_health_final.log、history_165_cache.json、history_165_final_crash.log。

手机新增 debug/安装/缓存备份工件均在 /data/local/tmp/processing/。未卸载酷安、未清数据、未改登录/安全数据、未重启手机/LSP、未改模块开关/作用域、未写 LSPosed 配置数据库。Frida/JADX/HTTP Toolkit/MT/IDA 本轮均确认可连接；没有附加 Frida 或切换 IDA 文件。

最终发布判断保持 NO，不以是否出现服务端风控为依据。完整发布说明和最终矩阵需在剩余验收完成后收尾。
