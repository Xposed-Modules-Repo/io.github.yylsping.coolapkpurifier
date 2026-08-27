# 16.x Reply Sponsor 研究与实现验收

日期：2026-08-28。代码检查点：`6c3397b`。本报告接续 `issue16x_reply_sponsor_research.md`，更新先前报告中 16.x Reply UNAVAILABLE 的结论；不重写先前阶段的观察记录。

## 结论

**16.5.1 / 16.6.1 的专用 Reply 目标已恢复为 INSTALLED。** 两版的首次解析和缓存启动均为 core READY，终态 `frameworkActive=false`。15.9.0 旧目标仍可安装并使用缓存。**自然 Reply sponsor 的实际移除仍未观察到**，不能将安装成功写成广告阳性路径已验证。

建议继续把 2.2.1 作为等待人工验收的本地候选，保留自然广告样本这一验收边界；不建议宣传“所有评论广告均已动态验证”。未创建远程提交、分支、PR 或 release，也未 push。

## 根因与三版本证据

原实现的发现、缓存校验、安装和 READY 后有限重试都围绕 `MultiFeedReplyViewHolder`，绑定形状限定为 `(Entity|FeedReply) -> void`。16.x 中该旧类不再可达，继续尝试原类名无法恢复功能。

三版业务 DEX 对照发现了独立的 `FeedReplySelfDrawViewHolder` 路径。**它在 15.9.0 就存在，不是 16.x 新增**；原来的旧类 Hook 并不能证明覆盖了这条路径。16.x 改变了类和方法的混淆命名，完整绑定实际为 `Object -> void`，因而旧发现条件漏掉了它。

| 版本 | 旧类 | 自绘赞助 holder / 完整 binder | 直接父类 |
| --- | --- | --- | --- |
| 15.9.0 / 2511271 | MultiFeedReplyViewHolder 存在 | FeedReplySelfDrawViewHolder / `ކ(Object)` | BindingViewHolder |
| 16.5.1 / 2607271 | 无旧类定义 | `sm4 / އ(Object)` | `p5` |
| 16.6.1 / 2608212 | 无旧类定义 | `fn4 / ވ(Object)` | `i5` |

上表混淆名仅用于复核证据，**生产源码没有硬编码它们**。甚至 16.5 的 `fn4`、16.6 的 `sm4` 分别是其他合成类，不能按混淆类名跨版本匹配。

两版 16.x 的 `FeedReplyListFragmentV8.onActivityCreated(Bundle)` 中存在相同注册序列：读取 holder 的布局字段 → 注册精确模板 `feedDetailReplySponsorCard` → 配置工厂 lambda → 注册 adapter。该工厂经评论 fragment 的静态方法构造目标 holder，构造器为 `(View, EntityAdHelper, DataBindingComponent)`。静态布局字段来自当前 APK 的 `R.layout.item_reply_self_draw`。

完整 binder 保存 Entity，读取 EntityCard 内部实体和广告对象，并读取 `rewardVideoVisibleInLayout`、`sponsorStyle`。其父类声明唯一抽象 `void(Object)` 契约。双参数回调和普通 Object 辅助方法不是该契约，未纳入 Hook。holder 内部还使用广告 SDK 展示对象，但本次没有 Hook SDK。

### 取证来源

三份 APK 本体均为保护壳，不能把壳 DEX 搜不到业务类当作不存在。通过临时诊断模块的现有 DexKit 会话导出 15.9 / 16.5 / 16.6 的 11 / 12 / 12 个 DEX，绑定已校验的安装版本与 APK hash。没有 Frida/IDA attach。

Android app 沙箱要求导出器先写自身 files 目录；完成后立即移入 `/data/local/tmp/processing/reply16x_export_{159,165,166}/` 并拉取。其他手机调试产物均在 processing。诊断代码已删除，`DexKitSession.java` 与原始备份逐字节一致。

