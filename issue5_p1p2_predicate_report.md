# Issue #5 Priority 1/2 — installedApk 精确入选谓词与 field 12 元素结构（E3 闭合报告）

分析时间：2026-08-26（夜间会话）
宿主：酷安 16.6.1（2608212）；模块：2.2.0 Mode A（冻结中）
工具：限域 IDA Pro MCP（只读分析 + rename/comment 元数据），IDB：`.tmp_audit/native/libNetHTProtect.so.i64`
本轮未触发任何风控操作、未修改 Mode A、未 patch 任何字节。

本轮完成执行规范中的 **Milestone A**（installedApk exact inclusion predicate → E3）与 **Milestone B**（field 12 element structure → 服务器实际获得的内容已明确）。

---

## 0. 本轮命名映射（已写入 IDB）

| 原名 | 新名 | 语义 |
|---|---|---|
| sub_2FA14C | netht_installedapk_append_loop | field 12 append 主循环 |
| sub_276E54 | netht_cache_snapshot_all | 缓存全量快照（rdlock 深拷贝） |
| sub_27BD94 | netht_fresh_pm_collect_snapshot | event6 "gt" 全量重采路径 |
| sub_276DDC | netht_cache_get_instance | 缓存单例 getter |
| sub_277278 | netht_cache_ingest_new_pkgs | 增量 ingest（只收缓存外新包） |
| sub_2777D4 | netht_enrich_gate_and_call | enrich 大小门 + 调用 |
| sub_277EB0 | netht_enrich_record_perms_components | 权限/服务/组件计数 enricher |
| sub_277A1C | netht_cache_insert | map+list 插入 |
| sub_2814C4 | netht_list_node_create304 | 节点分配 malloc(304) |
| sub_27BC08 | netht_promote_reported_to_skip | +280 → +281 promotion |
| sub_27BCA0 | netht_mark_snapshot_reported | snapshot 合并回写 +280=1 |
| sub_27CBDC | netht_cache_map_find | map 查找（hex key） |
| sub_2E9D48 | netht_str_to_upper_hex | 字节串→大写 hex（key 编码） |
| sub_241440 / sub_2413C8 | netht_md5_hex_string / netht_md5_digest | MD5 + "%02x" 32 字符 hex |
| sub_273B74 | netht_parse_pm_list_output | popen 解析 pm list |
| sub_26FCEC | netht_run_pm_list_third_party | pm list -f --show-versioncode -U -3 |
| sub_306000 / sub_306014 | netht_prebuild_ingest / netht_async_build_and_promote | 构建前 ingest / 构建后 promotion |
| sub_30737C | netht_report_scheduler | 上报调度器（节流 60×cfg[100] 秒） |
| sub_28CE04 | netht_read_oom_score_adj | 读 /proc/self/oom_score_adj |
| sub_2F3C84 | netht_event_queue_serialize | 事件队列序列化（append_loop 调用者） |
| sub_102E24 | netht_reptd_string_new_slot | repeated string 取新槽 |

---

## 1. New E3 edges（全部为本轮直接汇编/反编译证据）

