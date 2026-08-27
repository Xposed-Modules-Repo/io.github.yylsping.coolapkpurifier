# 酷安净化 2.2.1 — 本地 Release Candidate

版本：2.2.1 / versionCode 11。尚未发布，等待用户人工验收和单独的远程写入授权。

## 改动

- 正常 READY 清理完成后不保留 framework Hook；非 Xposed 的 Splash lifecycle guard 继续处理已解析及精确历史类名。注册失败时保留必要 Instrumentation 兜底并如实报告，不为零 Hook 指标牺牲覆盖。
- Reply 独立显示本次启动的安装状态。目标不可达不会把 core READY 误报成全部功能生效，也不会改写用户开关。
- Reply 重试最多 4 次定时、3 次 resume（至少间隔 30 秒）、总预算 120 秒；任一预算耗尽即注销回调并停止任务。
- 修复 15.9.0 class-only Reply 目标被缓存校验拒绝的问题；校验与实际安装共用精确类名和 bind 方法形状契约。
- 修复退出设置后再次进入时入口偶发缺失：每次页面 resume 最多尝试 5 次，安排的延迟累计 1.85 秒；pause/destroy 时取消，成功后停止，不增加 framework Hook。

## 兼容性与已知限制

本轮以同一候选覆盖安装项目内实际存在的 15.9.0、16.5.1、16.6.1；逐轮结果和验收边界见 [发布加固报告](issue5_mode_a_zf_release_hardening_report.md)。13.1.1 / 16.1.2 仅为历史曾验证版本，本轮没有对应 APK，未重新验证。

**默认开启的 Reply 专用赞助过滤在 16.5.1 / 16.6.1 为 UNAVAILABLE，当前不生效。** 设置页会显示“本次启动未找到适配目标，当前未生效”。核心 Splash/Feed 的 READY 不代表该功能可用。没有可靠替代目标，因此没有猜类名或恢复长期 ClassLoader Hook。

15.9.0 已验证 Reply 目标安装、缓存重启及正常评论浏览；没有自然捕获专用 holder 的 sponsor 阳性移除，不声明全场景覆盖。FullScreenAdActivity 未自然触发，只有 lifecycle 策略单测和覆盖日志验证。五项可选功能保持用户原有关闭状态，本轮未做开启后的实机功能验收。

全程保留酷安账号数据和模块配置，没有清数据、卸载酷安、重启手机/LSP 或修改作用域。一次用于取证的 Frida attach 后旧进程退出，原因未确定；探针未执行，该过程不计入正式 smoke，后续正式验收进程没有 Frida。工程验收不代表服务端风控问题已解决。

## 构建与候选校验

- Debug / Compatible / Release：各 234 tests，0 failures / errors / skipped。
- 源码提交：`f6df56f`；此提交之后构建并冻结候选，旧候选不计入最终矩阵。
- APK：`coolapk-purifier-v2.2.1.apk`，1,138,569 bytes。
- APK SHA-256: `706C622F50B9825319F20B52D5D27FC9BEBCBE331C5E833442F1B50888A06E94`
- Signer certificate SHA-256: `12B482270B217A377CF3881382C86EE9F1C3E2B8E4BE25EE5118F327F2858875`
- APK v2 签名校验通过；交付冻结为本轮实际安装并验证的原始构建产物，不混用仅 VCS 元数据变化的后续重打包件。

升级模块后只需正常重启作用域应用酷安；不要为了本候选清除酷安数据。发布决定及最终设备状态以验收报告为准。
