# 逆向资产与工具清单

本目录及 `../tools/` 保存酷安风控研究中产出的核心逆向资产（原始分析在本地审计环境完成，此处为面向复现的整理版）。
分析结论见本目录四份分报告；各资产的证据等级与上下文以报告为准。

## 目录结构

```text
assets/
  native/       目标安全组件原始 ELF（4 个 .so）
  idb/          已标注的 IDA Pro 数据库（3 个 .i64，输入路径已脱敏）
  dex/          业务 DEX 与运行时解密 DEX（4 个）
  dex/pangle-live-lite/   Pangle live.lite 组件 8 个 dex（metasec Java 侧）
  msdata/       metasec 本地库副本（9 个，root 拷贝）
  decompiled/   关键函数反编译证据（10 个，对应分报告章节）
tools/          自研分析脚本（34 个，路径已改为相对路径）
```

## assets/native —— 原始 ELF

| 文件 | 大小 | SHA-256（前 16） | 说明 |
|---|---|---|---|
| `libNetHTProtect.so` | 4.41 MB | `3AED7C73D421EBFD…` | NetHT 安全 SDK（网易易盾 HTProtect）v5.7.6，取自酷安 16.6.1（2608212）base.apk |
| `libnesec.so` | 931.1 KB | `2260655381381D4B…` | 易盾保护壳 native 解包器，取自 16.6.1 base.apk（三版本逐字节相同） |
| `libmetasec_ml.so` | 1.96 MB | `A05094EF4A409866…` | 字节/Pangle metasec 设备安全 SDK，取自设备 files/pangle_p/com.byted.live.lite/version-211448/lib/ |
| `libPglbizssdk_ml.so` | 1.08 MB | `000063803E28064C…` | Pangle 主组件同族瘦身变体，取自设备 files/pangle_p/com.byted.pangle/version-7805/lib/ |

## assets/idb —— IDA Pro 数据库（已标注）

| 文件 | 大小 | SHA-256（前 16） | 说明 |
|---|---|---|---|
| `libNetHTProtect.so.i64` | 55.21 MB | `80C30D980AED4046…` | IDA Pro 数据库（NetHT 5.7.6）：研究期全部语义重命名与函数注释；输入路径已等长脱敏 |
| `libmetasec_ml.so.i64` | 49.32 MB | `1BC69844BD1EA935…` | IDA Pro 数据库（metasec）：模块框架/root 检测/JNI vtable 探针等 11 处重命名与注释；路径已脱敏 |
| `libnesec.so.i64` | 5.38 MB | `833E638BED384FA7…` | IDA Pro 数据库（易盾壳解包器）：手工应用 RELA 重定位、恢复真实构造器；路径已脱敏 |

> 脱敏说明：三个 i64 在入库前已对内部记录的输入文件路径做**等长字节替换**（`X:esearch\…` 占位，每库 4 处，替换后复扫无本机路径残留）。
> 重命名/注释等元数据未改动；如需重建原始路径关联，可直接用 `assets/native/` 中的同名 .so 新建数据库并参考 `../tools/` 脚本。

## assets/dex —— DEX（含 Pangle live.lite 组件）

