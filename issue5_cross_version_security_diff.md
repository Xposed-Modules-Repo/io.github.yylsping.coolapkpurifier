# Issue #5 — P4B Cross-Version Security Capability Diff（正式报告）

分析时间：2026-08-27（第四会话）
工件：`history-apks/`（16.6.1=2608212 / 16.5.1 / 15.9.0）
工具：本地 Python+Capstone 自研栈字符串重建器（`.tmp_audit/stackstr.py`）、IDA Pro MCP（16.6.1 `libNetHTProtect.so.i64`，只读）、ELF/dex 解析脚本。
未 patch 任何字节、未在设备上运行旧版本 APK、未触发任何风控操作。

证据等级标注：
- **E3** = 字节级/指令级直接证据（反汇编、重建出的解码字符串、哈希比对）；
- **E2** = 强结构推断（全密钥集群扫描阴性/阳性、字符串池时间线）；
- **UNKNOWN** = 本轮无法闭合。

---

## 1. Artifact Manifest

| 项 | 15.9.0 | 16.5.1 | 16.6.1 (2608212) |
|---|---|---|---|
| APK SHA-256 | `5fc20463…` | `8d8ecbda…` | `946b98b1…` |
| APK size | 99,841,375 | 116,389,418 | 116,558,338 |
| classes.dex（易盾壳） | 94,680,212 B | 101,225,632 B | 101,621,224 B |
| 壳内明文字符串池 | 可读（565,184 条） | 可读（465,858 条） | 可读（479,460 条） |
| arm64 lib 数 | 63 | 52 | 52 |
| **libNetHTProtect.so** | `116cacb8…` 4,575,168 B | `3aed7c73…` 4,624,312 B | `3aed7c73…` 4,624,312 B |
| **libnesec.so** | `22606553…` 953,496 B | 同左 | 同左 |
| **libmetasec_ml.so** | **不在 APK 内** | 不在 APK 内 | 不在 APK 内 |
| QUERY_ALL_PACKAGES（manifest） | ✓ | ✓ | ✓ |
| com.android.permission.GET_INSTALLED_APPS（manifest） | ✓ | ✓ | ✓ |

**要点：**
1. **16.5.1 与 16.6.1 的 libNetHTProtect.so 逐字节相同**（SHA-256 全等）。两版之间 NetHT 能力、探针、config 默认值、序列化器**零差异**。16.6.1 相对 16.5.1 的安全侧变化只能在 Java/远程配置/服务端。
2. **libnesec.so 三版相同**（unchanged baseline，不再深挖）。
3. **libmetasec_ml.so 是穿山甲（Pangle/字节）广告 SDK 的动态下发组件**，路径：
   `/data/user/0/com.coolapk.market/files/pangle_p/com.byted.live.lite/version-211448/lib/libmetasec_ml.so`。
   其 root/Magisk/自动化应用检测链属于广告 SDK 供应链，**版本由 Pangle 组件更新决定，与酷安 APK 版本解耦**（E3，设备实测路径）。跨版本 metasec diff 从 APK 侧**不适用**（by design），记录为 N/A 而非 UNKNOWN。
4. 15.9.0 比 16.5.x 多 11 个 arm64 库（含阿里 SecurityGuard 全家桶 libsg*、SecurityBody Java 字符串如 `OPEN_SECURITYBODY_SCENE_FACERISK`）；16.5.1 起 Java 侧 SecurityBody 字符串消失——**15.9.0→16.5.1 之间发生过安全栈重组**（阿里 SecurityBody Java 层移除，网易 NetHT + Pangle metasec + 数之盾 Shuzilm 为主）。

## 2. Native Capability Diff — libNetHTProtect.so（15.9.0 vs 16.5.1/16.6.1）

方法：所有安全字符串均为栈上逐字节 `mov+strb` 构造 + 简单算术解码（密钥字典已建立：sub 0xb/0xa/0xa(=+6)/0x9/0x5/0x2、xor 0x32/0x28/0x0f/0x3d/0x0c/0x77、rollxor 0x32 等）。对两版 .text 做滑窗栈重建 + 全密钥集群验证。16.6.1 侧全部结果与既有 IDA E3 结论互证一致（pm 命令、"WZD"^0x77=" -3"、cfg[825] 读取等）。

