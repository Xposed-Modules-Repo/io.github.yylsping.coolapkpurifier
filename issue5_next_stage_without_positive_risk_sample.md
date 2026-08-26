# Coolapk Purifier Issue #5 — 下一阶段提示词
## 无风险阳性样本条件下继续闭合风控链

### 总目标

当前不再把“取得一次风险阳性响应”作为下一阶段前提。

风险触发具有明显非确定性，而且主动追求阳性可能以账号/device/session 风控为代价，成本过高。

因此本阶段必须在 **不主动触发风控、不修改 Mode A、不绕过安全逻辑** 的条件下，继续通过：

- 静态分析；
- 已有正常流量；
- 历史 APK/DEX/IDB；
- 只读 IDA Pro MCP；
- 已有本地日志/缓存；
- 版本差分；

推进根因归因。

---

# 一、已有事实：不要重新验证

把以下内容视为已完成：

1. Mode A 正式冻结。
2. `shouldShowAd` / `KEY_SPLASH_DECISION` 已删除。
3. Settings 5 个长期 framework Hook 已删除。
4. `LayoutInflater.inflate` / `View.setTag` fallback 已删除。
5. 当前相较 2.1.2 没有默认新增的长期 framework Hook。
6. 唯一默认新增 framework Hook：
   - `ClassLoader.loadClass(String)`
   - `ClassLoader.loadClass(String, boolean)`

   仅供 Reply discovery，约 200 ms 后全部卸载，`frameworkActive=false`。
7. Feed after-filter：
   - 先完整执行宿主；
   - 不改原 List；
   - 不改 response；
   - 不改 cursor/page；
   - 不伪造 impression；
   - 被删广告只是不进入后续展示/曝光。
8. NetHT 已 E3 闭合：

```text
pm list packages -f --show-versioncode -U -3
→ path/package/versionCode/UID
→ cache/enrich
→ AndroidSuspiciousInfo.installedApk
→ protobuf field 12
```

9. NetHT security blob getter 已 E3 闭合：

```text
AndroidSuspiciousInfo serializer
→ JNIFactory.aebd1811194e82d9(String)[B
```

10. HTTP attach point 已 E3 定位：

```text
DDI enabled
→ x-app-device 尾部固定 64-char 子字段
```

11. 当前仍为 E2：

```text
JNIFactory.aebd...(String)[B
→ Java wrapper
→ x-app-device final64
```

12. 已确认 NetHT 采集 LSPosed、Zygisk、ART odex、`libandroid_runtime.so` mapping、framework JAR mount 等信号。
13. `libmetasec_ml.so` 已确认 root/Magisk/ADB-root/automation-app 风险数据采集。
14. 12 份普通 Feed 响应没有风险专用 entity/template。
15. APK/恢复 DEX 中没有风险文案、显式 risk key、专用 risk builder。
16. 风险行来源保持 `UNKNOWN`。

不要因为没有阳性样本就把未知边强行补成结论。

---

# 二、本阶段禁止事项

不得把本阶段目标变成“设法触发风控”。

不要：

- 主动制造账号风险；
- 用测试账号反复撞阈值；
- 修改 security payload；
- spoof installed packages；
- 修改 `x-app-device`；
- patch NetHT；
- NOP 检测；
- 隐藏 LSPosed/Zygisk；
- 改模块包名用于规避；
- Hook risk function 返回正常；
- 修改请求来逃避检测；
- 为得到阳性而恢复 Frida/HTTP Toolkit 注入目标进程。

本阶段只做：

```text
read
trace
compare
attribute
prove
bound
```

---

# 三、Priority 1：恢复 installedApk 精确入选谓词

从：

```text
sub_26FCEC
→ sub_273B74
→ sub_277278
→ sub_276E54
→ sub_2FA14C
```

继续追。

必须恢复：

- cache schema；
- cache key；
- cache hit/miss；
- processed 标志；
- incremental 逻辑；
- event type；
- config gate；
- eligibility flag；
- risk annotation；
- TTL/time gate；
- append/skip 分支。

最后输出真实控制流支持的伪代码：

```text
bool shouldAppendInstalledApk(record, cache, eventType, config, time) {
    ...
}
```

未恢复条件写 `unknown_condition_X`。

最终回答：

