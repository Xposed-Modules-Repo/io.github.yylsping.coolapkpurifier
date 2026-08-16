# 酷安净化

基于 libxposed Modern API 102 的全版本酷安去广告模块。

## 功能

- 去除启动/开屏广告和全屏广告。
- 去除首页信息流广告与赞助卡片。
- 去除帖子回复区及评论中的赞助内容。
- 通过 DexKit 运行时指纹跨版本解析酷安 Hook 目标，不依赖具体混淆类名/方法名。
- SplashCritical 优先解析：启动时先解析并安装开屏 Hook，再后台完成 Feed/Entity getter 解析。
- 解析顺序：有效缓存 → 强 DexKit 指纹 → 弱 DexKit 指纹 → 框架兜底；任一阶段 0 个或多个候选都 fail-closed。
- 多版本持久缓存：按 stableTargetIdentity 保存，最多 5 个历史版本，完整缓存文件不超过 1 MiB，LRU 淘汰，原子替换写入。
- 同版本覆盖重装后 identity 不变，直接命中历史缓存；升级/降级到新版本后自动失效重扫。
- Bootstrap 终态零开销：READY 后关闭 DexKit Bridge、Resolver worker、RuntimeDexObserver、watchdog，并 retire 通用 Instrumentation Bootstrap Hook，Bootstrap 日志冻结。
- 无缓存首次适配时显示一次系统 Toast，提示开屏广告可能显示一次。
- 不包含联网、更新器、后台服务或周期性轮询。

## 兼容性

| 项目 | 要求 |
| --- | --- |
| 目标应用 | 酷安（包名 `com.coolapk.market`），运行时动态适配，不按版本分支 |
| 已实机验证 | 13.1.1 / 15.9.0 / 16.1.2 / 16.5.1 |
| Android | 6.0（API 23）及以上 |
| 框架 | 支持 libxposed Modern API 102 的 LSPosed |
| 模块版本 | 2.1.0（versionCode 7） |

模块不针对任何酷安版本做分支，Hook 目标全部由 DexKit 在运行时解析。若新版本功能失效，请先查看目标应用内：

- `files/coolapk_purifier_bootstrap.log`：启动时序与 Bootstrap 终态日志。
- `files/coolapk_purifier_cache_v3.json`：多版本解析缓存。
- LSPosed 模块日志：Resolver 候选数与 Hook 安装情况。

并提交包含酷安版本号、Android 版本和相关日志的问题报告。

## 安装

1. 从 GitHub Releases 下载并安装 APK。
2. 在 LSPosed 中启用模块，作用域只选择“酷安”。
3. 强制停止酷安后重新打开。

## 构建

需要 JDK 17、Android SDK 35，并可从 Maven Central 获取 `io.github.libxposed:api:102.0.0`。

运行单元测试并生成 debug 测试包：

```powershell
.\gradlew.bat test assembleDebug
```

测试 APK 输出到 `app/build/outputs/apk/debug/`。面向普通用户的已签名版本请从 GitHub Releases 下载。

## 许可证

本项目采用 [MIT License](LICENSE)。

## 免责声明

本项目仅供学习、研究和个人设备使用，与酷安及 LSPosed 项目无隶属或认可关系。使用前请确认符合当地法律及相关服务条款。
