# Issue #5 — Full-P4B 阶段总结报告（E1/E2/E5 完成）

分析时间：2026-08-27（第四会话）
执行规范：`issue5_next_stage_after_d1d4d5_with_full_p4b.md`
停止条件：完成 E1/E2/E5 三项 → 本报告为总输出（§13 八节格式）。
子报告：`issue5_cross_version_security_diff.md`（P4B）、`issue5_p5_generic_card_model.md`（P5）。
工具：IDA Pro MCP（16.6.1 NetHT IDB，只读+元数据已保存）、自研 Capstone 栈字符串重建器、
androguard、root 只读文件查找。未 patch 字节、未运行旧版 APK、未触发风控。

---

## 1. Active Security Signal Matrix（E1）

### 1.1 门控机制（本轮新闭合，全部 E3 指令级）

```text
冷启动 → SDK init → netht_register_probe_scheduler_once (sub_242628，一次性守卫)
  → 把探针调度器 sub_2483A8 注册进 SDK 回调表 (*(qword_4C2298+496))(buf,0,sub_2483A8,0)
  → sub_2483A8 顺序分发全部探针（0x248a2c–0x248d68）
  → 每个探针入口两道门：
      ① netht_linker_level_check (sub_2E9684)：linker 级别 ∉ {3,4,5} 才继续
         （级别来自 /system/bin/linker64 首字节解析缓存 dword_4C03A8，sub_1A570C）
      ② config 单例字节门（getter sub_26173C → LDRB [x0,#off]；CBZ 0 则跳过）
```

### 1.2 矩阵

| Signal | Trigger | Config gate | Default | Runtime 值 | Event | In aebd(6)/nuid? | 依赖 installedApk collector |
|---|---|---|---:|---|---|---|---|
| LSPosed maps 内容搜索 | 调度器，每进程一次 | **cfg[808]** (+0x328) | **0 = OFF** | UNKNOWN | 2000/"dal" | ✓ 链路 E3（queue→serialize→blob→nuid→X-App-Device） | 否 |
| zygisk ptrace/fork | 同上 | **cfg[825]** (+0x339) | **0 = OFF** | UNKNOWN | 2000/"zygisk" | 同上 | 否 |
| smaps Referenced/Shared_Clean | 同上 | **cfg[821]** (+0x335) | **0 = OFF** | UNKNOWN | 2000 | 同上 | 否 |
| fdinfo/mnt_id（framework JAR 挂载） | 同上 | **cfg[809]** (+0x329) | **0 = OFF** | UNKNOWN | 2000/"mnt" | 同上 | 否 |
| odex + inline-max-code-units=0 | 同上 | vtable 对象门（非 config 字节） | UNKNOWN | UNKNOWN | 2000/"odex" | 同上 | 否 |
| filePermisson（Android/data 外存扫描） | append_loop 内直调 | cfg[840] | 0 = OFF（D1） | UNKNOWN | field 9 | 同上 | 并行 |
| mis（相邻 UID 外存） | append 循环 | cfg[823] | 0 = OFF（D1） | UNKNOWN | 2000/"mis" | 同上 | 并行 |
| **installedApk（-3 第三方范围）** | 构建前 ingest（节流 60×cfg[100]=30min；前台） | bit11/bit22/cfg[512] 等 | **ON（2/2/200MB）** | UNKNOWN | field 12 | 同上 | 本体 |
| uid_match / changedPackages | append 循环 | 事件 mask {3,10}/{7} | — | — | type7/报告 | 同上 | 部分 |
| root/Magisk/自动化应用 | **metasec_ml（Pangle 广告组件）**，随广告 SDK 生命周期 | mssdk_setting | UNKNOWN | — | module2 字段 | 独立通道（宿主 attach 点仍 E2/未闭合） | 否 |

### 1.3 关键结论