| 文件 | 大小 | SHA-256（前 16） | 说明 |
|---|---|---|---|
| `coolapk_business_restored.dex` | 9.22 MB | `11BB1193D284455D…` | 酷安 16.6.1 业务 DEX（内存恢复 + magic 修复，Adler32/SHA-1 校验通过，11,278 类） |
| `main_useDDI.dex` | 9.31 MB | `84E371B89F28450A…` | 运行时 DEX：网络层（X-App-Device 组装、请求头拦截器、PostToken 通道） |
| `sdk_netht.dex` | 7.14 MB | `001CF2C5CDC9A24A…` | 运行时 DEX：NetHT Java wrapper（HTProtect / WatchMan / JNIFactory） |
| `wrapper_ref.dex` | 9.62 MB | `75BFE95280D36F1D…` | 运行时 DEX：壳 wrapper 相关类 |
| `pangle-live-lite/classes.dex` | 7.79 MB | `C498557CF55616F0…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |
| `pangle-live-lite/classes2.dex` | 1.33 MB | `B6727A4B890D4C7A…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |
| `pangle-live-lite/classes3.dex` | 10.66 MB | `4FB6680EA907B4FC…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |
| `pangle-live-lite/classes4.dex` | 10.51 MB | `4AABE7EABF3B5CAC…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |
| `pangle-live-lite/classes5.dex` | 11.95 MB | `4A93CCE46ECF665D…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |
| `pangle-live-lite/classes6.dex` | 10.63 MB | `2E370B27617424DF…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |
| `pangle-live-lite/classes7.dex` | 9.46 MB | `F18B5DBDA5853870…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |
| `pangle-live-lite/classes8.dex` | 8.62 MB | `C08D8D03C00367D3…` | Pangle live.lite 组件 APK（version-211448）解包 dex，metasec Java 侧分析源（SecInitTask2B / MSManager / x-bdms 上报 agent） |

运行时 DEX 提取自酷安进程内存（易盾壳运行期解密后），盘上 APK 中不存在；`main_useDDI.dex` / `sdk_netht.dex` 仅能字符串级或手工解析，androguard 可完整解析业务 DEX。

## assets/msdata —— metasec 本地库副本

来源：设备 `/data/user/0/com.coolapk.market/files/.msdata/mssdk/ml/`（root 拷贝，2026-08-28）。

| 文件 | 大小 | SHA-256（前 16） | 说明 |
|---|---|---|---|
| `mss_442656d8.bin` | 32.8 KB | `A4016115185DDE5E…` | metasec 风险应用签名库（33KB，内置黑名单：包名+类别/权重记录；root 副本） |
| `mss_9b8ed995.bin` | 0.7 KB | `26646FDF06ED88F0…` | metasec 本地库（加密二进制，726B，首日文件之一） |
| `msp_589c2233.bin` | 0.1 KB | `F0A999187521D1DF…` | metasec 本地库（加密二进制，90B） |
| `msp_092fde7a.bin` | 0.2 KB | `C252FD314BE7D33C…` | metasec 本地库（加密二进制，209B） |
| `msf3_04fa7481.bin` | 0.0 KB | `FB13156322E44560…` | metasec 本地库（加密二进制，32B，首日文件之一） |
| `msf3_0e6a186f.bin` | 0.0 KB | `6AF6221F91770734…` | metasec 本地库（加密二进制，8B） |
| `msf3_a2c1fbad.bin` | 0.0 KB | `E74F88BFD0371FAC…` | metasec 本地库（加密二进制，8B） |
| `msf3_3afcbc4b.bin` | 0.1 KB | `2CE0BFDB6605229E…` | metasec 本地库（加密二进制，124B） |
| `mss_1f149f2d.bin` | 0.0 KB | `5FECEB66FFC86F38…` | metasec 本地库（加密二进制，1B） |

文件名为内容 SHA-1（原样保留）。除 `mss_442656d8` 外均为加密二进制块，无可读字符串。

## assets/decompiled —— 关键函数反编译证据

| 文件 | 大小 | SHA-256（前 16） | 内容 → 对应报告章节 |
|---|---|---|---|
| `full_2FA14C.c` | 52.9 KB | `0FD14AD01C822854…` | installedApk append 主循环（field 12 谓词）→ 信号采集报告 §4 |
| `pA_sub_24646C_full.c` | 25.6 KB | `7BC02EAEFE1F71EC…` | ioctl 命令分发器（selector 语义）→ 信号采集报告 §7 |
| `pA_sub_2A658_full.c` | 26.4 KB | `F0B3CB2C5C0CF47F…` | black_module 周期扫描器 → 信号采集报告 §6 |
| `pA_ctor_full.c` | 10.2 KB | `08EE183C9F5AA2BC…` | 配置构造器默认值（门控矩阵）→ 信号采集报告 §3/§8 |
| `pA_gtfunc.json` | 22.2 KB | `369027BF3B8AF60A…` | getToken worker 反汇编（"gt" 语义）→ 传输链报告 §1、采集报告 §4.2 |
| `pA_sub_24188_full.c` | 23.0 KB | `F6F8791F66B15CCD…` | PackageParser JNI 富集引擎 → 信号采集报告 §6 |
| `pA_sub_244778_full.c` | 4.7 KB | `6FA4E1DF2531FF20…` | SDK init worker → 信号采集报告 §2 |
| `pA_sub_242788_full.c` | 7.4 KB | `4AA5185C518F3316…` | JNI init 入口（HTProtectConfig 读取）→ 信号采集报告 §7 |
| `nB_sub_9B83C.c` | 6.3 KB | `40458F388FD34607…` | 记录注册表键→容器偏移映射器 → 信号采集报告 §8 |
| `nB_scanner.c` | 29.2 KB | `C6A5FBDC54E7DA0C…` | black_module 扫描器（后期反编译版）→ 信号采集报告 §6/§8 |

函数语义命名（netht_* / ms_*）与各报告中的地址对照以 `assets/idb/` 数据库内注释为准。