### 2.1 installedApk collector

| 能力 | 15.9.0 | 16.5.1/16.6.1 | 证据 |
|---|---|---|---|
| `pm list packages -f -show-versioncode -U` 构造 | **PRESENT**（xor 0x28，构造点 0x26a928，位于 15.9.0 版 collector 函数 ≈sub_26A8xx 内） | PRESENT（sub 0xb，0x26fd24+） | E3：立即数逐字节解码 |
| **`-3` 第三方范围后缀** | **ABSENT**（构造点立即数流中无 '3' 编码字节 0x1b/0x44；全区域 xor28/xor77 零命中） | **PRESENT**（`"WZD"`^0x77，条件追加 `a2==1`，0x26fe88；与反编译一致） | E3 |
| 语义 | 枚举**不带第三方过滤**（下游过滤方式未恢复，E1） | 枚举范围 = 第三方包（P1 报告已 E3） | — |

**这是本轮最重要的客户端能力时间线结论：installedApk 的"第三方包范围化采集"是在 (15.9.0, 16.5.1] 区间引入的。**
结合 D5（缓存纯内存、每次冷启动首次 build = 事实全量）与 D4（本设备 OS 权限让 collector 只见宿主自己）：
该能力在本设备当前被权限门关闭，但在权限授予的 ROM 上自 16.5.1 起每次冷启动上报全部第三方包名。

### 2.2 注入/环境探针

| Probe | 15.9.0 | 16.5.1/16.6.1 | 证据 |
|---|---|---|---|
| `/proc/self/maps` 解析基座 | PRESENT（4 处构造） | PRESENT（4 处） | E3 重建 |
| **maps 内容搜索 "LSPosed" + 邻接 "is-top-app"** | **ABSENT**（重建+全密钥阴性；15.9.0 用路径法替代） | PRESENT（sub 0x5 "/proc/self/maps"、sub 0x2 "is-top-app"） | E3(新)/E2-(旧) |
| magisk 模块路径法 `zygisk_lsposed`/`taichi` | PRESENT（`/sbin/.magisk/modules/zygisk_lsposed`、`/sbin/.magisk/modules/taichi`，xor 0x8） | PRESENT（`/sbin/.magisk/mod…`、`gisk_lsposed`） | E3 |
| ptrace/fork `zygisk` 行为探针 | PRESENT（"zygisk"^0x32 簇 0x18dbf4 等） | PRESENT（0x29a21c = sub_29A16C 内） | E3 |
| odex 路径探针 `oat/arm/base.odex` | PRESENT（sub 5，0x290100） | PRESENT | E3 |
| **ART Hook 参数探针 `inline-max-code-units=0`** | **ABSENT**（重建+集群阴性） | PRESENT（sub 9："inline-ma"+"x"+"-code-units=0" @0x29412a/0x29492a） | E3(新)/E2-(旧) |
| smaps `Referenced:` 统计 | PRESENT（sub 8，0x295c28） | PRESENT | E3 |
| smaps `Shared_Clean:` | 未确认（重建阴性，可能密钥不同） | PRESENT（xor 0x0f） | E2- |
| `/proc/self/fdinfo/` + `mnt_id`（framework JAR 挂载探针） | **PRESENT**（fdinfo xor 0xc @0x294483；`mnt_id: %d` xor 9 @0x2949d4） | PRESENT（sub 5） | E3 |
| 模拟器检查（"emulator" 明文逐字节） | PRESENT | PRESENT | E3 |
| root 路径表（`/apex/com.android.art/bin/su` 等） | PRESENT | PRESENT | E3 |
| `scrcpy-server.odex`（投屏检测） | PRESENT（0x16ac78） | 未定向验证 | E3(旧) |
| **`shizuku`** | **ABSENT** | PRESENT（`shizuku_starter` @0x16f158） | E2 |
| **frida 相关字符串** | **ABSENT** | PRESENT（`pridas`/`RVMUDPRIDAS` 等片段 @0x8f390/0x2fe0b8） | E2 |