1. **"SDK 具备检测能力" ≠ "本设备当前激活"**：NetHT 的全部注入类探针（LSPosed/zygisk/smaps/mnt/odex）**默认门值为 0（OFF）**——构造器 `netht_config_ctor_defaults` 将 0x328/0x329/0x335/0x339/0x348 区清零（E3：STRH/STR WZR 反编译+互证）。
2. **运行时改门通道存在**：JNI 命令分发器（`d0f149b4da6ec477` 链）含 `STRB W?,[X9,X8]` 索引式写入 + `netht_named_key_record`（"gt" 记录器）——私有通道**可以**按索引改写门字节；当前实际值 UNKNOWN（字面偏移扫描证明：二进制内除构造器清零外，无其它字面偏移写门者；0x197454/0x22E41C/0x1C8A84 的同偏移写入均为栈缓冲/导入表误报，已逐一排除）。
3. 因此"服务端 rollout"假设获得**机制级支撑**：探针开关是服务端可远程翻转的位，客户端升级只是"提供了更多可开关的探针"。
4. installedApk collector 链是**默认全开**的例外（bit11/22=2>1 即 ON）——但本设备被 OS 权限门关闭（D4）。

## 2. Generic Card Model（E2/P5）

> **S2：需要已有通用 template ID，但无需 risk 专用客户端代码。**

详见 `issue5_p5_generic_card_model.md`。核心：通用 `card` 模板（EntityCard→viewholder/ޕ）直接把
服务端 title 渲染进 TextView + 嵌套实体 + 服务端样式键（cardContainerPadding*）；
`configCard` 实体被转成页面配置直接应用；textCard/textLinkCard 等模板族在模型层存在。
风险行若存在，最低成本路径 = 服务端插一个普通 card 行，客户端零配合。

## 3. Metasec Transport Graph（本轮新增：来源归属）

```text
Pangle(穿山甲) 广告 SDK
  → 动态下发组件 files/pangle_p/com.byted.live.lite/version-211448/lib/libmetasec_ml.so   [E3 设备实测]
  → detect_root_environment / automation apps / mssdk_riskapp_db                            [E3 前轮 IDA]
  → collect_module2_risk_fields → serialize_module2_payload                                 [E3 前轮]
  → SDK public/native interface                                                            [E2]
  → ??? Java wrapper / 网络通道 / 请求字段                                                  [UNKNOWN — 宿主 attach 点未闭合]
```

**新事实：metasec 是广告 SDK 供应链组件，与酷安 APK 版本解耦**（三版 APK 均不含该 so）。
其风险采集的版本时间线由 Pangle 组件更新决定——这是此前根因模型中缺失的**独立变量**。
（按规范 §4：宿主 attach 点若需继续追，须切 `libmetasec_ml.so.i64` IDB，本轮未进入。）

## 4. nuid Active-Signal Inclusion Graph（静态闭合）

```text
LSPosed ──┐ cfg[808]（默认 OFF）
Zygisk ──┤ cfg[825]（默认 OFF）
smaps  ──┤ cfg[821]（默认 OFF）          ┌→ sub_2F3308 全局事件队列（去重）
mnt    ──┤ cfg[809]（默认 OFF）          ├→ sub_2F3C84 序列化（同一次 build）
odex   ──┤ vtable 门                     └→ sub_2FF7AC/sub_308EFC → aebd(eventType=6) → nuid → X-App-Device
mis    ──┤ cfg[823]（默认 OFF）          [以上链路 E3；探针是否实际产出事件取决于门运行值]
installedApk → field 12（默认 ON，本设备 OS 权限 → 只含宿主自己）
```

回答规范核心问题——**"本设备 packageName 通道关闭时，nuid 是否仍携带注入/root 信号？"**：
- **结构上可以**（事件队列与 field 12 同一次 build 序列化，链路 E3）；
- **是否发生取决于探针门运行值**（默认全 OFF；私有通道可翻转；运行值 UNKNOWN）；
- 因此本设备 nuid 携带环境信号的现实概率 = f(服务端是否下发了探针开关)，而非客户端版本本身。

## 5. ROM / Permission Exposure Model（沿用 D4 实测 + 假设行）