本地证据：`.tmp_audit/reply16x/static_evidence.json`（各 DEX SHA-256、类/方法、注册及构造器引用）、`reply_index.sqlite` 和 `export_*`。反汇编索引按真实指令长度读取引用，不按字节猜测 opcode。导出 DEX 的索引均无解码错误；原始业务 DEX 不纳入 Git。

## 实现与安全约束

| 文件 | 关键变化 |
| --- | --- |
| ReplySelfDrawResolver.java | 以两版共有的 Kotlin 源码语义前缀和渲染日志精确标记发现类，再要求评论页模板注册读取布局字段、评论工厂调用构造器、binder 同时含两项业务字段标记；验证后唯一才接受 |
| ReplySelfDrawTarget.java / TargetVerifier.java | 要求 final holder、RecyclerView.ViewHolder 继承、由该基类声明的 View itemView、精确构造器、Entity/EntityAdHelper 实例字段、唯一匹配当前 APK 布局值的静态 int 字段、唯一直接父类抽象 Object binder 的具体 public 覆写；拒绝辅助/双参数/抽象/桥接方法及跨类伪造缓存 |
| EntityClassifier.java / EntityListHooks.java | 仅在 Reply 开关开启且已验证 getter 读到精确 `feedDetailReplySponsorCard` 时，在原 binder 完成后折叠该 holder 的 itemView；不改响应列表、分页计数或实体。复用时恢复原 visibility、minimumHeight、layout height |
| FeatureHooks.java | 新目标独立记录 primary 安装状态；Reply 可由旧 holder 或新 binder 满足；已安装后及时退休临时发现 Hook |
| HookCoordinator.java / LazyDiscoveryPolicy.java / TargetResolver.java | 新 method cache key 为 `feature.replySelfDraw`；16.x 旧缓存缺少 Reply 目标时允许正常有界解析；新目标安装后的缓存启动不扫描 DexKit。未增加 READY 后 DexKit worker 或 framework Hook |

发现规则不依赖具体版本号对应的混淆名字，布局数值也从当前宿主资源解析。采用 16.x 大版本门槛启用新发现路径，15.x 保留原行为；不扩大本轮到 15.9 自绘路径的安装覆盖。若未来版本删除上述语义或改变契约，拒绝猜测，Reply 可单独 UNAVAILABLE，不把它纳入 core 必需目标。

没有改动 Issue #6 native loader、原 RelatedData verifier/getter、SplashLifecycleGuard、HookLedger 或终态事务。用户持久化开关不会因解析失败被自动关闭。若未来现代目标不存在，选中 Reply 时每次启动会执行一次正常适配窗口内的解析，不能声称所有未知宿主都保留无扫描快路径。

### 排除的候选

- cardlist 通用 `(List, boolean) -> List` 方法也含 Reply 模板，但其注册入口在 MainV8ListFragment，且同时处理多类信息流卡片，不作为新 Reply 目标。
- EntityAdHelper 广告选择器跨多上下文，范围过大。
- 评论响应处理函数还维护列表、统计及分页，直接删入参可能影响正常评论，未采用。
- 16.6 的曝光日志标记在 16.5 不存在，未作为必要指纹。
- 不 Hook RecyclerView 基类、通用 ViewHolder、布局 inflation 或任意 Object binder。

## 自动验证

`testDebugUnitTest / testCompatibleUnitTest / testReleaseUnitTest`：**每变体 268 tests，0 failures / errors / skipped**，相较基线新增 10 项测试。

覆盖唯一父类绑定契约、辅助/双参数/抽象拒绝、错误布局及构造器、注册/工厂/业务标记缺失、重复和歧义候选、跨类和 class-only 错误缓存、新 method cache 持久化、旧缓存升级策略、开关隔离、普通回复/楼中楼结构/非广告/无 getter 保留，以及 holder 重复折叠与恢复。

Release lint：0 errors、13 warnings；assembleRelease、签名单证书策略和候选 SHA-256 检查通过。测试中的实体和 View 为明确的 JVM fixture，不冒充真实广告阳性。

## 实机验收

