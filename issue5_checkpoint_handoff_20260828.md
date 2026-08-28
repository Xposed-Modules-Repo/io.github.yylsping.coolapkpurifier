# Issue #5 研究Checkpoint 与 Handoff（2026-08-28 第五会话末）

> **【2026-08-28 第六会话更新】§6.1 主线已完成**：metasec attach point 与触发时机闭合到 E2/E3
> （SecInitTask2B.realInit→report("cold_start")、x-bdms-payload→TTNet 上行、getFeatureHash 进
> 每个 Pangle 广告请求、.msdata/mssdk/ml 实证、新检测面 JNI vtable 探针），详见
> `issue5_root_cause_closure_next_report.md` **附录 B**——接手者请先读附录 B，勿重做 metasec 线。
> 剩余 UNKNOWN 见附录 B.6。
>
> **【第六会话续 2 更正】**：①设备**实际已登录**（酷友44002572）——本文件 §3.6"疑似未登录态"
> 系误判，§6.2 登录复测**已完成**：登录态真实浏览后 NetHT 仍零加载（附录 B.9.2），"登录态触发"
> 假设推翻；②NetHT 调用者闭合 = `com.coolapk.market.manager.NetEaseProtectSDKManager` + 并行
> 数链魔方 SDK（cn.shuzilm.core），productId YD00000551137681 / 'PostToken.productId' 配置键
> （附录 B.9.3）；③触发门具体键仍 UNKNOWN（B.9.4）。

> 用途：后续模型直接接手 Issue #5 根因证据链闭合任务。本文自包含关键事实、资产、工具链与下一步，
> 但**仓库内最新报告永远优先于本文快照**。工作区：`D:\Python Worm\coolapk-purifier`。

---

## 0. 一句话状态

Issue #5（酷安风险提示根因）客户端证据链已推进到：**NetHT 通道结构性静默（门不可翻转 + 多数
会话根本不加载）+ metasec（Pangle 供应链）实测与观察窗口同日激活（08-26 10:43）+ 16.5.1→16.6.1
安全栈零差分** → 根因前两位 = **metasec 独立信号（缺 attach point）与服务端策略/阈值**。
下一步主线 = 用户已批准换 IDB 分析 `libmetasec_ml.so.i64`（闭 attach point），辅线 = 验证
NetHT 加载触发条件（登录态相关性）。**当前因用户原因暂停，等恢复后先做 metasec。**

---

## 1. 必读文件（按权威顺序，冲突时靠前优先）

1. `issue5_root_cause_closure_next_report.md` — **本轮主报告 + 附录 A（nesec 专项）**，最新最全。
2. `issue5_root_cause_closure_updated.md` — 研究规范（优先级/禁止项/停止条件/输出格式）。
3. `issue5_full_p4b_session_report.md` + `issue5_cross_version_security_diff.md` — 跨版本与门控矩阵基线。
4. `issue5_p1p2_predicate_report.md` — installedApk 谓词/field12 结构/aebd→X-App-Device 全 E3 链。
5. `issue5_d1d4d5_report.md` — 远程配置默认值/nuid 生命周期/应用可见性实测。
6. `AGENTS.md`（仓库根）— Git/设备/LSP 红线。`C:\Users\yylsping\.zcode\AGENTS.md` — 通用原则。
7. 本文件。

历史报告中**已被本轮推翻/修正的结论**见 §4 修正表——接手后不要把旧报告结论当事实引用。

---

## 2. 任务红线（必须遵守）

