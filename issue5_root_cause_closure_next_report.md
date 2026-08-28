# Issue #5 — 根因证据链闭合报告（next_report）

分析时间：2026-08-28（第五会话）
执行规范：`issue5_root_cause_closure_updated.md`
宿主：酷安 16.6.1（2608212，2026-08-28 02:18 最后一次重装）；模块：2.2.1 候选（冻结，本轮零改动）
工具：IDA Pro MCP（`.tmp_audit/native/libNetHTProtect.so.i64`，只读 + rename/comment 元数据已保存）、
离线 Python（壳 DEX 字符串池全集差分、APK 条目差分、androguard 业务 DEX 分析）、
设备 root 只读（pangle 目录 stat/ls、dumpsys）。
未 patch 任何字节、未注入 Frida、未开抓包代理、未触发风控操作、未修改模块与远程仓库。

证据等级沿用：E3 = 指令级/字节级/实测直接证据；E2 = 强结构推断；E1 = 弱支持；UNKNOWN = 证据不足。

---

## 1. Executive Conclusion

本轮在四条链上取得进展，其中两条是对既有根因模型的**重大修正**：

1. **【推翻既有结论】NetHT 探针门在 libNetHTProtect.so 内不存在任何运行时写入者。**
   配置单例指针 `qword_4C26E0` 仅被 getter（sub_26173C）引用；全库在 getter 调用后 10 条指令内
   不存在任何寄存器索引式 STRB；全分发器（sub_24646C，1673 条指令）零索引 STRB；
   结合 E1 轮已完成的"字面偏移写门者仅构造器"扫描，**探针门（cfg 808/809/821/825/788/840 等）
   是构造时一次写入（默认全 OFF）、之后在本库内不可变的**。
   P4B 轮"私有通道可按索引改写门字节 → 服务端可远程翻转探针门"的机制级断言被证伪。
   → "服务端 rollout 翻转 NetHT 探针开关"假设失去客户端机制支撑（本库内；跨库写入见 §10 UNKNOWN）。

2. **【重大时间线闭合】Pangle/metasec 组件在本设备的最早痕迹 = 2026-08-26 10:42–43**
   （`files/.pangle_i`、`files/pangle_com.byted.live.lite/{.msdata,ALOG,apminsight,keva,...}` 全部起始于该时刻），
   与 Issue #5 观察窗口（2026-08-25/26）精确重合。metasec（root/Magisk/自动化检测，E3）从
   "与 APK 版本解耦的假设变量"升级为"**与观察窗口同日激活的实测独立变量**"。

3. D1 轮对 sub_2A658 的"配置更新器（写 cfg[624/768/780/784]）"命名归属有误：它实际是
   **black_module 周期扫描器**（针对非 `/data/app/*.apk` 路径的 APK 文件发 `black_module` 安全事件，
   首次命中还会立即触发一次安全数据构建）。所谓"周期配置更新电池"整体上是**周期检测器电池**。

4. "gt" 键语义最终闭合：**每次 `aebd1811194e82d9`（getToken）调用都记录一次 "gt"**；
   首次 getToken 后 count==1，与 cfg[788]（默认 0 且本库内不可写）共同决定全量 pm 快照。
   即"gt 全量模式"在此构建中**结构上不可能被激活**。

5. 16.5.1 → 16.6.1 客户端差分：唯一 native 变更是 `librtmp-jni.so`（直播），安全栈
   （libNetHTProtect / libnesec）逐字节零变化；壳 DEX 字符串池差分未发现任何安全传输链新增。
   客户端版本间解释力进一步下降。

6. 展示层（Priority D）：客户端除 P5 已证的通用 card 能力外，还存在**专用警示行渲染资产**
   （`feed_warning` 布局族、`item_alert_message_card` + `AlertMessageCardViewHolder`（红底警示配色））。
   PRESENTATION_CAPABILITY 强化；实际风险行 endpoint 仍 UNKNOWN。

**综合根因排序变化**：由于 NetHT 探针门不可翻转且默认 OFF，本设备上 NetHT 通道实际可用的
环境信号收窄为"installedApk field 12（本机只含宿主自己，D4 实测）+ 周期扫描器电池可能注入的事件 +
nuid 基础设备身份"；**metasec（Pangle 供应链）与服务端策略/阈值成为解释 Issue #5 时间相关性的
前两位候选**，且二者首次获得了实测时间线支撑（前者 §7，后者靠排除法强化）。

---

## 2. New Evidence（本轮新增，全部标注等级）

### 2.1 JNI 注册表完整还原（E3）

`sub_24391C`（RegisterNatives，`com/netease/htprotect/factory/JNIFactory`，9 方法）按表序还原：

| # | Java native 方法（混淆名） | 签名 | native 实现 | 语义（本轮补全） |
|---|---|---|---|---|
| 1 | `hccd63688a790ca65` | `(Context;String;HTPCallback;HTProtectConfig;)V` | sub_242788 | **init**：读 config 的 channel/gameKey/host/getExtraData(String)（栈串解码，E3），转交 sub_244778 |
| 2 | `t76euy9fu8bv485zh` | `(String;String;String;String;String;int;String;)I` | sub_2430F4 | 数据上报类：首参须为 32 字符 key（businessId 形态） |
| 3 | `e9edd62242ad7aecf` | `(String;int)String` | sub_243474 | → sub_2458CC，记录键 `lse_<n>` |
| 4 | `r316e12523620efb7` | `(String;int)String` | sub_2435B4 | → sub_245B00，记录键 `lsd_<n>` |
| 5 | `r25d273c7ad4065c3` | `([BIIZIZ)[B` | sub_2436F8 | 字节串处理 |
| 6 | `d0f149b4da6ec477` | `(int;String;)String` | sub_24331C | **ioctl 命令分发器** → sub_24646C（见 2.2） |
| 7 | `f190da6241bff18bf` | `()V` | j_oOOOoooo0000o00O_0 | 导出符号 |
| 8 | `u233ace17d63ca9e` | `(ZIIIII)V` | sub_2438FC | 小桩 |
| 9 | `aebd1811194e82d9` | `(String;)[B` | sub_243B18 | **getToken/blob getter**（见 2.4） |

NetHT SDK 版本串：**"5.7.6"**（case 7 of ioctl 分发器返回；E3）。

### 2.2 ioctl 分发器（sub_24646C）selector 语义表（E3，冷路径含未反编译区已用反汇编直读）

| selector | 行为 |
|---|---|
| 1 | emulator 状态查询（解码键 "emuator:"） |
| 2 | **root 状态查询：返回 "root:0"/"root:1"**（栈串解码） |
| 3/12/20 | ctx 对象 vtable 读取（+48/+32/+16） |
| 4/5/6/10/11/15 | 返回空串 |
| 7/9 | 返回版本 "5.7.6" |
| 8 | token 通道（构造 "token" 串 → sub_2E92BC） |
| 13/14/16/17/18 | ctx 状态读写（16 还写 `ctx+392` 状态字） |
| 19 | **把 Java 传入字符串以键 `event_data`（type 0x270F）推入全局安全事件队列** |
| 21 | 同上，另一键（解码见证据文件；type 0x270F） |
| 22 | **把 Java 传入字符串以键 `keyevent`（type 0xFA2=4002）推入全局安全事件队列** |
| 100 | 记录键 `view_<字符串>`（页面/视图事件计数） |
| default | 返回 "unsupported request" |

关键点：**分发器全程只读配置、零写入**（1673 条指令反汇编复核：无任何索引式 STRB）。
每次调用先构造 `ioc_<selector>` 键做 named-key 计数（这就是"命令被记录"的机制）。

### 2.3 "gt" 键与 getToken 的绑定（E3，本轮最重要的单点闭合）

```
Java JNIFactory.aebd1811194e82d9(businessId)[B   (sdk_netht.dex)
→ sub_243B18: GetStringUTFChars → 调 0x247B1C worker:
    ① gate: byte_4C26C8(已 init) 否则返 201；参数空返 203
    ② netht_named_key_record(ctx, "gt")        ← 每次 getToken 计数 +1
    ③ ctx[280] = 1                              (sub_265388)
    ④ sub_213814(businessId,&out,&len)          ← 实际 blob 构建（eventType 6）
→ status==200 时返回 [4B status][blob] byte[]
```

与 P1 结论合并：append 循环的 `sub_266C30(ctx,"gt")==1` 即"**本进程首次 getToken**"。
全量 pm 快照条件 = 首次 getToken ∧ cfg[788]==1；cfg[788] 默认 0 且无运行时写入者（§2.5）
→ **gt 全量模式在此构建不可能发生**；installedApk 恒为"每包每进程一次"的增量模式。

### 2.4 探针门不可写（E3，双扫描互证）

1. 单例 getter sub_26173C → `qword_4C26E0`。xref：**全库仅 getter 自身 4 处引用**（init flag/alloc/ctor/store）。
2. 扫描 A：对全库每个 `BL sub_26173C` 调用点，检查其后 10 条指令内寄存器索引式 `STRB Wn,[Xn,Xm]`：**0 命中**。
3. 扫描 B：分发器 sub_24646C 全量反汇编（0x24646C–0x247EA8，1673 insns）：**0 索引式 STRB**。
4. E1 轮既有结论：字面偏移写门者全库仅构造器 netht_config_ctor_defaults（0x197454/0x22E41C/0x1C8A84 已排除为误报）。
5. 构造器尾部复核（E3）：`*(a1+800)=0x12C00000101`、`*(WORD*)(a1+808)=0`、`*(a1+825)=0`、
   `*(a1+820)=0`、`*(DWORD*)(a1+836)=10`、`*(BYTE*)(a1+840)=0` —— LSPosed/zygisk/smaps/mnt 门
   与 filePermisson/mis 门确认默认 OFF。

