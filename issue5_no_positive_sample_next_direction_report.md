# Issue #5：无风险阳性样本条件下的下一阶段方向报告

## 结论摘要

当前 Mode A 应继续冻结，不建议继续删除模块功能，也不建议为了验证而主动追求“风险阳性”。

现有证据已经把模块侧与宿主侧的关键链条推进到：

```text
第三方包 collector
  → AndroidSuspiciousInfo.installedApk
  → protobuf field 12
  → JNIFactory.aebd1811194e82d9(String)[B
  → x-app-device 尾部 64-char DDI/DDI-session 字段
  → Coolapk Feed/account/sync 请求
  → [server black box]
  → 风险状态 / 风险行（尚未闭合）
```

其中真正仍值得投入、且不需要拿账号风控做代价的方向，不再是“继续找哪个 Hook 会触发风控”，而是：

1. 精确闭合 `installedApk` 的入选条件和实际数据语义；
2. 把 `JNIFactory.aebd... → x-app-device[final64]` 的 Java 中间边从 E2 补到 E3；
3. 做 Coolapk / NetHT 的跨版本静态差分，回答“为什么近期开始出现”的时间相关性问题；
4. 审查 Feed 的通用 server-driven card 渲染能力，缩小风险行的可能生成模型；
5. 静态排查宿主私有目录/文件名是否进入安全 collector；
6. 建立“已确认信号 → payload 字段”的 influence matrix，而不是继续猜服务端阈值。

## 1. 为什么现在不应该继续修改模块

当前相较 2.1.2：

- 默认长期 framework Hook 已无新增项；
- `shouldShowAd` 已彻底退出正式实现；
- Settings 的 5 个长期 framework Hook 已删除；
- `LayoutInflater.inflate` / `View.setTag` fallback 已删除；
- 唯一默认新增 framework Hook 是 Reply discovery 的两个临时 `ClassLoader.loadClass`，约 200 ms 后退休并确认 `frameworkActive=false`；
- Feed after-filter 已确认不改 response/cursor/page，也不伪造 impression。

因此继续削减模块会进入明显的边际收益递减阶段。

下一步如果没有新的直接静态证据，修改模块反而会：

- 扩大变量；
- 降低功能；
- 破坏与 2.1.2 的连续性；
- 让后续归因更困难。

所以当前正式策略应是：

```text
Mode A = Frozen Baseline
```

只有出现新的直接调用链证据，才重新打开模块补丁讨论。

## 2. 第一优先级：闭合 installedApk 的“入选条件”

已经知道 NetHT 的源集合来自：

```text
pm list packages -f --show-versioncode -U -3
```

也就是第三方包集合。

但当前仍然不知道：

> 某一次 security payload 中，哪些第三方包最终会进入 field 12？

这是无需风险阳性也能继续推进的最高价值问题。

需要反向/正向追：

```text
sub_26FCEC
→ sub_273B74
→ sub_277278
→ sub_276E54
→ sub_2FA14C
```

重点恢复：

- cache key；
- incremental 条件；
- event type；
- eligibility flag；
- 时间/TTL；
- risk annotation；
- processed 标志；
- 空 annotation 是否仍 append；
- 哪些条件会跳过记录。

最终应得到接近以下形式的逻辑：

```text
if third_party_package
and cache_state == ...
and event_type in ...
and config_flag == ...
then append installedApk(...)
```

然后才能回答真正重要的问题：

> 一个普通安装的 Xposed 模块包，在默认启动过程中是否理论上会进入 `installedApk` payload？

这里只做归因，不做包名隐藏、伪造或绕过。

## 3. 第二优先级：解析 field 12 元素的真实内容

当前知道元素不是简单的 `InstallApkInfo` protobuf 子消息，而是 framing + MD5 + enriched payload。

继续恢复：

```text
marker + {MD5(payload)} + payload
```

确定 payload 内是否实际包含：

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

目标是判断服务器拿到的 field 12 到底能识别到什么程度。

## 4. 第三优先级：补齐 JNIFactory → x-app-device 的 E3 Java 边

当前：

```text
serializer → JNIFactory.aebd... = E3
x-app-device final64 attach point = E3
aebd... → final64 = E2
```

这一条值得补齐，因为它不依赖风险阳性。

优先找：

```text
x-app-device
MainInit.useDDI
MainInit.useDDISessionId
JNIFactory
aebd1811194e82d9
(String)[B
Base64
reverse
header builder
request interceptor
```

目标是直接得到：