- 研究轮**不改模块源码/Hook/resolver/设置页/Gradle**，不重打实验包；`MODULE_CODE_CHANGE = NOT_PART_OF_THIS_TASK`。模块 2.2.1 候选冻结（发布另等人工验收，当前因开屏漏网 `RELEASE_READY=NO`）。
- 不为取阳性制造风险条件；不以"未复现"下结论；不 spoof/tamper/hook probe 结果；不绕过反墓改。
- **未授权禁止任何远程 git 写入**（push/PR/release 等）；本地 commit 允许。
- 设备调试产物落 `/data/local/tmp/processing/`；装 APK 用 `adb push` + `su -c 'pm install -r -t [--user 0]'`。
- **不重启手机、不重启 LSP 守护进程**；模块开关只经 LSPosed GUI；重启作用域应用用官方方式。
- **IDA 换载入文件必须先停下征得用户同意**（本轮 metasec 换库已获口头批准，但执行前用户叫停——恢复后需再确认 IDB 已由用户换好，用 `server_health` 验证 `idb_path`）。
- 手机 frida（`/data/local/tmp`，进程名 `android.system.svc`）可用但**不要对酷安进程 attach 取自然样本**（易盾反墓改会杀进程；规范 §5 隔离原则）。mt-mcp：`http://192.168.2.251:8787/mcp`（curl 直连，不在 zcode 配置里）；工具不可用时优先 curl 自行解决，多次失败要停下报告。

---

## 3. 已闭合事实速览（E3 为主；接手后勿重做）

### 3.1 NetHT（libNetHTProtect.so 5.7.6，16.5.1==16.6.1 逐字节相同）

- **探针门不可翻转**：配置单例 `qword_4C26E0` 仅被 getter sub_26173C 引用；全库 getter 调用后
  0 处寄存器索引 STRB；ioctl 分发器（sub_24646C，1673 insns 全量反汇编）0 索引写入；字面偏移写门
  仅构造器 → **LSPosed/zygisk/smaps/mnt/odex/filepermisson/mis 门与 cfg[788]（gt 全量门）构造时
  一次写入默认 OFF，库内永不变**。
- **"gt" = 每次 getToken 计一次**：`aebd1811194e82d9`（sub_243B18）→ 0x247B1C worker 记录 "gt"
  → 首次 count==1 ∧ cfg[788]（恒0）→ **gt 全量模式结构性不可能激活**；installedApk 恒为增量。
- **"周期配置更新电池"实为周期检测器电池**（sub_24FFD4 注册，间隔 60s×cfg[84]=**1 分钟**/30min/10min）：
  11 个成员全是"节流→读记录注册表(key 203695656)→selector 检查→推事件入全局安全队列 sub_2F3308"。
  其中 **sub_2A658 = black_module 扫描器**（非常规路径 APK → "black_module" 事件；首发现触发
  sub_230B68(1) 立即构建 + 写 `/.hrecord...` + 疑似 exit(0)）；富集引擎 sub_24188 用 JNI 隐藏 API
  `android.content.pm.PackageParser` 解析任意 APK。
- **JNI 表全 9 方法已映射**（sub_24391C RegisterNAMES）：init=`hccd63688a790ca65`、ioctl=`d0f149b4da6ec477`
  （selector 1-22/100：root:0/1 查询、`event_data`/`keyevent` 事件注入、`view_` 计数、版本"5.7.6"、
  default "unsupported request"）、blob=`aebd1811194e82d9`。Java `HTProtectConfig`
  （channel/gameKey/host/getExtraData）只进 ctx，与 cfg 门无映射边。
- **NetHT 懒加载（本轮实测 E3）**：冷启动+浏览 feed ~10 分钟进程里 **libNetHTProtect.so 从未映射**、
  无 nuid/htprotect 日志 → 只有 NetHT 已加载的进程请求头才带 nuid；触发条件 UNKNOWN（疑似登录相关，
  待验证，见 §6.2）。
- glue 表 qword_4C2298（反 Hook 函数表）由 **NetHT 自建**（自身导入 dlopen/dlsym + "libc.so"/"linker"
  明文串），与 nesec 无关。

### 3.2 libnesec.so（易盾壳，953,496 B）