> 修正记录：E1 轮（`issue5_full_p4b_session_report.md` §1.3-2）"JNI 命令分发器含 STRB W?,[X9,X8]
> 索引式写入 → 私有通道可远程翻转门"的说法**不成立**（该形态写入在分发器及其前级 sub_24331C 均不存在）。
> 据此推导的"服务端可翻转探针位"机制级支撑同时撤销。

### 2.5 周期"电池"与 black_module 扫描器（E3）

- 探针调度器 sub_2483A8（由 init 内 `netht_register_probe_scheduler_once` sub_242628 注册进
  `qword_4C2298+496` 回调；GOT 槽 0x467CA8 存其地址）在分发探针的同时调用
  **netht_periodic_timers_register（sub_24FFD4）**注册 3 个周期定时器（间隔单位 µs，`60e6 × cfg[N]`）：
  | 定时器 | 周期公式 | 默认值（ctor OWORD 0x399C70/80 + 立即数） | 默认周期 |
  |---|---|---|---|
  | 扫描电池（sub_250310 → 11 个成员） | 60s × cfg[84] | cfg[84]=**1** | **每 1 分钟** |
  | sub_250410 | 60s × cfg[88] | cfg[88]=**30** | 30 分钟 |
  | sub_25057C | 60s × cfg[132] | cfg[132]=**10** | 10 分钟 |
  （第二/三个受 cfg[617] 门，默认 0=开放注册）
- **电池成员共性**（sub_37784/39A80/3B2EC/4A5EC/4F4D8/55BF0/28A53C + sub_2A658 + sub_473F8 + …）：
  均为"节流(sub_26CF8C) → 读记录注册表（sub_9B83C，key 203695656）→ 记录级 selector 检查
  （sub_2301B4/1E0/20C）→ 组装事件（sub_1E300）→ 推入全局安全队列（sub_2F3308），
  部分含 sub_230B68(N) 立即构建触发"。这是**周期检测器电池**，不是配置更新器。
- **black_module 扫描器（sub_2A658，已更名 netht_black_module_scanner）**：
  - 遍历注册表中 type tag `0x4204F8CA` 的记录；跳过 `starts_with("/data/app/") && ends_with(".apk")`
    的常规安装路径；对**非常规路径 APK**（栈串 `/data/app/`、`.apk`、`black_module`、`/files/` XOR/加差解码）
    生成 `black_module` 事件入安全队列；
  - 首次命中：`sub_230B68(1)` **立即触发 eventType 1 的安全数据构建**、写文件 `/.hrecord…`、
    经 `qword_4C2298+600(0)` 疑似 exit(0)（该表项语义 E2）、再调度任务；
  - 富集引擎 **netht_apk_parser_via_jni（sub_24188）**：通过 JNI 调隐藏 API
    `android.content.pm.PackageParser.parsePackage(File,int)/(File,String,DisplayMetrics,int)`
    解析任意 APK（API≥21 走静态两参版），即 installedApk 的权限/组件标注可不依赖 pm 命令获得。
- **sub_230B68(N)** = 立即构建触发器（`netht_selector_enabled(4)` 总门 → netht_report_scheduler(?, N)）；
  black_module 首次发现 → eventType 1 立即构建 → 下一次 getToken 即携带。

### 2.6 init 链（E3）

`sub_242788`（JNI init）读 Java `HTProtectConfig` 的 channel/gameKey/host/getExtraData(String) →
`sub_244778`（netht_sdk_init_worker）：product 号长度≥20 报 "product number illegal"；记录 "inmt" 键；
`netht_register_probe_scheduler_once(a3,a4)`；置 `byte_4C26C8=1`（所有 ioctl/getToken 的总门）。
Java 侧配置仅写入 ctx（sub_265114 → ctx+24 列表），**未映射进 native cfg 门**。

### 2.7 记录注册表（key 203695656）与事件队列的输入面（部分闭合）

- 读取者：电池家族约 20+ 函数（sub_28700/299FC/2A658/37784/39A80/3A8B8/3B2EC/3C030/3CCF0/3D644/3E9FC/3ED10/415CC/41E28/462B0/473F8/47ED8/49410/4A5EC/…）。
- **写入者：本轮未定位**（候选：文件访问拦截层/Java 层 ioctl 19/21/22 事件注入/其他探针）。UNKNOWN。
- Java 侧可经 ioctl selector 19/21/22 直推 `event_data`/`keyevent` 事件（E3），为注册表之外的
  另一条事件输入通道（是否被酷安业务层实际使用：UNKNOWN）。

---

## 3. Runtime Gate Mapping（规范 §4-A 要求的输出表）

| Probe/能力 | cfg offset | updater（写者） | remote/native key | source | runtime value |
|---|---|---|---|---|---|
| LSPosed maps 搜索 | cfg[808] | **无（ctor-only，E3）** | 无映射 | 构造器默认 | **恒 0 = OFF（本库内不可翻转）** |
| fdinfo/mnt_id | cfg[809] | 无（ctor-only） | 无 | 同上 | 恒 0 = OFF |
| smaps Referenced | cfg[821] | 无（ctor-only） | 无 | 同上 | 恒 0 = OFF |
| zygisk ptrace | cfg[825] | 无（ctor-only） | 无 | 同上 | 恒 0 = OFF |
| "gt" 全量快照门 | cfg[788] | 无（ctor-only） | "gt"=**首次 getToken 计数**（非远程开关） | 0x247B1C worker | 恒 0 → 增量模式 |
| filePermisson 外存扫描 | cfg[840] | 无（ctor-only） | 无 | 同上 | 恒 0 = OFF |
| mis 相邻 UID 扫描 | cfg[823] | 无（ctor-only） | 无 | 同上 | 恒 0 = OFF |
| event8 强制门/标注门 | cfg[624]/[674] | 无（ctor-only） | 无 | 同上 | 0 / 1（D1 默认） |
| installedApk 总门 | bit11/bit22（cfg[300]/[344]） | 无（ctor-only，默认 2=ON） | 无 | 同上 | **ON**（能力层面） |
| 电池/定时器间隔 | cfg[84]/[88]/[132] | 无 | 无 | ctor OWORD 常量 | 1min / 30min / 10min |
| 电池变更汇总门 | cfg[678] | 无 | 无 | ctor `*(WORD*)(a1+677)=1` | 0 = OFF |
| Java HTProtectConfig（channel/gameKey/host/extraData） | —（进 ctx，不进 cfg） | init 时一次性 | getExtraData(String) 通用 KV | sub_242788 | 与 cfg 门无映射边 |

**结论**：规范设想的 `HTTP/config response → parser → selector/index → cfg offset` 链在
libNetHTProtect.so 内**不存在后半段**：没有任何代码把远程/Java 输入写进 cfg 门。
"远程改配置激活探针"在本库内无实现路径（跨库/外部写者见 §10）。

---

## 4. Startup Security Timeline（Priority B，CONFIRMED/INFERRED/UNKNOWN 标注）

```text
进程启动
→ 易盾壳（libnesec）分级解密 + 载荷就地解密执行                    [CONFIRMED，附录 A]
→ runtime DEX 加载                                                [CONFIRMED，前轮]
→ Java: HTProtect.init(ctx, product, callback, HTProtectConfig)   [CONFIRMED，JNI 表 E3]
   ★ 仅当 NetHT 实际被加载时发生——普通浏览会话可整场不加载（附录 A.4，E3 实测）
    → sub_242788 读 channel/gameKey/host/getExtraData → ctx       [CONFIRMED]
    → sub_244778: 记录 "inmt"；注册探针调度器(经 qword_4C2298+496)；byte_4C26C8=1  [CONFIRMED]
→ 探针调度器 sub_2483A8 被宿主胶水回调触发（时点由 libnesec/wrapper 决定，本库不可见）[INFERRED：注册后、首次 getToken 前]
    → 顺序分发 LSPosed/zygisk/smaps/mnt/odex 探针
      ——每个都被 cfg 门拦下（默认 0）→ 全部不产出事件                [CONFIRMED 门值；探针“未产出”为 E2 推论]
    → 注册周期定时器：电池 1min / sub_250410 30min / sub_25057C 10min [CONFIRMED]
→ Java: initializeDeviceIDInternal → getToken(businessId)          [CONFIRMED，P3]
    → 0x247B1C: 记录 "gt"(=1) → sub_213814 构建 blob（eventType 6）[CONFIRMED，本轮]
    → 序列化事件队列 + installedApk field12（增量；gt 门恒 0）       [CONFIRMED]
→ nuid ← token；X-App-Device ← DID+nuid+机型串（Base64+reverse）    [CONFIRMED，P3]
→ 后续每分钟：电池扫描（black_module/bp/…）→ 命中即推事件 + 立即构建 [CONFIRMED 机制；实际命中内容 UNKNOWN]
→ 30min/10min 定时器（sub_250410/sub_25057C）内容未逐一展开         [UNKNOWN]
```

对规范六问的更新回答：
1. remote config 在首次 probe 前后都不会应用——**本库内根本没有 remote→cfg 的应用边**（本轮 E3）。
2. 第一次 nuid 使用的是 ctor 默认门（全 OFF 探针 + 增量 installedApk），之后也不会变。
3. gate 更新后触发新 probe：**gate 不会更新**（不可写）。
4. nuid refresh：仍是 init 完成/onLogin/onWifi/白名单请求触发 getToken（D5）；每次 getToken 都会
   序列化当时的事件队列（若电池在此期间命中过 black_module 等事件，则随 refresh 上行）。
