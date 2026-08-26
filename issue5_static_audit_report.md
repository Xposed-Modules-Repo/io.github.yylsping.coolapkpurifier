# Issue #5 风控后续静态审计报告

审计时间：2026-08-26  
宿主：酷安 16.6.1（2608212）  
模块：2.2.0（versionCode 10）  
本轮实验版本：Mode A（Activity-only）

> 动态风控具有 account/device/session/TTL 等非确定性。本报告中的单次阴性不用于排除路径，单次阳性也不直接归因。

## 1. 本轮新增静态发现

### 1.1 已取得并校验完整的酷安业务 DEX

网易易盾会在 Frida 注入后结束进程，因此停止继续用 Frida 扰动目标，改为模块进程内读取内存。旧的本地 dump 之所以不可解析，是旧传输链路把 `LF` 扩展成了 `CRLF`，并非易盾清除了 DEX 表。

重新以二进制方式拉取后，9,670,656 字节区域除开头四字节 `dex\n` 被覆盖外其余内容完整。恢复 magic 后，DEX 原始头校验全部通过：

- Adler32：`faeb5cd8`（expected = actual）
- SHA-1：`dc92260ce15e70b821cf8a5c1390e101622838d9`（expected = actual）
- 恢复文件 SHA-256：`11bb1193d284455dba363a5c69270ab9cf441bbc322238b59c74ee3af2cc3a7f`

证据文件：`.tmp_audit/regions_clean/coolapk_business_restored.dex`。

### 1.2 `shouldShowAd` 没有被跳过的业务副作用

恢复 DEX 中的目标为：

```text
Lkc5;->Ԯ(Context, ࡉ, String)Z
```

完整指令流只包含：

- 日志调用；
- 当前时间读取；
- `TENCENT_AD_ERROR_TIMESTAMP`、`SPLASH_AD_LAST_SHOW` 偏好读取；
- 远程配置值读取；
- 隐私状态读取；
- `PackageManager.getPackageInfo()` 后读取 `firstInstallTime`；
- splash 类型读取和布尔判断。

没有发现字段写入、SharedPreferences editor、时间戳更新、remote config consume、SDK 初始化、网络请求或其它状态迁移。它的两个直接调用点都在 `MainActivity.onCreate`：一处分流到 splash loader，另一处分流到恢复场景的插屏/其它路径。

`SPLASH_AD_LAST_SHOW` 的写入发生在真正展示边界：

- `kc5.ՠ(String, String, Activity)`：插屏展示路径；
- `SplashAdFragment.ࢿ()`：开屏展示路径。

因此三类风险需要重新区分：

1. PRE_BLOCK 跳过函数副作用：静态上已降为低，函数没有关键写副作用。
2. original=true 但 final=false 的状态不一致：低至中；决策函数本身只读，展示状态由 caller 后续路径写入。
3. 决策函数本身被监控或 trampoline 被识别：仍不能静态排除，但业务 DEX 未发现直接监控调用。

此前 AFTER_OVERRIDE 诊断版实机观测到两次 `original=true final=false`，说明函数确实会允许广告；它不证明覆盖返回值会触发风控。

### 1.3 Coolapk 主动检查“可读取的已安装应用数量”

Mode A 首次启动时，酷安自身弹出：

```text
检测到酷安可读取的应用较少，请检查是否已经给酷安授予『读取已安装应用列表』权限
```

这直接证明“应用列表能力/返回规模”是酷安当前活跃检查面，而不只是 SDK 中未使用的能力。当前测试没有授予权限，只点了“取消”。

它仍不能单独证明酷安在找本模块包名，也不能解释所有“未授权仍风控”的反馈；宿主私有文件、运行时注入和服务端行为检测仍是独立路径。

### 1.4 Kwad SDK 带有环境标志模型，但尚无活跃调用证据

业务 DEX 中 `com.kwad.sdk.k.a.d` 会序列化：

```text
isRoot, isXPosed, isFrameworkHooked, isVirtual,
isAdbEnabled, isEmulator, isGroupControl
```

但构造器、setter 和 `toJson` 未发现外部直接 xref。目前只能定性为“SDK 内存在该能力”，不能写成“酷安已经上报 Xposed”。反射或 native 间接调用尚未排除。

Kwad 获取安装应用列表的公开路径只在宿主提供 `KsCustomController` 时调用 `getInstalledPackages()`，否则返回空列表。恢复 DEX 中未找到酷安自定义实现；它可能位于其它 DEX、动态组件或反射注册路径。

### 1.5 Java 业务 DEX 未发现模块或常见注入特征硬编码

以下精确搜索结果均为 0：

```text
io.github.yylsping.coolapkpurifier
coolapk_purifier
libdexkit
XposedBridge / LSPosed / Zygisk / Magisk / Riru / Edxp
/proc/self/maps / mountinfo / TracerPid
DexPathList / BaseDexClassLoader
```

这降低了 Java 侧“按固定字符串直查”的概率，但不排除 native、远程下发或通用枚举匹配。

### 1.6 `libNetHTProtect.so` 已完成函数级归因：主动识别 LSPosed/Zygisk 并进入安全数据序列化

已用 IDA 分析 AArch64 `libNetHTProtect.so`（4,624,312 bytes，SHA-256 `3aed7c73d421ebfd5a9d234517ccb4d798ef14d5b82e64eabc9e595d74b60ff1`）。当前 IDB 为 `.tmp_audit/native/libNetHTProtect.so.i64`。

首先排除了一个误导性线索：明文 `/proc/self/maps` 的两个 xref（`sub_1D7294`、`sub_1DB208`）只是地址到映射区间/ELF 路径的通用辅助函数，没有扫描模块名；明文 `frida` 命中仍只是 `Friday`。但启动探针通过运行时解码隐藏了真正的检测词，确认存在以下活跃路径：

