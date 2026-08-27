# Issue #5 — P5 Generic Server-Driven Card Model（E2 交付物）

分析时间：2026-08-27（第四会话）
材料：恢复业务 DEX（`.tmp_audit/regions_clean/coolapk_business_restored.dex`，11,278 类）、
`main_useDDI.dex`（模型/网络层，字符串池可读）、既有 12 份 indexV8 成功响应样本（前轮）、
init 响应 textCard/configCard 样本（D1 会话）。
工具：androguard（业务 DEX 全量反汇编级分析）+ 原始字符串池上下文分析。
未触发风控、未修改任何东西。

---

## 1. 模型与分发结构（E3 静态，恢复 DEX 内直接指令证据）

```text
服务端 JSON row
  → 解析为 Lcom/coolapk/market/model/Entity;（接口：getEntityType()/getEntityTemplate()/getTitle()/getUrl()…）
  → 具体类 EntityCard（getEntities()/getTitle()/getUrl() + Builder）、ConfigPage（Builder: entityType/title/url）等
  → 各 Fragment/ViewHolder 对 entityTemplate 做显式 when-chain 匹配（无全局反射注册表）
  → 命中模板的 ViewHolder 用 DataBinding 渲染
```

关键实证：
- `DataListFragment.ٳ(List,Z)List`：遍历列表实体，`getEntityTemplate()` 与
  `configCard / flexList / iconTabLinkGridCard / pageTitle / subTabLinkCard / verticalColumnsFullPageCard` 等逐一比较；
  **template=="configCard" 的实体经 `ya3.ޣ(Entity)→JSONObject` 转成配置 JSON 应用于页面**（entityType/title/url 进 ConfigPage.Builder）。
  即：**服务端可通过任意列表响应注入配置卡片，客户端无专用代码即消费**。
- `DataListFragment.ཌ(EntityCard)`：把实体的 entityType/title/url 组装为 ConfigPage.Builder。
- **通用 `card` 模板 ViewHolder（`viewholder/ޕ`，绑定 EntityCard）**：
  - `EntityCard.getTitle()` → 绑定类 `yl7.Ϳ(String)` → **标题 TextView**（任意服务端文本）；
  - `EntityCard.getUrl()` → "see_more" HolderItem（跳转行）；
  - `EntityCard.getEntities()` → 嵌套 RecyclerView 子列表（实体可递归下发）；
  - `ya3.ޣ(Entity)` 的 JSON 里读取 `cardContainerPadding/Left/Top/Right/Bottom` 等键 →
    **服务端直接控制卡片样式**（无需客户端发版）。
- 模板名常量池（业务 DEX 中调用 getEntityTemplate 的方法内字符串）：
  `card、configCard、flexList、pageTitle、headCard、iconScrollCard、iconTabLinkGridCard、subTabLinkCard、
  verticalColumnsFullPageCard、sortSelectCard、cardDividerBottom、cardFixedToTop、feed、rating、
  imageCard、imageCarouselCard、imageScaleCard、iconLongTitleGridCard、readMoreScrollCard、see_more、
  searchHotListCard、hotSearch、sceneSearch、apkEvent、productSelect、feedCover、feedMiniCover、…`
- 模型/网络层 DEX（main_useDDI）字符串池存在 **`textCard`、`textCarouselCard`、`textLinkCard`、`textLinkListCard`**
  文本卡片模板族，以及 `configCardExtraData`、本地 `config_card_app/client/digit/discovery.json`。
- 调试基建佐证统一渲染框架：`CoolapkCardView`（所有卡片容器）内建 `show_card_info` 调试浮层，
  显示 `ViewHolderSimpleName => entityType -> entityTemplate`——即 (entityType, entityTemplate) 就是渲染分发的全部键。

## 2. 正常样本验证（前轮既有证据）

- 12 份 `/v6/main/indexV8` 成功响应中出现过 entityType=`card` 的行（普通卡片）——通用 card 模板在生产 Feed 流中真实使用；
- 今日 `/v6/main/init` 响应含 `textCard`/`configCard` 样本（D1 会话记录）；
- 未在任何响应中观察到风险专用 entity/template（阴性批次，不构成反证）。

## 3. 分类结论

> **S2：需要已有通用 template ID，但无需 risk 专用客户端代码。**

判定依据：
1. 存在多个**通用文本承载模板**（`card`/EntityCard 直接渲染 title 文本；textCard 族；pageTitle），
   服务端可下发任意文本内容 + 任意嵌套实体 + 服务端样式（padding 等）；
2. 分发键只有 (entityType, entityTemplate)，无第三类客户端 flag 参与 → 不满足 C1 的"客户端必须收到 flag 主动构建"；
3. 不是 S1：字符串文本不能脱离模板体系直接渲染——必须命中某个已知模板名（per-fragment when-chain 显式匹配）；
4. 全局已确认（前轮阴性）：客户端无风险专用文案/template/builder。

**含义（归因层）**：若 Issue #5 的风险提示行真实存在，其最低成本实现是服务端在任意列表响应中插入
`card`/`textCard`/`pageTitle` 行（复用通用渲染），客户端无需任何配合代码。这与服务端策略/rollout 模型相容，
也与"风险行来源未在客户端找到"的前轮结论一致。

## 4. 边界（诚实声明）

- Entity/EntityCard 的 JSON→对象解析函数本体位于未完全恢复的模型 dex（androguard 因尾部截断无法整 dex 解析），
  本轮证据是接口调用侧 + 字符串池，未逐字段恢复解析器（E2）；
- textCard 族的确切 ViewHolder 未逐一展开（其存在性与模板名已确认，渲染字段结构与 card 同构，E2）；
- 不声称"服务端一定使用该路径展示风险文案"——本报告只回答"是否足以显示"。