1. collector 是否理论上能看到本模块包；
2. “被 collector 看到”与“进入本次 field 12”是否不同；
3. field 12 是 full snapshot 还是 incremental subset；
4. 哪些条件决定某包进入 payload。

---

# 四、Priority 2：解析 field 12 元素真实内容

继续恢复：

```text
marker + {MD5(payload)} + payload
```

确定 payload 是否包含：

- packageName；
- apkPath；
- versionCode；
- UID；
- signature/hash；
- requested permissions；
- suspicious permission；
- file permission；
- apkLibName；
- risk annotation；
- install/update time；
- 其它字段。

输出：

```text
InstalledApkElement {
    segment/offset:
    semantic:
    evidence:
}
```

只做语义归因，不做隐藏或删改。

---

# 五、Priority 3：把 aebd... → x-app-device 从 E2 补到 E3

优先搜索：

```text
JNIFactory.aebd1811194e82d9
(String)[B
x-app-device
useDDI
useDDISessionId
Base64
reverse
request header
interceptor
header builder
```

目标直接证据：

```text
invoke aebd...
→ process status/payload
→ encode/transform
→ append ';'
→ append final64
→ putHeader("x-app-device", ...)
```

如果现有 DEX 找不到：

1. 列出已经搜索过的 DEX；
2. 根据 ClassLoader/JNI wrapper 证据确定缺失代码来源；
3. 只恢复那个具体动态 DEX；
4. 不重新全量 dump；
5. 不修改 Mode A。

若仍无法恢复，保持 E2。

---

# 六、Priority 4：Coolapk / NetHT 跨版本静态差分

若本地已有历史 APK/so，至少比较：

```text
Coolapk 16.5.1
Coolapk 16.6.1
一个 Issue #5 前长期认为正常的较早版本
```

比较：

## Native

- `libNetHTProtect.so`
- `libmetasec_ml.so`
- `libnesec.so`

记录：

- SHA-256；
- size/version；
- RegisterNatives table；
- `aebd...`；
- installedApk collector；
- LSPosed/Zygisk probe；
- ART probe；
- schema；
- output wrapper。

## Java/Dex

比较：

```text
useDDI
useDDISessionId
useDDIEvent
x-app-device builder
MainInit
Feed parser
generic card renderer
```

输出 capability diff 表。

如果 security collector 基本一致，提高 `server-side policy / rollout / threshold change` 的解释力。

如果 collector/transport 明显变化，给出具体新增路径和版本。

---

# 七、Priority 5：审查通用 server-driven Feed/card 渲染

当前已削弱：

```text
hard-coded risk text
explicit risk template
dedicated risk builder
```

现在回答：

> 服务端是否可以利用普通 card/entity 直接形成类似风险提示行？

审查：

```text
Entity
Card
entityType
entityTemplate
title
subTitle
message
description
extraData
rawData
style
generic ViewHolder
generic binder
template registry
```

追：

```text
Feed JSON
→ model
→ generic card
→ ViewHolder/binder
→ text fields
```

最终分类：

- S1：普通 card + text 即可；
- S2：需要已有 template ID，但无需 risk 专用客户端代码；
- C1：客户端必须收到 flag 后主动构建；
- UNKNOWN。

不要因为“可以”就写成“风险行一定这样产生”。

---

# 八、Priority 6：一次性排查宿主私有目录 collector

搜索/追踪：

```text
getFilesDir
File.list
File.listFiles
opendir
readdir
/data/user/0
/files
/cache
```

重点检查是否进入：

```text
filePermisson
ExtDataEntry
security event queue
protobuf string field
risk database
```

目标闭环：

```text
private file name
→ collector
→ serializer
```

如果没有，保持未确认，不做文件隐藏。

---

# 九、Priority 7：Security Signal Influence Matrix

维护：

| Signal | Collector | Local cache | Payload field/event | Transport | Timing | Evidence |
|---|---|---|---|---|---|---|

至少包括：

- LSPosed；
- Zygisk；
- ART odex；
- framework mount；
- third-party APK；
- root/Magisk；
- ADB-root；
- automation app；
- private files；
- Feed ad missing exposure。

证据等级：

```text
E3 direct
E2 strong structural
E1 capability/correlation
E0 hypothesis
```