- 真实动态表（节表被故意污染，用程序头+PT_DYNAMIC 解析，脚本 `ns_real_dynsym.py`）：
  53 导入（dlopen/dlsym/dlclose/dladdr/dl_iterate_phdr、popen/pclose、sigaction 族、文件 IO 族）+
  唯一乱名导出 @0x8eff2（size 1500，指向加密载荷，盘上为密文）。NEEDED 不含 NetHT。
- 分级自解壳：真实 INIT_ARRAY（经 RELA 恢复）= 0xd9464/0xd9508/0xd9bf4；ctor1 解密 ctor2 所需
  指针表；解包核心 0xE1440：栈构串→mprotect→dlopen。780KB 载荷（0x190–0xc4000）运行时**就地
  解密为 r-x 活代码**（实测映射 7c6e82d000+）。静态代码（0xd8040–0xe96d8，228 函数）无网络导入、
  无 NetHT 痕迹 → **nesec→NetHT 写门：静态零证据，两库解耦**；残留仅"加密载荷内部"这一不可静态
  验证路径。
- 运行时工件：`.cache/.3a55...`（4KB r--）+相邻 RWX 页；`files/.envelope/` 日志自清理（再次观测）；
  壳配置 pref `Y29uZmln...` = base64("config_5a387236a40fa374880002f4")。

### 3.3 跨版本（16.5.1 → 16.6.1）

- 唯一 native 变更 `lib/arm64-v8a/librtmp-jni.so`（直播）；NetHT/nesec 逐字节不变。
- 壳 DEX 字符串池全集差分（+68567/−55055）：安全面唯一 delta = "installedApk" 明文串消失
  （疑似移入加密区，E2）；Splash 子系统重构（与 16.6.1 开屏路径变化互证）；其余为 UI 库。
- 客户端版本间解释力≈0 → 时间相关性解释转移给服务端策略/外部供应链（Pangle）。

### 3.4 Pangle / metasec 时间线（E3）

- 主 Pangle SDK：session_order=57（长期存在，随 APK 捆绑）。
- **live.lite（metasec_ml.so 载体）：首会话 2026-08-26 10:42–43**（`.pangle_i`、数据目录 12+ 子项
  时间一致；三个 session pref 中两个 order=1 且 mtime 08-26 10:43），至今仅 4 会话；
  version-211448 为 08-27 21:30 更新；csj.ext version-1164 08-27 21:42；08-28 02:21 组件随应用
  重装刷新。**与 Issue #5 观察窗口（08-25/26）同日激活** — 当前根因第一位候选。

### 3.5 展示层（Priority D，收敛）

- 通用能力（P5 S2）+ 专用警示资产：`feed_warning` 布局族、`item_alert_message_card` +
  业务 DEX 类 `Lк;`（AlertMessageCardViewHolder，#19DB4437/#FDD9D7 警示配色）+ Entity→Dialog 通路
  （MessageCardDialogFragment）。okhttp 链无响应改写中间件（名为 Interceptor 的 Feed* 类是 UI 层
  置顶弹窗，假阳性已排除）。`ipRiskRating` 属腾讯网络组件（假阳性已排除）。
  `PRESENTATION_CAPABILITY=high`；`ACTUAL_RISK_ROW_ENDPOINT=UNKNOWN`。

### 3.6 本设备状态（D4 沿用）

- ColorOS `GET_INSTALLED_APPS` denied+USER_FIXED → NetHT pm collector 只见宿主自己 →
  field 12 不含模块包名（本机）。
- 酷安 16.6.1（2608212）firstInstall 2026-03-05，lastUpdate 08-28 02:18（splash 诊断重装，覆盖了
  升级时间）；模块 2.2.1 lastUpdate 08-28 15:08。当前疑似**未登录态**（无 user/session pref）。

---

## 4. 旧报告修正表（引用旧结论前必读）

