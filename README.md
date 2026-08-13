# 酷安净化

基于 libxposed Modern API 102 的酷安去广告模块。

## 功能

- 去除启动广告和全屏广告。
- 去除首页信息流广告和赞助卡片。
- 去除帖子回复区的赞助内容。
- 反射入口与实体访问器按运行时类型缓存；解析失败时默认保留原内容。
- 不包含联网、更新器、后台服务或周期性轮询。

## 兼容性

| 项目 | 要求 |
| --- | --- |
| 目标应用 | 酷安（包名 `com.coolapk.market`） |
| Android | 6.0（API 23）及以上 |
| 框架 | 支持 libxposed Modern API 102 的 LSPosed |
| 模块版本 | 2.0.0（versionCode 5） |

酷安内部类名和数据模型可能随版本变化。若升级酷安后功能失效，请先停用模块并提交包含酷安版本号、Android 版本和相关 LSPosed 日志的问题报告。

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

维护者本地的正式构建使用本项目专属发布密钥：

- 密钥：`signing-private/release.p12`
- 凭据：`signing-private/signing.properties`

这两个文件均已被 `.gitignore` 排除，不会提交到 GitHub。存在本地签名配置时，执行：

```powershell
.\gradlew.bat test assembleRelease
```

发布密钥决定 Android 能否覆盖升级。请加密备份整个 `signing-private/` 目录，切勿删除、重新生成或提交其中内容。由于本项目此前的 APK 使用测试签名，首次切换到该发布密钥时需要先卸载旧版；此后的正式版本可以直接覆盖升级。

## 许可证

本项目采用 [MIT License](LICENSE)。

## 免责声明

本项目仅供学习、研究和个人设备使用，与酷安及 LSPosed 项目无隶属或认可关系。使用前请确认符合当地法律及相关服务条款。
