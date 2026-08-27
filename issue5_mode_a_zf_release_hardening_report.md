# Mode A-ZF 2.2.1 发布加固 — 最终 RC 验收报告

2026-08-27，OnePlus PLQ110 / Android 16（API 36）。按 `issue5_mode_a_zf_release_hardening_with_history_regression.md` 和项目 AGENTS.md 执行；保留账号数据与用户配置。

```text
RELEASE_READY = YES
```

此结论指本任务定义的工程 RC，可交用户人工验收。**16.5.1 / 16.6.1 默认开启的 Reply 专用赞助过滤仍为 UNAVAILABLE，当前不生效**；按任务允许的非核心功能限制明示，不表示所有默认功能均有效。未进行任何远程写入；仍须用户人工验收和单独的 push / tag / release 授权。

## 1. Static blockers closure table

| 项目 | 闭合方式 | 验证结论 |
| --- | --- | --- |
| R1 Splash / Zero FW | 非 Xposed SplashLifecycleGuard 精确匹配 resolved/legacy，READY 后保留；guard 注册失败不强退 Instrumentation | 三版全部 READY/zero FW，specific + lifecycle，无 uncovered；FullScreen 未自然触发 |
| R2 Reply health | FeatureRuntimeHealth 独立于 core；日志与 UI 显示状态，不改持久开关 | 15.9.0 INSTALLED；两个16.x UNAVAILABLE，没有被 READY 隐藏 |
| R3 bounded retry | timed≤4、resume≤3、间隔≥30s、READY 后总时限120s；先耗尽即注销并取消任务 | 正式16.x各轮最终 unregister=true、observerActive=false；核心保持 READY |
| R4 version | 2.2.1 / versionCode11 | Manifest、候选文件名、签名和安装字节核对通过 |
| R5 history / docs | 实际3个APK，用同一冻结候选重做最终矩阵 | 历史各miss/hit，最终16.6.1 hit×2/miss×1；README/发布说明同步限制 |
| 回归发现：Reply cache | class-only Reply 校验与安装器共享精确类/方法契约 | 15.9.0保存8目标，缓存启动直接安装3个bind Hook |
| 回归发现：设置重进 | SimpleActivity resume 后有界等待视图；pause/destroy撤销 | 新候选各正式冷启动均3次设置往返，入口唯一、8开关值与配置哈希不变 |

源码检查点：`198a2f2`（R1–R4）、`7c06f95`（Reply cache）、`f6df56f`（Settings时序）。前阶段 `c741d05` / `155a74a` 保留。本次矩阵只采用 **f6df56f之后构建的706C622F…候选**，旧35416268…等候选不混入最终通过记录。

## 2. Splash coverage before / after

旧行为在specific Hook成立后退休Instrumentation，未解析legacy可能成为uncovered。新行为在正常READY、核心Splash能力与guard注册成功同时成立后才退休；非Xposed生命周期回调继续按精确类名处理，复用post-onCreate finish语义，不引入PRE_BLOCK或长期宽泛名称匹配。

| 类名 | 三版覆盖策略 |
| --- | --- |
| com.coolapk.market.view.splash.SplashAdActivity | resolved + verified specific onCreate Hook |
| com.coolapk.market.view.splash.SplashActivity | lifecycle 精确legacy兜底 |
| com.coolapk.market.view.splash.FullScreenAdActivity | lifecycle 精确legacy兜底 |
| com.coolapk.market.view.ad.SplashAdActivity | lifecycle 精确legacy兜底 |
| com.coolapk.market.view.ad.FullScreenAdActivity | lifecycle 精确legacy兜底 |

三份APK Manifest仅声明上表第一个名字；未声明不等于DEX类不存在，因此不冒报absent。日志相同simple-name可能代表不同包名。FullScreenAdActivity：**not dynamically triggered**，只验证策略单测、guard注册和覆盖日志，没有人为制造广告。

guard注册失败保留Instrumentation并如实报告非零framework；DEGRADED仍保留必要兜底，不能把此类启动计入zero FW。

## 3. Reply health、预算与缓存修复