| 旧报告 | 旧结论 | 修正（本轮 E3） |
|---|---|---|
| D1（issue5_d1d4d5_report.md §1.1 尾） | sub_2A658 = 配置更新器（写 cfg[624/768/780/784]） | **错误**。它不写任何 cfg；实为 black_module 周期扫描器。IDB 已更名 `netht_black_module_scanner` |
| E1/P4B（issue5_full_p4b_session_report.md §1.3-2） | JNI 分发器含索引式 STRB → 私有通道可远程翻转探针门 | **错误**。分发器及其前级 0 索引写入；门构造后库内不可写 → "服务端翻转 NetHT 门"假设失去机制支撑 |
| P4B 根因排序 #1 | 服务端 rollout 翻转 NetHT 探针门 | 降级为排除（本库内）；服务端策略假设保留但改经"对既有信号打分"而非改门 |
| D5/时序模型 | 每次冷启动 NetHT 初始化、nuid 常驻上行 | 修正：NetHT 懒加载；未加载会话请求头无 nuid（附录 A.4） |
| P1 "gt" 语义 | config["gt"]==1 疑似远程开关 | 修正：gt = getToken 调用计数（首次=1），且 cfg[788] 恒 0 → 全量模式不可能 |

---

## 5. 当前根因排序（第五会话版）

1. **metasec（Pangle）独立信号 + 服务端消费**——唯一"能力 E3 + 实测激活 + 与窗口同日"的检测面；缺 attach point（下一步主线）。
2. **服务端 policy/阈值/rollout**（对基础设备头/X-App-Token/行为序列打分）——客户端零 delta 排除法强化；展示层能力充分。
3. **NetHT 电池扫描器事件（black_module 等）**——能力 E3、默认每分钟跑；但记录注册表输入源 UNKNOWN，且仅 NetHT 已加载会话在场。
4. ROM/权限差异（其它设备条件路径）；5. ~~翻转 NetHT 门/LSPosed·Zygisk 经 NetHT~~（已排除）；6. 模块特有行为（无 M1-M5 证据，`KEEP MODE A-ZF FROZEN`）。

---

## 6. 下一步任务（恢复后执行）

### 6.1 主线：libmetasec_ml.so.i64 分析（用户已批准换库，恢复时先让用户确认 IDB 已换好）

文件：设备 `/data/user/0/com.coolapk.market/files/pangle_p/com.byted.live.lite/version-211448/lib/libmetasec_ml.so`（2,058,272 B）；本地应有 `.tmp_audit/native/libmetasec_ml.so(.i64)`（若无：`adb pull` 到该目录再让用户在 IDA 打开——**必须经用户，不得自行换**）。

分析目标（QUESTION/EDGE 形式）：
1. `module2 root/risk 字段 → serializer → 对外导出接口`（前轮已 E3：30 个 root/Magisk 路径表、
   adbd 命令探针、mssdk_riskapp_db）→ 找导出符号/JNI 注册表/回调表，确定**谁消费结果**
   （Pangle SDK Java 层？上行 endpoint？）。
2. metasec 采集的触发时机（广告请求时？独立周期？）→ 解释"为何 08-26 恰好激活"（live.lite 组件
   首次下载那天）。可配合只读设备证据（keva/.msdata 时间戳、hybrid_settings_downloader）。
3. 若发现网络上行：在已有 HTTP captures（若含 Pangle/CSJ 域名请求）中对齐字段。
停止条件：attach point 闭到 E2/E3，或证明结果只留在本地不上行。

### 6.2 辅线：NetHT 加载触发条件（UNKNOWN #10）

用户正常登录酷安使用 ≥10 分钟后（**由用户自行登录，模型不得操作账号**）：
`adb shell su -c 'pidof com.coolapk.market'` → `cat /proc/<pid>/maps | grep base.apk | grep r-xp`
看 0x64d378–0xab6330 偏移段（NetHT）是否出现 + `logcat -d --pid=<pid> | grep -iE "nuid|htprotect"`。
对照未登录会话（本轮 pid 8234 阴性样本）→ 回答"nuid 通道何时在场"。

### 6.3 候补（低优先）

