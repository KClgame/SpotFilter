# SpotFilter

**v1.3.0** · Minecraft **26.2** · Fabric · 纯客户端

MCC Island 钓鱼点扫描、筛选与坐标 HUD。走近带 `Fishing Spot` 标签的 Text Display 即可收录；按词条过滤、钉选坐标、世界透视引导。

> 只读客户端已收到的文本。钓鱼等级不足、服务器未下发词条时无法凭空还原。

---

## 依赖

| 组件 | 版本 |
| --- | --- |
| Minecraft | 26.2 |
| Java | 25+ |
| Fabric Loader | 0.19.3+ |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.158.0+26.2（或同游戏版本） |
| [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) | 1.13.13+kotlin.2.4.10 |

服务端无需安装。建议同时启用 **MCCI 材质包**，词条 icon 才能按官方字体显示。

---

## 安装

1. 安装 Fabric Loader（26.2）与上述依赖。
2. 将 `spotfilter-1.3.0.jar` 放入 `.minecraft/mods/`。
3. 启动游戏。控件里应出现 **SpotFilter** 分类。

构建：

```bash
./gradlew build
```

产物：`build/libs/spotfilter-1.3.0.jar`

---

## 按键（可在控件中改）

| 按键 | 作用 |
| --- | --- |
| **O** | 打开 / 关闭 Filter |
| **P** | 清空当前钓鱼点池（布局、筛选、Enabled 状态保留） |
| **L** | 开关坐标 HUD（Filter 打开时仍可预览） |
| Filter 内 **E** | 进入 Edit HUD |

清空后 HUD 显示灰色 `(No current fishing spot)`。

---

## 指令 `/sf`

客户端指令（单机/联机都可用）。`/spotfilter` 是同一套的别名。聊天里输入 `/sf` 可看帮助和当前状态。

| 指令 | 作用 |
| --- | --- |
| `/sf` / `/sf help` / `/sf status` | 帮助与当前 Enabled / Kind / HUD / 点数 |
| `/sf on` `/sf off` `/sf toggle` | 总开关（同 Filter 里 Enabled） |
| `/sf hud` `[on\|off\|toggle]` | 坐标 HUD |
| `/sf hud layout <compact\|detailed>` | HUD 单行 Compact / 多行 Detailed |
| `/sf hud scale <0.5–3>` | HUD 放大 |
| `/sf hud opacity <0–90>` | HUD 背景透明度 |
| `/sf hud pos <x> <y>` | HUD 屏幕位置 |
| `/sf kind <normal\|grotto\|toggle>` | 切换 Normal / Grotto |
| `/sf logic <and\|or\|toggle>` | 当前组 AND / OR |
| `/sf gui` | 打开 Filter 界面 |
| `/sf clear` | 清空点池（同 **P**） |
| `/sf list` `[pinned]` | 列出当前组匹配点 / 已钉选 |
| `/sf pin <id\|all>` `/sf unpin <id\|all>` | 钉选 / 取消 |
| `/sf autopin apply` | 按当前组规则重跑 Auto Pin |
| `/sf reload` `/sf save` | 重读 / 写出 `config/spotfilter.json` |

---

## Filter 界面

**O** 打开。世界不暂停，扫描继续。

顶栏：

- **Normal / Grotto** — 切换点类型。两组点、筛选和 Auto Pin **互不共用**；切走后另一组从列表、HUD、引导中隐藏（钉选状态保留）。
- **Mode: AND / OR** — 多槽组合。AND 必须同时满足；OR 满足任一即可。空槽忽略。
- **Detailed / Compact** — HUD 与列表的文本模式。Compact 每个点一行：名字（可空）、编号（Grotto 用 Stability Cost 色）、最多三个加成（数值+icon）、坐标、Stock。
- **Edit HUD** — 拖动位置；滚轮放大倍率（0.5×–3.0×）；**Shift+滚轮** 背景透明度。
- **Clear spots** — 同 **P**。
- **Enabled / Disabled** — 总开关。Disabled 时 HUD 与引导标记消失、不再播新点音效；仍可打开 Filter，扫描与筛选照常。再开 Enabled 会恢复 HUD 和已 Pin 标记。

### Stock 筛选

主界面单独一栏 **Stock**，不占用 F1–F3。可开关，并设置 `>` / `<` / `=` 与档位（Plentiful → … → Depleted）。与词条筛选同时生效（必须先过 Stock）。

### Grotto 与 Stability Cost

标签含 **Stability Cost** 的点视为 Grotto。读取 Cost 后面整块 `(数字)-(数字)%` 的颜色：

| 档位 | 颜色 | 排序 |
| --- | --- | --- |
| Low（最好） | `#65FEFE` | 优先 |
| Medium | `#55FE56` | |
| High（最差） | `#FEFE55` | 靠后 |

Grotto 模式下主界面和 Auto Pin 会出现 **Cost** 筛选（Low / Medium / High）。

每个 Grotto 点必定有且仅有一条 **+100%** 的 Fish / Pearl / Treasure / Spirit Chance，并按这条 Chance 分类。主 Icon 与引导颜色取 **最高级加成**（数值高者优先，同值 Special > Magnet > Hook）；没有其他加成则用该 Chance 的 Icon 和颜色。自定义 hex 仍可覆盖。

### Auto Pin

**Auto Pin** 里可建多条规则：各含 F1–F3 词条、独立 Stock、AND/OR（Grotto 另有 Cost）。命中的点会自动钉上。Normal 与 Grotto 各有一套规则。

