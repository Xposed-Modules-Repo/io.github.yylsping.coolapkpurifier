# 2.2.1 剩余功能回归（Issue #6 前检查点）

日期：2026-08-27；基线 ea8939a，模块 2.2.1 / 11。仅本地验收，未发布或修改远程。

## A. Reply Sponsor

| 宿主 | 专用目标 | 本阶段证据 |
|---|---|---|
| 15.9.0 | INSTALLED | 引用上一轮三版本基线，本阶段未重新安装该版本 |
| 16.5.1 | UNAVAILABLE | 实机普通评论浏览、有限重试耗尽、回调停止；core READY |
| 16.6.1 | UNAVAILABLE | 实机普通及嵌套多回复浏览；core READY |

最终没有引入新的 Reply target。旧 MultiFeedReplyViewHolder 的精确类在当前运行时不可达；现有业务 DEX 中保留名字字符串，但未确认其可用类定义或稳定 sponsor binder。已检查已有运行时 DEX 的类/方法形状、通用 reply holder 候选；JADX/MT 当前 APK 工作区只有壳 DEX，不能据此断言业务类不存在。证据不足以区分重命名、拆分或代理，也未发现能证明 loader/cache 错选的证据。没有猜混淆名、放松 verifier 或恢复长期 ClassLoader Hook。普通评论可浏览不代表 sponsor reply 已过滤。

## B. FullScreen 自然触发预算

16.6.1，原始配置，原候选 APK 706C622F50B9825319F20B52D5D27FC9BEBCBE331C5E833442F1B50888A06E94。每轮正常 force-stop/启动，观察启动与首屏导航约 11 秒，收集 Activity events、bootstrap、模块日志和 UI；没有改网络、权限、账户或广告状态。第 20 次停止此专项，不将后续功能/loader 启动计入追加触发预算。

| Run | READY | frameworkActive | FullScreenAdActivity observed | result |
|---:|---|---|---|---|
| 1 | YES | false | NO | READY / zero framework / no crash |
| 2 | YES | false | NO | READY / zero framework / no crash |
| 3 | YES | false | NO | READY / zero framework / no crash |
| 4 | YES | false | NO | READY / zero framework / no crash |
| 5 | YES | false | NO | READY / zero framework / no crash |
| 6 | YES | false | NO | READY / zero framework / no crash |
| 7 | YES | false | NO | READY / zero framework / no crash |
| 8 | YES | false | NO | READY / zero framework / no crash |
| 9 | YES | false | NO | READY / zero framework / no crash |
| 10 | YES | false | NO | READY / zero framework / no crash |
| 11 | YES | false | NO | READY / zero framework / no crash |
| 12 | YES | false | NO | READY / zero framework / no crash |
| 13 | YES | false | NO | READY / zero framework / no crash |
| 14 | YES | false | NO | READY / zero framework / no crash |
| 15 | YES | false | NO | READY / zero framework / no crash |
| 16 | YES | false | NO | READY / zero framework / no crash |
| 17 | YES | false | NO | READY / zero framework / no crash |
| 18 | YES | false | NO | READY / zero framework / no crash |
| 19 | YES | false | NO | READY / zero framework / no crash |
| 20 | YES | false | NO | READY / zero framework / no crash |

DYNAMIC_FULL_SCREEN_TRIGGER = NOT_OBSERVED_WITHIN_BUDGET

NOT_OBSERVED_WITHIN_20_COLD_STARTS

以上是有限观察窗口结论；不证明路径不存在、已执行 finish 或风控消失。Instrumentation 均退休。

## C. Issue #2 单项回归

各项分别通过设置 GUI 单独开启，保持三个默认项开启；正常浏览帖子、Feed、评论。没有自然取得足以确认过滤前后对照的目标内容，因此不把安装成功写成实际内容过滤成功。