```text
(1) netht_report_scheduler(sub_30737C) --async--> netht_async_build_and_promote(sub_306014)
    → sub_306194 → netht_event_queue_serialize(sub_2F3C84, eventType)
    → netht_installedapk_append_loop(sub_2FA14C, AndroidSuspiciousInfo, eventType)
    Evidence: 调用链 0x307A90/0x307B38 dispatch；0x2F5658 (sub_2F3C84 → sub_2FA14C)；
    sub_2F3C84 签名 (a1, a2, a3@W2=eventType)

(2) append 条件 rec+281==0；skip 条件 rec+281!=0
    Evidence: LDRB W27,[X28,#0x119] @0x2FA5C0；CBZ W27 @0x2FA6F4（为 0 才进处理块；
    编译器把处理块外移为冷路径，证明 +281!=0 是常见情况）

(3) 构建成功后 promotion：netht_mark_snapshot_reported 把 snapshot 内所有记录对应
    缓存节点 payload+280=1（STRB [X22,#0x128] @0x27BCFC）；随后
    netht_promote_reported_to_skip 遍历链表把所有 +280 已置位节点 payload+281=1
    （LDRB/STRB [X19,#0x128]/[#0x129] @0x27BC40/0x27BC48）
    Evidence: netht_async_build_and_promote 仅在输出非空（构建成功）时调 promotion @0x3060F8

(4) 缓存链表头 xmmword_4C2E70 全部操作者仅 3 个：snapshot/insert/promote
    → 无删除、无清空、+281 进程内永不清零
    Evidence: xref 扫描（0x276EBC/0x277CCC/0x277CDC/0x27BC30）

(5) ingest 门：cfg bit11(sub_2300E0(11)) && !throttle(26CF8C,20) &&
    (cfg[712] || oom_score_adj<=0) && 包名不在缓存
    Evidence: sub_277278 0x2772AC..0x277300；netht_read_oom_score_adj 解码
    "/proc/self/oom_score_adj"

(6) enrich 门：宿主自身包（vtable+88 自有名）豁免大小检查；其余包需
    apkSize(sub_2E95FC(raw)) != -1 && <= cfg[512]<<20
    Evidence: sub_2777D4 0x27782C..0x2778E8（CMP W21,W8,LSL #20 @0x2778E4）

(7) 288B 记录布局（消费端 X28 基址汇编 + 生产端模板 +24 位移对齐）
    → 见 §3 schema

(8) 缓存 key = pm 行路径字段（"package:" 与 ".apk=" 之间子串）的大写 hex
    Evidence: netht_str_to_upper_hex（311/343 双模式 nibble 编码）；
    netht_parse_pm_list_output 字段抽取顺序；netht_cache_map_find 按 key 比较

(9) "gt" 全量路径：eventType==6 && config["gt"]==1 && cfg[788]
    → netht_fresh_pm_collect_snapshot 重新跑 pm list，记录标志全零（全 append），
    且其记录 +0 为明文路径、与缓存 hex key 永不匹配 → merge-back 不命中
    → 全量快照不破坏增量状态
    Evidence: sub_2FA14C 0x2FA294 分支；sub_27BD94 memset 模板；sub_27BCA0 查找逻辑

(10) event 8 专属 append 门：cfg[624] || (hasAnnotation && cfg[674])；其余事件恒 append
     Evidence: sub_2FA14C LABEL_203 尾部分支（0x2FB4xx 区三处 slot 写入点条件）

(11) 元素帧：marker"$S_BF#A" + "{" + md5hex32 + "}" + payload
     Evidence: XOR-58 解码 7 字节 @0x2FA5A8 区；netht_md5_hex_string "%02x" 循环

(12) 9 个标注字节 = 9 个已解码权限/框架匹配（见 §3）；标注受 cfg bit22 门控

(13) 附加采集面（新确认，非 field 12）：
     - uid 共享/名称异常 → type 7 "uid_match" 事件（进全局事件队列）
     - /storage/emulated/0/Android/data 相邻 UID 扫描 → type 2000 "mis" 事件
       （cfg[823] 门控，事件 {0,3,5,8,10}）
     - 卸载/变更检测：component-8 列表剩余项 → "changedPackages" 报告（事件 {3,10}）
     Evidence: 均为 sub_2FA14C 内解码字符串 + sub_2F3308 全局队列调用
```

---

## 2. InstalledApk inclusion predicate（伪代码，每个条件附证据）

