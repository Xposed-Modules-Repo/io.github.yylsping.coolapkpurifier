# Issue #5 — D1/D4/D5 阶段报告（Remote Config / Private Collector / nuid Lifecycle）

分析时间：2026-08-26（第三会话）
执行规范：`issue5_next_stage_after_abc_e3.md`（停止条件：D1-D5 完成任意 3 项 → 本轮完成 D1、D4、D5）
工具：限域 IDA Pro MCP（`libNetHTProtect.so.i64`，只读 + 元数据）、HTTP Toolkit（已有捕获）、root 只读文件读取、`su <uid>` 模拟。
未启动风控会话、未修改 Mode A、未 patch 任何字节、未绕过 /proc 防护。

---

## 1. Remote Config Activation Table（D1）

### 1.1 Native 配置对象（848B 单例，`sub_26173C` 返回，构造函数 `sub_252F68`）

| Native selector | 结构偏移 | 默认值 | 消费点/效果 | 证据 |
|---|---:|---:|---|---|
| （节流因子） | cfg[100] | **30** | `sub_30737C`：构建节流 `≥ 60×cfg[100]` 秒 = **30 分钟** | 构造函数 `xmmword_399C80`(+96 OWORD) 第 2 dword |
| enrich 签名门 | cfg[432] | **1 (byte, ON)** | `sub_277278`：vtable+192 签名串 enrich | `*(_WORD*)(a1+432)=257` → 低字节 1 |
| APK 大小上限 | cfg[512] | **200 (MiB)** | `sub_2777D4`：非宿主包 APK > 200MB 不 enrich | `*(_QWORD*)(a1+512)=0x32000000C8` 低 dword 0xC8 |
| event-8 强制门 | cfg[624] | **0 (OFF)** | `sub_2FA14C`：event8 恒 append 的开关 | `*(_BYTE*)(a1+624)=0` |
| uid%1000 检查 | cfg[665] | **1 (ON)** | `sub_277278` 记录+136 类字段 | `*(a1+664)=0x1000101` 第 2 字节 |
| event-8 标注门 | cfg[674] | **1 (ON)** | event8 有权限标注即 append | `*(a1+669)=0x0101...` 字节 674=1 |
| ingest 前台豁免 | cfg[712] | **1 (ON)** | ingest 无视 oom_score_adj | `*(_BYTE*)(a1+712)=1` |
| acts 阈值 | cfg[784] | **10** | acts>10 才追加 `@@as:` | `*(_DWORD*)(a1+784)=10` |
| gt 全量门 | cfg[788] | **0 (OFF)** | event6+"gt"==1 且此门开 → 全量快照 | `*(_WORD*)(a1+788)=256` 低字节 0 |
| mis/Android-data 扫描 | cfg[823] | **0 (OFF)** | append 循环尾部相邻 UID 外存扫描 | `*(a1+820..832)=0` |
| filePermisson 扫描 | cfg[840] | **0 (OFF)** | `sub_27C00C` 外存目录扫描（本轮 D4 新发现） | `*(_BYTE*)(a1+840)=0` |
| bit11（ingest 总门） | cfg[300]（selector 11 = 256+4×11） | **2 (>1 = ON)** | `sub_2300E0(11)`：`sub_22FF8C(11)>1` | 构造函数 +288..+332 OWORD 全 2 |
| bit22（标注总门） | cfg[344]（selector 22） | **2 (ON)** | `sub_2300E0(22)`：权限标注块 | 同上 |
| "gt" 计数 | 上下文对象 B(+360) 的 key→计数表 | 空 | `sub_266C30(ctx,"gt")==1`：命令 "gt" 被记录恰一次时全量快照条件之一 | `sub_26578C` 记录器（JNI 实现内部 12 处调用） |

配置更新器：`sub_2A658`（写 cfg[624/768/780/784]）由 `sub_250310` 调用（无直接 xref = 线程/派发表入口）——NetHT 私有配置通道，当前运行值静态不可得（默认值如上；需受保护内存读取才能确认是否被服务端覆盖）→ **runtime override 状态 = UNKNOWN**。

### 1.2 Coolapk 侧远程配置（当前真实值，来自今日实际 `/v6/main/init` 响应，HTTP Toolkit 捕获）