5. login/Wi-Fi/前台事件触发新 build：沿用 D5 结论（CONFIRMED）。
6. 同进程 config 更新影响后续 blob：**不存在该机制**（cfg 不可变）。

---

## 5. Java / Remote Cross-Version Diff（Priority C）

方法：两版壳 classes.dex 字符串池**全集**差分（16.5.1: 461,385 串 / 16.6.1: 474,897 串；
+68,567/−55,055）+ APK 条目级差分（6139 vs 6165 条目）。

| 维度 | 结果 | 等级 |
|---|---|---|
| native 库 | **唯一变更为 `lib/arm64-v8a/librtmp-jni.so`**（87,912→92,072 B，直播推流库）；libNetHTProtect.so / libnesec.so 与其余全部 .so 逐条目不变 | E3 |
| 安全传输链字符串 | MainInit/useDDI*/PostToken/_v2_post_token/X-App-Device/nuid/SHUZILM/JNIFactory/aebd…：两版同在，无新增/删除 | E3（字符串级） |
| 安全相关唯一 delta | **"installedApk" 明文串从 16.6.1 壳池消失**（16.5.1 在）——最可能是 protobuf/model 类移入运行时加密区，语义不可判 | E2 |
| Splash 子系统 | 16.6.1 移除 "init splash sdk"/"splash SDK is ready" 等旧串，新增 `SplashJumpButton/SwipeIndicator/CoolapkSplashView`、`AdDetailTrigger`、`getSplashAdOpenType`、`splashAdSource` 等 —— 与模块侧已确认的 16.6.1 开屏路径重构（MainActivity 内嵌 SplashAdFragment）相互印证 | E2 |
| 其它增量 | media3/Compose/liquid glass UI、PlaybackCoordinator、EmotionPanel 等 UI 族；`sdk_init_*` 广告 SDK 协调器日志串 | E2（非安全面） |
| manifest | 150,236→150,908 B（小幅，未逐项展开） | E1 |
| 资源 | 1802 个条目变更（混淆名轮换为主） | E1 |

**判定**：
```text
16.5.1 → 16.6.1
client-side (native security, Java security transport strings) delta = NONE
client-side delta explanatory power ↓↓↓
server policy / external-supply-chain (Pangle) explanatory power ↑↑
```
（与 P4B "CLIENT_UNCHANGED_SERVER_MORE_LIKELY" 同向，且本轮把"唯一 native 变更"精确到 librtmp-jni.so。）

---

## 6. Server-Driven Presentation Model（Priority D）

新增证据（16.6.1 runtime DEX + 业务 DEX）：

1. **专用警示行渲染资产存在**：
   - `layout/feed_warning_0` + `FeedWarningBinding(Impl)` + tag 校验串（DataBinding 生成物，实际使用）；
   - `layout/item_alert_message_card_0` + `ItemAlertMessageCardBinding(Impl)` +
     业务 DEX 类 `Lк;`（源文件 `AlertMessageCardViewHolder.kt`，持警示配色常量 `#19DB4437`/`#FDD9D7`，
     extends 通用 ViewHolder 基类 `Li5;`）——**带红/粉警示配色的通用消息警示卡 ViewHolder**；
   - `MessageCard`（AutoValue 模型）+ `MessageCardDialogFragment`（Entity+Contacts 驱动的弹窗）——
     服务端实体驱动弹窗的又一实例（当前样本用于"分享成功/去私信"，证明 **Entity→Dialog** 通路存在）。
2. **分发键**：`AlertMessageCardViewHolder` 的实例化点未在业务 DEX 中找到直接 xref（仅自身 `<clinit>`），
   推测经工厂/反射注册或位于未恢复 dex 区（E2）；其宿主页面（消息中心 vs 主 feed）未定。
3. **网络中间件复核**：okhttp 链上的 Interceptor 均为请求侧（CoolMarketHeader/Cookie/ExtraPostField/
   DynamicHost/ImageReferer/KsHeader/NetworkFlow/RequestResponse/History/CookieSync），
   **未发现改写响应、注入列表行的 response middleware**；字符串含 "Interceptor" 的
   `NodeFeedDialogInterceptor/FeedReplyTopDialogInterceptor/FeedReplyRecommendDialogInterceptor`
   实为 UI 层 `SheetGroupModifier`（置顶/弹窗），非网络层 —— 假阳性已排除（E3）。
4. `ipRiskRating` 等字样经上下文核验属腾讯网络组件（ipSpeedTask/ipsSortedBySpeeds/ipneigh），
   与酷安安全流无关 —— 假阳性已排除（E2）。

**分类结论（更新）**：
```text
PRESENTATION_CAPABILITY = high confidence
  （P5 S2 通用 card/textCard + 本轮专用警示卡资产 + Entity→Dialog 实证通路）
ACTUAL_RISK_ROW_ENDPOINT = UNKNOWN
  （12 份 indexV8 + init 样本中无风险行；无法在本轮无阳性条件下收敛）
LIKELY_CARRIER（候选，按可注入性排序）：
  a. 任意列表响应中的通用 card/textCard 行（零客户端配合）
  b. 消息/通知列表的 alertMessageCard 行（专用资产已就位）
  c. Entity 驱动 Dialog（MessageCardDialogFragment 通路）
```

---

## 7. Pangle Independent Timeline（Priority E → CONFIRMED）

设备实测（root 只读 stat/ls；E3）：

| 项 | 时间（+0800） | 说明 |
|---|---|---|
| `files/.pangle_i/` | 2026-08-26 10:42 | Pangle 初始化标记目录 |
| `files/pangle_com.byted.live.lite/{.msdata,.msdata_lc,ALOG,apminsight,keva,live_kv,ttsdk,server.json,mena.czl}` | **2026-08-26 10:43** | live.lite 组件数据目录（`.msdata`/`.msdata_lc` 疑似 metasec 数据目录，E2）→ live.lite 组件当日已加载运行，metasec_ml.so 随组件 lib 目录分发 |
| `files/pangle_com.byted.csj.ext/` | 2026-08-26 12:59 | CSJ 扩展数据目录 |
| `pangle_p/com.byted.live.lite/version-211448/lib/libmetasec_ml.so`（2,058,272 B） | 创建于 2026-08-27 21:30:10 | 本轮 IDA 分析所用版本 |
| `pangle_p/com.byted.pangle/version-7805` | 2026-08-27 21:30:11 | |
| `pangle_p/com.byted.csj.ext/version-1164` | 2026-08-27 21:42 | |
| 酷安本体 lastUpdateTime | 2026-08-28 02:18:49 | 16.6.1 重装（splash 诊断期）；随后 02:21 live.lite base-1.apk/resMappingBak 刷新 |

判定：
```text
metasec 独立时间线 = CONFIRMED
最早设备痕迹 2026-08-26 10:42–43，与 Issue #5 观察窗口（08-25/26）同日。
08-26 10:42 之前无任何 Pangle 痕迹（12+ 独立子目录/文件时间一致指向首启，中等强度阴性证据）。
```
- 该组件 08-27 21:30 更新至 version-211448（前一版本号不可考，目录被重建）。
- 时间相关性成立；**因果仍未证**（metasec 结果如何进入酷安可见通道仍缺 attach point，前轮已知）。
- 注意：`.pangle_i` 与 `pangle_com.byted.pangle/` 对 root 显示 Permission denied（SELinux 域限制），
  仅时间戳可读；`08-25 或更早是否存在后被清理的组件` 形式上仍为 UNKNOWN，但现存全部数据目录
  无一早于 08-26 10:43，"当天首次激活"是当前最优解释。

---

## 8. Root-Cause Explanation Matrix（规范 §4-F，更新列：本轮变化）

| # | Hypothesis | Client capability | Runtime activation | Transport | Presentation | Version/timing | Confidence | 本轮变化 |
|---|---|---|---|---|---|---|---|---|
| 1 | remote probe gate rollout（NetHT 门翻转） | 探针代码在（E3） | **不可能：门 ctor 一次写、库内无写者（E3）** | — | — | 不适用 | **已排除（本库内）** | ▼▼ 从"机制级支撑"降为"无客户端机制" |
| 2 | LSPosed/Zygisk/ART 信号经 NetHT 上行 | 探针在（E3） | **默认 OFF 且不可开（E3）** | 不发生 | — | 不适用 | 低（本设备本构建） | ▼▼ 与 #1 连锁降级 |
| 3 | metasec（Pangle 供应链）独立信号 | root/自动化检测 E3（前轮） | **08-26 10:43 起在本设备实测运行（E3）** | attach point UNKNOWN | UNKNOWN | **与观察窗口同日（E3）** | 中 | ▲▲ 唯一获得实测时间线支撑的检测面 |
| 4 | 服务端 policy/阈值/rollout（对既有信号打分） | —（服务端） | — | nuid/X-App-Token 常驻（E3） | 通用+警示卡能力（E2/E3） | 排除法强化（客户端零 delta） | 中高 | ▲ 客户端 1/2 排除后解释力上升 |
| 5 | installedApk 包名规则 | E3（全链） | ON，但本机只见宿主自己（D4 E2） | 已 E3 | — | 权限门决定，非版本决定 | 低（本设备） | = 沿用 D4 |
| 6 | 模块特有行为 | 无 M1-M5 证据 | — | — | — | — | 低 | = |
| 7 | black_module/电池扫描器事件（非常规路径 APK） | **E3（本轮新识别）** | 电池每 60s 默认跑（E3） | 事件队列→blob→nuid（E3） | — | 注册表输入源 UNKNOWN | 中低（能力）/UNKNOWN（实际命中） | 新增行 |
| 8 | Java/runtime 初始化 delta（16.5.1→16.6.1） | 无安全面 delta（E3 字符串级） | — | — | — | 不成立 | 低 | ▼ 本轮闭合 |
| 9 | ROM 权限差异（应用列表暴露面） | E3 能力 | 本机关闭（D4） | 条件性 | — | 其它设备条件路径 | = | = |