Reply不加入core terminal readiness。DISABLED / DEFERRED / INSTALLED / UNAVAILABLE分别表达关闭、等待、目标已安装、当前不可用；`coreMissingRequired=[]`不是全部选中功能生效。UI在等待时显示“本次启动正在查找适配目标，尚未生效”，预算终止后动态显示“本次启动未找到适配目标，当前未生效”，不把用户开关改为false。

定时lane按2/8/20/60秒顺序延迟，最多4次，正常约90秒结束；resume最多3次且间隔至少30秒，总预算120秒。安装成功或任一预算耗尽后注销observer、取消队列任务并结束专用线程；不恢复framework Hook。15.9.0在bootstrap/cache阶段已安装，不需要启动post-READY重试observer；不能将其“没有停止日志”误判为漏清理。

15.9.0旧缺陷：已安装3个Reply bind Hook，但class-only `feature.replyHolder`被verifier以empty method descriptor拒绝，仅保存7个core目标。修复精确接受 `com.coolapk.market.viewholder.MultiFeedReplyViewHolder`，且要求至少一个非static、非abstract、返回void、单参数Entity/FeedReply的声明方法。验证器和安装器共用契约，其它不支持的空descriptor仍拒绝。

该问题不是已证实的宿主数据库降级故障。此前按AGENTS暂停后获得继续授权，以代码修复和模块目标缓存重建解决，未清账号数据。缓存修复后已做15.9.0→16.6.1双向检查；最终Settings修复候选又完成16.6.1预检→15.9.0→16.5.1→16.6.1的完整复核。两个16.x旧Reply类仍在Class.forName阶段CNFE，未被放宽校验错误接受。

## 4. 设置重新进入问题

旧候选扩大检查发现：硬件返回退出配置页，再从“我的”进入原生设置，净化入口偶发消失；进入原生设置子页再返回又出现。日志显示旧页面已释放，根因是onResume的单次注入早于标题/Compose视图就绪。

`PageInjectionRetry`每次resume最多5次，调度延迟0/100/250/500/1000ms，累计1.85秒（不保证主线程繁忙时的实际墙钟时间）。成功、pause、destroy、耗尽后取消并清引用；已出队的过期任务通过页面身份检查失效。下一次resume可重新开启有限窗口，不增加framework Hook。仅对既有SimpleActivity注入路径使用，不改目标解析。

新增6项测试覆盖延迟成功、耗尽停止、取消/过期回调、重复resume替换、下次resume恢复和页面隔离。最终候选在3版每个正式冷启动均做3次“我的→设置→净化→返回”循环，入口数量始终1；16.x还验证等待中的状态文字在原页面更新为UNAVAILABLE。没有切换用户开关，设置展示与持久化读取通过。

## 5. history-apks清单与实机矩阵

全路径、packageName、versionCode和完整SHA-256见 [历史APK清单](history_apk_regression_manifest.md)。实际目录只有15.9.0、16.5.1、16.6.1；13.1.1/16.1.2仅历史曾验证，本轮没有工件，未重新验证。每次均push到`/data/local/tmp/processing/`后root `pm install -r -t --user 0`，降级加`-d`；安装后核对version及base.apk字节哈希。

| Coolapk | APK hash前缀 | READY | Zero FW after READY | Splash | Feed | Reply | Settings | Result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 15.9.0 | 5fc2046309b3 | miss + hit YES | 全部YES | specific + lifecycle | 2 Hook、实际过滤、正常浏览/评论 | INSTALLED，3 bind，8目标缓存通过 | 两轮各3次往返、配置不变 | PASS |
| 16.5.1 | 8d8ecbdad946 | miss + hit YES | 全部YES | specific + lifecycle | 2 Hook、实际过滤、正常浏览/评论 | UNAVAILABLE，两轮observer停止 | 两轮各3次往返、动态状态、配置不变 | PASS_WITH_KNOWN_LIMITATION |
| 16.6.1 | 946b98b1dd30 | 最终hit×2 + miss×1 YES | 全部YES | specific + lifecycle | 2 Hook、实际过滤、正常浏览/评论 | UNAVAILABLE，三轮observer停止 | 三轮各3次往返、动态状态、配置不变 | PASS_WITH_KNOWN_LIMITATION |