| Remote key | 当前值 | 消费点（已 E3 反编译） |
|---|---|---|
| `MainInit.useDDI` | **"1"** | `ShuzilmSDKManager.ؠ` → 字段 `.ՠ`（DDI 复合设备 ID 开关） |
| `MainInit.useDDISessionId` | **"1"** | 同上 → `.ֈ`（ddid cookie/getSessionSync 开关） |
| `MainInit.useDDIEvent` | **"0"** | （event 通道关闭） |
| `MainInit.useDDIEventList` | createFeed/reply/like/likeReply/message/send | DDI event 白名单 |
| `PostToken.businessId` | **"aca7df55758868ea76460c100ef168e9"**（固定） | `RequestSessionIDUpdater.Ԫ` → `NetEaseProtectSDKManager.ׯ(businessId)` → `HTProtect.getToken` |
| `PostToken.List` | **[createFeed, reply]**（仅发帖/回复） | `ExtraPostFieldInterceptor` 仅对命中路径的 POST 加 `_v2_post_token` 表单字段 |
| `Stat.reportExpose` / `reportProgress` | "0" / "1" | （与既有结论一致） |
| `MediaPlayer.jar` | 服务端 hotfix dex URL（videoParser_2608200.dex） | 动态 dex 热修通道存在（P5 参考：服务端可下发可执行代码） |

### 1.3 规范 2.3 节六问回答

1. **16.6.1 是否打开 "gt"**：静态默认不打开（cfg[788]=0）；"gt" 是命令计数器==1 的条件而非布尔开关；运行时是否被服务端置位 = UNKNOWN。**默认行为 = 增量**。
2. **cfg[788] 当前值**：默认 0；运行值 UNKNOWN（受保护内存）。
3. **bit22 是否开启**：默认 ON（selector 22 = cfg[344] = 2 > 1）；运行值 UNKNOWN。
4. **权限标注是否实际进入 field 12**：默认门全开（bit22 ON、cfg[512]=200MB、本模块 1.1MB 通过）→ **能力上会**；但见 §4：本设备 collector 只枚举到宿主自己 → 实际上**无第三方包可标注**。
5. **cfg[512]/cfg[712]/cfg[100] 当前值**：默认 200 / 1 / 30（运行覆盖 UNKNOWN）。
6. **eventType 6 默认 full 还是 incremental**：**incremental**（cfg[788] 默认 0）。且冷启动缓存为空 → 首次 build 即"事实全量"（见 §3）。

---

## 2. Private Directory Collector（D4）：**NOT_FOUND（libNetHTProtect 范围）**

全库 `opendir` 仅 3 个调用者，逐一反编译：

| 调用者 | 扫描目标 | 产出 | 门控（默认） |
|---|---|---|---|
| `sub_27C00C` | **`/storage/emulated<U+200B>0/Android/data`**（外置存储，路径含零宽空格） | readdir → 跳过 `.`/`..`/纯数字名 → `snprintf("%s/%s")` + `stat` → **{目录名, st_mode} 对 → 正是 `AndroidSuspiciousInfo.filePermisson`（field 9）** | cfg[840]=0 **OFF** |
| `sub_193518` | 另一处固定路径（非安全子系统区） | 非 collector（辅助） | — |
| `sub_32CB5C` | 非 /data/data 扫描 | — | — |

**结论**：
- `/data/data/<pkg>`、`/data/user/0`、宿主 `files/`、`cache/` 的枚举在 libNetHTProtect 中 **NOT_FOUND**（opendir/fopen 无此类调用者；fstatat 仅用于 metasec 固定 root 路径表——前轮已证）。
- 模块文件（`coolapk_purifier_config.json`、`coolapk_purifier_cache_v4.json`、`libdexkit-10.so`，实机 files/ 目录确认存在）**没有任何已识别采集边**。
- `filePermisson` 字段的真实语义 = **外置存储 Android/data 下各包目录名+权限位**（不是宿主私有文件权限），且默认关闭。
- libmetasec_ml 侧维持前轮结论（固定 root/自动化路径表，无通用私有目录枚举，E2-前轮）。

---

## 3. nuid Lifecycle / Refresh Semantics（D5）