- 颜色默认：Normal 按主词条家族；Grotto 按最高级加成（没有则用 Chance 色）
- 规则里填写 `#RRGGBB` 则该规则钉上的点统一用这个色
- **Spot nickname**（可选）：命中后引导与 HUD 显示 `名字 #组内编号`（同一 nickname 从 1 递增），而不是全局 `#n`
- 不再命中规则的自动 Pin 会撤掉；手动 Pin 的点不受影响

### 三个筛选槽 F1 / F2 / F3

点击进入该槽配置（不要连点切换词条）：

1. **Perk** → 搜索页，输入关键字点选词条，或选 **None**。
2. Hook / Magnet 才有：
   - **Compare**：`>` / `<` / `=`
   - **Value**：`+10%` / `+20%` / `+30%`（Wayfinder Data 为 `+10`）
3. 固定加成（Elusive Chance、Wayfinder Data、Fish / Pearl / Treasure / Spirit Chance）**没有**数值比较，只判断有无该词条。Grotto 用 F1–F3 选 Fish/Pearl/Treasure/Spirit Chance 即可按类型筛选。
4. **Sort**：High → Low 或 Low → High。
5. **Clear this filter** 清空本槽。

总排序：有 F1–F3 时先按槽。默认 **不按发现顺序**。Grotto：Stability Cost（Low 最好）→ Fish / Pearl / Treasure / Spirit → 组内按加成（同系 Magnet/Hook 数值高者优先）→ Stock → 距离 → 坐标。Normal：Stock → 距离 → 坐标。

### 列表与钉选

匹配的点显示编号、坐标、Stock、词条。点击一行 **Pin**：

- 加入屏幕坐标 HUD
- 在原标签 **下方一格** 生成放大的仅客户端 `text_display`（透视名牌同时绘制，穿树叶）：未起 nickname 时 Grotto 写 `fish/pearl/treasure/spirit spot #n`，普通点写 `fishing spot #n`；有 nickname 则为 `名字 #组内编号`  
  Grotto 点色默认用 Chance 色（XP Magnet / Wayfinder Data 不参与染色）；自定义 hex 仍优先。

再点一次取消 Pin。

---

## 扫描与生命周期

- 每 tick 扫描渲染距离内的 `text_display`，标题含 `Fishing Spot` 则入池。
- 同一取整坐标更新 Stock/词条，不换号、不重复音效。
- 新编号播放一次经验球音效（Enabled 时）。
- 词条与 Stock 的 **颜色直接读取标签 Component**，与游戏里看到的一致。
- 走近（约 48 格且区块已加载）标签消失，或 Stock 变成 Depleted：自动 Pin 会撤掉。**手动 Pin** 的点会留下（引导还在）。走远卸载 **不会** 删。
- 本机本地时间整点（1:00、2:00…）自动清空池子，效果同 **P**。
- 一个点最多解析 3 条加成。仅 Strong Hook 与 Wise Hook 可同时出现；Magnet 互斥；Chance / Data 互斥。
- 主 Icon：Chance/Data > Hook/Magnet，再比数值；Strong 与 Wise 同为 +30% 时优先 Strong。

### 可识别词条

**Hook** `+10% / +20% / +30%`  
Strong · Wise · Glimmering · Greedy · Lucky

**Magnet** `+10% / +20% / +30%`  
XP · Fish · Pearl · Treasure · Spirit

**固定**  
`+5% Elusive Chance` · `+10 Wayfinder Data` · `+5% Pearl Chance` · `+1% Treasure Chance` · `+2% Spirit Chance`

Grotto 另有：`+100% Fish Chance` · `+100% Pearl Chance` · `+100% Treasure Chance` · `+100% Spirit Chance`

**Stock**  
Plentiful · Very High · High · Medium · Low

---

## 配置

`config/spotfilter.json`（首次打开 Filter 或改 HUD 后生成）

- HUD 位置、放大倍率、背景透明度、L 开关状态
- Enabled
- AND/OR 与三个筛选槽

钓鱼点池 **不** 写入磁盘，重进游戏 / 整点 / **P** 后需重新扫描。

---

## 单机测试数据包

仓库 `datapacks/spotfilter-test`（也可复制到存档 `datapacks/`）。

```
/reload
/function spotfilter:spawn
/function spotfilter:spawn_grotto
/function spotfilter:clear
```

`spawn` 在脚下刷 **50** 个普通测试点（10×5，间隔 3 格）。`spawn_grotto` 刷 **9** 个 Grotto 点（Stability Cost + 100% Fish/Pearl/Treasure/Spirit Chance）。请启用 MCCI 材质包。`clear` 只删 `spotfilter_test`，不影响 Pin 引导。

---

## 常见问题

**没有词条，只有 Fishing Spot / Stock？**  
MCC Island 约钓鱼 6 级才下发加成。Mod 只能解析客户端已经收到的文本。

**引导标记隔墙看不见？**  
标记开了 `see_through`。若仍被挡住，确认该点已 Pin，且 Filter 里是 **Enabled**。

**Disabled 和 L 有什么区别？**  
**L** 只藏 HUD。**Disabled** 藏 HUD、拆引导、关音效，扫描与 Filter 仍可用。

---

## 许可

[CC0 1.0](LICENSE)

源码：https://github.com/KClgame/SpotFilter
