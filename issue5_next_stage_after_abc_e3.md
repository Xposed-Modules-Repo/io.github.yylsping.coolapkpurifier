# Coolapk Purifier Issue #5 — 下一阶段审计提示词
## Milestone A/B/C 全 E3 后：Remote Config / Cross-Version / Generic Card / Private Collector

## 0. 总原则

当前已经完成并作为既定事实继承：

- Milestone A：installedApk exact inclusion predicate = E3
- Milestone B：field 12 element structure = E3
- Milestone C：installedApk → JNIFactory.aebd... → nuid → X-App-Device → HTTP = E3
- Mode A 继续冻结，不修改模块代码
- 不主动制造风险阳性
- 不 patch NetHT / metasec / Shuzilm
- 不 spoof installed packages
- 不修改 X-App-Device、nuid、token
- 不通过改模块包名验证服务器规则
- 不重复普通阴性 Feed 抓包
- 不研究如何绕过易盾 /proc 防护

已经闭合的主链不要重复：

```text
pm list -3
→ ingest/enrich/cache
→ installedApk field 12
→ serializer
→ JNIFactory.aebd1811194e82d9(String)[B
→ AntiCheatResult
→ nuid
→ Shuzilm DID + nuid
→ X-App-Device
→ CoolMarketHeaderInterceptor
→ HTTP
```

本阶段目标从“客户端是否采集”切换为：

1. 真实配置下到底启用哪些分支；
2. 这些安全能力跨 Coolapk 版本是否发生变化；
3. 风险行能否由通用 server-driven card 渲染；
4. 宿主私有目录/模块文件是否进入独立 collector；
5. nuid 的生命周期和刷新语义。

---

## 1. 已确认事实

### installedApk

默认模式：

```text
每个第三方包在单个进程生命周期内最多进入 field 12 一次
```

特殊全量模式：

```text
eventType == 6
&& config["gt"] == 1
&& cfg[788]
→ fresh pm collect
→ full snapshot
```

完整 ingest / enrich / append predicate 已 E3，不再重做。

### field 12

已 E3：

```text
"$S_BF#A" + "{" + md5hex(payload) + "}" + payload
```

其中：

```text
payload =
packageName
+ permission annotations
+ optional label
+ optional "@@as:<acts>"
```

服务器经 field 12 不直接得到：

```text
apkPath
UID
versionCode
signature
完整权限列表
minSdk/targetSdk
```

### transport

已 E3：

```text
AndroidSuspiciousInfo
→ JNIFactory.aebd...
→ AntiCheatResult
→ nuid
→ X-App-Device
→ Base64
→ reverse
→ Request.Builder.header(...)
```

并已区分三条通道：

```text
X-App-Device / nuid
_v2_post_token
ddid Cookie
```

---

# 2. Priority 4A — Remote Config Activation

这是下一阶段第一优先级。

当前 predicate 已经没有 unknown_condition，但还必须把真实配置映射闭合，否则无法知道真实会话走的是哪条分支。

目标配置：

```text
cfg bit11
cfg bit22
cfg[100]
cfg[512]
cfg[624]
cfg[674]
cfg[712]
cfg[784]
cfg[788]
cfg[823]
config["gt"]
```

## 任务

### 2.1 恢复 key 映射

对每个 selector/offset 找到：

```text
native accessor
→ config object
→ Java/remote key
→ default value
```

### 2.2 恢复当前真实值

优先来源：

- 已有正常 `/v6/main/init` 响应
- 已保存本地配置/cache
- 已恢复 runtime DEX
- IDA 中配置 parser
- 已保存日志

不要为了取值主动制造新风险会话。

### 2.3 输出表

| Native selector | Remote/Java key | default | current observed | source | effect | evidence |
|---|---|---:|---:|---|---|---|

必须回答：

1. 当前 16.6.1 是否实际打开 `"gt"`；
2. cfg[788] 当前值；
3. bit22 是否开启；
4. permission annotations 当前是否实际进入 field 12；
5. cfg[512]/cfg[712]/cfg[100] 的当前实际参数；
6. eventType 6 当前默认走 full 还是 incremental。

静态不可得则写：

```text
UNKNOWN
```

不得猜默认值。

---

# 3. Priority 4B — Cross-Version Security Capability Diff

目标：

> 判断 Issue #5 的近期出现更像客户端能力变化、远程配置变化，还是服务端策略/阈值变化。

如果本地存在历史 APK/so，至少比较：

```text
一个 Issue #5 前长期认为正常的旧版本
Coolapk 16.5.1
Coolapk 16.6.1
```

若缺某版本：

```text
BLOCKED_BY_MISSING_ARTIFACT
```

然后继续其它 Priority，不要无目的联网下载第三方 APK。