- `sub_2B7AD0` 打开 `/proc/self/maps`，定位 `dalvik-main`，复制最多 `0xF0000` 字节并提取字符串；随后运行时解码并直接搜索 `LSPosed`，还检查 `is-top-app` 邻接字符串。命中时以键 `dal` 构造 2000 号事件。
- `sub_29A16C` 通过 fork + `ptrace(PTRACE_ATTACH)`/wait/detach 组合探测，运行时解码出 `zygisk`，命中后构造 2000 号事件。
- `sub_294620` 从宿主 `base.apk` 路径定位 `oat/arm64/base.odex` 或 `oat/arm/base.odex`，拼接并执行等价于 `strings <odex> | grep inline-max-code-units=0` 的命令；命中时以键 `odex` 构造 2000 号事件。该 ART 编译参数会关闭小方法内联，常用于保证方法替换/Hook 语义，因此这是直接面向 ART Hook 兼容状态的检测。
- `sub_298F98` 解析 `/proc/self/smaps`，跟踪 `libandroid_runtime.so` 映射的 `Shared_Clean:`/`Referenced:` 等页统计并比较前后变化，异常时写入 2000 号事件。
- `sub_298170` 扫描 `/proc/self/fd`，定位指向 `com.android*.jar` 的 fd，再读取 `/proc/self/fdinfo/<fd>` 的 `mnt_id:`；挂载 ID大于等于 1001 时以键 `mnt` 写入 2000 号事件。这是 framework JAR 挂载来源/命名空间异常检测，不是普通 fd 枚举。

`sub_1E338`/`sub_2310C` 负责构造 160 字节事件，`sub_2F3308` 将其去重后插入全局队列；`sub_2F3C84` 会遍历并序列化该队列，`sub_2FF7AC`、`sub_308EFC` 最终产出安全数据 blob。因此 LSPosed/Zygisk/odex 等结果不是孤立的本地反调试动作，而是明确进入安全数据生成链。

相邻探针还包含 loop/ext4 挂载识别、直接 syscall 与 libc wrapper 的时延差、mprotect/mincore/kernel 行为等广义 root/injection 环境采集。它们证明保护层采用多信号聚合，但与本模块 2.2.0 版本差异的直接关系较弱。

静态结论需要保持两层：**“网易易盾主动采集 LSPosed/Zygisk/ART Hook 迹象”已确认；“这些信号单独导致本次风险行、或解释 2.2.0 才开始出现”仍未确认。** 同一设备此前已经运行于 LSPosed/Zygisk 环境，所以版本新增的常驻 framework Hook 更可能是既有基线上的额外可观察变化，而不是唯一检测来源。

### 1.7 `libnesec.so` 是故意破坏 ELF 元数据的自解包加载器

IDA 加载 `libnesec.so` 时报告 Section Header Table 损坏且区段重叠。两次 overwrite 结果相同，这不是偶发数据库故障：节表中的 `.gnu.fragment` 故意覆盖 `.gcc_except_table`、`.eh_frame_hdr`、`.eh_frame` 等区段，动态符号名和唯一导出地址也被污染。三个 `PT_LOAD` 程序头本身完整，因此 IDA 已加载全部运行时映射，不需要手工补入整段。

由于损坏的节表没有描述动态重定位，IDA 漏掉了 `DT_RELA` 指向的 24 个 `R_AARCH64_RELATIVE` 条目，导致 init-array 最初全为零。本轮已按动态表记录手工应用这些相对重定位并保存 IDB，恢复三个构造器：

```text
0xD9464  nesec_init_0
0xD9508  init continuation / loader bootstrap
0xD9BF4  nesec_init_2
```

恢复后的调用链显示它是自解析、自解包和自装载框架：

- 第一个构造器运行时解码 `/proc/self/maps` 与 `%lx-%lx %4s %*x %*x:%*x %*d%n`，检查一个目标地址及自行 `mmap`/`mprotect` 区域是否具有预期可执行权限。
- 后续构造器从当前模块基址解析 ELF header/program headers，建立约 408 字节的装载上下文，处理 payload、重定位和初始化，再加载隐藏代码。
- `sub_DF1DC` 明确执行自定义 ELF loader 流程：解析 program headers、映射/复制、重定位、保护属性和初始化。
- 文件前部约 119,740 字节区域熵为 `7.9984`，约 625,412 字节隐藏主体没有任何 IDA 可识别函数；全部约 228 个函数均位于后部 loader/辅助区。自动分析数秒完成正是因为主体仍处于高熵/编码状态。

因此当前静态边界是：loader 自身确认存在 maps/可执行映射自检，但尚未发现另一条独立的 LSPosed/Zygisk 事件链；隐藏 payload 未解包，不能把“未发现”写成“不存在”。`libNetHTProtect.so` 已经提供了直接检测与安全 blob 序列化证据，所以没有必要仅为重复证明而在此壳上无限逆向；如需审查 payload，应优先取得运行时解包后的内存映像。

### 1.8 `libmetasec_ml.so` 主动采集 root 与风险应用信号，并写入模块数据

已用 IDA 完成 AArch64 `libmetasec_ml.so` 分析（2,058,272 bytes，SHA-256 `a05094ef4a40986653834561917631b7873a315dc2d812d3219803b2d7475e09`）。该 ELF 结构正常，自动分析和 Hex-Rays 均完整结束；当前 IDB 为 `.tmp_audit/native/libmetasec_ml.so.i64`。库内有 4,861 个函数、92 个 init-array 构造器，并通过 `MSModuleCreator::register_t` 注册模块 ID 1、2、3、5、6。

五个控制流混淆的字符串解码器最终均归约为 8 字节循环 XOR。恢复全部构造器常量后，确认以下数据不是误报字符串，而是进入活跃采集链：

- `init_root_path_table`（`0x27A0C`）建立 30 项连续路径表。`detect_magisk_paths`（`0x76E80`）实际遍历 `/sbin/magisk64`、`/sbin/magiskhide`、`/sbin/magiskinit`、`/sbin/.magisk`；`find_existing_root_path`（`0x76F60`）遍历另外 26 项，包括 `/system/xbin/su`、`/system/bin/su`、`/sbin/su`、`/su/bin/su`、Superuser/SuperSU APK、KingRoot/RootGenius 等路径。两者通过等价于 `fstatat`/`faccessat` 的封装检查实际存在性，不只是初始化未使用的常量。
- `detect_root_environment`（`0xCDBC4`）把上述路径命中与两条命令探针合并：`execute_command_capture_output` 执行 `ps | grep adbd` 或 `ps -e | grep adbd`，`command_output_contains_root` 再在输出中搜索 `root`。
- 该布尔结果在 `collect_module2_risk_fields`（`0xCF04C`）中以运行时解密出的字段名 `root` 写入数据对象，调用链为注册模块 2 的 vtable 方法 `serialize_module2_payload`（`0xCBB74`）→ `collect_module2_payload`（`0xCB9C4`）→ `collect_module2_risk_fields` → `detect_root_environment`。这闭合了“检测 → 模块字段 → 序列化对象”的静态链。
- `init_automation_app_path_table`（`0x26F48`）建立 10 项风险应用目录表，包括 Auto.js、触动精灵、按键精灵、触摸精灵和若干 clicker/automation 应用。`detect_automation_app_dirs`（`0x69F6C`）逐项执行文件状态检查，命中返回 11；`collect_automation_risk_markers`（`0x72ACC`）随后加入解密值 `cka`，并写入其消息对象的字符串字段。
- `mssdk_riskapp_db`、`mssdk_setting`、`last_rp_time` 三个键也都有非构造器 xref：前者用于风险应用数据库读写，后两者分别用于 SDK 设置加载和风险采集时间间隔控制。因此该组件具有本地风险库及节流状态，而不只是一次性硬编码扫描。