### 2.3 附加采集面（16.6.1 已知，15.9.0 对照）

| 面 | 15.9.0 | 16.5.1/16.6.1 | 证据 |
|---|---|---|---|
| `filePermisson`：`/storage/em(ulat ed)0/Android/data` 外存扫描 | **ABSENT**（"storage/"/"Android/data" 全密钥零命中） | PRESENT（sub 0xa） | E2 强 |
| `changedPackages`（卸载/变更检测） | **ABSENT**（全密钥零命中） | PRESENT（sub 9 @0x2fac70） | E2 强 |
| `/storage/emulated/0/Android/data/%s/files/…` 每包 files 探测 | 未命中 | PRESENT（sub 9 @0x300020） | E2 |
| `BIND_ACCESSIBILITY_SERVICE` 权限标注 | PRESENT（0x2740e0） | PRESENT | E3 |

### 2.4 Config Default Diff

15.9.0：getter `sub_25C0F0` → ctor `sub_24E1C0`，对象 0x340（832B）。
16.6.1：getter `sub_26173C` → ctor `sub_252F68`，对象 0x350（848B）。

提取全部结构体立即数存储（自研指针偏移追踪，16.6.1 侧与 D1 已知值全部互证：30/200/257/256/0x1000101/selector=2 等）：

- **共有字段逐值相同**：构建节流因子 30、APK 上限 200、cfg[432]=257、gt 门=256（bit0=0）、`0x1000101`、selector 位值=2（>1=ON）等全部一致；
- **16.6.1 多 16 字节**：尾部新增字段（如 +0x344=10）服务于新探针/新采集面的门控；0x29d 附近有 2 字节结构插入（与 cfg[825] 之类新门字段一致——zygisk 探针在两版都读 +825，但新探针占用了新增空间）；
- zygisk 探针门：两版均为 config 对象 `+0x339`（825）字节门（E3：两版均存在 `bl getter; ldrb w?,[x0,#0x339]` 形态）。

**判定：默认值未变（Case C 变体）——能力代码扩张（新探针/新采集面/新门字段），既有 gate 默认值不动。** 远程是否覆盖运行值仍 UNKNOWN（受保护内存，规范禁止读取）。

## 3. Runtime DEX / Java Capability Diff

三版盘上 classes.dex 虽为易盾壳，但**字符串池明文可读**（E2 级时间线证据；代码级语义不可比，运行时解密）：

| Java 侧标记 | 15.9.0 | 16.5.1 | 16.6.1 |
|---|---|---|---|
| `MainInit` / `X-App-Token` / `X-App-Device` / `CoolMarketHeaderInterceptor` | ✓ | ✓ | ✓ |
| `useDDI` / `useDDISessionId` | ✓ | ✓ | ✓ |
| `PostToken` / `_v2_post_token` / `businessId` | ✓ | ✓ | ✓ |
| `JNIFactory` / `aebd1811194e82d9` | ✓ | ✓ | ✓ |
| `nuid` / `SHUZILM_DID` / `ddid` / `cn.shuzilm` | ✓ | ✓ | ✓ |
| `RequestSessionIDUpdater` / `AntiCheatResult` | ✓ | ✓ | ✓ |
| `isXPosed` / `isFrameworkHooked`（Kwad 环境标志模型） | ✓ | ✓ | ✓ |
| 阿里 SecurityBody Java 层 | ✓（仅 15.9.0） | ✗ | ✗ |

**结论：Java 侧 DDI/X-App-Device/PostToken/nuid/Shuzilm 传输链在 ≤15.9.0 已完整存在**（字符串级 E2；与 16.6.1 已 E3 的链路结构对照）。`useDDIEventList` 等个别字符串三版盘上均未见（在运行时解密区或名称不同），UNKNOWN。