## 3.1 Native diff

比较：

```text
libNetHTProtect.so
libmetasec_ml.so
libnesec.so
```

记录：

- SHA-256 / size / build metadata
- RegisterNatives table
- `aebd...`
- installedApk collector
- field 12 schema
- +280/+281 promotion
- `gt` full snapshot
- LSPosed probe
- Zygisk probe
- ART/odex probe
- framework mount probe
- uid_match
- mis
- changedPackages
- config lookup table

## 3.2 Runtime DEX / Java diff

比较：

```text
NetEaseProtectSDKManager
HTProtect
WatchMan
RequestSessionIDUpdater
ShuzilmSDKManager
AppConfig / X-App-Device builder
CoolMarketHeaderInterceptor
MainInit.useDDI
MainInit.useDDISessionId
PostToken.*
```

## 3.3 输出 capability 表

| Capability | old | 16.5.1 | 16.6.1 | first changed version | evidence |
|---|---:|---:|---:|---|---|

不要只说“so hash 不同”。

必须说明：

```text
能力是否新增 / 删除 / 逻辑变化 / 仅配置变化
```

### 结论规则

若客户端能力长期一致：

```text
server-side policy / rollout / threshold
```

解释力上升。

若代码一致但 remote config 改变：

```text
remote configuration rollout
```

单独列为高价值解释。

若 collector / transport 近期新增：

指出准确版本、函数和差分。

---

# 4. Priority 5 — Generic Server-Driven Card Model

当前已经削弱：

```text
hard-coded risk text
explicit risk key
dedicated risk template
dedicated risk builder
```

下一步不要等阳性样本，而是回答：

> Coolapk 普通 Feed/card 框架是否允许服务端通过现有通用 entity/card 字段直接渲染任意提示文字？

追：

```text
/v6/main/indexV8
→ JSON/model parser
→ Entity/Card
→ entityType/entityTemplate
→ template registry
→ ViewHolder/binder
→ TextView
```

重点字段：

```text
title
subTitle
message
description
text
extraData
rawData
entityTemplate
entityType
style
url
```

最终分类：

### S1
普通 card + server text 即可渲染任意提示。

### S2
需要一个已有通用 template ID，但无需 risk 专用客户端逻辑。

### C1
必须先收到某 flag，然后客户端主动构造 entity/card。

### UNKNOWN
仍不足。

要求：

- 用正常 Feed/card 样本验证 parser/binder；
- 可做离线 JSON/model 结构分析；
- 不需要风险阳性；
- “框架具备能力”不等于“风险行确定走此路径”。

---

# 5. Priority 6 — Private Directory / File Collector

field 12 已确认不包含模块文件名，因此如果存在模块文件采集，应走其它字段/event。

优先入口：

```text
AndroidSuspiciousInfo.filePermisson
AndroidSuspiciousInfo.ExtDataEntry
其它 repeated string
event 2000
```

反向找 producer：

```text
getFilesDir
File.list
File.listFiles
opendir
readdir
stat
fstatat
/data/user/0
/data/data
/files
/cache
```

重点确认是否存在：

```text
filename/path
→ collector
→ serializer/event
```

需要关注的字符串只用于归因：

```text
coolapk_purifier_*
libdexkit*
```

输出只能是：

```text
CONFIRMED
NOT_FOUND
UNKNOWN
```

禁止：

- 因理论可读就判定已上传；
- 设计文件隐藏；
- 改名规避；
- patch collector。

---

# 6. Priority 7 — nuid Lifecycle / Refresh Semantics

当前已 E3：

```text
aebd blob
→ AntiCheatResult
→ nuid
→ X-App-Device
```

继续回答：

1. nuid 是否每进程重新生成；
2. 是否持久化；
3. AtomicReference 之外是否有 SharedPreferences/DB；
4. refresh 条件；
5. businessId 来源和是否固定；
6. timeout/fallback 语义；
7. `gt` 是否只影响生成时的 security data；
8. installedApk 增量状态与 nuid refresh 的关系；
9. 进程重启后 cache 清空是否意味着下一次 nuid 再次包含第三方包；
10. `_v2_post_token` 是否共享同一底层 builder，还是独立 event/token 语义。

目标是解释：

```text
一次 field 12 build
→ 如何成为后续每个请求持续携带的设备身份输入
```

不需要推断服务端内部 nuid 算法。

---

# 7. Optional — App UID Package Visibility

仅在能够做到：

```text
不启动 Coolapk
不发网络请求
不修改权限
```

时执行。

目标：

> 模拟 Coolapk UID 执行 NetHT 相同的 `pm list packages ... -3`，统计当前权限状态下实际可见多少第三方包。

只记录：

