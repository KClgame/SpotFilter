# Changelog

SpotFilter 版本记录。最新在上。发布包见 [GitHub Releases](https://github.com/KClgame/SpotFilter/releases)。

## 1.9.0 — 2026-09-08

- Glowing highlight is an `item_display` icon 5 blocks above the spot (replaces the invisible pig). Icons stay upright and yaw toward the player; type and pair sum pick the item.
- Solo only limits world guides to highlighted spots. HUD still lists every pin.
- Hotkey to switch Glowing / Solo (default Y). Highlight on/off remains U.
- Highlight follows a spot id, not HUD list index.
- Normal and Grotto each keep Enabled/Disabled. Leaving a fishing island live-disables the overlay; coming back restores the saved pair.
- Auto Pin pack On/Off is independent per mode. Pack/rule Up/Down sets priority (top / #1 wins).
- Auto Pin unpins when perks/stock no longer match any rule. Island pool clears as soon as the clock hour changes.
- Keys screen shows each action name like Controls.

## 1.8.7 — 2026-09-07

- Filter overlay button is Enabled/Disabled again (still saved per Normal/Grotto).
- Auto Pin pack On/Off is independent per mode: a pack can be On for Normal and Off for Grotto.

## 1.8.6 — 2026-09-07

- Normal and Grotto each have their own On/Off. Leaving a fishing island live-disables the overlay; coming back restores the saved pair (e.g. Normal On, Grotto Off).
- Auto Pin priority is top-to-bottom (#1 wins). Packs: Up / Down / Edit / Export / Del at equal width. Rules: Up / Down / Del at equal width.

## 1.8.5 — 2026-09-07

- Highlight follows a spot id, not HUD list index, so a pin appearing or disappearing no longer jumps the glow to another spot.
- Auto Pin drops a pin when perks/stock no longer match any rule (hourly refresh at the same block no longer keeps a stale Lucky 30 pin on a Lucky 10).
- Island pool clears as soon as the clock hour changes, not at :01.
- Keys screen shows each action name like Controls.

## 1.8.4 — 2026-09-06

- Glowing item icons stay upright and yaw to face the player (`billboard: vertical`).

## 1.8.3 — 2026-09-06

- Glowing perk icons no longer render as missing-texture cubes. 26.2's item atlas only stitches `textures/item/`, so perk sprites are packed there instead of only under `textures/gui/perk/`.

## 1.8.2 — 2026-09-06

- Glowing highlight is an `item_display` icon 5 blocks above the spot (replaces the invisible pig). Icon follows spot type and pair sum: dual 30 uses axolotl bucket / pristine pearl / mythic treasure / pure spirit; 40–50 and Auto Pin / single-30 / chance spots use the detailed HUD perk icon; leftover fish use a sticky vanilla fish; leftover pearl/treasure/spirit use rough / common–rare / ordinary spirit. Outline color still matches the spot.

## 1.8.1 — 2026-09-06

- Highlight on/off hotkey (default U). Glowing vs Solo is a Filter button after Edit HUD, not in Keys.
- Grotto mode shows Temperate/Tropical/Barren, never island I1–I9.
- Auto Pin grouping updates when Stock or perks change (Plentiful → Very High no longer keeps the old group name).

## 1.8.0 — 2026-09-06

- Mod Menu Config opens Filter (optional).
- HUD highlight: `>` cursor, orange whites, ↑/↓, G lock (multiple). Keys page: record modifier A (right-click None); main key B in Controls. Highlight mode Glowing (invisible glowing pig) or Solo (HUD/guides show only highlighted spots). H clears locked highlights.
- Unpin keeps Auto Pin nicknames and grouping.
- HUD lists by label number. Numbers change only with perk/stock attributes or first-seen order, not distance or coordinates.
- Fish-spot marker red is `#FC5454` when Strong Hook is highest or tied with Wise Hook / Fish Magnet.

## 1.7.8 — 2026-09-02

- Pin guides follow Logical Zoom: that mod scales the world projection matrix and does not change camera FOV, so HUD projection now applies the same XY scale.

## 1.7.7 — 2026-09-01

- Guide HUD scale is 1.0× nearby and 1.35× at range (was 2×–7×, which was huge on screen).
- Island → Grotto (and other place changes) keep the old pool hidden, do not drop nearby old spots, and retag mis-stamped scans so Grotto spots are detected after the sidebar updates.

## 1.7.6 — 2026-09-01

- When a Depleted spot is repaired (stock restored), Auto Pin can pin it again.
- Pin guides are last-layer HUD projections instead of client `text_display` entities, so cutout leaves no longer cover them. Chat and menus still cover the guides.

## 1.7.5 — 2026-09-01

- Disable and island/grotto switch hide HUD, guides, and the Filter list for those spots; the pool is kept. Switching back or re-enabling shows them again. **P** / `/sf clear` still wipes the pool.
- `/sf gui` and `/sf filter` open Filter on the next client tick after chat closes.

## 1.7.4 — 2026-09-01

- Detect fishing island from scoreboard `MCCI: <name>` (I1–I9 and Temperate/Tropical/Barren grottos). Switching island or grotto unpins and clears the spot pool, then Auto Pin only the new island.
- Auto-enable when the world id contains `fishing` or the sidebar is a fishing island. Manual Enabled / `/sf on|off` overrides auto until the world id changes.

## 1.7.3 — 2026-09-01

- Normal Filter row: Stock and Auto Pin now share the same right edge as Pair / F1–F3 (no extra pixels on Auto Pin).
- Ignore labels containing **Event Fishing Spot**; those entities are not scanned.

## 1.7.2 — 2026-09-01

Filter / perk-picker lists treat any non-zero mouse-wheel delta as at least one row, so high-res mice and Disable Hotbar Scrolling no longer stall scrolling. Arrow keys and Page Up/Down also move the Filter list.

## 1.7.1 — 2026-09-01

- Auto Pin only decides on a spot's first detection; later scans do not re-pin. Manual unpin sticks.
- Auto Pin UI switches Normal / Grotto and lists that mode's rules (with counts).
- Default packs are copied only on first mod load; missing files are not repaired later and packs can be deleted.

## 1.7.0 — 2026-08-31

- Auto Pin config packs: builtin **fish / pearl / treasure / spirit / xp_wayfinder** plus **blank**. Default Normal and Grotto rules shipped in the jar (xp_wayfinder Grotto empty). Multiple packs can be enabled in parallel. Duplicate rule names share one numbering group.
- Pearl / Treasure / Spirit Chance can be selected as filters in both Normal and Grotto (fixed presence, no numeric compare). Wayfinder Data remains Normal-only; Fish Chance remains Grotto-only.
- Typing in Auto Pin / pack name fields no longer treats **O** as back to Filter.
- Filter top-row buttons use equal widths.
- Grotto pin / world-guide color: Auto Pin `#RRGGBB` > Stability Cost color > perk family color.

## 1.6.2 — 2026-08-30

Perk picker is mode-exclusive: Normal can use Wayfinder Data (not Grotto 100% Chances); Grotto can use Fish Chance (not Wayfinder Data or other 100% Chances).

## 1.6.1 — 2026-08-30

Filter 顶栏新增 Kick Depleted（在 Enabled 之前）。On 时 Depleted 会取消钉选并移出附近点池；Off 时保留。顶栏按钮均有悬浮说明。

## 1.6.0 — 2026-08-30

- 替换 Treasure Magnet icon
- 新增 Pair 筛选：按点种类把两条加成相加（鱼=Strong+Wise，珍珠/宝藏/魂=对应 Hook+Magnet），10%–60%，比较符与 Between；Normal / Grotto 分开
- Normal 刷新改为整点+1 分钟，或 2 秒内 ≥3 个点同时变化；同一 xz 不同 y 只留新点
- Grotto 刷新改为聊天检测 `Your Grotto has become unstable`

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