## 4. Probe First-Introduced Matrix

| Capability | 15.9.0 | 16.5.1 | 16.6.1 | first introduced | 语义变化 |
|---|---|---|---|---|---|
| pm collector（无 -3） | ✓ | — | — | ≤15.9.0 | 全量枚举 |
| **pm collector + `-3` 第三方范围** | ✗ | ✓ | ✓ | **(15.9.0, 16.5.1]** | 上报集 = 第三方包 |
| zygisk（ptrace + 路径法） | ✓ | ✓ | ✓ | ≤15.9.0 | 无 |
| LSPosed：maps 内容搜索 + is-top-app | ✗ | ✓ | ✓ | (15.9.0, 16.5.1] | 从路径法升级为内存特征 |
| ART `inline-max-code-units=0` | ✗ | ✓ | ✓ | (15.9.0, 16.5.1] | 直接检测 ART Hook 兼容编译 |
| fdinfo/mnt_id、smaps、magisk 路径表、模拟器 | ✓ | ✓ | ✓ | ≤15.9.0 | 无 |
| filePermisson（Android/data 外存扫描） | ✗ | ✓ | ✓ | (15.9.0, 16.5.1] | 新采集面 |
| changedPackages | ✗ | ✓ | ✓ | (15.9.0, 16.5.1] | 新采集面 |
| shizuku / frida 字符串 | ✗ | ✓ | ✓ | (15.9.0, 16.5.1] | 新增检测词 |
| Java DDI/PostToken/nuid 传输链 | ✓ | ✓ | ✓ | ≤15.9.0 | 无（字符串级） |
| metasec_ml（Pangle） | N/A（动态下发） | N/A | N/A | 随广告组件 | 与 APK 版本解耦 |

注：15.9.0 与 16.5.1 之间的 16.0–16.4 未取样，"first introduced" 以闭合区间表述。

## 5. Coolapk × Purifier Timeline

| 时间/版本 | Coolapk 安全能力 | 远程配置能力 | Purifier hook surface |
|---|---|---|---|
| ≤15.9.0（历史） | NetHT：pm collector（无 -3）、zygisk ptrace+路径法、fdinfo/mnt、smaps、magisk 表、模拟器；Java DDI/PostToken/nuid 链；阿里 SecurityBody 并存 | useDDI 等开关字符串已存在 | （2.x 之前，无模块） |
| (15.9.0, 16.5.1] | **NetHT 大版本更新**：+`-3` 第三方范围、+LSPosed maps 搜索、+inline-max ART 探针、+filepermisson、+changedPackages、+shizuku/frida、+16B config；SecurityBody Java 层移除 | DDI/PostToken 开关（当前值见 D1） | — |
| 16.5.1（=16.6.1 的 so） | 与 16.6.1 **逐字节相同** | 同 | 2.1.2（2026-08-17~19）：DexKit resolver、无长期 framework hook |
| 16.6.1（2608212） | Java/资源层小改（so 不变） | useDDI=1、useDDISessionId=1、PostToken.List=[createFeed,reply] | **2.2.0（2026-08-25）：+5 个 Settings framework hook、+2 个 layout/tag fallback hook、Reply 临时 ClassLoader hook** → Mode A（08-25/26）全部移除，回到 2.1.2 面 |
| 2026-08-25/26 | Issue #5 观察窗口 | 同上 | Mode A 冻结 |

## 6. Root-Cause Impact（按 §6.6 判定规则）

**逐项判定：**

1. **CLIENT_CAPABILITY_CHANGED = 成立（NetHT，区间 (15.9.0, 16.5.1]）**
   - `-3` 第三方范围化 installedApk 采集；
   - LSPosed 从路径法升级为 maps 内容搜索（对 LSPosed 用户检测面显著增强）；
   - `inline-max-code-units=0` 直接检测 ART Hook 编译参数；
   - filepermisson/changedPackages/shizuku/frida 新增。
   - **限制**：这些变化在 16.5.1 就已全部到位（16.5.1==16.6.1 so 相同）；若 Issue #5 在 16.5.1 期间未出现而 16.6.1 才出现，则 native 能力变化无法解释该增量，需归因于 Java/远程/服务端。