- NetHT 电池记录注册表（key 203695656）写入者（NetHT IDB 内做插入侧 xref）。
- sub_250410/sub_25057C（30/10min 定时器）内容。
- `AlertMessageCardViewHolder(Lк;)` 分发 template 与宿主页面。
- nesec 载荷离线解密（工作量大，仅在 metasec 线索枯竭后再评估）。

---

## 7. 资产清单

### 7.1 IDB（均在 `.tmp_audit/native/`，已含本轮 rename/comment 并 save）

- `libNetHTProtect.so.i64` — 本轮 +7 更名：`netht_black_module_scanner(0x2A658)`、
  `netht_periodic_scan_battery_entry(0x250310)`、`netht_sdk_init_worker(0x244778)`、
  `netht_ioctl_dispatcher(0x24646C)`、`netht_apk_parser_via_jni(0x24188)`、
  `netht_periodic_timers_register(0x24FFD4)`、`netht_jni_aebd_gettoken(0x243B18)`；
  上轮命名沿用（installedapk_append_loop=2FA14C、event_queue_serialize=2F3C84、
  queue_insert=2F3308、ctor=252F68、getter=26173C、named_key_record=26578C 等）。
- `libnesec.so.i64` — 本轮 +6 更名：`nesec_tramp_dlopen(0xD9340)/dlclose(0xD9350)/dlsym(0xD9360)/
  mprotect(0xD9380)/sysconf(0xD92F0)`、`nesec_unpack_core_dlopen_site(0xE1440)`。
- `libmetasec_ml.so.i64` — **待用户准备**（上一轮 IDA 曾加载过，结论见 issue5_static_audit_report §1.6-1.9 区）。

### 7.2 关键反编译/脚本（`.tmp_audit/`，pA*=NetHT 轮、pC*=差分、pD*=展示层、ns_*=nesec 轮）

- `ida_call.py` — **IDA MCP 调用器**（见 §8）。`ida_session.txt` 是会话票据。
- pA：`pA_sub_24646C_full.c`（ioctl 分发器）、`pA_24646C_disasm.json`、`pA_sub_2A658_full.c`
  （black_module）、`pA_gtfunc.json`（gt worker @0x247b1c 反汇编）、`pA_ctor_full.c`（默认值：
  cfg OWORD 0x399C70/80 → 84=1/88=30/100=30/108=10；+132=10；+616..617=0；+677=1,+678=0；
  +800..840 门区）、`pA_scan_writer.py`（写门扫描 0 命中）、`pA_sub_24188_full.c`（PackageParser）、
  `pA_sub_244778_full.c`（init worker）、`pA_sub_242788_full.c`（init JNI 入口，config getters 解码：
  channel/gameKey/host/getExtraData）。
- pC：`pC_dex_diff.py`（16.5.1 vs 16.6.1 全集差分）。
- pD：`pD_card_analysis2.py`/`pD_dump_classes.py`/`pD_xref_k.py`/`pD_dump_out.txt`。
- ns：`ns_real_dynsym.py`（**通用技巧**：绕过污染节表恢复真实动态表）、`ns_got_refs.py`、
  `ns_imports.py`、`ns_adrp_scan.py`、`ns_strxref.py`。
- 运行时 DEX：`.tmp_audit/p3stream/{main_useDDI,sdk_netht,wrapper_ref}.dex`（main_useDDI 不能整
  解析，只能字符串级；sdk_netht 混有腾讯组件串，注意假阳性）；业务 DEX
  `.tmp_audit/regions_clean/coolapk_business_restored.dex`（androguard 可解析，11,278 类）。
- 三版壳 classes.dex：`.tmp_audit/reply16x/corpus/`。APK：`history-apks/`。

### 7.3 设备（192.168.2.251:35897 无线 adb，root su 可用）