```text
bool shouldAppendInstalledApk(record, cache, eventType, config, now) {
    // ---- 阶段 0：进入缓存（被 collector 看到）----
    // netht_cache_ingest_new_pkgs：
    if (!config.bit11)                          return false;  // E3: sub_2300E0(11)
    if (throttled(id=20))                       return false;  // E3: sub_26CF8C(20)
    if (!config[712] && oom_score_adj > 0)      return false;  // E3: /proc/self/oom_score_adj
    if (cache_contains(hex(pmLine.path)))       return false;  // E3: map 查找
    // 枚举源 = pm list packages -f --show-versioncode -U -3（仅第三方包）
    // enrich 门：
    if (pkg != ownPackage && apkSize > config[512] MiB) return false;  // E3
    // 宿主自身包：豁免大小检查，仍 enrich
    // 通过 → 插入缓存，payload+280=0, +281=0

    // ---- 阶段 1：进入本次 snapshot ----
    if (eventType == 6 && config["gt"] == 1 && config[788]) {
        snapshot = fresh_pm_list();             // 全量重采；pm 失败则 0 条 → field 12 空
        foreach r in snapshot: r.flags = 0;     // 全部候选，无 incremental 过滤
    } else {
        snapshot = cache_all_nodes();           // 全部缓存节点，无筛选
    }

    // ---- 阶段 2：逐记录 append 判定（netht_installedapk_append_loop）----
    foreach record in snapshot {
        record.flag280 = 1;                     // visited（无条件）
        if (record.flag281 != 0) continue;      // E3: 已上报 → 跳过（进程内一次性）
        // uid/name 异常检查只发 type-7 事件，不阻断 append
        // （同 UID 异名 → "uid_match"；自家名前缀但 UID 不同 → "uid" 事件）

        payload = record.packageName;                                   // rec+48
        if (config.bit22) {
            if (f96 && f97 && f102) payload += "@@su and alert and readclop";
            if (f98)                payload += "@@inject";
            if (f97 && f99)         payload += "@@writesec";
            if (f97 && f100)        payload += "@@bindacc";
            if (f101)               payload += "@@unknown";
            if (f103)               payload += "@@shizuku";
            if (f104)               payload += "@@mockloc";
        }
        if (record.label != "")        payload += "@@" + record.label;   // rec+72
        if (record.acts > config[784]) payload += "@@as:" + itoa(record.acts);  // rec+200

        element = "$S_BF#A" + "{" + md5hex(payload) + "}" + payload;

        if (eventType == 8) {
            if (config[624] || (hasAnnotation && config[674]))
                appendField12(element);        // 门通过才 append
        } else {
            appendField12(element);            // 恒 append
            if (eventType in {0,3,5,10}) extraProcess(record.apkPath);  // sub_301320
        }
    }

    // ---- 阶段 3：构建后状态迁移 ----
    markAllSnapshotNodes(flag280 = 1);         // netht_mark_snapshot_reported
    if (buildSucceeded)                        // 输出非空
        promoteAll(flag280 -> flag281);        // netht_promote_reported_to_skip
    // → 默认路径下每个包在整个进程生命周期内最多进入 field 12 一次
}
```

**未恢复条件**：无。`config[...]` 各偏移的远程默认值（624/674/784/788/512/712/788/823/bit11/bit22、`"gt"`、节流秒数因子 cfg[100]）由服务端/本地配置对象提供，静态不可得默认值——但谓词结构本身全部 E3。

### 执行规范三-节的 4 个问题

1. **collector 是否理论上能看到本模块包？**
   能。`pm list packages -f --show-versioncode -U -3` 枚举全部第三方包；本模块是普通第三方包，非宿主自身、无大小豁免问题（1.1 MB），理论上一定被解析、enrich 并进入缓存。

2. **"被 collector 看到"与"进入本次 field 12"是否不同？**
   不同，而且是三段不同：
   - 进入缓存（看到）≠ 进入 snapshot（默认全量快照所有缓存节点，但新包必须先过 ingest 门）；
   - 进入 snapshot ≠ append（+281 已置位的记录被跳过；event 8 还有 cfg[624]/cfg[674] 额外门）；
   - append 发生后，构建成功即 promotion，**同包在本进程内不再进入 field 12**。

3. **field 12 是 full snapshot 还是 incremental subset？**
   两种模式：
   - 默认：**严格增量**——每包每进程生命周期至多一次（首次成功构建后 promotion，永不重发）；
   - `eventType==6 && config["gt"]==1 && config[788]`：**全量快照**（绕过缓存重跑 pm list，标志全零，全部 append；且不污染增量状态）。
   aebd1811194e82d9 blob getter 主链即 eventType=6，因此服务器侧该开关决定每次取到的是增量还是全量。

