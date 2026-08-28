# SpotFilter

**v1.0.6** · Minecraft **26.2** · Fabric · 纯客户端

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
2. 将 `spotfilter-1.0.6.jar` 放入 `.minecraft/mods/`。
3. 启动游戏。控件里应出现 **SpotFilter** 分类。

构建：

```bash
./gradlew build
```

产物：`build/libs/spotfilter-1.0.6.jar`

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

## Filter 界面

**O** 打开。世界不暂停，扫描继续。

顶栏：

- **Normal / Grotto** — 切换点类型。两组点、筛选和 Auto Pin **互不共用**；切走后另一组从列表、HUD、引导中隐藏（钉选状态保留）。
- **Mode: AND / OR** — 多槽组合。AND 必须同时满足；OR 满足任一即可。空槽忽略。
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

Grotto 模式下主界面和 Auto Pin 会出现 **Cost** 筛选（Low / Medium / High）。引导标记默认用该 Cost 颜色；自定义 hex 仍可覆盖。

### Auto Pin

**Auto Pin** 里可建多条规则：各含 F1–F3 词条、独立 Stock、AND/OR（Grotto 另有 Cost）。命中的点会自动钉上。Normal 与 Grotto 各有一套规则。

- 颜色默认：Normal 按主词条家族；Grotto 按 Stability Cost
- 规则里填写 `#RRGGBB` 则该规则钉上的点统一用这个色
- **Spot nickname**（可选）：命中后引导与 HUD 显示 `名字 #组内编号`（同一 nickname 从 1 递增），而不是全局 `#n`
- 不再命中规则的自动 Pin 会撤掉；手动 Pin 的点不受影响

### 三个筛选槽 F1 / F2 / F3

点击进入该槽配置（不要连点切换词条）：

1. **Perk** → 搜索页，输入关键字点选词条，或选 **None**。
2. Hook / Magnet 才有：
   - **Compare**：`>` / `<` / `=`
   - **Value**：`+10%` / `+20%` / `+30%`（Wayfinder Data 为 `+10`）
3. 固定加成（Elusive Chance、Wayfinder Data、Pearl / Treasure / Spirit Chance）**没有**数值比较，只判断有无该词条。
4. **Sort**：High → Low 或 Low → High。
5. **Clear this filter** 清空本槽。

总排序：**F1 > F2 > F3**，Grotto 再按 Cost（Low 最好），然后 Stock、距离、编号。

### 列表与钉选

匹配的点显示编号、坐标、Stock、词条。点击一行 **Pin**：

- 加入屏幕坐标 HUD
- 在原标签附近绘制仅客户端透视名牌：默认 `fishing spot #n`；Auto Pin 填了 nickname 则为 `名字 #组内编号`  
  Normal 颜色取主词条；Grotto 取 Stability Cost 颜色（自定义 hex 优先）。

再点一次取消 Pin。

---

## 扫描与生命周期

- 每 tick 扫描渲染距离内的 `text_display`，标题含 `Fishing Spot` 则入池。
- 同一取整坐标更新 Stock/词条，不换号、不重复音效。
- 新编号播放一次经验球音效（Enabled 时）。
- 词条与 Stock 的 **颜色直接读取标签 Component**，与游戏里看到的一致。
- 走近（约 48 格且区块已加载）标签消失 → 视为 Depleted，从池和 HUD 删除。走远卸载 **不会** 删。
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

`spawn` 在脚下刷 **50** 个普通测试点（10×5，间隔 3 格）。`spawn_grotto` 刷 **9** 个带 Stability Cost 的 Grotto 点（Low / Medium / High 颜色）。请启用 MCCI 材质包。`clear` 只删 `spotfilter_test`，不影响 Pin 引导。

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
