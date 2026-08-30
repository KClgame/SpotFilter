# SpotFilter

**v1.6.2** · Minecraft **26.2** · Fabric · 纯客户端

MCC Island 钓鱼点扫描、筛选、坐标 HUD 与世界透视引导。走近标题含 `Fishing Spot` 的 Text Display 即可收录。

Client-only Fabric mod that scans MCC Island fishing-spot labels, filters and sorts them, pins coordinates to a HUD, and draws see-through world guides.

> 只读客户端已经收到的文本。钓鱼等级不足、服务器未下发词条时无法凭空还原。

- 发布：https://github.com/KClgame/SpotFilter/releases
- 源码：https://github.com/KClgame/SpotFilter

---

## 目录

- [依赖](#依赖)
- [安装](#安装)
- [按键](#按键)
- [指令 `/sf`](#指令-sf)
- [Filter 界面](#filter-界面)
- [Normal 与 Grotto](#normal-与-grotto)
- [筛选](#筛选)
- [分组、编号与颜色](#分组编号与颜色)
- [Auto Pin 与 `rules.txt`](#auto-pin-与-rulestxt)
- [HUD](#hud)
- [世界引导](#世界引导)
- [扫描与生命周期](#扫描与生命周期)
- [可识别词条](#可识别词条)
- [配置](#配置)
- [单机测试数据包](#单机测试数据包)
- [常见问题](#常见问题)
- [构建](#构建)
- [许可](#许可)

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
2. 从 [Releases](https://github.com/KClgame/SpotFilter/releases) 下载 `spotfilter-1.6.2.jar`，放入 `.minecraft/mods/`。
3. 启动游戏。控件里应出现 **SpotFilter** 分类。

---

## 按键

默认可在「控件 → SpotFilter」里改。

| 按键 | 作用 |
| --- | --- |
| **O** | 打开 / 关闭 Filter |
| **P** | 清空当前钓鱼点池（布局、筛选、Enabled 状态保留） |
| **L** | 开关坐标 HUD（Filter 打开时仍可预览） |

清空后 HUD 显示灰色 `(No current fishing spot)`。

---

## 指令 `/sf`

客户端指令，单机和联机都可用。`/spotfilter` 是同一套别名。聊天输入 `/sf` 可看帮助和当前状态。

| 指令 | 作用 |
| --- | --- |
| `/sf` `/sf help` `/sf status` | 帮助与当前 Enabled / Kind / HUD / 点数 |
| `/sf on` `/sf off` `/sf toggle` | 总开关（同 Filter 里 Enabled） |
| `/sf hud` `[on\|off\|toggle]` | 坐标 HUD 显示 |
| `/sf hud layout <compact\|detailed\|toggle>` | HUD 单行 Compact / 多行 Detailed |
| `/sf hud scale <0.5–3>` | HUD 放大 |
| `/sf hud opacity <0–90>` | HUD 背景透明度 |
| `/sf hud pos <x> <y>` | HUD 屏幕位置 |
| `/sf kind <normal\|grotto\|toggle>` | 切换 Normal / Grotto |
| `/sf logic <and\|or\|toggle>` | 当前组 AND / OR |
| `/sf gui` `/sf filter` | 打开 Filter 界面 |
| `/sf clear` `/sf refresh` | 清空点池（同 **P**） |
| `/sf list` `[pinned\|all]` | 列出当前组匹配点 / 已钉选 |
| `/sf pin <id\|all>` `/sf unpin <id\|all>` | 按组内编号钉选 / 取消 |
| `/sf autopin apply` | 按当前组规则重跑 Auto Pin |
| `/sf reload` `/sf save` | 重读 / 写出 `config/spotfilter.json`，并读写 `config/spotfilter/rules.txt` |
| `/sf rules` `/sf rules reload` | 查看 / 重载 Auto Pin 规则文件 |

`/sf pin` 的 `id` 是当前组排序后的 **#编号**，不是扫描顺序。

---

## Filter 界面

**O** 打开。世界不暂停，扫描继续。

顶栏（鼠标悬停可看说明）：

- **Kick Depleted** — 与 Normal/Grotto、Mode、Enabled 同一行（Enabled 左侧）。On：Stock 变成 Depleted 时取消钉选并踢出附近点池（含手动 Pin）。Off：Depleted 仍可留在钉选里。
- **Enabled / Disabled** — 总开关。Disabled 时 HUD 与引导消失、不再播新点音效；仍可打开 Filter，扫描与筛选照常。
- **Normal / Grotto** — 切换点类型。两组点、筛选和 Auto Pin **互不共用**。切走后另一组从列表、HUD、引导中隐藏（钉选状态仍保留，切回来还能看见）。
- **Mode: AND / OR** — 多槽组合。AND 必须同时满足；OR 满足任一即可。空槽忽略。
- **Detailed / Compact** — HUD 与列表的文本模式，见 [HUD](#hud)。
- **Edit HUD** — 拖动位置；滚轮放大（0.5×–3.0×）；**Shift+滚轮** 改背景透明度。
- **Clear** — 同 **P**，立刻清空点池。

列表：匹配的点显示编号、坐标、Stock、词条。点击一行 **Pin** / **PINNED** 切换钉选。

---

## Normal 与 Grotto

标签含 **Stability Cost** 的点视为 Grotto，其余为 Normal。

Stability Cost 读取 Cost 后面整块 `(数字)-(数字)%` 的颜色：

| 档位 | 颜色 | 含义 | 排序 |
| --- | --- | --- | --- |
| Low | `#65FEFE` | 最好 | 优先 |
| Medium | `#55FE56` | | |
| High | `#FEFE55` | 最差 | 靠后 |

Grotto 模式下主界面和 Auto Pin 会出现 **Cost** 筛选（Low / Medium / High）。

每个 Grotto 点必定有且仅有一条 **+100%** 的 Fish / Pearl / Treasure / Spirit Chance，并按这条 Chance **分类**。HUD 主 Icon 取 **最高级额外加成**（数值高者优先，同值 Special > Magnet > Hook）；没有其他加成则用该 Chance 的 Icon。**XP Magnet、Wayfinder Data 不参与主 Icon 和点色。**

---

## 筛选

### Stock

主界面单独一栏 **Stock**，不占用 F1–F3。可开关，比较符：`>` `>=` `<` `<=` `=` `Between`（Lower + Upper 两个档位）。

**Depleted 默认不出现在列表里。** 只有把 Stock 打开，且比较条件包含 Depleted（例如 `= Depleted`，或 Between 覆盖到 Depleted）才会显示。这条优先于 F1–F3。

档位从高到低：Plentiful → Very High → High → Medium → Low → Depleted。

### Pair（perk1 + perk2 总和）

主界面 **Pair** 栏，不占用 F1–F3。按点种类把两条可变加成相加（缺的那条算 +0%），范围 **+10%～+60%**，比较符与 Stock 相同：`>` `>=` `<` `<=` `=` `Between`。

| 点种类 | 相加的两条 |
| --- | --- |
| fish | Strong Hook + Wise Hook |
| pearl | Glimmering Hook + Pearl Magnet |
| treasure | Greedy Hook + Treasure Magnet |
| spirit | Lucky Hook + Spirit Magnet |

Normal 与 Grotto **各有一套** Pair 设置。Grotto 仍按上面配对，100% Chance 不计入总和。Auto Pin 规则里也可设 `pair >= 40`。

### 三个筛选槽 F1 / F2 / F3

点击进入该槽配置（不要连点切换词条）：

1. **Perk** → 搜索页，输入关键字点选词条，或选 **None**。
2. Hook / Magnet 才有数值比较：
   - **Compare**：`>` `>=` `<` `<=` `=` `Between`
   - **Value**：`+10%` / `+20%` / `+30%`（Wayfinder Data 为 `+10`）。Fish Magnet 另有 **`+200%`**。**Between** 时为 Lower / Upper 两个值。
3. 固定加成没有数值比较，只判断有无该词条。**Normal 只能选 Wayfinder Data**（不含 100% Chance）；**Grotto 只能选 Fish Chance**（不含 Wayfinder Data 和其他 100% Chance）。二者互斥。Hook / Magnet / Elusive Chance 两边都能选。
4. **Sort**：High → Low 或 Low → High。多槽同时启用时，按 F1 → F2 → F3 依次比较。
5. **Clear this filter** 清空本槽。

---

## 分组、编号与颜色

列表、HUD、引导、`/sf pin` 使用 **组内编号**，每组从 **#1** 起。**不是**扫描先后。

组顺序：

1. Auto Pin **nickname** 自定义组（规则从上到下）
2. `fish spot` → `pearl spot` → `treasure spot` → `spirit spot`
3. 无法归组的点显示为 `fishing spot`

**Grotto** 用 +100% Chance 归组。  
**Normal** 用加成归组：

| 组 | 词条 |
| --- | --- |
| fish | Strong Hook、Wise Hook、Fish Magnet、Fish Chance、Elusive Chance |
| pearl | Glimmering Hook、Pearl Magnet、Pearl Chance |
| treasure | Greedy Hook、Treasure Magnet、Treasure Chance |
| spirit | Lucky Hook、Spirit Magnet、Spirit Chance |

**XP Magnet、Wayfinder Data 不分组。** 同一点有多条可分组加成时，取数值最高者；同值优先 Magnet。

组内再按：当前 F 槽 Sort → Grotto 的 Cost（Low 优先）→ 加成强度 → Stock → 距离。

文案：`组名 #组内编号`。有 nickname 则为 `名字 #组内编号`。

### 点色（HUD 编号 / 世界引导）

优先级：

1. Auto Pin 规则里的 `#RRGGBB`
2. **Grotto**：Fish / Pearl / Treasure / Spirit Chance 本身的颜色（XP Magnet、Wayfinder Data 不染色）
3. **Normal**：主词条家族色

家族默认色：

| 家族 | 颜色 | 词条 |
| --- | --- | --- |
| Strong | `#FC5454` | Strong Hook、XP Magnet、Elusive Chance |
| Wise | `#2199F0` | Wise Hook、Fish Magnet、Wayfinder Data、Fish Chance |
| Pearl | `#8636FF` | Glimmering Hook、Pearl Magnet、Pearl Chance |
| Treasure | `#FC7D3F` | Greedy Hook、Treasure Magnet、Treasure Chance |
| Spirit | `#23C525` | Lucky Hook、Spirit Magnet、Spirit Chance |

Compact 模式下 Grotto 的名字和 `#n` 另用 **Stability Cost 色** 显示，方便扫 Cost。

---

## Auto Pin 与 `rules.txt`

**Auto Pin** 里可建多条规则：各含 F1–F3、独立 Stock、AND/OR（Grotto 另有 Cost）。命中的点会自动钉上。Normal 与 Grotto 各有一套。

- **Spot nickname**（可选）：命中后引导与 HUD 显示 `名字 #组内编号`（同一 nickname 从 1 递增）
- 规则里填写 `#RRGGBB` 则该规则钉上的点统一用这个色
- 不再命中规则的自动 Pin 会撤掉；**手动 Pin** 不受影响
- Stock 变成 **Depleted** 时，自动 Pin 会撤掉；手动 Pin 会留下（引导还在，Stock 标成 Depleted）

规则的**源文件**是 UTF-8 文本：

```
config/spotfilter/rules.txt
```

进游戏、`/sf reload`、`/sf rules reload`、Auto Pin 界面的 **Reload rules.txt** 都会读取。Filter 里改规则并保存会写回这个文件。首次启动若不存在，会写出带注释的模板。

```
[normal]
name=Big Fish
nick=大鱼喵喵
color=#2199F0
mode=AND
enabled=true
f1=Strong Hook >= 20
f2=Wise Hook >= 20
stock >= High

[grotto]
name=Cheap Pearl
nick=珍珠
f1=Glimmering Hook >= 20
cost <= Medium
```

要点：

- `[normal]`（或 `[island]`）与 `[grotto]` 分段。空行结束一条规则。`#` 或 `//` 开头为注释。
- `name=` 开始一条新规则。`nick=` / `nickname=`、`color=` / `hex=`、`mode=AND|OR`、`enabled=true|false`。
- `f1` / `f2` / `f3` 为词条。也可直接写一行词条条件，自动填入空槽。
- Compare：`>` `>=` `<` `<=` `=` `between`（或 `..`）。Fish Magnet 支持 `= 200`。
- 固定词条只写名字，例如 `f3=Pearl Chance`。
- `stock >= High`、`stock between Medium Plentiful`。
- Grotto：`cost <= Medium`（也可用 `stability=`）。

词条别名示例：`strong`、`wise`、`fishmagnet`、`wayfinder`、`fishchance`。完整显示名（`Strong Hook`）始终可用。

---

## HUD

钉选的点画在屏幕上。文本块整体居中，四周留白均匀。无钉选时显示灰色 `(No current fishing spot)`。

| 模式 | 内容 |
| --- | --- |
| **Compact** | 每个点一行：组名、`#n`、最多三个加成（数值 + **8×8** icon）、坐标、Stock。Grotto 的 100% Chance 不占这三格（有其他加成就显示加成，否则才显示 Chance）。 |
| **Detailed** | 标题行 + 每条加成单独一行，左侧缩进并带该词条 icon。Grotto 标题旁可带 Cost 区间。 |

**L** 只藏 HUD。打开 Filter 时仍可预览。Disabled 则 HUD 与引导一起关掉。

Fish Chance 的 HUD icon 与 Wayfinder Data 相同（游戏内 MCCI 字体也接近）。

---

## 世界引导

已 Pin 的点在原标签 **下方 0.5 格** 生成仅客户端 `text_display` 实体（F3 实体列表里能看到，标签 `spotfilter_marker`）。不再叠一层较小的 gizmo 字。

文案：`fish spot #1 15m`（有 nickname 则为 `珍珠 #2 15m`）。距离与名字之间是空格，没有冒号。

缩放：约 8 格内为正常大小，走远逐渐放大，约 56 格达到最大。

---

## 扫描与生命周期

- 每 tick 扫描渲染距离内的 `text_display`，标题含 `Fishing Spot` 则入池。
- 同一取整坐标更新 Stock/词条，不换号、不重复音效。
- 新点播放一次经验球音效（Enabled 时）。
- 词条与 Stock 的 **颜色直接读取标签 Component**，与游戏里看到的一致。
- 走近（约 48 格且区块已加载）标签消失：自动 Pin 会删点；**手动 Pin** 留下并标 Depleted。
- 走远卸载 **不会** 删。
- **Normal 刷新**（只清空岛上点，Grotto 保留）：
  - 本机本地时间 **整点后 1 分钟**（1:01、2:01…）
  - 或约 2 秒内至少 3 个 Normal 点内容变化 / 新出现（同一 xz 不同 y 会替换旧记录，避免双开）
- **Grotto 刷新**：聊天出现 `Your Grotto has become unstable` 时只清空 Grotto 点。
- 一个点最多解析 3 条加成。Grotto 会保留 1 条 100% Chance + 最多 2 条其他加成。
- 主 Icon：Chance/Data > Hook/Magnet，再比数值；Strong 与 Wise 同为 +30% 时优先 Strong。Grotto 见上文「最高级额外加成」。

钓鱼点池 **不** 写入磁盘。重进游戏 / 整点 / **P** 后需重新扫描。

---

## 可识别词条

**Hook** `+10%` / `+20%` / `+30%`  
Strong · Wise · Glimmering · Greedy · Lucky

**Magnet** `+10%` / `+20%` / `+30%`（Fish Magnet 另有 Grotto 特殊 `+200%`）  
XP · Fish · Pearl · Treasure · Spirit

**固定**  
`+5% Elusive Chance` · `+10 Wayfinder Data` · `+5% Pearl Chance` · `+1% Treasure Chance` · `+2% Spirit Chance`

Grotto 另有：`+100% Fish Chance` · `+100% Pearl Chance` · `+100% Treasure Chance` · `+100% Spirit Chance`

**Stock**  
Plentiful · Very High · High · Medium · Low · Depleted

---

## 配置

| 文件 | 内容 |
| --- | --- |
| `config/spotfilter.json` | HUD 位置/缩放/透明度/布局、总开关、当前 Kind、Filter 槽 |
| `config/spotfilter/rules.txt` | Auto Pin 规则（源文件，UTF-8） |

改 JSON 后用 `/sf reload`。改 `rules.txt` 后用 `/sf reload` 或 `/sf rules reload`。在 Filter 里保存会同时写回这两个文件。

---

## 单机测试数据包

仓库 `datapacks/spotfilter-test`（也可复制到存档 `datapacks/`）。作弊开启后：

```
/reload
/function spotfilter:spawn
/function spotfilter:spawn_grotto
/function spotfilter:clear
```

| 函数 | 作用 |
| --- | --- |
| `spotfilter:spawn` | 脚下 **50** 个普通测试点（10×5，间隔 3 格） |
| `spotfilter:spawn_grotto` | **9** 个 Grotto 点（Stability Cost + 100% Fish/Pearl/Treasure/Spirit Chance） |
| `spotfilter:clear` | 只删带 `spotfilter_test` 标签的测试实体，不影响 Pin 引导 |

请启用 MCCI 材质包。详见 [`datapacks/spotfilter-test/README.txt`](datapacks/spotfilter-test/README.txt)。

---

## 常见问题

**没有词条，只有 Fishing Spot / Stock？**  
MCC Island 约钓鱼 6 级才下发加成。Mod 只能解析客户端已经收到的文本。

**引导被树叶挡住？**  
引导实体仍在，但字走透视名牌阶段，切面树叶应能透过。若完全看不见：确认该点已 Pin，Filter 为 **Enabled**，且当前 Kind（Normal/Grotto）与该点一致。

**Disabled 和 L 有什么区别？**  
**L** 只藏 HUD。**Disabled** 藏 HUD、拆引导、关音效，扫描与 Filter 仍可用。

**Depleted 的点怎么还钉着？**  
自动 Pin 会撤；你自己点 Pin 的会留下。列表默认隐藏 Depleted，要用 Stock 筛选显式包含它。

**`+200%` Fish Magnet 显示成 `+100%`？**  
1.4.5 起按 10/20/30/200 解析。请确认 jar 为 **1.5.0**。

**点池自己空了？**  
Normal 在整点+1 分钟或一波多点同时变时清空；Grotto 只在聊天出现 `Your Grotto has become unstable` 时清空。筛选和 Auto Pin 规则保留。**P** 仍清空全部。

**服务端要装吗？**  
不用。纯客户端。

---

## 构建

```bash
./gradlew build
```

产物：`build/libs/spotfilter-1.6.2.jar`

需要 JDK 25。变更记录见 [CHANGELOG.md](CHANGELOG.md)。

---

## 许可

[CC0 1.0](LICENSE)

作者：[KClgame](https://github.com/KClgame)  
源码：https://github.com/KClgame/SpotFilter