| 场景 | 结果 |
| --- | --- |
| 16.6.1 使用旧 12 项缓存升级 | 找到 3 个 Object 形状候选，仅真实 binder 通过；Reply INSTALLED、READY、零 framework |
| 16.6.1 新缓存启动 | 13 项缓存直接安装；`dexkitScan=false`，不创建 bridge、不安装临时 Feature loadClass Hook |
| 16.6.1 GUI 关闭 Reply 后重启 | Reply DISABLED，业务 ledger 无新 Reply Hook，READY、零 framework |
| 16.5.1 首次解析 / 缓存启动 | `sm4.އ(Object)` 安装成功；缓存 `dexkitScan=false`；Reply INSTALLED、READY、零 framework |
| 15.9.0 首次解析 / 缓存启动 | 旧类三条 Entity/FeedReply 方法继续安装；Reply INSTALLED、READY、零 framework |
| 16.6.1 正常内容 | 普通帖子及有 2309 条回复的热门帖子可打开；正常评论、内联楼中楼内容显示；回复编辑器可打开并取消，没有发布内容或订阅通知 |
| 最终元数据包 16.6.1 | cache → GUI 开启相关推荐触发解析 → 原配置 cache；三次均 READY、零 framework，RelatedData getter 与 Reply binder 同时安装成功 |

启动记录均无 crash buffer 内容，原配置 SHA-256 保持/恢复为 `8955EAD0BA2586DF1C729D0BC679E84812493B901BB3FF18EF0E7E72F09D35C6`。关闭测试只通过设置 GUI 改动 Reply 开关，结束后恢复原始配置字节及 revision。

一次导航脚本未能从回复编辑器退出到设置，其后名为 `candidate_166_reply_off` 的启动实际上仍为开关开启，**不计作关闭测试**；有效证据为 `candidate_166_reply_off_valid`，包含 GUI false 和日志 DISABLED。未确认任何通知订阅按钮。

未观察到 `removed reply sponsor via self-draw`，因此自然阳性移除为 **NOT_OBSERVED**。评论上方曾显示其他推广卡；未取得其新 Reply binder 调用证据，不能把视觉文案直接当作该模板命中，也未为此扩大过滤范围。没有伪造响应、点击广告或触发风控来制造样本。

最终设备为 Coolapk 16.6.1 / 2608212、模块 2.2.1 / 11，停留在原账号“我的”页面。配置 revision=1、pendingAdaptation=none、默认三项开启、五个可选项关闭，原文件哈希一致。`final_acceptance.json` 记录文件核对；`final_166_restored_trace.log` 确认 13 项缓存、dexkitScan=false、Reply INSTALLED、READY、零 framework。本轮未清应用数据、改应用列表权限、重启手机/LSP、安装 Manager、改 LSP 数据库或模块作用域。

## 候选与剩余边界

候选为 `dist/coolapk-purifier-v2.2.1.apk`，版本 2.2.1 / 11。代码提交后的打包与三版本已测包比较，ZIP 内仅 `META-INF/version-control-info.textproto` 改变，classes.dex、资源和 so 均完全一致；最终元数据包另在 16.6.1 验证。

APK 大小 1,157,985 bytes；SHA-256：`56FFD126A3A69F718D0FD966F10F291090358839032290F78F3F49D45C61B709`。手机安装 base.apk 与此一致。签名证书仍为 `12B482270B217A377CF3881382C86EE9F1C3E2B8E4BE25EE5118F327F2858875`，release notes preflight 通过。报告提交后不重新打包这份已验收文件。

仍待自然广告样本确认实际折叠、异步渲染后的持续隐藏，以及不同赞助形式的覆盖。旧 15.9 自绘赞助路径也未在本轮新增安装。13.1.1、16.1.2 和其他未来版本未重新验收。前阶段 FullScreen 与五个可选功能的自然阳性边界继续保留，不由本次 Reply INSTALLED 推翻。

最终能力状态：**Reply INSTALLED；自然内容移除证据未闭合**。INSTALLED 表示目标安装状态，不是全场景效果保证。
