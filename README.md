# 酷安净化

基于 libxposed Modern API 102 的酷安去广告模块。

## 功能

- 去除启动广告和全屏广告。
- 去除首页信息流广告和赞助卡片。
- 去除帖子回复区的赞助内容。
- 通过 DexKit 运行时指纹跨版本解析酷安 Hook 目标，不依赖具体混淆类名/方法名。
- 解析结果按目标 APK 身份持久缓存；升级或降级后自动失效重扫，缓存命中时不再全量扫描。
- 解析顺序：有效缓存 → 强 DexKit 指纹 → 弱 DexKit 指纹 → 框架兜底；任一阶段 0 个或多个候选都 fail-closed。
- 不包含联网、更新器、后台服务或周期性轮询。

## 兼容性

| 项目 | 要求 |
| --- | --- |
| 目标应用 | 酷安（包名 `com.coolapk.market`） |
| Android | 6.0（API 23）及以上 |
| 框架 | 支持 libxposed Modern API 102 的 LSPosed |
| 模块版本 | 2.1.0（versionCode 7） |

模块不针对任何酷安版本做分支，Hook 目标全部由 DexKit 在运行时解析。若新版本功能失效，请先查看目标应用内 `files/coolapk_purifier_resolver.log` 与 LSPosed 日志，并提交包含酷安版本号、Android 版本和相关日志的问题报告。

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