Reply为默认开启但非core功能，按任务§9.6的非核心限制例外分类，并在README和发布说明突出披露。不能据此宣传16.x三项默认净化均有效；Splash/Feed核心失败没有被豁免。

### 同一最终候选的冷启动记录

READY时间相对本次attach，不是端到端性能基准。完整trace字节SHA与机器记录核对一致。

| Coolapk | 记录 | 路径 | READY ms | Reply终态 | trace SHA前缀 |
| --- | --- | --- | ---: | --- | --- |
| 15.9.0 | rc2_159_miss | miss/rebuild | 1336 | INSTALLED | a53498076944 |
| 15.9.0 | rc2_159_hit | hit | 242 | INSTALLED | 2996fd7353c9 |
| 16.5.1 | rc2_165_miss | miss/rebuild | 1072 | UNAVAILABLE | aa8a372c8803 |
| 16.5.1 | rc2_165_hit | hit | 176 | UNAVAILABLE | 9013ae86816b |
| 16.6.1 | rc2_1661_hit1 | hit | 181 | UNAVAILABLE | 326b7f644578 |
| 16.6.1 | rc2_1661_hit2 | hit | 202 | UNAVAILABLE | 6e2ed5c834c0 |
| 16.6.1 | rc2_1661_miss | miss/rebuild | 743 | UNAVAILABLE | 350aebab2822 |

三版均捕获Feed业务Hook实际 `removed ... filtered item(s)`。15.9.0记录removed3和1，16.5.1记录removed1；具体本地证据见下节。正常Feed刷新/滚动、评论打开无本次PID crash buffer记录。Feed after-filter代码未改变，cursor/page/response语义由现有测试保护，不声称完成新的网络逐字段对比。

15.9.0 Reply验证目标安装、缓存启动和正常评论；没有自然捕获专用Reply holder sponsor阳性移除，不以“屏幕没有广告”冒充该路径阳性证据。五项Issue#2可选配置均false，保持不变，未计入开启后的实机功能验收；未恢复LayoutInflater/View.setTag fallback。16.x评论上方帖子内推广属于未开启的可选路径，不当作已过滤。

## 6. 机器可读终态样例

15.9.0，rc2_159_hit：

```text
runtime health state=READY coreMissingRequired=[] frameworkActive=false frameworkActiveHooks=[] splashLifecycleGuard=true selectedFeatureStatus={splash:INSTALLED, feedSponsor:INSTALLED, replySponsor:INSTALLED} selectedFeatureProblems=[]
```

16.5.1 hit与16.6.1 hit2，定时预算结束样例：

```text
reply retry lifecycle observer stopped unregister=true
reply retry stopped timedAttempts=4 observerActive=false
runtime health state=READY coreMissingRequired=[] frameworkActive=false frameworkActiveHooks=[] splashLifecycleGuard=true selectedFeatureStatus={splash:INSTALLED, feedSponsor:INSTALLED, replySponsor:UNAVAILABLE} selectedFeatureProblems=[replySponsor:retryBudgetExhausted]
```

16.6.1 hit1因页面往返先耗尽resume预算，约65秒终止，日志为timedAttempts=3，而不是4；这是先耗尽预算即停止的预期行为，不把每轮伪写成相同次数。每轮等待最终health后才计为通过。Settings/Splash生命周期注册日志核对未重复；重试停止后继续页面往返不会重新启动Reply discovery。幂等、取消和失败分支另有单元测试覆盖。

最后miss轮部分启动模块日志未捕获，包括fullReady与Reply初始注册行；持久trace中的HookLedger明确保留两个不同feed-filter业务句柄、specific Splash以及零framework，最终health/observer停止和UI证据完整。该轮不凭缺失行宣称Reply精确注册次数，也不伪补fullReady文本。其余轮次完整coverage日志与本轮guard=true、同一策略代码共同支持覆盖结论。

## 7. Reply取证边界和数据保留

已有16.6.1运行时工件包含旧类名字符串，但未找到可靠类定义/替代方法；损坏原始dump的解析失败不能证明类不存在。MT原始16.5.1工作区只有1DEX/30壳层类，Reply名称搜索无结果；JADX壳层工作区也未补足证据。无法确定新版是更名、holder替换还是loader可达性问题，因此保持UNAVAILABLE，不猜类名、不恢复常驻loadClass Hook。