---

## 9. Updated Root-Cause Ranking

| 排名 | 假设 | 依据 |
|---|---|---|
| 1（↑） | **metasec（Pangle）独立信号 + 服务端消费** | 唯一同时满足"检测能力 E3 + 本设备实测激活 + 激活时刻与观察窗口重合"的客户端检测面；剩余缺口仅在 attach point（其结果如何回传）|
| 2（↑） | **服务端 policy/阈值/rollout（对既有信号：nuid 基础身份、X-App-Token、行为序列）** | 客户端 16.5.1→16.6.1 安全栈零 delta（E3）+ NetHT 探针通道结构性静默（本轮 E3）→ 时间相关性解释转移到服务端；展示层能力充分（§6）|
| 3（新增） | **NetHT 电池扫描器事件（black_module 等）** | 新识别的活跃采集面（默认每分钟）；但其记录注册表输入源 UNKNOWN，实际能否命中本设备状态未知 |
| 4（=） | ROM/权限差异路径（其它设备的应用列表全量上传） | 条件性，本机不适用 |
| 5（▼▼） | ~~服务端翻转 NetHT 探针门~~ / ~~LSPosed·Zygisk 信号经 NetHT 上行~~ | 本轮 E3 排除（本库内无翻转机制、门恒 OFF）|
| 6（=） | 模块特有行为 | 无新证据 |

说明：#1/#2 并列主导且互不排斥（metasec 采集 → 服务端打分 → 展示，本就是同一条链的两段）。

---

## 10. What Remains UNKNOWN（诚实边界）

1. **跨库写入探针门**：已在附录 A 部分收敛——静态可分析范围内零证据、两库解耦、glue 表系
   NetHT 自建；唯一残留 = libnesec 780KB 加密载荷内部行为（需复现商业壳解密链，或运行时读
   内存——前轮已知会触发反墓改升级）。另因 NetHT 懒加载（A.4），该问题实际影响面进一步缩小。
2. **记录注册表（key 203695656）的写入者**：电池扫描器的输入从何而来（文件访问拦截？
   Java ioctl 事件？其它探针？）——决定 black_module/bp 等扫描器在本设备的实际产出。
3. `qword_4C2298+600`（疑似 exit）、+496（回调注册）、+8 的确切语义（宿主胶水表，运行期构造）。
4. sub_250410/sub_25057C（30min/10min 定时器）的具体扫描内容。
5. `AlertMessageCardViewHolder` 的分发 template 与宿主页面；`feed_warning` 布局的消费场景。
6. metasec 结果的宿主 attach point（沿用前轮）；version-211448 的前一版本号。
7. 风险行实际 endpoint/response（无阳性样本，结构性无法闭合）。
8. `installedApk` 明文串在 16.6.1 壳池消失的语义（重定位 or 移除，需运行时解密区比对）。
9. Coolapk 16.6.1 的准确升级时间（lastUpdateTime 被 08-28 重装覆盖）。
10. **NetHT 加载/初始化的触发条件**（登录态？特定请求？特定页面？）——附录 A.4 新增；
    待用户正常登录使用后复查 maps 即可回答（无需任何风险操作）。
11. libnesec 加密载荷（~780KB，含真实 wrapper 逻辑）内部行为——需离线复现壳解密链或运行时取证。

---

## 11. Module Impact

```text
MODULE_CODE_CHANGE = NOT_PART_OF_THIS_TASK
FOUND_DIRECT_EDGE = NO
```

- 本轮全部新证据未出现任何"当前模块行为 → 活跃安全采集"直接边：
  - NetHT 注入探针（唯一可能"看见"Hook 环境的 NetHT 面）门恒 OFF 且不可翻转（本轮 E3），
    模块的 libxposed/Instrumentation/临时 ClassLoader Hook 不进入该通道（本构建、本库内）；
  - black_module 扫描器针对**设备文件系统中的非常规路径 APK 文件**，与模块运行时行为无交集；
    模块自身是常规 `/data/app/` 安装包，即便在 installedApk 通道也已被 D4 的 OS 权限门挡住（本机）；
  - metasec 检测 root/Magisk/自动化——这是设备环境基线，与模块增删无关。
- `KEEP MODE A-ZF FROZEN / NO MODULE PATCH` 继续成立；且本轮"探针门不可翻转"让
  "靠删 Hook 降低检测面"的设想进一步失去对象。

---

## 12. Evidence Boundary / Why Further Closure Is Blocked

1. **NetHT 门运行值**：受保护内存/运行期读取被规范禁止；但现在已无必要——静态已证门
   无写入者，运行值恒等于 ctor 默认值（本库内）。
2. **跨库写门**：需要切 IDB 到 libnesec.so（或恢复其运行期胶水表），按约定需用户批准换载入文件。
3. **注册表写入者**：需要继续在 NetHT IDB 内做插入侧 xref（下一会话可做，边际收益中等）。
4. **metasec attach point**：需要切 `libmetasec_ml.so.i64`（同样需用户确认）+ 动态观察，
   且 metasec 属广告 SDK 内部协议，静态闭合成本高。
5. **风险行 endpoint**：无阳性样本条件下结构性不可闭合；只能靠日常使用中自然出现时保存响应。
6. **16.0–16.4 版本取样、历史 remote config 值**：无本地工件，保持区间表述。

---

---

# 附录 A：libnesec.so 专项研究（第五会话续，用户批准换 IDB 后完成）

背景：上轮 §10.1 遗留"跨库写入探针门"UNKNOWN，需要分析 libnesec.so（易盾壳 native 解包器）。
工具：IDA Pro MCP（`libnesec.so.i64`）+ 离线 ELF 解析 + 设备 root 只读运行时观测。
未 patch 字节、未注入、未登录、未触发风控操作。

## A.1 静态结构（E3）

**真实动态符号表恢复**（IDA 视图受故意污染的节表误导；改经程序头 + PT_DYNAMIC 手工解析，
脚本 `.tmp_audit/ns_real_dynsym.py`）：

- **53 个导入**（完整清单见脚本输出）：dl 族 **dlopen/dlsym/dlclose/dladdr/dl_iterate_phdr**、
  文件 IO 族（fopen/fread/fwrite/lseek/read/write/fstat/stat）、信号族
  **sigaction/sigprocmask/raise/abort/exit**、内存族 mmap/mprotect/munmap、
  字符串/解析族、以及 **popen/pclose**（命令执行）与 uname/readlink/getenv/basename。
- **唯一导出**：符号名在真实 dynstr 中同样是被污染的乱码；`st_value=0x8eff2`、size=1500，
  指向加密载荷内部（该处盘上字节为 `00 00 b2 00` 重复密文，非代码）。
- **NEEDED**：liblog/libz/libdl/libandroid/libc/libm/libstdc++——**不含 libNetHTProtect**。
- **真实 INIT_ARRAY**（盘上 init_array 区为全零，经 DT_RELA 的 R_AARCH64_RELATIVE 重定位恢复）：
  三个构造器 `0xd9464 / 0xd9508 / 0xd9bf4`（+FINI 0xd93f0 等）。上轮"三构造器"结论确认。
- **16 个经 PLT 实际调用的导入**：sysconf/munmap/fgets/__cxa_finalize/mmap/dlclose/dlopen/
  sscanf/sigaction/dlsym/fopen/memset/fclose/atoi/mprotect/raise——典型"分级解包 + 信号反调试"画像。

**自解壳结构判定**：
- 磁盘 0x190–0xc4000（~780KB）= 高熵加密载荷（真实 wrapper 逻辑）；静态代码仅
  0xd8040–0xe96d8（~70KB，228 函数，大量为 4–16 字节 trampoline）。
- ctor1（0xd9464，调 dlsym trampoline）先解密 ctor2 所需的指针表——ctor2（0xd9508）解引用
  `off_EDE38 → 0x17590` 等盘上密文区并以 `BLR` 调用其中函数指针（分级解包，E3）。
- 解包核心调用点 0xE1440：栈构字符串（逐字节 STRB）→ mprotect → **dlopen** → 结果检查
  （dlopen 目标名运行期构造，静态不可读；候选：解密后载荷或 libc 反 Hook 解析）。
- **静态可分析范围内零 NetHT 痕迹**：无 NetHT 符号名、无 NEEDED 依赖、无对 NetHT 任何静态引用。

## A.2 运行时观测（E3，设备 root 只读，不注入）

