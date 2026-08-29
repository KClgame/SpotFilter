SpotFilter 测试数据包
====================

把本目录放到存档的 datapacks/ 下（仓库里已有 datapacks/spotfilter-test）。
作弊开启，然后：

  /reload
  /function spotfilter:spawn
  /function spotfilter:spawn_grotto
  /function spotfilter:clear

spawn
  在脚下刷 50 个普通钓鱼点（10×5 网格，间隔 3 格）。
  含不同 Stock 与 Hook / Magnet / Chance 组合，用来测扫描、筛选、Compact HUD。

spawn_grotto
  刷 9 个 Grotto 点：Stability Cost（Low / Medium / High）
  以及 +100% Fish / Pearl / Treasure / Spirit Chance。

clear
  只删除带 spotfilter_test 标签的测试实体。
  不会拆掉 SpotFilter 的 Pin 引导（引导不是世界实体）。

请启用 MCCI 材质包，词条 icon 才会按官方字体显示。
pack 格式：data 107（Minecraft 26.2）。

In English
----------
Cheats on: /function spotfilter:spawn (50 island spots),
spotfilter:spawn_grotto (9 Stability Cost spots),
spotfilter:clear (test entities only). Enable the MCCI resource pack.
