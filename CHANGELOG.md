# Changelog

SpotFilter 版本记录。最新在上。发布包见 [GitHub Releases](https://github.com/KClgame/SpotFilter/releases)。

## 1.5.4 — 2026-08-29

去掉叠在下面的小号 gizmo 引导字，只保留客户端 `text_display`，高度为原标签下方 0.5 格。

## 1.5.3 — 2026-08-29

引导标签提高一格（与 Fishing Spot 同高）。可见字改到 gizmo 最高层（`ALWAYS_ON_TOP`）最后画，用来穿过切面树叶。

## 1.5.2 — 2026-08-29

修复 1.5.1 一进游戏就黑屏。

原因是对 `TextDisplayRenderer` 的 mixin 在运行时把原版 render state 强转成 mixin 类，任意 `text_display`（含标题/世界标签）一渲染就抛 ClassCastException，画面卡住变黑。已移除该 mixin。

引导仍生成客户端 `text_display`，可见文字继续走透视名牌阶段（切面树叶挡不住）。生成失败只打日志，不再把游戏打崩。

## 1.5.1 — 2026-08-29

恢复客户端引导 `text_display` 实体（该版本 mixin 会导致黑屏，请用 1.5.2）。

## 1.5.0 — 2026-08-28

Auto Pin 规则文件 `config/spotfilter/rules.txt`。

- 启动与 `/sf reload` 时读取
- `[normal]` / `[grotto]` 分段
- `name=` `nick=` `color=` `f1=Strong Hook >= 20` `stock >= High` `cost <= Medium`
- Filter 里改规则并保存会写回该文件
- `/sf rules` 查看状态；界面有 Reload rules.txt

## 1.4.5 — 2026-08-28

- 解析 Fish Magnet `+200%`（以及 10/20/30）
- Fish Chance 使用 Wayfinder Data 的 icon
- 引导改为透视名牌，切面树叶挡不住

## 1.4.4 — 2026-08-28

修复 Compact HUD 词条 icon 只显示 16×16 贴图左上 1/4。

## 1.4.3 — 2026-08-28

- 引导文案用空格代替冒号：`fish spot #1 15m`
- Compact HUD：8px icon 对齐字号，数值与 icon 贴得更紧

## 1.4.2 — 2026-08-28

引导靠近时缩小一档、走远放大。标签带距离。

## 1.4.1 — 2026-08-28

- Between 比较改为 Lower + Upper 两档（F1–F3、Stock、Stability Cost）
- Depleted 默认隐藏；仅当 Stock 筛选打开且比较包含 Depleted 时显示

## 1.4.0 — 2026-08-28

按组从 #1 编号：

- 自定义 Auto Pin nickname 优先（每组 1, 2, 3…）
- 然后 fish / pearl / treasure / spirit spot（各从 #1）
- Normal 按加成归组：Strong/Wise Hook + Fish Magnet = fish；Glimmering/Pearl Magnet = pearl；Greedy/Treasure Magnet = treasure；Lucky/Spirit Magnet = spirit
- XP Magnet、Wayfinder Data 不分组

## 1.3.1 — 2026-08-28

编号在排序之后分配（Cost、家族、词条等），从 #1 起。HUD、列表、引导、`/sf pin` 使用该编号。

## 1.3.0 — 2026-08-28

- 引导放大，名牌随距离变大
- 未命名 Grotto：`fish/pearl/treasure/spirit spot #n`
- 默认 Grotto 排序：Stability Cost，再 Fish→Pearl→Treasure→Spirit，再对应加成（不是扫描顺序）
- Grotto 引导默认用 Chance 色；XP Magnet、Wayfinder Data 不染色

## 1.2.1 — 2026-08-28

Detailed 模式：加成行缩进，并带该词条 icon。

## 1.2.0 — 2026-08-28

Compact / Detailed 布局。

一行一个点：名字（可选）、`#id`（Grotto 用 Cost 色）、最多三个加成数值 + icon、坐标、Stock。

- Filter 按钮：Compact / Detailed
- `/sf hud layout compact|detailed|toggle`

## 1.1.1 — 2026-08-28

- 恢复钉选点下方的客户端引导
- Depleted 自动取消 Auto Pin；**手动 Pin** 保留引导

## 1.1.0 — 2026-08-28

客户端指令 `/sf`（别名 `/spotfilter`）。

总开关、HUD、Kind、逻辑、GUI、清空、列表、钉选、Auto Pin、reload/save。

## 1.0.7 — 2026-08-28

Grotto 按必有的 +100% Fish / Pearl / Treasure / Spirit Chance 分类。HUD icon 取最高额外加成。

## 1.0.6 — 2026-08-28

Normal / Grotto 模式。Stability Cost 颜色档位。两组筛选与 Auto Pin 独立。Auto Pin 可选 nickname。

## 1.0.5 — 2026-08-28

HUD 文本块居中，四周留白均匀。Filter 界面布局保持 1.0.2。

## 1.0.4 — 2026-08-28

撤回 Filter 界面布局试验。HUD 内部居中。

## 1.0.3 — 2026-08-28

UI 留白与工具栏实验（随后撤回）。

## 1.0.2 — 2026-08-28

独立 Stock 筛选、Auto Pin 规则、自定义 `#RRGGBB` 点色、引导随距离缩放。

## 1.0.1 — 2026-08-28

解析 `Stock: Depleted`。变为 Depleted 时自动取消 Auto Pin。

## 1.0.0 — 2026-08-28

Minecraft 26.2 Fabric 纯客户端：扫描 MCC Island 钓鱼点、筛选、钉选 HUD。