- 本机 APK `extractNativeLibs=false`：全部 so 以 STORED 方式从 base.apk **就地映射**
  （安装目录 lib/arm64 为空）。已确认的就地加载 so（按 APK 偏移归属）：
  libauth/libdu(umeng)/libhttpdns/libucrash/libumeng-spy/libxgVipSecurity/**libnesec**。
- **libnesec 加载于 7c6e82d000+**：其 800KB 载荷段（LOAD0）运行时为 **r-x 映射**——
  即"就地解密后作为活代码执行"，乱名导出 0x8eff2 解密后即为真实入口。
- 壳的其它运行时工件：`.cache/.3a5505535732c68fab3089f8df24c0dc`（4KB，r--映射）紧邻
  一个 RWX 匿名页；`files/.envelope/` 日志用后即被清空（再次观察到此前报告过的自清理现象）；
  壳配置 pref `Y29uZmlnXzVhMzg3MjM2YTQwZmEzNzQ4ODAwMDJmNA.sp` = base64("config_<易盾appkey>")。

## A.3 对上轮遗留 UNKNOWN 的回答

| 遗留问题 | 本轮结论 |
|---|---|
| nesec 是否写 NetHT 探针门 | **静态可分析范围内零证据，且两库完全解耦**（无 NEEDED、无符号互引；见 A.4 运行时新事实）。唯一理论路径 = 780KB 加密载荷运行时经 dlsym+内存写——需复现商业壳解密链或运行时读内存（前轮已触发反墓改升级，规范禁止绕过）才能彻底排除 → 保持 UNKNOWN，但**实际重要性大幅下降**（A.4） |
| NetHT glue 表（qword_4C2298）来源 | **闭合（E2 强）**：libNetHTProtect 自身导入 dlopen/dlsym 且含 "libc.so"/"linker" 明文串 → 反 Hook 函数表由 NetHT **自建**，与 nesec 无关 |
| nesec 自有网络/配置通道 | 静态导入**无任何 socket/SSL**——静态解包器无网络能力；载荷内部是否有网络逻辑 UNKNOWN |

## A.4 【重要新事实】libNetHTProtect 在普通浏览会话中根本不加载（E3）

观测会话：冷启动（官方 am start）→ 主页面 → 唤醒 → 反复滑动 feed，持续约 10 分钟：
**libNetHTProtect.so 从未出现在进程映射中**（对照 APK 偏移 0x64d378–0xab6330 无任何映射），
logcat 无任何 htprotect/nuid/netht/shuzilm/ddi 痕迹；而 UI 正常渲染、壳（nesec）正常工作。

推论与修正：
1. **"每次冷启动 NetHT 初始化 + nuid 进 X-App-Device"需要修正**：NetHT 是懒加载/条件加载
   （触发条件 UNKNOWN——本会话设备疑似未登录；按约不替用户登录验证）。仅当 NetHT 已加载的
   进程，其请求头才可能携带 nuid/DDI 复合值；未加载会话只有基础 X-App-Device。
2. 对根因矩阵的影响：NetHT 全部信号路径（installedApk field 12、电池扫描器、事件队列）
   **只存在于 NetHT 已加载的会话**。若日常浏览会话普遍不加载 NetHT，则 Issue #5 观察窗口内
   服务器可见的客户端安全信号主要来自：基础设备头 + metasec（Pangle）+ 行为序列——
   NetHT 路径权重进一步下降，metasec/服务端策略权重进一步上升。
3. 本设备无登录痕迹（无 user/session pref），登录态与 NetHT 加载的相关性待用户在登录状态下
   复测（无需做任何风险操作，仅需正常使用后由我复查 maps）。

## A.5 Pangle 会话计数补充（E3）

- 主 Pangle SDK：`pangle_com.byted.pangle_embed_last_sp_session` session_order=**57**——长期存在。
- live.lite（metasec 载体）：三个 session 文件，`session_order=1`（两个，mtime 08-26 10:43）
  → `session_order=4`（今日）——**metasec 载体至今仅 4 个会话，08-26 首启结论加固**。

## A.6 附录产物

- 脚本：`.tmp_audit/ns_real_dynsym.py`（真实动态表恢复）、`ns_imports.py`、`ns_strxref.py`、
  `ns_adrp_scan.py`、`ns_got_refs.py`。
- IDB 元数据：6 处 rename（nesec_tramp_dlopen/dlclose/dlsym/mprotect/sysconf、
  nesec_unpack_core_dlopen_site）+ 2 处注释，已保存。

---

## 附：本轮产物清单

- IDB 元数据：7 处 rename（`netht_black_module_scanner` / `netht_periodic_scan_battery_entry` /
  `netht_sdk_init_worker` / `netht_ioctl_dispatcher` / `netht_apk_parser_via_jni` /
  `netht_periodic_timers_register` / `netht_jni_aebd_gettoken`）+ 3 处函数注释，已 `idb_save`。
- 证据文件（`.tmp_audit/`）：
  - `pA_sub_24646C_full.c` / `pA_24646C_disasm.json`（分发器全量）
  - `pA_sub_2A658_full.c` / `pA_2A658_disasm.json`（black_module 扫描器）
  - `pA_sub_244778_full.c`（init worker）、`pA_sub_242788_full.c`（init JNI 入口）
  - `pA_sub_24188_full.c`（PackageParser 引擎）、`pA_gtfunc.json`（gt worker 反汇编）
  - `pA_ctor_full.c`（构造器默认值）、`pA_scan_writer.py`（写门扫描，0 命中）
  - `pC_dex_diff.py`（跨版本全集差分）
  - `pD_card_analysis*.py` / `pD_dump_classes.py` / `pD_xref_k.py` / `pD_dump_out.txt`（展示层分析）
- 本报告：`issue5_root_cause_closure_next_report.md`。

---

# 附录 B：libmetasec_ml.so 专项研究（第六会话，attach point 闭合）

分析时间：2026-08-28 晚（第六会话）。工具：IDA Pro MCP（`libmetasec_ml.so.i64`，用户已换库）、
androguard 4.1.3（live.lite / Pangle 主组件 dex 调用者分析）、mt-mcp（酷安 16.6.1 主 APK 字符串核查）、
设备 root 只读（文件名/时间戳/maps，未读受保护进程内存）。未 patch 字节、未注入、未开抓包、
未修改模块。证据等级沿用 E3/E2/E1/UNKNOWN。

## B.0 一段话结论

本轮把上一轮根因排序第 1 位（metasec 独立信号）的最大缺口 **attach point 与触发时机闭合到
E2/E3**：metasec 是 Pangle（字节系）广告/直播供应链的设备安全 SDK，结果经 **(a) live.lite 线
`report("cold_start")`/事件 → x-bdms-payload 头 → TTNet 上行 mssdk 后端（aid 219989）**、
**(b) Pangle 广告线 `getFeatureHash(请求体)` 签名头 + token 写入每一个广告请求**、
(c) 本地 `.msdata/mssdk/ml` 风险库三条路输出；触发 = 组件初始化 + 每次广告请求 + 事件上报，
"08-26 激活" = live.lite 组件当天首次下发初始化（前轮 UNKNOWN #6 就此闭合）。另发现
metasec 的 **JNIEnv vtable inline-hook 探针**（新检测面）。NetHT 结论零改动。

## B.1 native 边界（E3）

- **导出面（完整 dynsym 解析，脚本 `ms_exports.py`；155 项中 defined 仅 11）**：
  `JNI_OnLoad`（0x3F610）、`MSModuleCreator::MSModuleCreator`（0x3F560）、
  `MSModuleCreator::register_t`（0x45554）、6 个 `MSPBDataHelper::kString*` protobuf 占位常量。
  **无任何 `Java_*` 导出** → 全部 Java 接口经 JNI_OnLoad 内 RegisterNatives 注册（注册点位于
  混淆控制流内，静态未直接展开；Java 侧 dex 已补齐 native 方法全清单，见 B.2）。
- **导入面**：socket/connect/bind/sendto/recvfrom/inet_addr/epoll_wait +
  popen/fork/dlopen/dlsym/prctl/syscall/sigaction 族——采集与反调试全套能力。
- **模块框架闭合**：`register_t` 全部 5 处调用点还原——模块 **1**（sub_A7468，注册器
  ms_register_module_1）、**3/5/6**（sub_C76B4 冷区，W1=3/5/6）、**2**（sub_CECE8，工厂
  sub_CECBC，vtable `off_1E60E0`，`serialize_module2_payload` 位于 slot+0x58）；
  **模块 4 为运行期懒注册**（ms_module4_lazy_register=0x42A04：查 map 无 ID 4 时经
  sub_FD200(0x1000009) 构建后 sub_3F518 注册）——前轮"注册模块 1/2/3/5/6"补全为
  "1/2/3/5/6 静态注册 + 4 运行期注册"。
- 模块基类构造器 ms_module_base_ctor（0xC94B0，vptr=off_1E60E0）；模块分发模式
  （0x2EE0C 区域反汇编）：`ms_module_by_id_get(N)`（0x3F450→map）→ `BLR vtable+0x28`。
- **【新检测面】ms_jnienv_vtable_integrity_probe（0x6C3A4，E3）**：取 JNIEnv vtable 的
  **FindClass(6)/GetMethodID(33)/GetStaticMethodID(113)/GetStringUTFChars(169)/RegisterNatives(215)**
  五个函数指针，对每个经 `process_vm_readv(getpid(), …)` 直读目标函数序言 4 字节
  （绕过任何 inline hook 的页内改写），与解码期望值比对；不匹配经
  ms_finding_recorder_append（0xF4A0C，28 个调用者的通用"发现记录"汇聚点）写入模块发现
  列表，进入序列化链。语义 = **检测 JNI vtable 的 inline hook 行为本身**（ART hook 框架常见
  手法），而非匹配特定工具名。库内明文串仍无 Xposed/LSPosed/Zygisk 字样（前轮口径不变）。
- **原生 socket 面 = 本地探测，非数据上行（E3）**：
  - ms_local_bind_port_probe（0x5A954）：socket+bind 探测本地端口；EADDRINUSE 时记录事件；
  - ms_localhost_http_probe（0x5B0C8）：向 127.0.0.1 发 `GET / HTTP/1.1`（本地 HTTP 服务探测）。
- **设置键 kDisableIpCollection（E3）**：ms_setting_disable_ip_collection_reader（0x9A6B4）
  从设置对象读该键（==1 关闭），在 `collect_module2_risk_fields`（0xCF04C）内门控 **IP 采集**
  → 模块 2 采集面新增"本机 IP"一项（可被宿主配置关闭，且 Java 侧 SecConfig.getDisableIpCollection()
  正是同一键的上游，见 B.2）。
- mssdk_riskapp_db / mssdk_setting / last_rp_time（前轮 E3）继续有效。

## B.2 Java 侧 attach point（E3；live.lite 组件 dex）

组件 APK：`/data/user/0/com.coolapk.market/files/pangle_p/com.byted.live.lite/version-211448/apk/base-1.apk`
（59,584,677 B，08-28 02:21 刷新；拉取至 `.tmp_audit/ms_java/`，8 个 dex 全量 androguard 分析）。

- **唯一 JNI 桥**：`com.bytedance.mobsec.matrix.a.a(I,I,J,String,Object)→Object`（static native
  单入口分发器；`com.bytedance.mobsec.matrix.utils.m.TN` 为其 retrofit2 上报接口：p1/p2→Call）。
- **MSManager 公开 API**（`com.bytedance.mobsec.metasec.ml.MSManager/MSManagerUtils/MSConfig`）：
  `getToken()`、`frameSign(String,int)`、`getFeatureHash(String,byte[])`、`getReportRaw(String,int,Map)`、
  `report(String)`、`postEventMessage(MSBusinessHelper)`、`set{DeviceID,BDDeviceID,InstallID,
  SessionID,MsSettingConfig,CollectMode}`；后端 `com.bytedance.mobsec.metasec.a.ax`（实现 `az$a`）
  全部方法经 `matrix.a` 进 native。
- **初始化与触发（E3，本轮最关键单点）**：
  `com.bytedance.android.live.saas.middleware.sec.SecInitTask2B.realInit(Application, SecConfig)`：
  1. `MSConfig.Builder("219989", <硬编码 license 串>, String.valueOf(IAppInfo.appId()))`
  2. `setBDDeviceID(appLog.getDid())` / `setDeviceID` / `setInstallID(appLog.getInstallId())`
  3. `addAdvanceInfo("kOA1"|"kDisableIpCollection"|"kS1", …)`（OAID 开关 / **IP 采集开关** /
     传感器开关——与 B.1 native 设置键闭环）+ `setOaid(json.oaid)`
  4. 宿主权限门：`isCanUsePhoneState/isCanUseWifiState/isCanUseWriteExternal/isCanGetAndUseAndroidID/alist()`
     （**宿主"可读应用列表"权限包装器直接决定 metasec 应用采集面**）
  5. `MSManagerUtils.init(ctx, config)` → `setCollectMode(I)` → **`reportColdStart` → `MSManager.report("cold_start")`**
  事件入口：`SdkSecImp2B.report(String) → MSManagerUtils.get("219989").report(String)`；
  CJPay 的 `CJPayMSSDKManager.report` 同样经 `get("219989")`。
- **上行通道（E3）**：metasec Java agent（混淆包 `g/a/a/*`，113 类）：`g/a/a/ak` 用
  `com.bytedance.retrofit2.client.Request` + `com.bytedance.ttnet.utils.RetrofitUtils`（**TTNet/cronet**，
  组件内 libsscronet.so 佐证）构建上报，header **`x-bdms-payload` / `x-bdms-ctrl` / `x-t-zhg`**、
  query `&cdi=0.3&sh=report_sync`、sync/async 双模式、失败重试落 `t_report_synclog` 表；
  `g/a/a/an` 为 HttpURLConnection 降级路径。**libmetasec_ml.so 自身不做数据上行**（B.1）。
  上报 host 由配置驱动，静态未捕获硬编码域名（UNKNOWN，见 B.6）。
- 共存证据：组件内嵌 Turing 验证码配置（verify.snssdk.com / vcs.snssdk.com / secsdk-captcha CDN）。

## B.3 Pangle 主组件第二安全面（E3，本轮新增变量细化）

`pangle_p/com.byted.pangle/version-7805/`（08-27 21:30 更新）自带 **`libPglbizssdk_ml.so`**
（1,137,040 B；拉取至 `.tmp_audit/native/`）：导出仅 `JNI_OnLoad`；Java 类
`com.volcengine.mobsecBiz.metasec.ml.PglMS/PglMSManager/PglMSManagerUtils/PglMSConfig` +
`com.volcengine.mobsecBiz.matrix.pgla`（与 metasec_ml 同构的单入口 native 分发器）+
`PglITokenObserver`；agent 包 `ms.bz.bd.c.Pgl.*`（约百类，i0=getToken/report/getFeatureHash 后端）。
酷安主 APK 静态 dex 无 mobsecBiz 字样（mt-mcp 全文核查）——该 SDK 全部位于动态下发组件。

**广告请求签名链（E3，transport edge 闭合）**：
`com.byaztp.hc.f`（Pangle 集成类）`.ys()` = `PglMSManagerUtils.init+initToken`；
`.f(String,byte[])` = `getFeatureHash`；`.fx(String)` = `report`。
`com.byaztp.sc.fx.f`（**广告请求构建器**）在 `doHttpReqSignReady` → `doHttpReqSign` 阶段：
请求体 byte[]（`pvc/e.a()`）→ `hc/f.f(String, byte[]) → Map` → 签名 Map `putAll` 进请求头
（`znj/v` builder），`hc/f.fx()`（token 串）写入请求 JSON。
→ **每一次 Pangle 广告请求都携带 metasec 族 native 采集器产出的签名头 + token**；
酷安 feed 广告走 Pangle 时即触发（进程内实时签名，非离线缓存）。

## B.4 设备侧运行时验证（E3，root 只读）

- **`.msdata` 目录语义闭合（前轮 E2 → 本轮 E3）**：`.msdata/mssdk/ml/` 路径直接含
  `mssdk/ml`（= mssdk + metasec_ml）。文件名/时间戳可读（内容被 SELinux 拒，与 handoff §7
  一致）：
  | 文件 | 大小 | mtime |
  |---|---|---|
  | .msf3_04fa7481… / .msp_589c2233… / .mss_9b8ed995…（726B） | — | **2026-08-26 10:43–10:46**（首日） |
  | **.mss_442656d8…** | **33,620B** | **2026-08-28 15:19** |
  | .msf3_3afcbc4b…（124B） | — | 2026-08-28 15:19 |
  | .mss_1f149f2d…（1B）/ .msf3_0e6a186f…（8B）/ .msp_092fde7a…（209B） | — | 2026-08-28 15:40 |
  → **metasec 在 08-28 12:51 / 15:19 / 15:40 三个时点活跃**（白天会话），前轮"至今仅 4 会话"
  计数继续被刷新；33KB 大文件形态与 mssdk_riskapp_db/设置库一致（内容未证实）。
- live.lite 数据目录 08-28 15:40 活动：`socket_pipe`、`tt_net_config.config`、`IUtUidStore.xml`、
  `annie_setting_sp.xml`、`applog_stats.xml`；`applog_stats_219989.xml` 二次确认 aid=219989。
- **当前晚间会话（pid 8234，19:14 起）maps 中 0 个 pangle 映射**：Pangle/metasec 为
  **懒加载**——仅当广告 SDK 初始化（首次广告请求/live 组件任务）时映射；未激活会话完全
  不在场。与 NetHT 懒加载（附录 A.4）平行：**两大安全采集面都是"按需出现"**。

## B.5 触发时机模型（规范 §4-B 对位，CONFIRMED/INFERRED/UNKNOWN）

```text
live.lite 组件下发/更新（Zeus 组件加载）
→ SecInitTask2B.realInit
   → MSConfig(aid=219989, license, did/installId, kDisableIpCollection/kOA1/kS1, oaid)
   → MSManagerUtils.init → native JNI_OnLoad → RegisterNatives（混淆内）
   → 模块 1/2/3/5/6 就绪（4 懒注册）
   → setCollectMode → report("cold_start")
      → native 采集（root/Magisk/自动化应用/IP/JNI vtable 完整性/本地端口…）
      → 模块序列化（protobuf, MSPBDataHelper）→ 聚合
      → Java agent（g/a/a.ak）→ x-bdms-payload → TTNet → mssdk 后端     [E2/E3]
→ 此后每次 MSManager.report(event) / getToken / frameSign / getFeatureHash 均采集 [E3]
→ Pangle 广告线：每个广告请求 doHttpReqSign（getFeatureHash+token）             [E3]
→ 周期性上报：sync/async 双模式 + report_setting（SDKMonitor 侧 base_polling_interval
   30s 存在同族键；metasec 自身周期值 UNKNOWN）
```

- "为何 08-26 恰好激活" → **CONFIRMED**：live.lite 组件 08-26 10:42-43 首次下发初始化
  （前轮 §7 时间线 + 本轮 init 链），激活即采集上报；组件 08-27 21:30 更新至
  version-211448 后初始化链不变。
- 展示层（Priority D）、NetHT 结论（§1-§4、附录 A）：零改动。

## B.6 补充闭合（第六会话续，同晚）

1. **libPglbizssdk_ml.so ELF 级差分（E3，离线 dynsym 对比，无需 IDB）**：
   - 导出仅 `JNI_OnLoad`（metasec_ml 另有 MSModuleCreator/MSPBDataHelper 导出）；
   - 导入 130 vs 135；**Pglbizssdk 完全没有 socket/connect/bind/sendto/recvfrom/inet_addr**
     ——native 侧连本地网络探测都没有，采集纯靠 faccessat/fstat/__system_property_get/
     文件检查 + JNI；metasec_ml 独有 bind/connect/sendto/recvfrom/inet_addr/uname/getenv
     （对应 B.1 本地探针）。
   - 判定：同族瘦身高变体（1.1MB vs 2.0MB）；**B.7 段的"本地端口/服务探测"仅 metasec_ml 有**。
2. **上报 host 的设备侧可读性核查（E3）**：`pangle_com.byted.live.lite/server.json`（可读）
   = TTNet 配置（dispatch host 仅 tnc0-*.zijieapi.com、webcast5-open-lf.douyin.com，无 mssdk 专用
   host）；keva `hybrid_settings.blk`（可读）仅含 lynx 设置；`tt_net_config.config` 仅 device_id/tnc
   etag。→ **mssdk 上报 host 不存在于本设备任何可读存储**；且 `g/a/a/ak` 的请求 URL 参与方
   包括 native `matrix.a` 返回值（URL/路径可能由 native 产出）。B.6.1 边界进一步收紧。
3. **module 4（补充）**：工厂 sub_FD200 受全局状态门（off_1F6EF8/1F6F00 非零才构建）控制
   ——懒注册还依赖某个 init 期状态；工厂链为多级混淆间接跳转，语义维持 UNKNOWN。
4. **last_rp_time 节流值**：键串全部运行时栈构造解码（IDB 无明文），定位消费点需重做
   五解码器 × 92 构造器常量恢复（前轮方法）；当前会话未执行——方法已记录，留待有需要时。
5. **本机登录态复查（E3）**：shared_prefs（857 项）与 databases 均无账号/user/session 存储
   → 设备仍为未登录态；§6.2（NetHT 加载是否登录态触发）仍等待用户自行登录后复测 maps。

## B.6U What Remains UNKNOWN（增量，更新后）

1. x-bdms 上报具体 host——已排除"设备可读存储"来源（见 B.6.2）；剩余可能：native 产出或
   服务器下发后仅存内存。无阳性自然抓包前不收敛；如抓须标 `CAPTURE_ENVIRONMENT=PROXIED`。
2. module 4 语义（懒注册 + 状态门控，参数 0x1000009；工厂链多级混淆）。
3. libmetasec_ml 内 RegisterNatives 注册表全量展开（Java 侧已闭环，边际收益低）。
4. `.mss_442656…`（33KB）内容（SELinux 拒读；未证实是否 mssdk_riskapp_db）。
5. libPglbizssdk_ml 函数级采集项清单（ELF 级差分已闭合，见 B.6.1；函数级需换 IDB，暂缓）。
6. metasec 自身周期上报间隔（B.6.4 方法已记录）。
7. **NetHT 加载/初始化触发条件**（继承主报告 UNKNOWN #10）——等待用户登录后复测。

## B.7 Module Impact

MODULE_CODE_CHANGE = NOT_PART_OF_THIS_TASK
FOUND_DIRECT_EDGE = NO

- metasec 检测的是**环境与行为基线**（root/Magisk 路径、adbd、自动化应用目录、JNI vtable
  inline-hook、本地端口/服务、IP、（其它设备）应用列表），与模块运行时行为无独占交集：
  模块不以 127.0.0.1 提供服务、不改 JNIEnv vtable（LSPosed 的 Java 层 hook 不 inline-patch
  JNI vtable 函数本体）、不占用非常规端口；模块自身 APK 位于常规 /data/app/ 路径。
- 宿主权限门 `alist()`（IHostPermissionWrapper）决定 metasec 应用采集面——本机
  GET_INSTALLED_APPS=denied（D4）同样约束该通道（E2 推断，与 NetHT pm collector 平行）。
- `KEEP MODE A-ZF FROZEN` 继续成立。

## B.8 产物清单

- IDB（`libmetasec_ml.so.i64`，已保存）：11 处 rename——ms_jnienv_vtable_integrity_probe
  (0x6C3A4)、ms_module_base_ctor(0xC94B0)、ms_module4_lazy_register(0x42A04)、
  ms_setting_disable_ip_collection_reader(0x9A6B4)、ms_local_bind_port_probe(0x5A954)、
  ms_localhost_http_probe(0x5B0C8)、ms_module_by_id_get(0x3F450)、
  ms_register_modules_3_5_6(0xC76B4)、ms_register_module_1(0xA7494)、
  ms_register_module_2(0xCECE8)、ms_finding_recorder_append(0xF4A0C)；+5 处注释。
- 脚本：`.tmp_audit/ms_exports.py`（通用 ELF dynsym 解析）、ms_find_regnatives.py、
  ms_jni_sites.py、ms_vcall58.py、ms_vtable.py、ms_pb_users.py、ms_netcallers.py、
  mt_call.py（mt-mcp 无状态调用器）。
- 取证：`.tmp_audit/ms_java/`（live_lite_base1.apk、pangle_main_comp.apk、classes*.dex、
  caller_result*.txt / sec_classes*.txt / pgl_analysis.txt / hc_f_callers.txt / sc_fx2.txt 等）；
  `.tmp_audit/native/libPglbizssdk_ml.so`（1,137,040 B，自设备 version-7805 拉取）。
- 设备（只读）：`.msdata/mssdk/ml` 文件名+时间戳表、live.lite 数据目录时间戳、pid 8234 maps
  pangle 零映射观测。

## B.9 第六会话续 2：登录态更正与 NetHT 触发条件研究（UNKNOWN #10 攻坚）

### B.9.1 【更正】设备实际为登录态（E3，UI 截图实证）

前轮（§3.6 / A.4）"当前疑似未登录态（无 user/session pref）"**系误判**：登录态存储不在
可读 pref 文件名名单内（857 个 pref 中无 user/session/account 命名；内容受 SELinux 保护
不可读）。UI 截图（`.tmp_audit/login_check.png`）确认：已登录 **酷友44002572**（Lv.0，
90/300 EXP，0 动态/关注/粉丝，含"账户不安全请设置密码与邮箱"提示条）。

### B.9.2 登录态 NetHT 阴性实证（E3，推翻"登录态触发"假设）

观测会话：pid 8234（19:14 起，登录态），真实浏览：首页 feed 滑动 6 屏 + 打开帖子详情 +
评论区滑动 4 屏（截图 feed_check.png / detail_check.png 证实内容真实渲染），等待后复查：

- maps：0x64d378–0xab6330（libNetHTProtect）**始终零映射**；唯一加载的壳内安全组件仍是
  libnesec（0x0/0x483b000/0x483c000/0x484b000）+ 常规 umeng/auth/httpdns/ucrash/xgVipSecurity；
- logcat（--pid）：`nuid|htprotect|netht|shuzilm` **0 条**；
- Pangle/live.lite 组件映射同样为 0（懒加载结论再证）。

→ **"NetHT 加载疑似登录相关"假设被推翻**。判定：NetHT 加载由业务侧门控（服务端配置键或
特定业务流程），与登录态无必然关系。与旧观察"DDI 开关后 x-app-device 出现 64-char 扩展
字段"（早期 P3/D5 轮）拼合：开关性质，当前会话该门为关。

### B.9.3 NetHT 业务调用者闭合（E3，runtime DEX 迷你解析器）

`main_useDDI.dex`（nesec 运行时 DEX dump，androguard 解析失败，改手工 DEX 解析）：

- **调用者类 = `com.coolapk.market.manager.NetEaseProtectSDKManager`**（真实业务类名，含
  协程链 `initSDKAndID$1 / initializeDeviceIDInternal$1/$2$deviceID$1 / onRealInit$1/$2$1/$2`；
  类簇内另含 OaidManager 的 OAID 证书下载逻辑：`com.coolapk.market.cert.pem`、
  "Oaid证书下载成功，保存MD5"/"Oaid证书下载失败"）；
- 并存独立 SDK：**数链魔方 `cn.shuzilm.core`（Main/Listener/BuildConfig）** +
  `com.coolapk.market.manager.ShuzilmSDKManager`（getSessionSync/initID/postEventAndGetSessionID/
  updateRetryJobStatus）——数链魔方是与 NetHT 并行的另一设备风险 SDK，此前报告中
  X-App-Device 链的 "SHUZILM" 串归属至此闭合；
- HTProtect 封装（混淆类 `Lඞ;`）持有：**productId = "YD00000551137681"**（易盾产品号）、
  配置键 **"PostToken.productId"**、日志 tag "shuzilm "、"getToken result: token:"——
  与 P3 轮 PostToken/X-App-Device/nuid 链完全对齐；
- 该业务管理器**不在静态壳 dex 与业务 restored dex**（两者 htprotect 引用均为 0），
  全部位于运行时 DEX——与 nesec 加壳模型一致；
- dump 内部分 class_def 破损（insns_size 乱值），LoginCheckManager 与 NetEaseProtect 的
  调用边归属置信度受限（E1-E2）：观察到 LoginCheckManager/KSDocHelper/ClearCacheHelper
  对管理器 lambda 的 invoke 位点，但不作为最终结论。

### B.9.4 UNKNOWN #10 更新后状态

```text
NetHT 加载/初始化触发条件：
  - 登录态触发         → 已排除（B.9.2，E3 阴性）
  - 调用者身份         → 已闭合（B.9.3，E3）：NetEaseProtectSDKManager（+ 并行数链魔方 SDK）
  - 触发门具体键/当前值 → UNKNOWN（业务侧配置门；需门开时的会话或 main_useDDI 完整反编译）
```

对根因矩阵：hypothesis #2（服务端 policy/rollout）再获结构性支撑——NetHT 通道是否在场
由业务配置门决定，客户端代码恒在；metasec 线（B.1-B.5）不变。

### B.9.5 产物补充

- `.tmp_audit/ms_java/htprotect_dex_scan.txt` / `htprotect_callers2.txt` / `htprotect_callers3.txt`
- `.tmp_audit/ms_java/mini_dex_callers.txt` / `nepsm_details2.txt` / `nepsm_callers.txt` /
  `logincheck_detail.txt` / `nepsm_class.txt`（main_useDDI.dex 迷你解析器系列输出）
- 截图：`login_check.png`（我的页-已登录）、`feed_check.png`、`detail_check.png`
- maps 快照：`maps_8234_evening.txt` / `maps_8234_after_scroll.txt` / `maps_8234_final.txt`

---

# 附录 C：libNetHTProtect 记录注册表写入侧与配置应用通路（第六会话，IDB 切回后）

分析时间：2026-08-28 深夜（第六会话续）。工具：IDA Pro MCP（用户已切回
`libNetHTProtect.so.i64`）。只读 + rename/comment 元数据已保存；未 patch 字节。
目标 = 第五会话遗留 UNKNOWN #2（记录注册表 key 203695656 的写入者）与 #4
（sub_250410/sub_25057C 定时器内容），顺带核实第五会话"探针门不可翻转"结论在
新发现的配置应用通路下是否仍然成立。

## C.1 注册表结构（E3）

- 全局单例：`netht_record_mgr_holder_get`（0x9FAE4，全局 `qword_46DAD8`，24B holder +
  1896B 管理对象）；`netht_record_mgr_obj`（0x9FB8C）= holder+16。
- `netht_key_to_registry_offset`（0x9B83C）= **键→偏移映射器**：~40 个整型键各映射到管理对象
  内一个容器（+48/+96/+144/…/+1800）。**key 203695656 → +48**（black_module 扫描器读的容器，
  记录 40 字节步长，类型 tag 0x4204F8CA = 非常规路径 APK）。
- 第五会话"约 20+ 电池成员读 key 203695656"需精确化：直接以该键调用映射器的**只有
  black_module 扫描器一处**；其余电池成员各自使用不同键/容器（每个键 = 独立注册表）。

## C.2 写入侧闭合（E3）——三条路径全部汇聚于 netht_command_batch_apply

`netht_command_batch_apply`（0x30D808，已更名）= **命令批处理器**：
- 入参 (registryObj, payload, override)；payload 经 sub_100EDC 解析出状态码；
- **code 200** → 遍历类型化命令列表：
  - **type 5 → 配置应用**：`netht_config_commands_apply`（0x2617D0）作用于配置单例
    （getter sub_26173C → `qword_4C26E0`）+ `netht_config_normalize`（0x261D00）规范化；
  - **type 9 → 记录插入**：`netht_record_cmd_verify_insert`（0x79758，MD5 校验
    `netht_payload_verify_md5`/0x79824 后）→ `netht_record_insert_wrapper`（0x9FB94）→
    `netht_record_json_insert`（0x9FDB0，解析含 **"version"** 字段的 JSON 记录，插入管理器
    map，更新管理对象 +384 序列号）；
  - **type 14** → 其它操作（sub_230C58/sub_7AEEC）；
- **code 5509** → 清 ctx+396；code 非 200 → 错误串（栈解码 "inmt data ..."）。

**三条喂入路径**（全部 E3）：

1. **ioctl selector 16（Java 注入）**：0x246878（跳转表 case 16，注释确认）→ glue vtable
   +0x98 取串 → `netht_hash_to_registry`（0x30CF8C，按参数 1 哈希选目标容器）→
   `netht_command_batch_apply` → 成功写 **ctx+392 = 0** / 失败 -1。
   ——第五会话 ioctl 表"selector 16 写 ctx+392 状态字"就此完全解开：**selector 16 =
   Java 运行时下发命令批/记录**（酷安侧 NetEaseProtectSDKManager 经 HTProtect wrapper 可达）。
2. **本地伪装资源加载**：`netht_registry_payload_loader`（0x241D20，**唯一调用者 = 探针调度器
   sub_2483A8**）：先试文件 `<vtable+8 前缀>/home_button_*.xml`（栈解码路径），失败则经组件
   AssetManager 读 **`assets/home_button_colorful3.xml`（伪装名）**；尾部 16 字节 MD5 校验
   → 解密 → 命令批应用 → 同样写 ctx+392。**本设备实测不存在该文件**（酷安 base.apk、
   Pangle 两组件 APK、app files 目录均无——E3 阴性，与 NetHT 未加载、门关一致）。
3. **跨注册表传播**：`netht_registry_cross_propagate`（0x230BF8）：从键 1207439958 的注册表
   取 JSON，提取键 "/v4/c"（栈解码）→ 命令批应用。

第五会话"写入者 UNKNOWN（候选：文件访问拦截层/Java ioctl 事件/其它探针）"就此闭合：
**无文件访问拦截；注册表由"命令批"数据驱动**（Java ioctl 16 / 伪装资源文件 / 跨注册表传播）。

## C.3 配置应用通路的精确边界（E3，强化第五会话结论）

`netht_config_commands_apply` 解析类型化命令（type 0 = cfg+48 map 删项，type 1/2 = map
写项），随后 `netht_config_normalize` 对 **~50 个数值字段做范围钳制**（写入集合实证）：

- 间隔字段：cfg[84]（电池间隔）、cfg[88]（30min 定时器）、cfg[132]（10min 定时器）
  钳制 **[1,60] 分钟**；cfg[792]/[804]/[836]/[416] 等阈值/计数钳制；
- 模式字段：cfg[336]/[340]/[344]（installedApk 模式族）钳制 {1..4}、默认 2；
  cfg[300]±数组（a1[16..20]，偏移 256–324）逐元素钳到 2；
- map 字符串项：cfg+48/+24/+8 三个映射的增删；
- 附：type-5 附带 `netht_cmd_open_app_settings`（0x263388，栈解码
  "/android_setting_info_game19…"）——引导宿主跳转应用设置页的命令，非门写。

**探针门字节核对（本轮逐项核对写入集合）**：788/800/808/809/821/823/825/840
**全部不在 sub_261D00 与两个 map 处理器（0x2618E4/0x261AC8）的写入偏移集合内**。
第五会话结论"探针门 ctor 一次写、库内不可翻转"**在新的配置应用通路证据下仍然成立**，
且从"未发现写者"升级为"写者已定位、其写入白名单不含门"。

> 修正说明：第五会话 §2.4"单例 getter sub_26173C 全库仅 4 处引用"表述的是全局
> `qword_4C26E0` 的数据引用（init flag/alloc/ctor/store）；getter 函数本身的 code 调用点
> 实为 **100 处**（含本轮新定位的 netht_command_batch_apply type-5 路径、apk parser、
> 全部电池成员）。扫描 A（调用点后 10 条指令索引式 STRB = 0）不受影响。

## C.4 定时器内容闭合（E3，UNKNOWN #4 关闭）

- **`netht_battery_sweep_30min`（0x250410，30 分钟定时器）**：顺序执行 **16 个电池成员**
  （loc_41ED0/415CC/299FC/3A8B8/47ED8/3CCF0/[cfg[617] 门] 4A5EC/54A6C/3C030/
  [cfg[705] 门] 49410/51688/28700/462B0/4D5EC/580C4/51E34）；若 cfg[678]（默认 0 = 关）
  且任一成员命中 → **sub_230B68(7) 立即构建 eventType 7**。
- **`netht_pkg_cache_refresh_10min`（0x25057C，10 分钟定时器）**：`netht_selector_enabled(11)`
  + ctx 条件 ==1 → **netht_cache_ingest_new_pkgs（增量包缓存刷新）** → sub_3ED10 处理
  ——即 installedApk 增量通道的周期补充源（与 getToken 时的增量快照互补）。

## C.5 对根因矩阵的影响

- 假设 #7（电池扫描器事件）：输入源从 UNKNOWN → **E3 闭合**（命令批数据驱动；
  本设备伪装资源不存在 → 实际输入只剩 Java ioctl 16 注入，是否存在取决于酷安业务侧，
  仍 UNKNOWN 但边界已收窄）。
- 假设 #1/#2（NetHT 门翻转/服务端改门）：维持排除，且证据升级（C.3）。
- 新增精确事实：**服务端/宿主可远程调的旋钮 = 扫描节奏（1–60 分钟）、阈值、installedApk
  模式、注册表内容注入**；不可调 = 探针门。这与"对既有信号打分/调度"的服务端策略模型
  （假设 #2）完全相容——服务器调节奏与阈值、不改采集器开关。
- 顺带闭合：UNKNOWN #4（定时器内容）。

## C.6 IDB 元数据

16 处 rename（netht_record_mgr_holder_get / netht_record_mgr_obj /
netht_key_to_registry_offset / netht_command_batch_apply / netht_registry_payload_loader /
netht_record_insert_wrapper / netht_record_json_insert / netht_record_cmd_verify_insert /
netht_payload_verify_md5 / netht_registry_cross_propagate / netht_config_commands_apply /
netht_config_normalize / netht_battery_sweep_30min / netht_pkg_cache_refresh_10min /
netht_cmd_open_app_settings / netht_hash_to_registry）+ 5 处函数注释，已 `idb_save`。

## C.7 产物

- `.tmp_audit/nB_sub_9B83C.c`（键映射器）、`nB_scanner.c`（扫描器现版反编译）、
  `nB_reg_callers.py`（访问器调用点/键值扫描）、`nB_users_survey.txt`（单例使用者普查）