- 酷安 16.6.1；本轮观测 pid 8234（未登录态，NetHT 阴性样本）；maps 全量快照在会话记录
  （apk r-x 偏移→lib 归属：libauth=0x11ac000、libdu=0x22e8000、libhttpdns=0x2fbc000、
  libucrash=0x3538000、libumeng-spy=0x354c000、libxgVipSecurity=0x3668000、
  **libnesec=0x483b000+（LOAD0 r-x 于 7c6e82d000）**、**NetHT 0x64d378–0xab6330 无映射**）。
- APK 内 so 偏移表：`zipfile` 枚举 `history-apks/CoolApk-16.6.1-...apk` 的 header_offset（本轮脚本示例见报告 §A.2）。
- Pangle 状态：live.lite version-211448（08-27 21:30 装）、session pref 见 §3.4。

---

## 8. 工具链复现（含本轮踩坑）

### 8.1 IDA Pro MCP（`http://127.0.0.1:13337/mcp`，zcode 配置里没有，须自连）

```bash
# 首次 initialize 拿 Mcp-Session-Id（存 .tmp_audit/ida_session.txt），之后：
python "D:/Python Worm/coolapk-purifier/.tmp_audit/ida_call.py" <tool> '<json参数>'
```
- 工具：server_health / decompile / disasm / xrefs_to / py_eval / py_exec_file / set_comments /
  idb_save / search_text / imports / get_bytes 等。
- **坑**：① xrefs_to 用 `{"addrs":["0x.."]}`；② disasm 大函数返回截断，按提示
  `curl -o out.json http://127.0.0.1:13337/output/<uuid>.json` 取全量；③ py_exec_file 的 path
  **用正斜杠**且参数名 `file_path`；④ set_comments 用 `{"items":[...]}`，rename 走
  py_eval `idc.set_name`；⑤ Windows 反斜杠会打断 JSON，一律正斜杠；⑥ MCP 断连先重试
  `rm .tmp_audit/ida_session.txt` 重新 initialize，多次失败停下报告用户。
- **NetHT IDB 函数边界有误**：0x247b1c（gt worker）有自己的序言但被并入 sub_24646C，
  Hex-Rays 不输出该冷区 → **直接 disasm 该地址**，别信 decompile 完整性。

### 8.2 mt-mcp / 其它

- mt-mcp：`curl -X POST http://192.168.2.251:8787/mcp -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d '<jsonrpc initialize>'`，工具表在其 initialize 响应里（mt_apk_* 系列）。
- androguard 4.1.3：`from androguard.core.dex import DEX`（旧 import 路径失效）；
  source file 取法 `dv.get_cm_string(c.get_source_file_idx())`；业务 DEX 用它，main_useDDI 会
  `unpack` 崩（只能字符串级）。
- 设备：`adb shell su -c '...'`；启动酷安 `am start -n com.coolapk.market/.view.main.MainActivity`。
- **反墓改雷区**：读 `/proc/<pid>/mem` 会触发升级（先零填充后 EACCES 连 maps 一起拒）；
  前轮成功法=冷启动后单窗口一次流式读（`adb exec-out` 设备零落盘）。本轮只读了 maps（安全）。

---

## 9. Git 状态

- 最近两提交：`54f9e04`（nesec 附录 A + 时序修正 + UNKNOWN 更新）、`48b208c`（主报告）。
- 工作区尚有**模块侧未提交改动**（splash 相关源码/README 等，属模块工作流，研究轮不要碰、
  不要顺手提交）。`.tmp_audit/` 按惯例不入库。
- 报告写作惯例：只提交 .md；本地 commit 仅检查点。

---

## 10. 恢复口令（给下一个模型的第一条行动清单）

1. 读本文件 §0-§6；2. `python .tmp_audit/ida_call.py server_health` 确认当前 IDB；
3. 若用户已换 `libmetasec_ml.so.i64` → 按 §6.1 开工；若还在 libNetHT/libnesec → **停下问用户**；
4. 严格 §2 红线；5. 阶段产出写回 `issue5_root_cause_closure_next_report.md`（或新附录 B），
   本地 commit；6. 遇 MCP 多次断连/需换 IDB/需登录验证 → 停下找用户。