在本轮恢复的 62 个唯一可打印字符串中没有直接出现 `Xposed`/`LSPosed`/`Zygisk`，但这不能证明模型、其它动态字符串或未追踪路径中不存在相关信号。当前可以确认的是：`libmetasec_ml.so` 提供了独立于 `libNetHTProtect.so` 的 root、ADB-root 和风险应用采集/序列化通道；尚不能确认它直接匹配本模块包名，也不能据此证明该 SDK 通道是 2.2.0 才新增。

### 1.9 2.2.0 的设置页实现扩大了常驻 framework hook 面

原 2.2.0 `SettingsHooks` 会长期 Hook：

```text
Instrumentation.callActivityOnResume
Instrumentation.callActivityOnDestroy
Activity.finish
Activity.onBackPressed
Activity.dispatchTouchEvent
```

这些 Hook 与广告净化本身无关，却是 2.2.0 版本相关、进程全局且长期存在的新增可观察面，比“shouldShowAd 跳过副作用”更符合当前静态证据。

本轮已改为一次 `Application.registerActivityLifecycleCallbacks()`，不再 Hook 上述五个 framework 方法。设置入口和配置页已实机验证可用。

### 1.10 当前执行链与 Hook 生命周期

```text
CoolapkModule.onPackageReady
  → HookCoordinator.install
    → Instrumentation create safety net（framework Hook 长期保留，bootstrap callback 在 READY 后退休）
    → RuntimeDexObserver / ClassLoader discovery（临时）
    → Application.attach
      → config + Application lifecycle callback
      → runtime DEX / cache verify
      → SplashCriticalResolver（只解析 Splash Activity）
      → Issue2Resolver / NormalResolver
      → Activity splash finish + Feed after-filter
      → READY
        → retire bootstrap / lazy discovery hooks
```

READY 后的核心长期修改为：

- Splash Activity create 后 finish：presentation/navigation 边界；
- Feed 方法先执行宿主，再过滤返回 List：data after-filter；
- 可选业务 method/holder hooks：render/data；
- Application lifecycle callback：observer，不修改 framework 方法实现。

### 1.11 应用列表权限与 SDK provider 路径进一步收敛

当前设备的 `dumpsys package com.coolapk.market` 结果为：

```text
android.permission.QUERY_ALL_PACKAGES: granted=true
com.android.permission.GET_INSTALLED_APPS: granted=false
```

这与实机出现“可读取的应用较少”提示一致：酷安声明了标准全量查询权限，同时仍受 ColorOS/OPlus 的 `GET_INSTALLED_APPS` 运行时权限门控。恢复 DEX 中 `m8c` 的相关路径也已确认：

- `READ_DEVICE_APPS` 保存用户同意状态；
- 检查设备是否存在 `com.android.permission.GET_INSTALLED_APPS`；
- 检查该权限是否已授予；
- 未授予时走系统权限请求或“读取安装应用列表”同意对话框。

精确的“可读取应用较少”字符串存在于资源 `str_show_permission_check_message`，但恢复业务 DEX 中没有直接资源引用或对应数量阈值；`m8c` 的权限请求入口也未找到普通 Java 直接 caller。这说明数量检查很可能位于尚未恢复的动态 DEX、native/壳 payload 或反射路径，不能从当前 Java DEX 伪造出一个不存在的调用链。

同时，恢复 DEX 中没有任何 `KsCustomController` 子类、`new-instance KsCustomController`，也没有宿主调用 Kwad `getDevInstalledPackages()` 的证据。因此 Kwad 的 installed-packages controller 从“待找主要入口”下调为 SDK capability；当前更应追踪酷安自身权限检查和两个 native 安全组件。

### 1.12 `installedApk` collector → JNI getter → `x-app-device` attach point 已闭合到 E2/E3 边界

对 `libNetHTProtect.so` 的原始字符串/类型元数据补扫又找到同一安全数据模型中的字段描述：

```text
AndroidSuspiciousInfo.installedApk
InstallApkInfo.susPermissionList
AndroidSuspiciousInfo.filePermisson
AndroidSuspiciousInfo.apkLibName
AndroidSuspiciousInfo.ExtDataEntry.key/value
```

随后结合 Capstone 与当前 `libNetHTProtect.so.i64` 的 IDA MCP 只读分析，已确认这些字段不是孤立 schema 名：

- `0x1ECC00` 从对象 `+0x30/+0x38` 读取 `installedApk` repeated-vector 的 count/data，逐项写入 protobuf wire tag `0x62`（field 12, length-delimited）；
- `0x1E8658` 对 `InstallApkInfo.susPermissionList` 写 tag `0x2A`（field 5）；
- `0x1ECA80` 对 `filePermisson` 写 tag `0x4A`（field 9）；
- `sub_308EFC` 直接调用 `sub_2FF7AC` 生成序列化结果，并由 `sub_306194` 继续交给 `sub_2C3110` 的输出阶段。

本轮进一步从同一 IDB 反向闭合了真正的生产者：

