# Issue #6 — DexKit native loader 可靠性验收

> 后续设置页与文档优化见[设置验收报告](settings_ui_and_docs_followup_report.md)，当前候选信息见[发布说明](release-notes-2.2.1.md)。本文保留本阶段的历史实测范围及 APK 校验值。

日期：2026-08-27～28。基线为 Issue #5 剩余功能回归完成后的 **8a7dab3**，实现提交 **72cc5f7**。模块维持 **2.2.1 / versionCode 11**。仅本地操作，等待人工验收及明确的远程写入授权。

## 1. 故障层级与根因边界

已重新读取 [Issue #6 全文及评论](https://github.com/yylsping/coolapk-purifier/issues/6)。五段启动记录合计：19 次 runtimeDexReady、5 次 runtimeLoaderChanged（已有 runtime ClassLoader）、36 次 cacheMiss、36 次 native 加载失败；没有 bridgeCreateStart 或 resolver 扫描。cache 记录为 verified=0 / total=0 / persistedSettled=false，属于没有可用条目的 miss，未见 cache corruption 证据。

代码将 bridgeCreateStart 放在 native loader 成功后，因此“bridgeCreateEnd failed=unable to load dexkit native library”不能理解为 Bridge 已经创建。四段完整记录最终因 deadline DEGRADED，缺少 Splash、Feed、entity accessors；另一段日志不完整。已删除的 splashDecision 不恢复。

- **已确认**：DexKit native bootstrap failure / loader reliability bug；不是这些日志能证明的广告指纹失配。
- **静态确认的脆弱点**：使用酷安 PackageManager 查模块路径；只选 SUPPORTED_ABIS[0]；仅靠文件非空复用；失败后重复加载同一文件；吞掉二次错误；按 versionCode 命名；失败被转换成可重试的 bridge unavailable；私有 filesDir 长期残留旧 so。
- **未确认**：报告者设备实际触发的是包可见性、错误 ABI、文件损坏、权限还是 linker namespace。旧日志缺少底层 cause，无法排序为某个“高概率唯一根因”。本机也未重现报告者原始失败，不把本机成功替代根因确认。

原始响应留在本地 .tmp_audit/remaining_regression/issue6_remote.json；没有向 Issue 发评论或修改状态。

## 2. 最终加载链

```text
libxposed API 102 getModuleApplicationInfo
→ base / split APK 与 nativeLibraryDir
→ 当前进程位数及对应有序 ABI 列表
→ APK 中实际存在的兼容 lib/<abi>/libdexkit.so
→ 验证 ELF class / machine、大小、SHA-256
→ 优先加载模块 nativeLibraryDir/libdexkit.so
→ 必要时私有 code_cache 临时提取
→ System.load 成功
→ DexKitBridge.create(runtime ClassLoader)
→ Resolver → 终态事务 → READY
```

模块入口仅保存框架提供者，首次需要 DexKit 时才取路径。loader 不调用宿主 PackageManager；宿主自身版本识别所用 getPackageInfo(com.coolapk.market) 保留，与查询模块包不同。API 能力同时由本地 API102 JAR 的 javap 和 [libxposed 官方接口](https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.html)核对。

ABI 使用 [Process.is64Bit](https://developer.android.com/reference/android/os/Process#is64Bit()) 及相应 SUPPORTED_64_BIT_ABIS / SUPPORTED_32_BIT_ABIS，交集为空时明确报错。64位进程不会把32位 so 当 fallback；其架构约束见 [Android NDK ABI 文档](https://developer.android.com/ndk/guides/abis)。首选 ABI 未打包时才选择列表内后续兼容 ABI，未知 ABI 不猜测。

提取身份包含 versionCode、ABI、**承载 so 的 APK 完整 SHA-256** 和 entry CRC；内容仍独立校验大小、SHA-256 和 ELF。临时文件写完 sync、校验、设为只读，在同目录原子替换；不把新字节复制进可能映射的 so。正确旧文件可复用；已知损坏在 dlopen 前拒绝。已有文件校验/加载失败或新提取加载失败后，最多强制重提取一次；两个 fallback 尝试结束仍失败则停止。模块 native 目录尝试最多一次，因此最坏为一次共享路径加两次私有路径，跨 session 不重复预算。

成功或失败结束后删除本次提取文件；清理专属目录内符合严格命名规则的旧构建/中断临时文件，不递归删除目录或无关文件。native 成功后，仅清理旧 loader 的精确 libdexkit-数字.so / .tmp 文件；不删除 APK 安装目录中的共享 so。正常路径无需提取。当前私有目录已不含旧 libdexkit-10.so、libdexkit-11.so，也没有新提取目录残留。清理不是隐匿机制，不改 maps、堆栈或 PackageManager 返回值。

## 3. 诊断与终态

阶段日志包含 moduleLocation、abiSelect（supported / available / selected）、loadExisting、extract / reextract、systemLoad、cleanup；包含版本、APK hash、entry、size、CRC、内容 hash。每次失败记录 Throwable 类型/信息及最底层 cause；重试错误保留 suppressed，最终异常保留 cause。

永久失败由 LoadFailure 传播，不再变成 WAIT_RUNTIME_DEX。协调器复用原有 generation / loader 事务，提前 FORCE_DEGRADED，terminalSnapshot 明示 **degradedReason=DEXKIT_NATIVE_LOAD_FAILED**；不会重新调度相同 native 失败。已 supersede 的 session 仍不能提交终态；worker 仍独占并关闭 Bridge。native 成功后 Bridge 自身创建失败仍走原有有界 runtime 重试，二者不混淆。

DEGRADED 时保留原有必要的 Splash Instrumentation 安全兜底政策；没有为失败路径虚报 zero framework。正常 READY 后 retirement / HookLedger 的约束不变，没有新 framework Hook、长期轮询、PM Hook 或反检测代码。

## 4. 构建产物检查

JNI packaging 保持 useLegacyPackaging=true，四个实际 ZIP 条目均为 deflate（压缩方式8），未伪造 APK!/ 直接加载；由安装器提供 native 路径，或正常临时提取。

| ABI | ELF class / machine | so bytes | so SHA-256 |
|---|---|---:|---|
| arm64-v8a | 2 / 183 | 290408 | 0bbdc53bd8c534f77a96962af918909751204e5ac8a93c5fe20031fc02aebc04 |
| armeabi-v7a | 1 / 40 | 188244 | 13a47717187a85fe8721c2ab3d15dbcebc0a0e0c02a03ae7d27815a9e553b8c2 |
| x86 | 1 / 3 | 310856 | ab8f81997c5980db5434fb95ea328a7d0183a76e0ee2d1b38b93fda8ebf55c30 |
| x86_64 | 2 / 62 | 303096 | d030731c3eac0fe65eab17e2e396f2e211182b81653256dc77e2c96f47c9a183 |

release R8 mapping 中 DexKitBridge 与 native JNI 方法名保留，DexKit/FlatBuffers keep 规则不变；真实 release APK 的 Bridge 创建和 resolver 查询成功进一步验证 JNI 可调用。仅 arm64 有实机 native 执行证据，其他 ABI 的设备执行不冒充已测。

最终候选：

- 源码：72cc5f7（继承 8a7dab3 的 Related Data 修复）。
- APK：dist/coolapk-purifier-v2.2.1.apk，**1,152,589 bytes**。
- APK SHA-256：**A175521334EF555CA19B9572A43E8409147CB1AFCB28AB77D563360D513837D2**。
- 签名证书 SHA-256：12B482270B217A377CF3881382C86EE9F1C3E2B8E4BE25EE5118F327F2858875。
- 本地 stageReleaseCandidate 校验单一 signer 与 v2/v3 签名策略；安装后 base.apk 哈希一致。

## 5. 专项测试矩阵

gradlew test / test stageReleaseCandidate：Debug、Compatible、Release 各 **258 tests，0 failures、0 errors、0 skipped**。新增 loader 17 项、session 2 项、terminal transaction 1 项；覆盖选择、损坏、自愈上限、嵌套 cause、同版本不同构建、split、目录失败、旧产物清理与无关文件保留、Bridge 分类和终态不回翻。

### 真实 Android JNI：独立 ART 测试

没有附加酷安，也没有安装额外测试 APK。在 /data/local/tmp/processing/ 中以 app_process 启动独立测试进程，使用与生产逐字节相同的 loader 源码和最终 APK 内真实 so。源码 SHA-256 为 ec32bf9b1e87a26e32bd12d87250862fe23a0aaa3847fce632619be9237acb3b。测试夹具注入 module location / 提取目录 / 可控首次加载错误；生产包不含开关或测试后门。

| 场景 | native load | Bridge（unlink 后） | Resolver / 酷安 READY / Feed / Splash |
|---|---|---|---|
| 首次无提取文件 | 一次成功 | valid，dexNum=1 | N/A，独立 ART |
| 正确 existing 文件 | 校验复用，一次成功 | valid | N/A |
| truncate 为3字节 | 校验拒绝，重新提取，一次实际 load 成功 | valid | N/A |
| 正确文件首次 load 受控失败 | 重提取，第二次 load 成功 | valid | N/A |
| 永久受控 UnsatisfiedLinkError | 恰好两次，后续调用复用同一失败 | 未创建；分类准确 | N/A；协调器提前降级另由事务单测验证 |

五项均 PASS，最终 extractedFiles=0。这些进程由 shell/su 启动，**不能替代酷安 UID/SELinux 域内 fallback 的完整 READY 验收**；本机正式模块采用共享 native 路径，没有为了迫使 fallback 修改已安装 APK 的 native 文件或系统权限。该限制保留。

### JVM 故障矩阵与权限边界

- 首项 ABI 缺失、后续兼容项存在：PASS；只剩错误位数：明确失败；32位进程选择32位项：PASS；错误标注 ELF：加载前失败。
- module location 不可用及嵌套 SecurityException：一次失败、保留 root cause；目录不可写等价的目录创建失败：明确 extract 阶段。
- 框架信息含 split APK：PASS，整个 location/engine 契约不需要 host Context/PackageManager。
- **没有切换酷安“读取应用列表”权限**，两组权限状态实机对照 NOT_RUN，服从后提供的 Issue #5 剩余回归安全边界。当前权限下框架定位/加载成功，以及代码无模块 PM 查询，是本轮实际证据；不写成已验证所有 ROM 权限策略。
- 永久 native 失败在当前 session 立即提交有明确 source 的 DEGRADED；后到 deadline/READY 不能覆盖；native 失败后 Bridge opener 不执行，session 不重新 load。此项是单元测试及代码路径验证，未在酷安进程人为制造永久错误。

## 6. 最终候选的酷安16.6.1验收

| 启动场景 | Native / Bridge | Resolver | READY（trace相对时间） | Feed / Splash / framework |
|---|---|---|---:|---|
| 原始配置，清空仅模块 resolver cache | 框架 native 一次成功；Bridge dexNum=11 | feed、feed#2、4个entity getter、Splash | 803ms | 完整安装，zero framework |
| 单独开启 Related Data | 框架 native 一次成功；Bridge成功 | 增加已验证 getRelatedData getter | 791ms | 业务 hook 安装，zero framework |
| 恢复原始配置与原 cache | cache hit，完全跳过 native / Bridge | 12条cache校验 | 238ms | 完整安装，zero framework |

三次均无目标进程 crash，Instrumentation retired，frameworkActive=false / frameworkActiveHooks=[]。以上是 bootstrap trace 相对时间，不是严谨的跨机启动性能对照。

此前同实现的中间候选 f54b96e…亦完成 native miss READY 与 Feed/帖子浏览；首次首页出现加载失败，下拉刷新后恢复并记录 EntityAdHelper 移除条目。该现象不能仅凭时间关系归因于 loader。最终候选的设置入口、开关恢复、Feed smoke 与登录态再检查记录在本地 final 文件中。

Splash exact hook 与 lifecycle guard 继续安装并覆盖，正常启动可进首页；没有自然取得 FullScreenAdActivity 的新增阳性 finish 证据。Issue #5 的20次专项预算已经结束，不再延长。16.x Reply 仍 UNAVAILABLE，普通评论浏览不代表专用 sponsor 过滤；五项可选功能的内容阳性边界沿用[剩余功能报告](issue5_2.2.1_remaining_feature_regression_report.md)。

最终设备：酷安16.6.1 / 2608212，Purifier2.2.1 / 11，原登录态保留。原配置逐字节恢复：

```text
SHA-256 = 8955EAD0BA2586DF1C729D0BC679E84812493B901BB3FF18EF0E7E72F09D35C6
revision=1, pendingAdaptation=none
remove_splash_ads=true
remove_feed_sponsor=true
remove_reply_sponsor=true
其余五项=false
```

原 resolver cache 亦备份并恢复；最终比较确认原条目和目标内容一致，仅既有 touch 逻辑更新 lastUsedAt。最终截图确认同一登录账号、正常 Feed 页面和设置项，模块日志记录 EntityAdHelper 移除一项。未清宿主数据。模块安装均先 push 到 processing，再 root pm install -r -t --user 0。未改 LSP 启用/作用域，未重启手机或 LSP。最后检查的所有 MCP 能力均可用；无 Frida/IDA 动态附加。

## 7. 与 Issue #5 的关系及发布结论

**直接根因关系未确认。** 去掉模块包可见性依赖、减少长期私有 so、终止无意义失败重试，可能减少共同的运行环境暴露或异常操作；未证明这些文件或操作是风险触发源。本轮没有进行受控风控频率比较，不能声称复现率下降。未主动制造风险阳性、改设备标识/token、安全SDK、账户、权限或网络。

最新既有静态结论（field12无模块包名、私有目录collector未找到、正常READY零framework、shouldShowAd退出）不因旧Issue背景而推翻，也不在本轮重新宣称验证。普通帖子里含系统提示截图不是本设备发生的风险提示。

本地证据目录：.tmp_audit/remaining_regression/。主要文件 issue6_final_*、issue6_final_apk.json、issue6_final_tests.log、issue6_final_build.log、android_native_*.log、android_native_harness/、config_before.json / cache_before_issue6.json。含个人页面的截图和调试原始资料不加入公开提交。

**自主验收通过，候选待人工验收；未发布。** loader 已知脆弱点及失败分类已修复，设备范围/权限对照/独立ART与宿主fallback之间的证据限制明确保留。不得解读为所有设备或服务端风控问题已解决。
