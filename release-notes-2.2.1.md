# 酷安净化 2.2.1 — 本地 Release Candidate

版本：2.2.1 / versionCode 11。尚未发布，等待用户人工验收和单独的远程写入授权。

## 改动

- 继承 Mode A-ZF：正常 READY 清理后无 framework Hook；Splash lifecycle guard、必要失败兜底、Reply有限重试、HookLedger与设置入口修复保留。
- 修复16.x开启相关推荐时因旧 holder 不可达而 DEGRADED：采用唯一且严格验证的 Feed.getRelatedData 业务 getter，保留旧专用 holder 兼容路径。
- 恢复16.5.1 / 16.6.1 Reply专用目标：以评论模板注册、共有源码/渲染标记、动态布局资源及父类抽象绑定契约定位自绘binder；不硬编码混淆名，不扩大到RecyclerView基类。仅在开关开启且精确模板匹配时折叠，复用时恢复原视图状态。旧缓存缺少目标可重新适配，新缓存直接安装。
- Issue #6 native loader 从 libxposed API102模块信息定位，不依赖酷安 PackageManager 查模块包。按当前进程位数匹配实际打包 ABI；优先加载模块native目录，必要时临时提取。
- 提取文件绑定 versionCode、ABI、APK SHA-256和CRC；校验大小、内容hash与ELF，损坏或加载失败最多重提取一次，结束后清理本次提取及专属目录内已知旧产物。
- native 永久失败保留底层cause并提前 DEGRADED，明确 DEXKIT_NATIVE_LOAD_FAILED；不再反复误报成等待Bridge或广告目标失配。

## 实际验证范围与限制

[剩余功能报告](issue5_2.2.1_remaining_feature_regression_report.md)覆盖16.6.1五项逐一开启、20次有界自然冷启动，以及16.5.1相关推荐修复对照。五项路径成功安装/验证，但未取得足以确认实际过滤的内容阳性，保留 NOT_TRIGGERED。20次未自然观察到FullScreenAdActivity，不代表路径不存在或已经执行finish。

**16.5.1 / 16.6.1 Reply已恢复INSTALLED，自然赞助实际移除尚未观察到。** [Reply专项报告](issue16x_reply_sponsor_research_report.md)记录两版首次解析/缓存启动，以及15.9.0旧目标回归，均为READY和零framework。16.6.1还验证普通评论、内联楼中楼和GUI关闭Reply后的DISABLED；未发布评论或订阅通知。13.1.1不在本轮范围。

[Issue #6报告](issue6_native_loader_reliability_report.md)记录前一候选的16.6.1 native miss、相关推荐开启、cache hit、设置与Feed/Splash smoke；本次未改native实现，其测试继续通过。独立Android ART测试验证真实so首次提取、旧文件复用、损坏恢复、一次重试及永久失败上限，不等同酷安UID/SELinux域内fallback全链验收。没有切换应用列表权限。

原配置、原登录态保留，未清数据或重启手机/LSP；没有Frida/IDA动态附加。工程验收不代表已确定报告者设备的底层失败原因，也不代表风控消失。

最终元数据包在16.6.1完成缓存启动、相关推荐开启后的重新解析、原配置恢复后的缓存启动，均为READY、零framework、Reply INSTALLED。RelatedData getter安装正常；收尾保持原账号、默认三项开启和五个可选项关闭，配置文件哈希及revision与原始值一致。

## 构建与候选校验

- Debug / Compatible / Release：各268 tests，0 failures / errors / skipped；Release lint：0 errors、13 warnings。
- 源码提交：6c3397b；继承Issue #6检查点72cc5f7与Issue #5检查点8a7dab3。
- APK：coolapk-purifier-v2.2.1.apk，1,157,985 bytes。
- APK SHA-256: `56FFD126A3A69F718D0FD966F10F291090358839032290F78F3F49D45C61B709`
- Signer certificate SHA-256: `12B482270B217A377CF3881382C86EE9F1C3E2B8E4BE25EE5118F327F2858875`
- stageReleaseCandidate校验签名及单一证书集合，最终16.6.1安装后base.apk hash一致。三版本已测包与提交后候选ZIP内容仅version-control-info元数据不同，DEX、资源、so完全相同；最终元数据包另在16.6.1验证。后续仅补报告的提交不重打包。

升级模块后正常重启作用域应用酷安即可，不需清数据。自主验收完成后等待用户人工验收与发布授权。