- `sub_26FCEC` 在 Android SDK ≥ 26 时构造并执行 `pm list packages -f --show-versioncode -U -3`；`-3` 直接把初始集合限定为第三方包；
- 执行器 `sub_273B74` 通过 `popen(..., "r")` 逐行解析 `package:`、`.apk=`、`versionCode:` 和 `uid:`，形成每项 112 bytes 的 APK 路径、包名、versionCode、UID 记录；
- `sub_277278` 将这些记录扩展为 288-byte 缓存项并补充 APK/签名/文件信息，`sub_276E54` 再快照缓存供 security builder 使用；
- `sub_2FA14C` 中 `X21` 为 `AndroidSuspiciousInfo`，`X21+0x28` 是 repeated-string 容器基址；`0x2FB4D0/0x2FB4E4/0x2FB560` 对该容器调用 `sub_102E24` 获取新槽位并以 `sub_310D4` 写入字符串，和 serializer 读取的 `+0x30/+0x38` count/data 完全重合；
- 每个写入元素由 enriched payload、可选 risk annotation、MD5 hex 和 framing marker 组成；它不是原始 `pm` 行，也不是 protobuf `InstallApkInfo` 子消息。`sub_241440` 生成 32 字符 MD5 hex，最终形态为 marker + `{MD5(payload)}` + payload。

因此当前可写成 E3 的闭环是：

```text
pm list packages -f --show-versioncode -U -3
  → sub_273B74 解析 path/package/versionCode/UID
  → sub_277278 缓存与 enrich
  → sub_276E54 快照 eligible/incremental 记录
  → sub_2FA14C append AndroidSuspiciousInfo+0x28 repeated string
  → sub_1EC8BC 读取 +0x30/+0x38
  → protobuf field 12 (wire tag 0x62)
```

这不是“系统全部安装包”，也不是“只收可疑 APK”：collector 的来源是第三方包全集，但 event type、缓存处理标志和配置门控会让某次 payload 只携带 eligible/incremental 子集；风险标注为空时仍存在 append 路径。尚不能越级声称某次运行一定包含本模块、field 12 一定非空或服务端如何解释该元素。

同库的 `sub_24391C` 以 `RegisterNatives(..., 9)` 注册 `com/netease/htprotect/factory/JNIFactory`。表项已完整恢复：

| Java native method | JNI signature | native function | 直接用途/路径 | 等级 |
|---|---|---:|---|---:|
| `hccd63688a790ca65` | `(Context,String,HTPCallback,HTProtectConfig)V` | `sub_242788` | Context/callback/config 初始化 | E3 |
| `t76euy9fu8bv485zh` | `(String,String,String,String,String,int,String)int` | `sub_2430F4` | 校验首个 32-char 字符串后进入初始化/配置 core | E3 |
| `e9edd62242ad7aecf` | `(String,int)String` | `sub_243474` | `sub_2458CC` 字符串变换 | E3 |
| `r316e12523620efb7` | `(String,int)String` | `sub_2435B4` | `sub_245B00` 字符串变换 | E3 |
| `r25d273c7ad4065c3` | `([BIIZIZ)[B` | `sub_2436F8` | 调用者 byte[] 的通用变换；返回 4-byte status + output | E3 |
| `d0f149b4da6ec477` | `(int,String)String` | `sub_24331C` | `sub_24646C(selector,string)` 命令分派 | E3 |
| `f190da6241bff18bf` | `()V` | thunk `0x2436F4 → 0x2455E0` | 无参初始化/刷新入口，细分语义未命名 | E2 |
| `u233ace17d63ca9e` | `(boolean,int,int,int,int,int)V` | `sub_2438FC → 0x2478A0` | 运行参数/配置设置入口 | E2 |
| `aebd1811194e82d9` | `(String)[B` | `sub_243B18` | security token/blob 取数接口；返回 4-byte status + payload | E3 |

其中最终 security payload 的取数链已直接闭合到最后一项：

```text
JNIFactory.aebd1811194e82d9(String)[B
  → sub_243B18
  → loc_247B1C（设置内部 "gt" 模式）
  → sub_213814
  → sub_30865C(eventType=6)
  → async: sub_306014 → sub_306194
       └→ sub_308EFC → sub_2FF7AC → AndroidSuspiciousInfo serializer
       └→ sub_2C3110 output wrapping
    fallback: sub_308CC0
       └→ sub_308EFC → sub_2FF7AC
       └→ sub_2C3C70 output wrapping
  → Java byte[] = little-endian status + wrapped payload
```

`sub_243B18` 在 status=200 时创建 `payloadLength+4` 的 Java byte[]，先写 4-byte status，再写 native 输出，因而“serializer → 精确 JNIFactory 方法 → Java byte[]”是 E3。`r25...` 某些变换模式也能经 `sub_A72F0 → sub_30974C → sub_308CC0` 触发 security builder，但它接受调用者 byte[]，不是本轮选定的独立 blob getter。`sub_2C3110/sub_2C3C70` 已确认是 serializer 之后、Java 返回之前的 wrapping 阶段；尚未把其中每一步严格命名为 encrypt/compress，故不越级标注具体算法。

启动期 `/v6/main/init` 下发的远程配置包含：

```text
MainInit.useDDI=1
MainInit.useDDISessionId=1
MainInit.useDDIEvent=0
```

同时列出了 createFeed、reply、like、likeReply、message、send 等写操作的 DDI event 范围；Coolapk 请求还携带设备/会话身份头。结合 `libNetHTProtect.so` 已确认的“探针事件 → 全局队列 → security blob”和 `libmetasec_ml.so` 已确认的风险字段序列化，可以把链路推进到：

HTTP Toolkit 中已有两组可重复的同启动周期差分：

- `/v6/main/init` 请求的 `x-app-device` 长度为 132 chars，按宿主实际编码执行“reverse → Base64 decode”后为 99 bytes 基础设备记录；
- 该响应下发 `useDDI=1` 与 `useDDISessionId=1` 后，约 0.3–0.5 秒内的 `/v6/main/indexV8` 和 `/v6/account/checkLoginInfo` 请求均使用完全相同的 212-char `x-app-device`；
- 解码后仍是同一基础结构，但长度变为 159 bytes，末尾新增一个分号分隔的固定 64-char 字段；两组启动周期的长度、结构和字段值均复现；
- 同期请求没有独立 DDI query/body 参数；抓到的 `/v6/service/sync2` 也使用 212-char 扩展头，而它的 form body 只有 `reportProgress`。较早一条 sync cookie 出现 `ddid=<UUID>`，但它不是每个已捕获 endpoint 都携带，不能与扩展头末尾字段混为一谈。

因此可以 E3 确认 DDI/DDI-session 开关生效后的具体 HTTP attach point 是请求头 `x-app-device` 的末尾 64-char 子字段，至少覆盖 Feed 与登录检查，且可扩展到 sync；`/v6/main/init` 自身在取得开关前只发基础版本。由于 JNIFactory Java wrapper 不在 base APK 的 30 个壳类、当前 JADX 工作区或既有恢复 DEX 中，`aebd...(String)[B → x-app-device[final64]` 目前是“时序、长度和单一 native getter 一致”的 E2 强结构边，尚未达到直接 Java invoke/put-header 的 E3。