```text
invoke JNIFactory.aebd...
→ byte[] status/payload
→ encode/transform
→ append ';'
→ append final64
→ putHeader("x-app-device", ...)
```

如果 wrapper 不在现有 DEX：

- 先检查已有动态 DEX/ART/OAT/壳加载清单；
- 只有证据明确表明 wrapper 位于未恢复代码时，才恢复那个具体动态 DEX；
- 不重新做无目的全量 dump；
- 不修改冻结的 Mode A。

## 5. 第四优先级：做宿主/安全 SDK 跨版本差分

这是当前最有价值的新方向之一。

目标是回答：

> LSPosed/Zygisk/installedApk 等安全采集可能长期存在，为什么 Issue #5 在近期版本/时间窗口才明显出现？

若手头有历史 Coolapk APK/提取物，比较：

```text
16.5.1
16.6.1
以及一个 Issue #5 前长期正常使用的较早版本
```

比较：

- `libNetHTProtect.so` hash/version；
- `libmetasec_ml.so` hash/version；
- NetHT collector；
- `installedApk` schema；
- `JNIFactory` native table；
- DDI 配置 key；
- `MainInit.useDDI*` 行为；
- `x-app-device` 构造逻辑；
- root/Xposed/Zygisk/ART probe；
- 本地 risk DB schema；
- Feed 通用 card parser。

如果这些能力基本一致，更支持：

```text
server-side rule / rollout / threshold change
```

如果 native collector 或 DDI attach 明显变化，则获得新的版本相关解释。

## 6. 第五优先级：审查通用 server-driven card 模型

没有风险阳性样本，仍然可以继续缩小风险行来源。

当前已削弱：

```text
APK 内硬编码风险文案
显式 risk template
专用 risk builder
```

下一步应审查：

> Coolapk 的普通 Feed/card 框架是否允许服务端用已有通用 entity/card 字段直接渲染任意文本行？

重点追：

```text
Feed JSON
→ Entity/Card
→ entityType/entityTemplate
→ generic ViewHolder/binder
→ title/message/description
```

最后把模型收敛为：

- S1：普通 card + text 即可；
- S2：需要已有 template ID，但不需要 risk 专用客户端代码；
- C1：客户端必须收到某 flag 后主动构建；
- UNKNOWN：仍不足。

## 7. 第六优先级：一次性排查宿主私有目录 collector

搜索并追踪：

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
protobuf field
risk database
```

目标只有：

```text
private file name → collector → serializer
```

若没有闭环，保持未确认，不引入隐藏方案。

## 8. 建立 Security Signal Influence Matrix

建议维护：

| Signal | Collector | Payload field/event | Timing | Transport | Evidence |
|---|---|---|---|---|---|
| LSPosed | NetHT | event 2000/security blob | startup | x-app-device/DDI | E3/E2 |
| Zygisk | NetHT | event 2000 | startup | x-app-device/DDI | E3/E2 |
| ART odex | NetHT | event 2000 | startup | x-app-device/DDI | E3/E2 |
| third-party APK | NetHT | installedApk field 12 | collector-dependent | x-app-device/DDI | E3/E2 |
| root/Magisk | metasec | module2.root | collector-dependent | unknown bridge | E3 |
| automation apps | metasec | risk markers | collector-dependent | unknown bridge | E3 |
| module private files | ??? | ??? | ??? | ??? | E0/E1 |

下一阶段目标不是继续增加信号，而是把：

```text
collector → storage → serializer → transport
```

补齐。

## 9. 当前根因模型

目前最合理的是：

```text
LSPosed/Zygisk/ART/root
+
第三方安装包信息
+
其它安全/设备信号
        ↓
NetHT / metasec 聚合
        ↓
DDI / session security transport
        ↓
server-side black box
        ↓
账号/设备/session 风险状态
        ↓
某次后续 Feed 返回或通用 card 展示
```

其中：

- 客户端采集和发送安全信号已有强证据；
- 服务器如何打分不可见；
- 风险行具体生成方式仍未确定；
- 2.2.0 模块侧最明显的长期 Hook 差分已经由 Mode A 清除；
- 不值得通过主动触发风险来填最后一个黑盒。

## 10. 当前工程决策

继续保持：

```text
Mode A frozen
```

暂不新增模块补丁。

只有以下新证据才值得重新修改模块：

1. 当前仍存在的某个 Hook 被安全 collector 明确观察；
2. 模块特征文件被直接写入 security payload；
3. 某个净化行为直接进入安全校验；
4. Reply 临时 ClassLoader Hook 与检测窗口存在直接边；
5. Feed 删除行为被安全逻辑直接消费。
