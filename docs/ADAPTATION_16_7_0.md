# 酷安 16.7.0 适配

宿主：16.7.0（2610291）。模块：2.7.0（19）。实机：PLQ110 / Android 17。

## 目标裁决

从实际运行的宿主提取并恢复 10 个业务 DEX，索引 94,682 个类。原始 APK
只有壳，未用它代替业务 DEX。离线 generator 显式使用 16.6.5（2610162）
作为 baseline；其 8 项扫描结果再结合原始指令、布局字段与数据类型核对。

完整 profile 包含 14 个目标，7 个变化、7 个保持不变：

| target | 16.7.0 裁决 |
| --- | --- |
| detailSponsor | `$$AutoValue_Feed.getDetailSponsorCard(): Entity`，不变 |
| detailSponsorUi | `com.coolapk.market.view.ad.ֈ`，binder U+0785、布局字段 U+0786，不变 |
| replySponsor | `b25`，binder U+0785、布局字段 U+0789，布局 `item_reply_self_draw` |
| sameTopic | `MainV8ListFragment.onInsertRecommendListEvent(i57)`；语义方法 U+021C |
| topicDeviceRecommend | `gg4`，assembler U+078E `(Feed, gg4, Composer, int): Unit` |
| topicDeviceRecommendUi | `nw4`，terminal U+052D `(Modifier, FeedTarget, FeedDetailV13ViewModel, Composer, int): void` |
| autoComment | 原控制器 U+037F，不变 |
| autoCommentPrompt | 原 lambda 的 terminal U+052A，不变 |
| relatedData | `$$AutoValue_Feed.getRelatedData(): List`，不变 |
| relatedIconListUi | 原 U+0794 owner、U+0785 binder、子类及 callback 字段，不变 |
| relatedContentUi | 原 reply U+052E owner、U+0793 `(HolderItem): void`，不变 |
| searchHotCapsuleUi | `jw`，binder U+0785，构造器 `(View, DataBindingComponent)` |
| searchHotWordsUi | `x2f`，binder U+0785，构造器 `(View, DataBindingComponent, q97)` |
| searchHotRankingsUi | `w2f`，binder U+0785，构造器 `(View, DataBindingComponent, q97, xy3)` |

回复赞助与详情赞助通过各自 `<clinit>` 的布局映射消歧；搜索 holder 同时
核对 `Card` 实例字段和 binder 中的 cast / 赋值，排除共用布局但使用其他
数据类型的 holder。还确认 `gg4` 的展示链实际调用 `nw4` terminal。
Unicode 名称由离线元数据搬运，回归测试使用 code point 转义。

保留全部旧 validated profile。未知 versionCode 继续 fail-closed，不采用
相邻版本或模糊候选替代。动态开屏和首页 resolver 无需更改，实机均为 READY。

## 配置页与存储

“酷安净化”作为独立原生分组插入设置最上方。配置页由酷安 Activity 所属的
Dialog 承载；返回后原生设置及导航仍在，不唤起模块 Activity。旧风险提示已
移除，保留更改后强制停止并重新打开酷安的提示。

配置页复用宿主 `right_in` / `right_out` 动画资源（16.7.0 为 300 ms），
进入时从右侧滑入，返回时向右侧滑出，并遵循系统过渡动画缩放。
顶部返回、系统返回及边缘返回手势均走同一退出流程；销毁宿主时立即释放窗口。
页面背景覆盖整个窗口（包括透明系统栏区域），系统栏和刘海安全边距施加于
页面内部，避免导航栏底部透出原生设置。页内重启提示不带末尾句号。

设置与解析缓存分别存于宿主 `files/coolapk_purifier_config.json` 和
`files/coolapk_purifier_resolver_cache.json`。实机文件属于宿主 UID，权限
`0600`；原子替换成功后才确认开关保存，失败回滚内存选择。

宿主文件缺失时尝试只读导入旧框架设置；存在后以本地快照为准。实机已有的
前 9 项开启、搜索页热榜关闭成功迁移，全开 / 全关回归后恢复了原选择。
多次冷启动均从宿主文件加载，缓存命中时 `dexkitScan=false`。

移除了跨应用配置 Activity、框架写入器和 libxposed service 依赖。模块 APK
无 Activity / Provider / Service / Receiver；进入设置前后均无需模块进程。
未修改系统跨应用启动权限、模块启用状态或作用域数据库。

## 验证与边界

- Debug / Compatible / Release 各 398 项单测通过；离线工具 3 项通过。
- Release lint 0 错误，13 项提示；release architecture gate 全部通过。
- 全开时 8 个版本绑定功能均为 INSTALLED；开屏与首页为动态 READY。
- 全关冷启动时，8 个版本绑定功能均为 DISABLED，两项动态功能为
  NOT_REQUIRED，不开启 DexKit / runtime observer。
- 开屏取得 finish 信号及移除确认；首页有赞助项过滤记录；回复赞助取得
  新 binder 的实际目标与修改记录；自动评论提示及机型 UI terminal 均触发。
- 相关推荐中的推广与单条推荐取得实际折叠记录；搜索的三个 UI 层均取得
  observed / suppressed 记录。搜索历史、正常正文与回复保留。
- 全关后热门搜索、搜索热榜、首页热词、机型卡及推广卡恢复显示。
- 配置页进退动画经实机录屏检查；顶部返回、系统返回、边缘返回手势及连续
  进入 / 返回正常，宿主配置文件 SHA-256 在动画回归前后保持一致。
- 导航栏漏底问题已复现并修复：旧内容区域止于 y=2744，底部 56 px 透明；
  修复后页面覆盖完整 1272×2800 窗口，滚动与底部点击不会露出或打开底页。
- 同话题动态通过临时只读探针确认：来源为 `/v6/main/indexV8`、来源判定
  与原生开关均为 true，新 `i57` 事件实际进入目标，宿主 handler 正常返回。
  本轮收到的 payload 为空，因此验证了真实链路与透传行为，未取得非空
  `feedRecommendListCard` 的实际过滤样本。临时探针已从交付源码移除。
- 帖子内推广的 getter 原值透传及 UI binder 契约通过，但本轮未遇到
  `sponsorForFeedDetail` 样本，不能宣称已实测该类广告折叠。
- 相关推荐的“内容相关”子类型本轮未遇到目标样本；对应目标保持不变并通过
  实机契约校验。不能用其他子类型的成功替代它的实际触发证据。

测试仅重启作用域应用，未重启手机或 LSPosed 守护进程。设备侧调试文件均
置于 `/data/local/tmp/processing/`；原始数据与截图保存在本地忽略目录。
本轮只有本地开发和设备验收，未执行远程写入。

最终候选包：`dist/coolapk-purifier-v2.7.0.apk`。已核对设备安装包与本地
候选包的 SHA-256 一致，包内 manifest 资产与源码一致，临时探针及旧配置
Activity / 写入器均不在交付 DEX 中。

```text
SHA-256: ba39055f6d5847085069333965f9ab5d8fdc7e1eb305a06a151ab9fa053cab17
```