| Environment | QUERY_ALL_PACKAGES | OEM GET_INSTALLED_APPS | AppOps | NetHT pm 可见性 | 模块包可见？ |
|---|---|---|---|---|---|
| 当前 ColorOS（实测） | granted | denied | ignore(USER_FIXED) | 1（仅宿主） | **no** |
| 假设：OEM 权限授予 | granted | granted | allow | 全量（111 包级） | **possible** |
| 其它 ROM | — | — | — | HYPOTHETICAL / USER REPORT | — |

用于解释用户反馈差异，不用于设计规避。

## 6. Cross-Version Result（E5/P4B 摘要，全文见专报告）

- **libNetHTProtect.so：16.5.1 == 16.6.1 逐字节相同**；15.9.0 为不同构建。
- **(15.9.0, 16.5.1] 新增**：pm collector `-3` 第三方范围化、LSPosed maps 内容搜索（替代旧路径法）、
  `inline-max-code-units=0` ART 探针、filePermisson 外存扫描、changedPackages、shizuku、frida 字符串、+16B config。
- **既有能力（≤15.9.0）**：pm collector（无 -3）、zygisk ptrace+路径法、fdinfo/mnt_id、smaps、
  magisk/taichi 路径表、模拟器、**Java 侧 DDI/PostToken/nuid/Shuzilm/X-App-Device 全链字符串**。
- **Config 默认值**：共有字段逐值不变（30/200/257/256/2/0x1000101…），新能力=新字段。
- **判定**：`CLIENT_CAPABILITY_CHANGED`（区间内成立）+ `CLIENT_UNCHANGED_SERVER_MORE_LIKELY`
  （16.5.1→16.6.1 增量）+ `MIXED`；`REMOTE_CONFIG_CHANGED` UNKNOWN（无历史样本）。
- metasec：N/A（Pangle 动态组件，独立时间线）。

## 7. Updated Root-Cause Ranking

| # | 假设 | 本轮变化与依据 |
|---|---|---|
| 1 | **服务端 policy/rollout（探针门开关下发）** | ↑↑ 机制级支撑：探针默认 OFF + 私有通道可翻转门（E1）；16.5.1→16.6.1 native 零差异（E5） |
| 2 | **LSPosed/Zygisk/ART 探针信号** | ↑ 能力在 16.5.x 新增且本设备直接命中面（环境真实存在）；但激活与否取决于 #1 |
| 3 | root/Magisk（metasec/Pangle） | = 独立时间线（广告组件 version-211448），与 APK 版本解耦——新识别的变量 |
| 4 | Purifier 2.2.0 hook 面 | = 无新直接边；Mode A 已回到 2.1.2 面 |
| 5 | 应用列表/模块包名 | ↓（本设备权限门关闭；但注意该能力本身是 16.5.x 新增——授权 ROM 上时间相关性成立） |

分层表述（按规范要求）：
- **active client signal**：installedApk field 12（仅宿主自己）+ nuid/X-App-Device 常驻（内容含哪些事件 UNKNOWN）；
- **inactive capability（默认关）**：LSPosed/zygisk/smaps/mnt/odex/filepermisson/mis 探针；
- **OS permission gate**：GET_INSTALLED_APPS denied → collector 只见宿主；
- **remote config**：useDDI=1 等已固化；NetHT 探针门运行值 UNKNOWN；
- **server policy**：16.5.1→16.6.1 客户端零 native 差异下的最强解释项；
- **module-specific behavior**：无 M1-M5 级证据。

## 8. Module Recommendation

```text
KEEP MODE A FROZEN
NO MODULE PATCH
```

无任何新的"当前 hook → active probe"直接边。E1 反而表明：最活跃的检测面由服务端开关控制，
模块侧删功能不改变环境信号的存在性。

---

## 遗留 UNKNOWN（诚实边界）

1. 探针门（cfg 808/809/821/825/840…）的**运行时实际值**——需受保护内存/运行时读取（规范禁止绕过；
   Frida 注入又会引入被检测信号本身，本轮不做）；
2. odex 探针的 vtable 门语义；
3. metasec 宿主 attach point（需切 IDB 时先告知用户）；
4. Entity JSON 解析器逐字段恢复（E2 级足够回答 P5 问题，未 deeper）；
5. 远程配置历史值（无样本）；16.0–16.4 未取样（first-introduced 为区间表述）。