```text
第三方包 collector（path/package/versionCode/UID）
  → eligible/incremental enrich/cache
  → NetHT AndroidSuspiciousInfo.installedApk
  → protobuf field 12
native LSPosed/Zygisk/ART/root/风险应用探针
  → native security/risk payload
  → JNIFactory.aebd1811194e82d9(String)[B
  → （Java wrapper 未恢复，E2）x-app-device 尾部 64-char 字段
  → Feed/account/sync 请求
  → （仍待确认）服务端 risk flag / Feed 风险行生成条件
```

主线已闭合到具体 native getter 和具体 HTTP header 子字段；剩余缺口是中间 Java invoke/put-header 的直接字节码证据，以及服务端响应或客户端哪一分支生成风险行。现有 collector、序列化、JNI 和抓包差分足以说明该通道不是纯 SDK capability，但不能声称某个单独信号必然触发风控。

### 1.16 【2026-08-26 夜间会话】Priority 1/2/3 闭合：installedApk 谓词 + field 12 结构 + aebd→X-App-Device（全 E3）

完整报告见 `issue5_p1p2_predicate_report.md`。核心结论：

- **【P1】入选谓词已到 E3**（无 unknown_condition）：`pm list -3` → ingest 门（cfg bit11、节流20、oom_score_adj≤0 或 cfg[712]、缓存 miss）→ enrich 门（宿主自名豁免；其余 APK ≤ cfg[512] MiB）→ 记录 `+281==0` → append（event 8 另需 cfg[624] 或（有标注 && cfg[674]））。
- **【P1】默认模式为严格增量**：构建成功后 `netht_mark_snapshot_reported`(+280) → `netht_promote_reported_to_skip`(+281)，且缓存链表无删除/清空者 → **每个包在进程生命周期内最多进入 field 12 一次**。`eventType==6 && config["gt"]==1 && cfg[788]` 时走 `netht_fresh_pm_collect_snapshot` 全量路径（标志全零、明文路径 key 与缓存 hex key 永不匹配，不污染增量状态）。
- **【P2】field 12 元素帧**：`"$S_BF#A" + "{" + md5hex32(payload) + "}" + payload`；`payload = 包名 + [标注] + ["@@"+label] + ["@@as:"+acts]`。标注为 9 项已解码权限匹配：`@@su and alert and readclop`（SUPERUSER+ALERT_WINDOW+ACCESS_MOCK_LOCATION）、`@@inject`、`@@writesec`、`@@bindacc`（BIND_ACCESSIBILITY_SERVICE）、`@@unknown`、`@@shizuku`、`@@mockloc`，受 cfg bit22 门控。
- **【P2】服务器经 field 12 只拿到包名+权限标注+label+可选 acts**；apkPath/UID/versionCode/签名/组件计数/min-target SDK 都只留在本地记录，不进该字段。
- 新确认三条并行采集面：共享 UID 异名 type-7 `uid_match` 事件；`/storage/emulated/0/Android/data` 相邻 UID（[self−200, self+200]∩[10200,10500]）目录扫描 → type-2000 `mis` 事件（cfg[823]）；component-8 差集 → `changedPackages`（卸载/变更，事件 {3,10}）。
- 缓存 key = pm 行路径子串的大写 hex（`netht_str_to_upper_hex`）；MD5/`%02x` 由 `netht_md5_hex_string` 确证。
- **【P3，同夜完成】`aebd... → x-app-device` 升 E3**（详见报告 §11-14）：
  - 历史阴性根因：头名实为 **`"X-App-Device"`（大写）**，此前全部小写检索漏检；wrapper 位于运行时解密 DEX（盘上 APK 仅 30 个易盾壳类）。
  - `NetEaseProtectSDKManager.ׯ → HTProtect.getToken → WatchMan.getToken → DynamicTask → WatchMan.O000000o → factory.O000000o(String)[B → JNIFactory.aebd1811194e82d9`。
  - blob 在**启动期一次性取数生成 nuid**（"nuid loaded"），nuid 与 Shuzilm DID（`cn.shuzilm.core.Main`，RSA 公钥 init；`MainInit.useDDI/useDDISessionId` 即其开关）组成 `X-App-Device` 复合串首字段 → Base64 → **reverse** → 去换行补位 → `HttpClientFactory$CoolMarketHeaderInterceptor` 写头。抓包"DDI 后末尾固定 64-char"= reverse 后的复合串首字段，结构精确吻合。
  - 平行通道同闭环：`_v2_post_token` POST 表单（RequestSessionIDUpdater 白名单 init/indexV8/checkLoginInfo + PostToken.* 远程配置）；`ddid` Cookie（Shuzilm getSessionSync）。
- 模块决策：**KEEP MODE A FROZEN**（M1–M5 均无新直接证据；模块包按普通第三方包处理，无包名专属匹配）。
- IDB 已写入 23 个 rename + 8 条注释并保存；未 patch 任何字节；环境异常（/proc 反篡改升级、processing 目录 root 文件被未知机制删除）已记录在报告 §15。

### 1.13 再删除两个长期 framework Hook

原 2.2.0 的 Issue #2 UI fallback 在用户启用任一相关功能后会长期 Hook：

```text
LayoutInflater.inflate(int, ViewGroup, boolean)
View.setTag(Object)
```

这些全局入口分别已有更窄的替代边界：Auto Comment 和 Topic Recommend 使用 Coolapk 业务方法；Related Data 和 Detail Sponsor 使用专用 ViewHolder/业务 getter；Same Topic Feed 使用验证后的 `entityTemplate` 数据过滤。当前补丁已删除 `ContentLayoutHooks`、相关 plan/readiness 和仅验证资源存在性的 fallback evidence；开启 Issue #2 功能也不再安装这两个 framework Hook。readiness 现在只接受实际业务 method hook、专用 holder hook或 T4 的语义证据。

### 1.14 Feed after-filter 的一致性边界

当前两个 Feed Hook 精确落在宿主：

```text
EntityAdHelper.<obfuscated>(List, boolean) -> List
EntityListFragment.<obfuscated>(List, boolean) -> List
```