```text
冷启动
→ ShuzilmSDKManager.initSDKAndID: cn.shuzilm.core.Main.init(ctx, <RSA公钥>) + setConfig
→ Shuzilm DID（'D' 前缀）→ 持久化 pref 'SHUZILM_DID'（x5c 包装）
→ 完成后回调 NetEaseProtectSDKManager.ނ() → initializeDeviceIDInternal（协程）
→ ׯ(businessId) → HTProtect.getToken(businessId, path)
   [主线程校验 → FutureTask+Thread+超时 → WatchMan$DynamicTask
    → WatchMan.O000000o → factory.O000000o(String)[B → JNIFactory.aebd...(eventType 6 blob)
    失败回退: ioctl(301, businessId) / ioctl("302|"+code)]
→ AntiCheatResult → nuid → AtomicReference（日志 "nuid loaded"）
→ ଵ.֏(): [useDDI=1] Shuzilm DID + nuid（'[-_0]' 剔除）
→ ɴ.ވ(): [DID+nuID] + "; ; ; ; " + MANUFACTURER + "; " + BRAND + MODEL + DISPLAY + OAID + [运行期追加 DDI 会话尾字段]
→ Base64 → reverse → 去换行补位 → ("X-App-Device", value) → 每个请求常驻上行
```

规范第 6 节十问：
1. **每进程重新生成？** nuid 生成走启动期 getToken；是否有持久化短路读取 = UNKNOWN（"nuid loaded" 日志暗示可能有加载路径；可读 prefs 无命中，DB 被拒）。
2/3. **持久化？** 可读存储未发现；AtomicReference 为运行态持有；denied DB 内是否存在 = UNKNOWN。
4. **refresh 条件**：ShuzilmSDKManager.initID 完成、onLoginEvent/onWifiEvent（`ShuzilmSDKManager` 观察者）、RequestSessionIDUpdater 对 init/indexV8/checkLoginInfo 的白名单触发。
5. **businessId** = `PostToken.businessId`（固定 aca7df…68e9，init 响应下发）。
6. **timeout/fallback** = FutureTask.get(超时) → ioctl(301)/ioctl("302|"+N)（已 E3）。
7. **gt 只影响生成时 security data** ✓（append 循环选择 fresh-collect，不影响后续状态机）。
8. **installedApk 增量与 nuid refresh**：每次 getToken 都走 blob builder；field 12 只在缓存有未上报记录时有内容。
9. **进程重启后**：**缓存子系统（0x260000-0x2A0000）无任何 fopen/fwrite** → 缓存纯内存 → **每次冷启动首次 build 的 field 12 = 当时可见的全部第三方包（事实全量）**。
10. **`_v2_post_token`**：与 nuid 共用 `HTProtect.getToken → aebd` 管道，但语义独立——仅 `PostToken.List`（createFeed/reply）命中的 POST 表单追加 token 字段；nuid 是启动期一次 + 常驻头。

实机验证（今日捕获）：当前 X-App-Device 基础值解码 = `DU6FZe2C8AflAY5zBrRvGN07NODgqn52Qk06; ; ; ; OnePlus; OnePlus; PLQ110; PLQ110_16.0.3.503(CN01); null`（99B，与 `ɴ.ވ()` 构造序完全一致；首字段 'D' 前缀 = Shuzilm DID ✓）。
附带：`files/exid.dat`（99B JSON `{appkey:"5a387236a40fa374880002f4", channel:"coolapk", umid:"92b4…f1af"}`）= 网易 APM exid，与 X-App-Device 无关（同为 99 字节属巧合）。

---

## 4. 【本轮最重要修正】App UID 包可见性实测 —— 本设备上 collector 看不到本模块

规范第 7 节可选项实测（不启动酷安、不发请求、不改权限）：

```text
root 视角:            pm list -3        → 111 个第三方包（含全部 6 个 yylsping 模块）
酷安 UID(10400) 视角:  pm list -3        → 1 个第三方包（仅酷安自己）
系统状态: QUERY_ALL_PACKAGES=granted；ColorOS com.android.permission.GET_INSTALLED_APPS
          = denied + AppOps "Uid mode: GET_INSTALLED_APPS: ignore"（USER_SET|USER_FIXED 用户拒绝）
```

三个独立信号一致（dumpsys / su-uid 模拟 / 此前实机"可读取的应用较少"提示）。

**推论（E2 强，静态+运行交叉）**：NetHT collector 的 `pm list packages -f --show-versioncode -U -3` 以应用自身 uid 执行 → 在本设备上**只枚举到酷安自己** → **field 12 实际只含宿主包，不含本模块**。"应用列表上传本模块包名"在本设备当前权限状态下**不成立**；该通道的完整 predicate（Milestone A）仍是正确的能力描述，但其激活依赖 OS 级应用列表权限。