此前旧候选取证尝试中，短时Frida attach后PID17254退出；MCP记录process-terminated/crash=null，探针尚未运行。不能把时间先后归因到某安全组件，也没有多loader枚举结论。该过程不算正式smoke，本报告的最终候选七轮均未附加Frida。五个MCP已确认可用，无IDA换文件；未继续安全SDK逆向或注入改造。

最终手机停留 **Coolapk16.6.1 / 2608212 + Purifier2.2.1 / 11**。原登录资料仍可见，模块配置始终三个true、五个false，SHA-256：

```text
8955EAD0BA2586DF1C729D0BC679E84812493B901BB3FF18EF0E7E72F09D35C6
```

最终强制miss只把模块自己的`coolapk_purifier_cache_v4.json`移至`/data/local/tmp/processing/cache_before_rc2_1661_miss.json`备份，再正常重建；未清账号、数据库、缓存目录或安全状态。其余手机调试/安装工件同样在processing目录。未卸载酷安、未重启手机/LSP、未改scope/模块启用状态、未写LSPosed数据库。

## 8. Tests / build / staging

执行 `gradlew test`、`gradlew assembleRelease stageReleaseCandidate` 成功。Debug / Compatible / Release各 **234 tests，0 failures / errors / skipped**：原201 +11 Splash lifecycle +6 runtime health +4 retry budget +6 Reply verifier/cache +6 Settings retry。

| 项目 | 最终值 |
| --- | --- |
| APK | dist/coolapk-purifier-v2.2.1.apk |
| Version | 2.2.1 / code11 |
| 源码及APK内嵌VCS revision | f6df56f39d54272e3396c29d2995e60c3d58d05e |
| APK size | 1,138,569 bytes |
| APK SHA-256 | 706C622F50B9825319F20B52D5D27FC9BEBCBE331C5E833442F1B50888A06E94 |
| Signer certificate SHA-256 | 12B482270B217A377CF3881382C86EE9F1C3E2B8E4BE25EE5118F327F2858875 |
| 验证 | apksigner v2通过；assembled == staged == installed module base.apk |

候选在源码提交之后构建并冻结。AGP会在重新打包时更新VCS元数据，所以文档提交后的staging/preflight使用`-x assembleRelease`处理已验证产物，不能让新的打包字节冒充实机候选。旧35416268…候选存在设置重进缺陷，旧C8C49445…仅VCS变化的重包也已排除；均不交付。

最终 `verifyReleaseNotesPreflight -x assembleRelease` 通过：发布说明内APK哈希、签名集合与冻结候选一致；`git diff --check` 通过。

## 9. 证据索引

本地 `.tmp_audit/azf_hardening/` 保存原始证据，不将账号截图与完整日志提交远程：

- `rc2_acceptance_summary.json`、`rc2_final_device.json`、`rc2_candidate.json`、`rc2_test_summary.json`、`rc2_signer.txt`。
- `rc2_159_*`、`rc2_165_*`、`rc2_1661_*` 的安装JSON、trace/result/health、settings_result、UI XML/PNG、模块日志和当前PID crash检查。
- `rc2_159_feed_scroll*`、`rc2_159_comments*`、`rc2_165_feed*`、`rc2_165_comments*`、`rc2_1661_feed*`、`rc2_1661_comments*`。
- `rc2_observer_audit.json`：最终停止后往返及注册/尝试计数；`settings_retry_tests.log`、`settings_fixed_stage.log`、`rc2_preflight.log`。
- 早期根因/边界证据：`reply_static_review.json`、`mt_165_workspace.json`、`mt_165_reply_search.json`、`final_1661_settings_reopen_late*`。这些不充当最终候选通过记录。

## 10. 最终交付决定

R1–R5与本轮新发现的缓存、设置时序缺陷已按上述范围闭合。README、[发布说明](release-notes-2.2.1.md)、[历史工件清单](history_apk_regression_manifest.md)与候选一致。

```text
RELEASE_READY = YES
```

停止于本地RC交付，等待用户人工验收；无远程发布授权，不push/tag/release/PR。工程验收不证明服务端风控问题已消失，也不以是否出现风控作为就绪依据。