拦截顺序是先完整执行宿主，再检查返回的 `List`。只有命中广告实体时才新建一个过滤后的 `ArrayList`；原始 `List` 不会被修改。模块没有持有或修改 response、cursor、page、request id、广告对象字段，也不会伪造曝光成功回调。

因此可以静态确认：

- 服务端响应对象及其中独立保存的 cursor/page 元数据不会被模块直接改写；
- 传给下游 adapter 的可见列表会自然压缩 position 和 item count；
- 恢复业务 DEX 中的 `RecyclerViewExposureHelper$endExposure$1.invokeSuspend` 会遍历已经进入 `InExposureData(data, position)` 队列的项目，逐项调用 `p8d(..., data, position, false, true)` 后清空队列；这直接证明曝光结束链按已收集项目及其 adapter position 工作，而不是按原始 Feed response 整批伪造；
- 被 after-filter 删除的实体不进入正常 adapter/ViewHolder/可见性链，因此不进入该曝光队列是 E2 的结构结论；队列的完整入队函数在当前恢复 DEX 中仍是损坏 stub，不能把这条边升级为 E3；
- “服务端下发但长期无曝光”仍是机制上可能的状态差，但这条 Feed after-filter 在 2.1.2 已存在，版本相关性弱于 2.2.0 新增 Hook 面。

恢复业务 DEX 中没有找到 `/v6/feed/reportExpose`、`reportExpose` 或 `reportProgress` 的直接字节码引用；启动配置中当前还观察到 `Stat.reportExpose=0`。结合上述按可见项维护的曝光队列，当前更准确的边界是：模块不会伪造“已展示”状态，也不破坏宿主在返回 List 之前完成的副作用；它只是让被删除广告自然缺少后续曝光。该行为可能影响广告投放/频控统计，但没有直接证据把它连接到安全风控，而且它不是 2.2.0 新增机制。

### 1.15 风险行来源仍未确定，当前证据只排除一部分客户端硬编码模型

本轮对恢复的 9.67 MiB 宿主业务 DEX、APK 解包资源和可用运行时 DEX 做了定向搜索：没有找到 Issue 风险文案、`risk`/风控 flag、风险专用 `entityTemplate`，也没有找到带这些常量的 Feed item builder。宿主 DEX 中存在通用 `Entity.getEntityType/getEntityTemplate` 分派和大量普通 Feed/card 模板，但这只能证明统一实体渲染框架，不能证明风险行由客户端合成。

已有 12 份成功的 `/v6/main/indexV8` 响应被完整分页并解压检查；其中只出现普通 `card`、`feed`、`feed_reply`、`feedRelation`、`feedTarget`、`iconLink`、`image_1`、`imageText`、`product`、`topic`、`user` 等 entityType，以及普通 Feed/card/image 模板，没有风险专用 entity/template。内容中的“风险/安全/root”等词只来自普通帖子或话题正文，不是风险提示结构。这个阴性批次只证明“当前捕获窗口没有风险行”，不能证明服务端模型不存在。

因此风险行来源当前必须标为 **未确定**：

- “服务端直接下发风险 row/text”与现有证据相容；
- “服务端下发 flag/text、客户端用通用 card builder 合成”同样未被排除；
- “APK 内硬编码风险文案或显式 risk template 后本地构造”因全局定向搜索无命中而被削弱，但不是形式化反证；
- 在取得一次风险阳性的原始 Feed 响应或对应 UI XML 之前，不能把 security blob 与风险行生成条件越级写成 E3，也没有新的直接证据支持模块侧补丁。

## 2. 当前假设排序

| 等级 | 假设 | 静态证据 | 动态证据 | 排序变化与可证伪条件 |
|---|---|---|---|---|
| 高 | 应用列表/私有目录枚举 + 远程规则参与风控 | NetHT 直接执行 `pm list packages ... -3`，解析 path/package/versionCode/UID，经缓存/enrich 写入 `installedApk` field 12；`libmetasec_ml.so` 另会实查自动化应用目录并维护 `mssdk_riskapp_db` | 用户反馈与本机权限提示均支持；DDI 开关后 `x-app-device` 出现稳定 64-char 扩展字段 | 第三方包 collector → native blob getter 已 E3，DDI attach point 已 E3，getter → header 子字段为 E2；仍需风险阳性响应证明服务端关联 |
| 高 | LSPosed/Zygisk/ART 注入环境被 native 主动采集 | `dalvik-main` 中直接搜 `LSPosed`；ptrace 探针解码 `zygisk`；odex 编译参数、smaps 与 framework JAR mount 探针均构造 2000 号事件并进入安全 blob | Frida 注入会被保护层击杀 | 从“中”上调为确认的检测通道；但环境长期存在，单独不足以解释 2.2.0 起始相关性 |
| 高 | root/Magisk/ADB-root 环境被另一 native SDK 采集 | `libmetasec_ml.so` 实查 30 个 root/Magisk 路径，执行 adbd 命令探针，并把结果写入模块 2 的 `root` 字段 | 当前设备本身处于 root 环境 | 独立采集和字段写入已确认；仍不能单独解释版本起始相关性或最终服务端策略 |
| 中高 | 2.2.0 新增常驻 framework hooks 增加既有检测通道中的可观察变化 | 五个 Settings framework Hook 和两个可选 layout/tag framework Hook 均为版本相关；native 确认会观察 ART 堆、odex、`libandroid_runtime.so` 和 framework JAR mount | 最新窄化构建进入 READY，临时 Hook 正常退休 | Mode A 已移除全部七个；多设备/多轮若风险显著下降才增强，单轮阴性不能证伪 |
| 中 | 宿主私有目录中的模块文件被检测 | 宿主可无权限直接读取 config/cache/log/libdexkit，名称高度可识别 | Java DEX 无精确字符串命中 | 需定位 native/远程通用目录枚举；未确认前不做复杂隐匿 |
| 中 | 启动已判定，Feed 后延迟展示 | 符合服务端 flag/TTL 模型 | “刷帖时突然出现”用户反馈 | 需把首次风险 UI 与启动、Feed 响应时间关联 |
| 中低 | Feed 删除广告导致下发、展示、impression 状态不一致 | after-filter 不改原 List/response/cursor；宿主曝光结束链按已收集的 `InExposureData(data, position)` 逐项处理，被删广告自然不进入下游曝光队列 | 风险 UI 在 Feed 中出现 | 状态差被收窄为“下发但未展示/未曝光”；不伪造 impression，且机制早于 2.2.0，未找到与安全风控的直接边 |
| 中低 | 默认 Reply Sponsor 触发临时 ClassLoader hook 被检测 | 默认配置会在启动期安装，READY 后退休 | 新进程日志确认已退休 | 检查 hook 实际驻留窗口和移除成功率；可做仅关闭 reply sponsor 的隔离模式 |
| 低至中 | `shouldShowAd` 返回值覆盖/方法 trampoline 被监控 | 目标点确实曾被 Hook；无直接检测 xref | AFTER_OVERRIDE 仅确认 original 常为 true | Mode A 已完全移除此目标；多轮差异只能调整概率，不能单轮归因 |
| 低 | PRE_BLOCK 跳过 `shouldShowAd` 关键副作用 | 完整 DEX 反证：函数只有读取和判断 | 无副作用缺失证据 | 已从最高危下调；若其它版本函数体变化需重新审计 |