```text
count
是否包含本模块
与 root/system 可见数量的差异
```

如果无法安全模拟：

```text
SKIP
```

这不是必做项。

---

# 8. Remote Package-Name Matching 的证据边界

当前 E3 已证明服务器输入包含：

```text
packageName
```

因此允许结论：

```text
服务器技术上具备按包名做远程匹配的输入。
```

但禁止结论：

```text
服务器已针对 io.github.yylsping.coolapkpurifier 建立规则。
```

除非未来存在直接服务端/配置/响应证据。

不要通过改模块包名实验这个假设。

---

# 9. Security Signal Influence Matrix

更新：

| Signal | Collector | Payload | Transport | Lifetime | Version introduced | Remote configurable | Evidence |
|---|---|---|---|---|---|---|---|

至少包含：

- third-party packageName
- permission annotations
- label
- acts
- LSPosed
- Zygisk
- ART odex
- framework mount
- uid_match
- mis
- changedPackages
- root/Magisk
- automation apps
- private files
- nuid
- `_v2_post_token`
- ddid

本轮新增重点列：

```text
Version introduced
Remote configurable
```

最终目标是区分：

```text
client capability change
vs
remote config change
vs
server policy change
```

---

# 10. IDA Pro MCP — 默认允许，严格限域

默认允许 IDA Pro MCP。

## 允许

- decompile
- disassembly
- xref
- callers/callees
- indirect call
- data-flow
- struct reconstruction
- JNI
- protobuf
- config parser
- rename/comment/type
- 多版本 IDB 对照

## 禁止

- patch bytes
- NOP
- assemble
- 修改 return
- 改 security payload
- 改 nuid/token
- 绕过 probe
- 导出 patched binary

P4 跨版本可以同时打开两个 IDB，但必须围绕同一已知 capability 做对应比较，不允许无目标全库漫游。

每次分析前必须声明：

```text
QUESTION:
KNOWN START:
EDGE TO CLOSE:
EXPECTED EVIDENCE:
```

---

# 11. `/proc` 与 `/data/local/tmp/processing` 异常

## /proc

已知连续读取会触发保护升级。

禁止研究绕过。

只有确实缺 P4/P5 必需 runtime DEX 时，才沿已验证流程：

```text
明确目标
→ 冷启动
→ 单窗口
→ 一次性读取
→ exec-out
→ 设备零落盘
```

## processing 自动删除

当前不属于 Issue #5 主线。

若单独排查，只做：

```text
不启动 Coolapk
→ 创建无敏感内容 root-owned sentinel
→ 观察是否仍删除
```

用于区分：

```text
通用 ROM/管理器/守护清理
vs
与 Coolapk 活跃相关
```

不要设计规避清理机制。

---

# 12. 模块修改门槛

默认：

```text
KEEP MODE A FROZEN
NO MODULE PATCH
```

只有出现：

### M1
当前 Hook → security collector

### M2
模块私有文件 → security payload

### M3
Feed after-filter → security validator

### M4
Reply temporary ClassLoader hook → security probe

### M5
本模块包名被客户端本地规则专门匹配

才重新提出模块修改。

“服务器可能按包名远程匹配”本身不触发改包名。

---

# 13. 本阶段停止条件

完成任意 3 项后先更新总报告：

### D1
Remote Config mapping + current values 完成。

### D2
Cross-version capability diff 完成。

### D3
Generic card model 收敛到 S1/S2/C1/UNKNOWN。

### D4
Private-directory collector 收敛到 CONFIRMED/NOT_FOUND/UNKNOWN。

### D5
nuid lifecycle/refresh 语义闭合。

然后重新做根因排序，不要求风险阳性样本。

---

# 14. 最终输出格式

## 1. Remote Config Activation Table

## 2. Cross-Version Security Capability Diff

## 3. Generic Card Model
S1 / S2 / C1 / UNKNOWN

## 4. Private Directory Collector
CONFIRMED / NOT_FOUND / UNKNOWN

## 5. nuid Lifecycle
ASCII 调用图 + 生命周期

## 6. Security Signal Influence Matrix
加入 version + remote-config 两列

## 7. Updated Root-Cause Ranking
明确区分：
- client capability
- remote config
- server policy
- module-specific behavior

## 8. Module Recommendation

默认：

```text
KEEP MODE A FROZEN
```

任何修改建议必须引用新的 direct edge。

---

# 最终原则

> Milestone A/B/C 已经证明“客户端看到了什么，并怎样把它送出去”。

> 下一阶段要回答的是“这些能力何时启用、什么时候出现、不同版本是否变化、风险 UI 是否可以由通用服务端内容驱动”。

> 不需要拿账号风控作为实验代价。

> 不用规避实现替代证据。