2. **REMOTE_CONFIG_CHANGED = 部分/UNKNOWN**
   - NetHT 私有 config 运行值不可读（受保护内存）；useDDI/PostToken 当前值已固化（D1），但**历史值无样本**；
   - Java 侧开关骨架 ≤15.9.0 已存在 → "开关本身新增"不成立；"服务端何时把 useDDI 从 0 切 1" UNKNOWN。

3. **CLIENT_UNCHANGED_SERVER_MORE_LIKELY = 对 16.5.1→16.6.1 增量成立**
   - 16.5.1 与 16.6.1 的 NetHT/nesec 完全相同、Java 骨架相同 → 若 Issue #5 时间相关性落在 16.6.1 或 Purifier 2.2.0 侧，则客户端 native 能力零变化，解释力转移给：服务端策略/阈值/rollout、Purifier 2.2.0 新增 hook 面、或 Pangle 组件独立更新（metasec version-211448 与 APK 版本解耦——**这条独立变量此前未入模型**）。

4. **MIXED 成分**
   - 本设备当前：应用列表通道被 OS 权限关闭（D4）→ 16.5.x 新增的 `-3` collector 在本设备**未激活**为模块包名上传；本设备活跃输入面仍是 LSPosed/Zygisk/ART/root（其中 LSPosed-maps 与 inline-max 恰是 16.5.x 新增探针——**即使包名通道关闭，这些探针对本设备直接命中**）。

**综合根因排序更新：**
1. **LSPosed/Zygisk/ART 探针 + 服务端策略**（升）：其中两个最强探针（LSPosed maps 搜索、inline-max ART 参数）正是 16.5.x 新增——时间相关性与能力变化方向一致；
2. root/Magisk（metasec/Pangle，版本解耦，独立变量）；
3. 服务端 policy/threshold/rollout（16.5.1→16.6.1 零 native 差异下的唯一解释项）；
4. Purifier 2.2.0 hook 面（2.2.0 确实新增过 framework hook，但 Mode A 已回到 2.1.2 面；无直接证据链）；
5. 应用列表/模块包名（本设备权限门关闭，降为其它 ROM 条件路径；但注意**该能力本身就是 16.5.x 新增**——在授权 ROM 上时间相关性同样成立）。

## 7. 与执行规范的对照

- §6.1 manifest ✓；§6.2 NetHT diff ✓（installedApk/探针/config）；§6.3 metasec ✓（判 N/A：Pangle 动态组件）；§6.4 runtime DEX ✓（字符串级，代码级 INSUFFICIENT-诚实标注）；§6.5 远程配置 schema：客户端 key 骨架三版同在，运行值历史 UNKNOWN；§6.6 判定 ✓；§6.7 时间表 ✓；§6.9 本报告即交付物 ✓。

## 8. 对模块的结论

```text
KEEP MODE A FROZEN
NO MODULE PATCH
```

本轮无任何新的"当前 hook → active probe"直接边。跨版本结论反而进一步支持冻结：本设备最活跃的检测面（LSPosed-maps/inline-max/zygisk-ptrace/root）在 Purifier 2.1.2 时代就同样存在，模块侧删功能不改变这些环境信号。

## 附：本轮方法学（可复用）

- 栈字符串重建器：`.tmp_audit/stackstr.py`（mov/movz/movk→strb/strh/str/stur，sp/指针 write-back 追踪）+ 全密钥暴力解码；
- 全 .text 能力扫描：`.tmp_audit/p4b_fullscan.py`（产物 `p4b_fullscan_{old,new}.json`）；
- 构造器默认值提取：`.tmp_audit/p4b_ctor2.py`（16.6.1 已知值全互证）；
- 已知局限：重建器窗口起点敏感（存在假阴性，重要阴性均以"定点全密钥集群验证"二次确认）；字符串池 diff 只有 E2 级。