## 3. 最危险的代码路径

### 3.1 当前最高价值宿主路径

```text
Coolapk 启动/隐私检查
  → 数量过少时显示权限提示
NetHT sub_26FCEC
  → pm list packages ... -3
  → 解析/缓存/enrich 第三方包记录
  → AndroidSuspiciousInfo.installedApk field 12
  → JNIFactory.aebd1811194e82d9(String)[B
  → （Java wrapper 未恢复，E2）x-app-device 尾部 64-char 字段
  → Feed/account/sync 请求
  → （待确认）服务端 account/device/session risk flag
  → 后续 Feed 响应展示风险行
```

“权限提示”、NetHT 自身的第三方包 collector、native blob getter 与具体 HTTP attach point 已分别确认；“本模块出现在本次 eligible 子集 → getter 的 Java 返回值直接填入 header 尾字段 → 风险阳性响应/风险行”尚未确认。

### 3.2 已确认的 native 安全数据路径

```text
sub_2483A8 启动探针调度
  → sub_2B7AD0：dalvik-main → "LSPosed" / is-top-app
  → sub_29A16C：ptrace/fork → "zygisk"
  → sub_294620：base.odex → inline-max-code-units=0
  → sub_298F98：libandroid_runtime.so smaps 变化
  → sub_298170：com.android*.jar fd → mnt_id
  → 构造 eventCode=2000 的 160-byte record
  → sub_2F3308 全局事件队列
  → sub_2F3C84 / sub_2FF7AC / sub_308EFC
  → serialized security blob

AndroidSuspiciousInfo schema
  → sub_26FCEC：pm list packages ... -3
  → sub_273B74：path/package/versionCode/UID
  → sub_277278 / sub_276E54：enrich/cache/snapshot
  → sub_2FA14C：append installedApk repeated string
  → sub_1EC8BC：field 12 serializer
  → filePermisson / apkLibName / ExtDataEntry
  → InstallApkInfo.susPermissionList
```

这条链已确认 installedApk 的采集、加工、字段写入、序列化与精确 JNIFactory blob getter；抓包已把 DDI attach point 定位到 `x-app-device` 尾部 64-char 子字段。当前仅缺 getter → header 的直接 Java invoke/put-header 字节码，以及服务器风险策略和风险阳性响应/风险行证据。

同一 APK 中还存在第二条已确认链：

```text
libmetasec_ml.so init-array
  → init_root_path_table：4 个 Magisk + 26 个 su/root 工具路径
  → detect_magisk_paths / find_existing_root_path
  → detect_root_environment
      └→ ps | grep adbd / ps -e | grep adbd → 搜索 "root"
  → collect_module2_risk_fields：字段 "root"
  → collect_module2_payload
  → serialize_module2_payload（注册模块 ID 2）

init_automation_app_path_table
  → detect_automation_app_dirs：10 个自动化/连点器目录
  → collect_automation_risk_markers：标记 "cka"
  → collector message string field
```

这证明酷安所带 SDK 不只观察注入环境，也会聚合 root 与风险应用信号；但两条 native 链均更像长期存在的环境基线，当前最有解释力的 2.2.0 版本差分仍是新增 framework Hook 面。

### 3.3 2.2.0 原设置 Hook 路径

```text
SettingsHooks.install
  → Hook Instrumentation resume/destroy
  → Hook Activity finish/back/touch
  → 常驻整个 Coolapk 进程
  → 可能被 stack/trampoline/framework integrity 检测观察
```

这是目前最强的版本差分型模块风险路径，本轮补丁已消除。

### 3.4 已降级的 Splash decision 路径

```text
MainActivity.onCreate
  → kc5.Ԯ(...) / shouldShowAd（只读判断）
  → true: SplashAdLoader 或其它广告展示路径
  → 真正展示时才写 SPLASH_AD_LAST_SHOW
```

PRE_BLOCK 虽不理想，但“跳过关键状态写入”的核心假设被完整字节码反证。Mode A 仍选择彻底不 Hook 该方法，以减少 observable surface。

### 3.5 2.1.2 与当前 Mode A 的 Hook surface 差分

| Hook 点 | 层级 | 2.1.2 | 当前 Mode A | READY 后 | 默认启用 | 结论 |
|---|---|---:|---:|---:|---:|---|
| `Application.attach(Context)` | framework | 是 | 是 | 是 | 是 | 既有 bootstrap 边界，非 2.2.0 新增 |
| `Instrumentation.callActivityOnCreate` 两个 overload | framework | 是 | 是 | 是（callback 退休，只留 Splash safety gate） | 是 | 既有 safety net，非 2.2.0 新增 |
| RuntimeDexObserver 的 `ClassLoader.loadClass` 两个 overload | framework | 是 | 是 | 否 | 是 | 临时；最新进程 2 个均卸载成功 |
| Feature lazy 的 `ClassLoader.loadClass` 两个 overload | framework | 否 | 是 | 否 | 是（Reply） | 2.2.0 新增，但仅约 200 ms；最新进程 2 个均卸载成功 |
| Splash Activity `onCreate` | app business | 是 | 是 | 是 | 是 | 必要 presentation Hook |
| Feed `(List, boolean)->List` 业务方法 | app business | 是 | 是（当前 2 个） | 是 | 是 | 既有 data after-filter |
| Reply sponsor ViewHolder bind | app business | 否 | 是 | 是 | 是 | 2.2.0 新增业务 Hook，不是 framework |
| Issue #2 method/holder hooks | app business | 否 | 可选 | 是 | 否 | 仅用户开启对应功能 |
| `shouldShowAd` | app business | 否 | 否 | 否 | 否 | 已彻底删除，不再优化 |
| Settings 的 5 个 Activity/Instrumentation Hook | framework | 否 | 否 | 否 | 否 | 已由 lifecycle callback 替代 |
| `LayoutInflater.inflate` / `View.setTag` | framework | 否 | 否 | 否 | 否 | 本轮彻底删除，业务 Hook 替代 |