| Feature | target installed / verified path | dynamic content | zero framework | result |
|---|---|---|---|---|
| AUTO_COMMENT | RecyclerViewItemFullVisibleControllerKt 静态业务方法 | NOT_TRIGGERED | YES | HOOK_PATH PASS |
| TOPIC_RECOMMEND | d14 的 Feed/Composer 业务方法，唯一已验证 row assembler | NOT_TRIGGERED | YES | HOOK_PATH PASS |
| RELATED_DATA | $$AutoValue_Feed.getRelatedData()List，修复后 | NOT_TRIGGERED | YES | HOOK_PATH PASS |
| SAME_TOPIC_FEED | 已验证 MainV8ListFragment entityTemplate 语义过滤路径，不额外 hook predicate | NOT_TRIGGERED | YES | SEMANTIC_PATH PASS |
| DETAIL_SPONSOR | $$AutoValue_Feed.getDetailSponsorCard()Entity | NOT_TRIGGERED | YES | HOOK_PATH PASS |

### Related Data 发现并闭合的回归

原实现只接受 RelatedDataViewHolder，16.6.1 单项开启后在约 19.9 秒进入 DEGRADED，缺少 feature.relatedData:holderHook，保留安全兜底 Instrumentation。不能将这一结果算通过。

修复采用窄业务 getter：精确 getRelatedData、无参、返回 List、非静态/非抽象、继承确切 Feed；唯一匹配才接受。保留旧专用 holder 兼容路径。启用时只替换非空 List 返回值，不修改原集合；关闭、null、空集合和非列表值保持原样。Readiness 要求相同 generation 的有效 descriptor 和已安装 hook，不接受空 descriptor 的伪 primary。

修复后 16.6.1 与 16.5.1 均 READY、frameworkActive=false、frameworkActiveHooks=[]、Instrumentation retired，正常帖子与评论可用。没有自然记录到 nonempty related getter 的过滤事件；静态 getRelatedData 引用扫描也不能替代 UI 阳性证据。

## D. 配置与设备完整性

测试前后配置逐字节一致，SHA-256：

8955EAD0BA2586DF1C729D0BC679E84812493B901BB3FF18EF0E7E72F09D35C6

revision=1、pendingAdaptation=none，三个默认项 true，五个可选项 false。已停止宿主后恢复原文件，再恢复 16.6.1（2608212）并冷启动，READY / zero framework / no crash；原登录态保留，不清数据。16.5.1 定向降级安装成功且未出现降级故障。手机未重启，LSP 守护未重启，未改 LSP 配置数据库。

宿主 APK SHA-256：946B98B1DD30397976512F8196909F38A2A6FD1A047AD728F7C221A6ECE0E02B。

## E. 构建、证据与 Release Impact

基线三变体各 234 测试通过。修复后 gradlew test assembleRelease：Debug / Compatible / Release 各 **238 tests，0 failures，0 errors，0 skipped**；git diff --check 通过。新增相关回归在生产修复前确认失败，修复后通过。

本阶段实机安装 APK：2.2.1 / 11，1,139,809 bytes，SHA-256 E5684C9CFA73F96C2AE223F3A93412B43196A8AE44C8F6DB6759594BF3C9937C。这是 Issue #6 前中间产物，不覆盖最终发布候选身份。

本地证据目录 .tmp_audit/remaining_regression/：full_screen_01..20_result.json 及对应 logs/XML，reply_shapes.json、reply_binder_candidates.json、related_xrefs.json，五项选择/启动/浏览记录，related_fixed_1661*、related_fixed_165*、restore_1661*，测试日志和配置备份。手机实际落盘的调试产物均在 /data/local/tmp/processing/；含个人页面截图的证据不纳入公开提交。13.1.1 = OUT_OF_SCOPE。

RELEASE_STATUS = STILL_READY

Related Data 新 blocker 已修复并定向复验；Reply UNAVAILABLE 与动态内容 NOT_TRIGGERED 均按任务规则保留边界。此结论只适用于本阶段；随后继续 Issue #6 原生库可靠性修复，最终候选以其验收报告为准。