4. **哪些条件决定某包进入 payload？**
   汇总：① 是第三方包（-3）；② ingest 门（cfg bit11、节流 20、oom_score_adj≤0 或 cfg[712]、缓存 miss）；③ enrich 门（宿主自身豁免；其余 APK ≤ cfg[512] MiB）；④ 记录 +281==0（首次）或处于 gt 全量路径；⑤ eventType==8 时 cfg[624] 或（有标注 && cfg[674]）；⑥ 构建成功（失败则本轮不 promotion，下轮重试）。

---

## 3. Field 12 元素 schema（InstalledApkElement）

元素是 **repeated string**（wire tag 0x62 = field 12, length-delimited），内容为一条 ASCII 帧：

```text
element := "$S_BF#A" "{" <32-char lowercase md5 hex> "}" <payload>
payload := packageName [annotation]* ["@@" label] ["@@as:" acts]
```

| 片段 | 来源（记录偏移） | 语义 | 证据等级 |
|---|---|---|---|
| `"$S_BF#A"` | 常量（XOR-58 解码） | 帧 marker（字面如此，7 字符，语义标签未知） | E3（字节级） |
| `{md5hex}` | netht_md5_hex_string(payload) | payload 完整性校验（MD5, "%02x"） | E3 |
| packageName | rec+48（std::string） | 包名（与宿主自名比对的同字段） | E3 |
| `@@su and alert and readclop` | f96&&f97&&f102 | 同时请求 ACCESS_SUPERUSER + SYSTEM_ALERT_WINDOW + ACCESS_MOCK_LOCATION（字面含 "readclop" 拼写，原样报告） | E3（门）/E3（字面） |
| `@@inject` | f98 | 请求 android.permission.INJECT_EVENTS | E3 |
| `@@writesec` | f99（需 f97 前置） | 请求 WRITE_SECURE_SETTINGS（前置 ALERT_WINDOW） | E3 |
| `@@bindacc` | f100（需 f97 前置） | 请求 BIND_ACCESSIBILITY_SERVICE（无障碍） | E3 |
| `@@unknown` | f101 | 请求 android.permission.UNKNOWN | E3 |
| `@@shizuku` | f103 | 请求 moe.shizuku.manager.permission.* | E3 |
| `@@mockloc` | f104 | 请求 ALLOW_MOCK_LOCATIONS | E3 |
| `"@@" + label` | rec+72 | 包 label（sub_27B9AC 从包信息提取，键 "|a"/"v"） | E2（语义为 label 是强推断） |
| `"@@as:" + acts` | rec+200（>cfg[784] 时） | 组件计数之一（键 "acts"，推测 activities；"as" 字面） | E3（键/门）/E2（as=acts 推断） |
| 标注整体 | cfg bit22 门控 | 全部标注可被远程关闭 | E3 |

**进入缓存但未进入 field 12 payload 的记录字段**（仅存在于本地记录/其他通道）：
apkPath（rec+24）、UID（rec+196）、versionCode（rec+192）、六项组件计数其余五项（sers/pros/recs/pers/mets，rec+204..220）、minSdk/targetSdk 字符串（rec+232/256）、两条 cfg[432] 门控的 vtable+192 字符串（rec+144/168，疑似签名摘要，E2）、uid%1000 标志（rec+224）、appid 邻差（rec+284）。
→ **服务器经 field 12 实际只拿到：包名 + 敏感权限标注 + label + 可选 acts 数**；不含路径/UID/版本号/签名/完整权限列表。

其他并行通道（非 field 12，但同循环内触发）：
- 共享 UID 异名 → type 7 `uid_match` 事件（全局事件队列，与 LSPosed/Zygisk 2000 号事件同队列）；
- `/storage/emulated/0/Android/data` 相邻 UID（[self-200, self+200]∩[10200,10500]）目录扫描 → type 2000 `mis` 事件（cfg[823] 门控）；
- component-8 列表剩余项 → `changedPackages` 报告（事件 {3,10}），即卸载/变更检测。

---

## 4. JNIFactory.aebd… → x-app-device final64

本轮未触碰，维持 **E2**（结构边：eventType=6 blob getter ↔ DDI 头尾 64 字段）。P3 待做。

## 5. Cross-version security diff / 6. Generic Feed model / 7. Private-directory collector