当前相对 2.1.2 唯一默认新增的 framework Hook 是 Feature lazy 的两个 `ClassLoader.loadClass` 临时 Hook；它们用于发现 Reply ViewHolder，并已在最新进程 200 ms 时全部卸载。长期 framework surface 已回到 2.1.2 的集合，没有默认新增项。

## 4. 推荐的最小补丁

本轮已实现 Mode A，当前工作区 diff 的原则是“删目标、删 Hook、不增加诊断常驻面”：

1. 从 `Issue2Resolver` 删除 splash decision 的解析。
2. 从 `FeatureHooks` 删除该 target 的安装与返回值覆盖。
3. 从 `FeatureTargetReadiness`、`TargetResolver`、`TargetVerifier` 删除 decision readiness/key/verifier 死代码。
4. Splash 只保留已有的 Activity/presentation 边界处理。
5. `SettingsHooks` 删除五个 framework hooks，改用 Application lifecycle callback。
6. 删除 `LayoutInflater.inflate` / `View.setTag` 两个全局 fallback Hook，Issue #2 只接受业务 method/holder/语义路径。
7. 删除一次性 DEX dump 与 decision 诊断日志代码。
8. 保留 feature/runtime 临时 ClassLoader Hook 的 READY 退休机制。

补丁可直接回滚，未改配置格式和缓存 schema。旧缓存中的 `feature.splashDecision` 首次会被忽略并重建，后续计划稳定为 7 个目标。

实机结果：

```text
settings lifecycle callbacks registered frameworkHooks=0
feature target plan ... keys=[splash_base, feed, feed#2,
  getter.entityTemplate, getter.entityId, getter.title, getter.entityType]
coordinator state=READY
feedInstalled=2
splashSpecificInstalled=[...SplashAdActivity]
```

设置页“酷安净化”入口和 8 个开关页均已打开验证。

## 5. 动态实验设计

总原则：**单次阴性不排除，单次阳性不直接归因。** 每轮记录 build hash、模式、账号/设备、启动时间、首次风险 UI 时间、此前风险状态和应用列表权限，不清除或重置无法控制的服务端变量后强行归因。

建议按以下顺序积累：

1. Mode A（当前）：无 decision hook、无 Settings framework hooks，其余功能保持原配置。
2. Mode D：在 Mode A 基础上关闭全部 Splash 净化，隔离“Activity finish”本身。
3. Reply-off：在 Mode A 基础上仅关闭 reply sponsor，隔离启动期 feature ClassLoader lazy hook。
4. Feed-off：在 Mode A 基础上仅关闭 Feed 过滤，观察风险 UI 是否仍在刷帖阶段出现。
5. 应用列表权限保持现状，不在同一轮同时切换；若测试权限变量，必须单独成组且记录 Coolapk 实际可见包数量。

PRE_BLOCK 和 AFTER_OVERRIDE 已不再是首选正式方案。若需要严格对照，可保留离线构建，但不建议在同一已被标记的 account/device session 上把单轮表现当结论。

## 6. 需要用户收集的日志

只需收集能区分路径的最小集合：

- 模块 build SHA-256 / 模式；
- Coolapk 版本、设备、账号是否已出现过风控；
- 应用列表权限状态，以及是否出现“可读取应用较少”提示；
- 本轮启动时间、首次看到风险行的时间、当时页面/操作；
- 同一进程的模块日志片段：`feature target plan`、`coordinator state`、`resolver fullReady`、`settings lifecycle callbacks`；
- 风险行截图或 UI XML及其前后一次 Feed 请求时间（若可得）。

不需要再次采集全量内存 dump，也不要重复 Frida 注入。

## 7. 后续证据目标

1. `installedApk collector → field 12 serializer → JNIFactory.aebd... → x-app-device attach point`：**全链已于 2026-08-26 夜间会话闭合到 E3**（P1 谓词、P2 元素结构、P3 Java 边，见 1.16 与 `issue5_p1p2_predicate_report.md`），客户端侧管道已无未知边；不再重复追。
2. 当前最高价值输入是一次**风险阳性**的原始 Feed 响应及同时间 UI XML/截图，用它直接判断 server row、server flag + 通用 builder，或其它展示模型；没有阳性样本时保持"未确定"。
3. `AuthUtils`（`X-App-Token = AuthUtils.getToken(ctx, X-App-Device 值)`）定义不在已恢复的 4 个 dex 中（native 或未恢复 dex，E2）；如后续需要可在同窗口策略下恢复其所在 dex。
4. Feed after-filter 的状态差已收窄为"服务端下发但客户端不展示/不曝光"；该机制早于 2.2.0，除非发现安全风控直接消费广告曝光缺失，否则不触发模块补丁。
5. Mode A 继续冻结；只用多轮、带 account/device/session/TTL 记录的观测调整假设概率，新的模块改动必须由直接证据触发。
6. （更新）P1/P2/P3 已完成；剩余 P4 跨版本差分、P5 通用 card 渲染模型、P6 宿主私有目录 collector。

## 验收记录

- `gradlew test assembleRelease`：通过（JDK 17）。
- 当前窄化 Release APK：1,112,997 bytes。
- 当前 APK SHA-256：`dbfa344805ab98d6334c1d54d0704cb6892aa68d3261fc3fa1ba480eec7d5e8c`；设备已安装 APK 哈希一致。
- 安装方式：push 到 `/data/local/tmp/processing/` 后由 root `pm install -r -t --user 0` 安装。
- 仅重启作用域应用 Coolapk；未重启手机、未重启 LSP 守护进程、未修改 LSPosed 数据库或作用域。
- 设备已安装当前 Mode A 窄化构建，不再是一次性 DEX dump 诊断版；最新进程 200 ms READY，`missingRequired=[]`，feature/runtime 两组临时 ClassLoader Hook 均完全退休且 `frameworkActive=false`。
- 设备私有目录中的 522 MiB 一次性 dump 已删除；`processing` 中诊断 APK 和二进制副本已删除，保留最终 APK 与 UI 验收文件。
