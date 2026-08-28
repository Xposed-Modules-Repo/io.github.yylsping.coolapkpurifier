# 自研分析脚本

风控研究期间编写/迭代出的分析脚本精选（原始一次性脚本经过筛选：剥离了本机路径、MCP 端点等私有内容；Python 脚本均通过语法编译验证）。

**路径约定**：脚本内的输入路径已从本机绝对路径改为相对路径，按 `../assets/` 的目录布局放置文件后即可使用，或按需自行修改。

## 离线 ELF / native 分析（依赖 capstone）

| 脚本 | 用途 |
|---|---|
| `stackstr.py` | **栈字符串重建器**（核心方法）：NetHT 等库的安全字符串全部为栈上 `mov/movz/movk → strb/strh` 逐字节构造 + 算术解码，本工具做滑窗重建与多密钥变体解码 |
| `p4b_fullscan.py` | 全 .text 能力扫描：结合 `stackstr` 做全密钥暴力解码，输出命中清单（跨版本差分用） |
| `p4b_ctor2.py` | 配置对象构造器默认值提取（指针偏移追踪，输出全部立即数存储） |
| `p4b_align_scan.py` | 对齐扫描辅助（`p4b_fullscan` 依赖） |
| `ms_exports.py` | 通用 ELF 动态符号表解析（导出/导入清单） |
| `ns_real_dynsym.py` | 节表被故意污染时，经程序头 + `PT_DYNAMIC` 恢复真实 dynsym/RELA（易盾壳 libnesec 用） |
| `aarch64_disassemble_range.py` / `aarch64_string_xrefs.py` / `aarch64_call_xrefs.py` | capstone 反汇编区间 / 字符串 xref / 调用 xref 的小工具 |

## DEX 离线分析

| 脚本 | 用途 |
|---|---|
| `repair_dex_header.py` / `recover_string_block.py` / `restore_wiped_dex_magic.py` | 内存 DEX 修复（magic 重写、字符串块恢复；业务 DEX 头部 4 字节修复即用后者完成） |
| `decode_strings.py` | DEX/内存中的混淆字符串解码 |
| `minidex.py` / `scan_dex_candidates.py` | 迷你 DEX 解析器与候选区扫描（androguard 无法整解析的运行时 DEX 用） |
| `inspect_dex_methods.py` / `pD_card_analysis2.py` / `pD_dump_classes.py` / `pD_xref_k.py` | androguard 业务 DEX 分析（类/方法/调用侧） |
| `pC_dex_diff.py` | 两版壳 DEX 字符串池**全集差分**（跨版本报告 §3.2 方法） |
| `extract_ascii_strings.py` | 通用可打印字符串提取 |

## IDA 内置 Python（配合 `../assets/idb/` 使用）

在 IDA 的 Script file / python 控制台内运行，不依赖外部 MCP：

| 脚本 | 用途 |
|---|---|
| `ms_jni_sites.py` / `ms_vtable.py` / `ms_netcallers.py` | metasec（libmetasec_ml）JNI 注册位点 / vtable / 网络调用侧分析 |
| `nB_reg_callers.py` | NetHT 记录注册表访问点/键值扫描 |

## Frida / 设备侧（易盾反篡改敏感，注意使用方式）

| 脚本 | 用途 |
|---|---|
| `dump_dex.py` / `dump2.py` / `dump3.py` / `spawn_dump.py` | 运行时 DEX 内存提取（壳解密后窗口期） |
| `trace_dex_methods.py` / `trace_loaded_netht.js` | DEX 方法追踪 / NetHT 加载观察 |
| `memscan.sh` / `dump_all.sh` | root 只读内存扫描与批量 dump（`/proc/<pid>/mem` 流式读取） |

> 警示：对目标进程的动态操作会触发易盾反篡改升级（连续读取 `/proc/<pid>/mem` 会先零填充返回、后连 maps 一并拒绝）；本研究最终采用"冷启动后单窗口一次流式读取 + 设备零落盘"的方式完成 DEX 提取。详见总览报告方法论一节。
