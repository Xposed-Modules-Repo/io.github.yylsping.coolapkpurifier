# 2.2.1 设置页与文档链接优化验收

日期：2026-08-28。导航修复检查点：`b197973`；按用户补充要求删除 Reply 状态副标题：`ac4f8f0`。本轮仅修改设置 UI、相关构建检查和文档，未改变既有去广告算法、默认开关、缓存格式或 READY 判定。

当前候选及校验值以[发布说明](release-notes-2.2.1.md)为准；使用方式见[README](README.md)。此前[剩余功能](issue5_2.2.1_remaining_feature_regression_report.md)、[Reply](issue16x_reply_sponsor_research_report.md)及[Issue #6](issue6_native_loader_reliability_report.md)报告保留各自的实测范围和历史包校验值，不代表本轮重新执行了全部广告场景。

## 根因与实现

1. **入口固定且残留点击区域**：旧代码给宿主 ComposeView 加 topMargin，再把入口加到外层 FrameLayout。入口不属于原生滚动列表，滚动只能移动 Compose 内容。已删除固定入口、外部占位、视图树文字定位及相关动画；现在在精确 `SettingEntranceComposeFragment.initData()` 返回后，把宿主真实 model 的单项分组插到原始分组列表第 0 位，由宿主 LazyColumn/原生 renderer 管理布局、回收、滚动和点击。
2. **返回退出整个设置 Activity**：旧配置页只是同一 Activity 内容区的覆盖 View，没有独立的返回处理。Android 返回仍落到宿主 SimpleActivity，因而退出整个设置。现配置页为该 Activity 所属的全页 Dialog，由系统处理 back/cancel；顶部按钮只 dismiss。没有新增 Activity 或篡改 task 栈，不隐藏、替换或 finish 宿主页面。关闭移除子页登记，宿主销毁时清理所属 Dialog，重复打开和迟到 dismiss 不会误删新子页。
3. **dist 文档链接错误**：根目录 release notes 直接复制到 dist 后，三个报告的同级相对路径失效。`stageReleaseNotes` 仅把本地 Markdown 目标改为 `../…`，保留外链和锚点；`verifyDocumentationLinks` 从每份实际文件所在目录解析目标并阻止断链。根目录原有有效路径保持不变。

静态证据来自现有 15.9.0 / 16.5.1 / 16.6.1 原始 DEX。生产代码不硬编码混淆后的 model/字段名，要求精确 Fragment/父类、public final 无参 `initData`、唯一 List 字段、普通 ArrayList 分组、统一 final model、四字段/构造器契约及匹配的 Compose renderer；任何不确定情况都跳过，不使用固定层兜底。重复执行仅去掉模块自己的分组，保留每个原生分组和条目的对象身份及顺序。

图标使用宿主资源名 `ic_setting`。零图标在宿主会变成红色居中按钮，已明确拒绝。宿主 Kotlin Function1 代理使用宿主 ClassLoader 和 Unit；首次混淆包实测发现 R8 重写这些反射名称导致入口安全跳过，已加入最小 keep 规则及 Compatible/Release mapping 检查。配置页另处理系统栏对比度和内容 inset，避免标题进入状态栏。

用户随后要求移除“本次启动已安装……”提示。已删除设置页 Reply 状态 TextView 和对应 UI 监听器，保留原开关、运行时健康记录及日志诊断；没有删除适配流程或将 INSTALLED 当成实际广告移除证据。

## 修改文件

| 文件 | 用途 |
| --- | --- |
| `SettingsEntryInjector.java` | 原生模型契约、插入与幂等 |
| `SettingsHooks.java` | 原生业务 Hook、配置页所有权及两种返回 |
| `OwnedSettingsPages.java` | Activity 与子页的身份登记、销毁清理 |
| `HookCoordinator.java` | 仅调整 SettingsHooks 构造参数 |
| `app/proguard-rules.pro` | 保留宿主反射所需 Kotlin 类型名称 |
| `app/build.gradle.kts` | 文档副本路径转换、链接及混淆守卫 |
| `SettingsEntryInjectorTest`、`PageStateRegistryTest`、`AttachHandoffPolicyTest` 及宿主夹具 | 新增 7 项回归、更新构造调用 |
| README、release notes、历史报告索引说明、本报告 | 当前行为、相对链接及候选证据 |

## 本地检查

- Debug / Compatible / Release 各 **275 tests**，0 failures / errors / skipped。
- Release lint：0 errors、13 warnings；不把既有警告描述为全部消除。
- 新增覆盖：第一个原生分组、保留宿主对象、重复 initData 幂等、宿主回调/Unit、拒绝空/混合/歧义模型及零图标、子页重复打开、旧 dismiss 不影响新子页、独立 owner 和销毁清理。
- `verifySettingsReflectionNames` 检查两种混淆产物；文档链接检查覆盖 README、根目录报告/说明及 dist 副本。单元测试证明数据归属和生命周期登记，实际滚动与 Android back 另由实机验收证明。

## 实机验收

设备 PLQ110，保留既有 LSPosed 环境。完整流程检查入口第一项、下滑消失、点击原坐标不打开模块、滚回顶部、系统返回、顶部返回、连续三轮进出、原生“界面显示”及返回、退出整个设置后重新进入。

| 酷安版本 | 原生首项、滚动及点击 | 系统/顶部返回与重复进出 | 原生设置导航 | 启动 |
| --- | --- | --- | --- | --- |
| 15.9.0 / 2511271 | 通过 | 通过，三轮 | 通过 | READY / frameworkActive=false |
| 16.5.1 / 2607271 | 通过 | 通过，三轮 | 通过 | READY / frameworkActive=false |
| 16.6.1 / 2608212 | 通过 | 通过，三轮 | 通过 | READY / frameworkActive=false |

上述三版使用同一导航修复候选 `b197973`（SHA-256 `0B661DD33F2EDE4F44EF21F0CDD0B6581A0D587630978B6ADB4419B4AD07B16D`）。通过 dumpsys 确认配置页打开和两种返回均保留同一个原生设置 Activity 实例；退出整个设置后再进入才创建新实例。滚出后 XML 无模块入口，点击原坐标不会打开模块；16.5/15.9 可正常命中该位置的原生设置项。

16.5 首次过程遭遇电脑侧 ADB daemon 连接超时，未算通过；连接恢复后四个 MCP 均正常，补验完整流程通过。未重启手机或 LSP，也未出现降级后的应用错误。

删除状态提示后的最终包 `ac4f8f0` 在已恢复的 16.6.1 再次完整通过相同导航流程，额外确认每次进入均无“本次启动……”副标题、8 个开关与原始值一致；Reply 日志仍为 INSTALLED。最终包没有重新在两版旧宿主上验证文字删除，三版导航证据与最终包复核分开记录。

最终 APK 为 1,162,197 bytes，SHA-256 `666DA9ADF6B43E48A7197D301F149EAFBDC46ACBD69C2B2D3AF6C1EFFEC287BF`，安装后 base.apk 一致。手机恢复酷安 16.6.1 / 2608212，保留原登录态和默认三项开启、五项可选关闭；配置文件字节、revision 与原始备份一致，SHA-256 `8955EAD0BA2586DF1C729D0BC679E84812493B901BB3FF18EF0E7E72F09D35C6`。新包启动仍为 READY、零 framework Hook，无崩溃日志。后续仅文档提交，不重新打包。

证据保存在本地 `.tmp_audit/settings_ui/`，手机侧所需产物统一位于 `/data/local/tmp/processing/`。包括 baseline 固定入口/错误返回截图、最终各版 XML/截图、Activity 实例记录、启动 trace、模块日志及结构化验收结果。没有修改账号、权限、订阅或发布内容，没有 Frida/IDA 动态附加、清数据、重启手机/LSP 或写远程仓库。

## 发布判断

本轮设置问题修复完成后仍建议以 **2.2.1 候选**提交人工验收，不自动发布。Reply 自然赞助实际移除、FullScreen 自然触发等上一轮未观察到的内容仍保留原限制；本轮设置页通过不能替代这些证据，也不说明服务端风控消失。