每轮优先补边，不要无限增加新假设。

---

# 十、IDA Pro MCP：默认允许，严格限域

用户本机有 IDA Pro，默认允许使用 IDA Pro MCP。

## 默认首选 IDB

```text
.tmp_audit/native/libNetHTProtect.so.i64
```

第二：

```text
.tmp_audit/native/libmetasec_ml.so.i64
```

`libnesec.so.i64` 只有已有直接证据表明目标链进入壳 payload 时才使用。

## 允许

- decompile；
- disassembly；
- xref；
- callers/callees；
- indirect call；
- vtable/function pointer；
- data-flow；
- object field read/write；
- struct reconstruction；
- JNI RegisterNatives；
- protobuf field reconstruction；
- rename/comment/type/struct 等 IDB 分析元数据修改。

## 禁止

- patch bytes；
- assemble；
- NOP；
- 修改 native return；
- 修改 JNI table；
- 修改 security payload；
- 修改检测逻辑；
- 导出 patched `.so`；
- 设计反检测 patch。

即：

```text
允许修改 IDB 的分析元数据
禁止修改真实程序机器码
```

---

# 十一、IDA MCP 每次分析前必须声明

```text
QUESTION:
KNOWN START:
EDGE TO CLOSE:
EXPECTED EVIDENCE:
```

例如：

```text
QUESTION:
field 12 中某个第三方包为何被 append？

KNOWN START:
sub_2FA14C append point

EDGE TO CLOSE:
cache/enrichment record → append predicate

EXPECTED EVIDENCE:
direct branch conditions / field reads
```

禁止无目标地“继续把整个 so 逆向一遍”。

---

# 十二、不主动找阳性的观察原则

Mode A 可继续正常使用，但只作为自然观测：

```text
出现 → 记录
不出现 → 不做结论
```

不要：

- 提高使用频率；
- 刻意重复敏感操作；
- 为捕获样本改变账号行为；
- 用账号风控换证据。

---

# 十三、何时才重新修改模块

默认：

```text
NO MODULE CHANGE
```

只有出现以下直接证据之一：

### M1
当前仍存在的某 Hook 被 security collector 明确观察。

### M2
模块特征文件被直接写入 security payload。

### M3
Feed after-filter 状态被安全逻辑直接消费。

### M4
Reply 临时 ClassLoader Hook 与启动安全 collector 存在直接边。

### M5
当前模块包名被本地规则明确匹配并转成 risk field。

否则继续冻结 Mode A。

---

# 十四、本阶段停止条件

完成以下任意两个里程碑后，先更新总报告：

## Milestone A
`installedApk exact inclusion predicate` 闭合到 E3。

## Milestone B
field 12 element structure 足以明确服务器实际获得哪些包信息。

## Milestone C
`aebd... → x-app-device final64` Java 边补到 E3。

## Milestone D
完成 Coolapk/NetHT 跨版本 capability diff。

## Milestone E
通用 card 渲染模型收敛到 S1/S2/C1/UNKNOWN。

## Milestone F
私有目录 collector 被确认或得到较强静态反证。

---

# 十五、最终输出格式

## 1. New E3 edges

```text
A → B
Evidence:
IDA/file/address:
```

## 2. InstalledApk inclusion predicate

伪代码 + 每个条件证据。

## 3. Field 12 element schema

结构和语义。

## 4. JNIFactory → x-app-device

当前 E2/E3 状态。

## 5. Cross-version security diff

表格。

## 6. Generic Feed/card model

S1 / S2 / C1 / UNKNOWN。

## 7. Private-directory collector

CONFIRMED / NOT FOUND / UNKNOWN。

## 8. Security Signal Influence Matrix

完整更新。

## 9. Root-cause ranking

重新排序，但不依赖风险阳性。

## 10. Module change recommendation

默认：

```text
KEEP MODE A FROZEN
```

若建议修改，必须引用新的直接静态证据。

---

# 最终原则

> 下一阶段不靠“触发一次风控”推进。

> 核心任务是把已经确认存在的安全信号，继续追到明确的数据内容、传输边界和版本变化。

> 服务端黑盒可以保持黑盒；只要客户端侧 collector → payload → transport 足够闭合，就能形成高质量根因判断。

> 不要用规避、隐藏或篡改实现替代证据。