风险面重估：
- 本设备：应用列表通道关闭 → 服务器看不到模块包名（经此通道）；剩余活跃信号 = LSPosed/Zygisk/ART/framework-mount（NetHT 探针）、root/Magisk（metasec）、nuid 本身的环境特征。
- 其它设备（权限已授予的 ROM）：通道全开，111 包级别的全量上传（每次冷启动一次）。
- 这直接解释了为何用户反馈与本地实验出现分歧的可能性：**风控输入面因 ROM 权限状态而异**。

---

## 5. Security Signal Influence Matrix（更新列：Version introduced / Remote configurable）

| Signal | Collector | Payload | Transport | Lifetime | Version introduced | Remote configurable | Evidence |
|---|---|---|---|---|---|---|---|
| third-party packageName+标注+label+acts | pm list -3（应用 uid） | field 12 | aebd blob → nuid → X-App-Device | 每进程一次（冷启动全量） | ≤16.5.1（待 P4B 确证） | bit11/22、cfg[512/624/674/712/784/788] + OS 权限门 | E3（本轮+前轮） |
| filePermisson（外存 Android/data 目录名+st_mode） | sub_27C00C opendir+stat | field 9 | 同上 | 每次构建 | 待 P4B | cfg[840]（默认 OFF） | E3（本轮新） |
| mis（相邻 UID 外存扫描） | append 循环内 | type 2000 事件 | 事件队列 | 每次构建 | 待 P4B | cfg[823]（默认 OFF） | E3（前轮） |
| uid_match / changedPackages | append 循环内 | type 7 事件 / 报告容器 | 事件队列 | 每次构建 | 待 P4B | 事件 mask {3,10} | E3（前轮） |
| LSPosed/Zygisk/ART/framework-mount | NetHT 探针 | type 2000 | 事件队列→blob→nuid | 启动 | 待 P4B | 部分 cfg 位 | E3（前轮） |
| root/Magisk/自动化应用 | metasec 固定路径表 | module2 字段 | 独立通道 | 启动/节流 | 待 P4B | mssdk_setting | E3（前轮） |
| 模块私有文件 | —（NOT_FOUND） | — | — | — | — | — | E3 阴性（本轮） |
| nuid | aebd blob 产物 | X-App-Device 首字段 | 每请求头 | 进程级+持久化 UNKNOWN | 待 P4B | useDDI=1（当前开） | E3 |
| `_v2_post_token` | getToken(businessId) | POST 表单字段 | createFeed/reply | 每次发帖/回复 | 待 P4B | PostToken.*（当前仅 2 路径） | E3 |
| ddid | Shuzilm getSessionSync | Cookie | init/indexV8/checkLoginInfo | TTL | 待 P4B | useDDISessionId=1 | E3 |

## 6. Updated Root-Cause Ranking（按 client capability / remote config / server policy / module-specific 区分）

| 排名 | 假设 | 本轮变化 |
|---|---|---|
| 1（升） | LSPosed/Zygisk/ART/root 环境信号 + 服务端策略 | 本设备应用列表通道关闭后，这是唯一确认活跃的设备指纹输入面 |
| 2（降） | "应用列表上传模块包名触发" | **本设备被 OS 权限门关闭**（E2 实测）；降为"其它 ROM/授权设备才可能"；能力本身 E3 |
| 3（新增） | ROM 权限状态差异解释用户分歧 | 同一模块在不同设备暴露面完全不同（111 包 vs 1 包） |
| 4 | 远程配置 rollout（useDDI/PostToken/gt 等） | 当前值已固化采集（useDDI=1 等）；变化历史需 P4B/历史抓包 |
| 5 | 模块特有行为 | 无新证据（M1-M5 均无） |

## 7. Module Recommendation

```text
KEEP MODE A FROZEN
```

无 M1-M5 级新证据；且本轮证明本设备 field 12 不含模块包名，模块侧风险面进一步缩小。

## 遗留 UNKNOWN（诚实边界）

- NetHT 内部 cfg 的运行时覆盖值（bit11/22、cfg[788/840] 等）——需受保护内存读取，规范禁止绕过，保持 UNKNOWN；
- nuid 是否有持久化短路（denied DB 内）；
- P4B 跨版本差分未做（本地仅 16.6.1 全量工件；16.5.1 需用户提供 APK/so 后再比）；
- P5 通用 card 模型未做（候选入口已就绪：恢复的业务 dex + 今日 init 响应中的 textCard/configCard 样本）。