本轮未做（P4/P5/P6 待做）。私有目录方面本轮新增一条相关边界：Android/data 扫描的是**外部存储**（`/storage/emulated/0/Android/data/<pkg>`），不是宿主私有目录 `/data/user/0/*`；后者仍未发现 collector（保持未确认）。

---

## 8. Security Signal Influence Matrix（本轮更新行）

| Signal | Collector | Local cache | Payload field/event | Transport | Timing | Evidence |
|---|---|---|---|---|---|---|
| third-party APK（含本模块） | pm list -3 → ingest/enrich | 内存链表+map，hex(path) key，append-only | field 12 字符串元素（包名+权限标注+label+可选acts），**每进程每包一次**；或 "gt" 全量 | aebd…(eventType 6) → x-app-device final64 | 构建前 ingest；节流 60×cfg[100]s；前台（oom_adj≤0）才 ingest | E3（本轮闭合） |
| 敏感权限（9 项：SUPERUSER/ALERT_WINDOW/INJECT_EVENTS/WRITE_SECURE_SETTINGS/BIND_ACCESSIBILITY/UNKNOWN/ACCESS_MOCK_LOCATION/shizuku/ALLOW_MOCK_LOCATIONS） | 包信息 vtable+88 | 记录 f96..104 | field 12 标注子串（cfg bit22 门控） | 同上 | 同上 | E3（本轮闭合） |
| 共享 UID 异名 | append 循环内比对 | 无 | type 7 `uid_match` 事件 | 事件队列→security blob | 每次 build | E3（本轮新增） |
| 相邻 UID 外部存储目录 | /storage/emulated/0/Android/data 扫描 | 无 | type 2000 `mis` 事件（cfg[823]） | 事件队列→security blob | 事件 {0,3,5,8,10} | E3（本轮新增） |
| 卸载/变更 | component-8 列表差集 | 无 | `changedPackages` 报告 | 报告 a2+16 容器 | 事件 {3,10} | E3（本轮新增） |
| LSPosed/Zygisk/ART/framework-mount/root 等 | 既有结论不变 | — | type 2000 事件 | 事件队列 | 启动 | E3（前轮） |

## 9. Root-cause ranking 影响

- "应用列表参与风控"假设从"高"升级为**结构性确认**：第三方包全集 → 缓存 → field 12（一次性增量或 gt 全量）→ aebd blob getter。**但 field 12 携带的内容比此前假设的少**（无 UID/版本/签名/完整权限列表），服务器拿到的核心就是包名+9 项敏感权限标注+label。
- 重要修正：**默认模式下每个包只在进程生命周期内上报一次**。这意味着"装了模块后每次请求都在上报包名"不成立；首次构建后 field 12 对老包为空。风险窗口集中在：冷启动首轮构建、以及 "gt" 全量开关被服务端打开时。
- 新增采集面（uid_match、Android/data 相邻 UID 扫描、changedPackages）扩大了环境感知范围，但均为通用环境信号，与模块 2.2.0 差分无特定指向。

## 10. Module change recommendation

```text
KEEP MODE A FROZEN
```

理由：本轮全部新证据（M1–M5 判据逐一核对）——
- collector 把本模块包当作普通第三方包处理，无模块包名专属匹配（M5 不成立）；
- field 12 元素内容 = 包名+权限标注+label，不含 Hook/文件特征（M2 不成立）；
- 未发现任何指向模块现有 Hook 面的直接消费边（M1/M3/M4 不成立）。
模块包被采集是**所有第三方应用共同面对的基线行为**，不构成模块特有风险；继续冻结。

## 验收记录

- IDB 注解：23 个函数 rename + 8 条函数注释已写入并保存（`idb_save` ok）。
- 未修改任何机器码；未使用 patch/patch_asm；未触发设备端操作。
- 证据文件：`.tmp_audit/full_2FA14C.c`、`full_snapshot_funcs.c`、`full_cache_funcs.c`、`full_enrich_funcs.c`、`full_enrich2_funcs.c`、`full_flag_funcs.c`、`full_build_wrappers.c`、`full_md5_caller.c`、`offset_scan2.txt`、`disasm_windows.txt`、`callsite.txt`、`prologue_check.txt`、`decode_strings.py`。
