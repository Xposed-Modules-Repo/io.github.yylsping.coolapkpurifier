# Issue #5 — A/B/C 全 E3 后的下一阶段结论

当前客户端主链已经闭合：第三方包枚举会在真实 predicate 下进入 installedApk field 12；field 12 元素含 packageName、权限标注、label 和可选 acts；随后进入 JNIFactory.aebd...，生成 nuid，并被组装进 X-App-Device 后长期随 Coolapk API 请求上行。

因此下一阶段不再继续追 installedApk 主链，也不继续修改 Mode A。当前更有价值的是五件事：

1. **Remote Config Activation**：把 bit11/bit22/cfg[100]/512/624/674/712/784/788/823/gt 映射到真实 remote key 和当前值，明确真实会话到底走 incremental 还是 gt full snapshot、权限 annotation 是否实际开启。
2. **Cross-Version Security Diff**：比较一个 Issue #5 前的旧版本、16.5.1、16.6.1 的 NetHT/metasec/runtime DEX capability，区分 client capability change、remote config rollout 和 server-side policy change。
3. **Generic Server-Driven Card**：证明普通 card/entity 是否足以让服务端直接显示任意文本，从而在无风险阳性样本时继续缩小风险行来源。
4. **Private Directory Collector**：从 filePermisson/ExtDataEntry/event 反追宿主私有目录文件名是否进入安全 payload；若无直接边，保持 UNKNOWN，不做隐藏。
5. **nuid Lifecycle**：确定 nuid 的进程/持久化/刷新语义，解释“一次性 field12 build”如何转化为后续每请求持续携带的设备身份。

当前根因判断应更新为：

- “客户端会上传第三方包名与环境安全信号”已经是结构性事实；
- “服务器可以按包名远程匹配”在技术上成立，但“已经针对本模块建立规则”仍无直接证据；
- 当前最关键的时间相关性问题是：客户端 capability / remote config 是否在近期发生变化；
- 如果跨版本客户端能力基本不变，则 server-side rollout / threshold 的解释力显著上升；
- 当前没有新证据要求修改 Mode A。

工程决策保持：

```text
KEEP MODE A FROZEN
NO MODULE PATCH
```
