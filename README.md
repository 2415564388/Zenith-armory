# Zenith Armory / 天顶军械库

Forge 1.20.1 模组：一件 Curios 护符，吞噬 TaCZ 枪械，按枪械种类永久提高射击伤害。图标素材取自 Project Zenith 枪械垫，由 `tools/MakeIcon.java` 抠底缩放为 64×64。

**Tooltip：** 默认显示一句简介、已吞噬数量、伤害加成、真伤进度/状态，以及“按住 Shift 查看详情”；按住 Shift 后显示完整机制说明。

## 配置

首次启动生成 `config/gundevourer-common.toml`，修改后需重启游戏生效：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `damage.flatDamagePerGun` | `1` | 每吞噬 1 种枪械增加的固定射击伤害 |
| `damage.gunsPerPercentStage` | `9` | 每多少种枪械提升一次百分比伤害 |
| `damage.percentPerStage` | `10` | 每阶段提升的枪械伤害百分比 |
| `trueDamage.threshold` | `368` | 解锁真伤所需吞噬的枪械种数 |
| `trueDamage.percent` | `10.0` | 真伤占该次命中伤害的百分比 |
| `trueDamage.minHealth` | `1.0` | 真伤最多把目标压到的最低血量（至少 1，真伤不会击杀） |

Tooltip 与伤害结算均实时读取配置值。

## 玩法

（以下数值均为默认配置，可在配置文件中调整。）

1. 合成/创造模式拿到“天顶军械库”。
2. 鼠标左键拿起宝珠（放在光标上）。
3. 在任意容器（背包、箱子、创造物品栏等）里，用右键点击一把 TaCZ 枪械。
4. 枪械被消耗，宝珠 NBT 记录该枪械 ID；同一种枪械只能吞噬一次。
5. 把宝珠装备到 Curios 的 `charm`（护符）槽后生效：
   - 每吞噬一种枪械，射击基础伤害 +1
   - 每吞噬 **9** 种枪械，额外 **+10%** 枪械伤害（406 种满收集可叠到 +450%）；百分比基数 = 枪械基础伤害 + 饰品固定加伤
   - 同一佩戴者只能装备一个天顶军械库，且只有第一个生效
   - 吞噬达到 **368** 种后解锁：每次枪械命中额外造成该次伤害 **10%** 的绝对真实伤害（绕过护甲/减伤/无敌帧直接扣血，但**最低只压到 1 点血，不会直接击杀**）

## 实现要点

- 吞噬：重写 `Item#overrideStackedOnOther`（SRG `m_142207_`），在任意 `AbstractContainerMenu` 中由 Forge 调用；用 `IGun.getIGunOrNull(stack)` 判断枪械、`IGun.getGunId(stack)` 取枪械 ID；NBT key 为 `DevouredGuns`（`ListTag<StringTag>`）。
- 加伤：监听 TaCZ 的 `EntityHurtByGunEvent.Pre`（仅服务端），按“每发总伤害”加 `已吞噬种类数`。
- **百分比加伤**：借用 gunsmithlib 的 `gunsmithlib:bullet_damage` 属性。gunsmithlib 每 2 tick 把当前枪械单发伤害写入该属性基数，并在 `EntityHurtByGunEvent.Pre` 的 HIGHEST 阶段读取属性值；我们给玩家属性挂 `MULTIPLY_TOTAL` 修正（每 9 种 +0.10），由 gunsmithlib 自动叠到每颗弹丸的伤害上。
- **绝对真伤**：参考 abisburst_truedamage。在 `EntityHurtByGunEvent.Pre` 的 **LOWEST** 优先级（flat/百分比都改完之后）直接扣血，爆头倍率手动乘上；若 boss 重写 setHealth 不生效，反射写 `LivingEntity.f_20961_` 同步血量。
- **真伤不击杀**：真伤最低只会把目标压到 **1 点血**，不会直接打死。死亡、掉落、击杀归属全部交给原版/目标模组的正常伤害死亡流程，避免绕过它们的死亡动画、延迟掉落、火焰烧掉落等问题。
- **亚波伦兼容**：参考 abisburst_truedamage 的 `ApollyonTrueDamageHandler`。亚波伦有专属受击冷却和冥界自定义血量：
  - 冥界内：改 `Apollyon2Interface.revelaionfix$setApollyonHealth(max(1, current - 真伤))`
  - 非冥界：临时把 `revelaionfix$getHitCooldown` 和 `allTitlesApostle_1_20_1$getHitCooldown` 清零 → `setHealth(max(1, 当前血量 - 真伤))` → 恢复冷却
  - 同样遵循“最低 1 血、不直接击杀”
  - 通过 `ModList` 软检测 `goety` / `goety_revelation`（RevelationFix 是 GoetyRevelation 的嵌套 JarJar），缺席时自动跳过，不做硬依赖。
- **PvP**：玩家对玩家时用 `Player#canHarmPlayer` 检查服务端 PvP 设置和队伍关系。PvP 关闭/同队时：真伤跳过，本饰品的固定加伤跳过，并把已由 gunsmithlib 应用的本饰品百分比从本次伤害中撤销。注意 TaCZ 1.1.8 本身没有 PvP 检查，基础子弹伤害仍可能命中玩家；本模组只保证“饰品的额外加成”不参与。
- **真伤豁免（简化策略）**：真伤无视 boss 的特殊无敌帧、`invulnerable`、护甲、减伤和伤害上限。只有以下目标跳过真伤：
  - 实体类型标签 `gundevourer:true_damage_immune` 内的实体（默认 `dummmmmmy:target_dummy`、`minecraft:armor_stand`、`feibiblade:fibi` 小菲比、`pasterdream:shadow_npc_0`）
  - 车万女仆中有主人的女仆（软兼容检查；野生女仆正常吃真伤）
  - 需要保护其它 NPC/boss 时，把它的实体 ID 加进 `gundevourer:true_damage_immune` 标签即可（数据包可覆盖/扩展）。
  - 不再检查 `isInvulnerableTo`，否则会连 boss 的临时无敌帧一起挡掉。
- **多弹头（霰弹）修正**：TaCZ 会把一发总伤害按 `bullet_amount` 平摊到每颗弹丸（`EntityKineticBullet.damageModifier = 1 / 弹丸数`）。加伤也乘同样的系数，所以整发全中时总加成仍是 +N，而不是每颗弹丸各 +N。系数通过反射读取 `damageModifier`，读不到时用 `TimelessAPI` 里的 `bullet_amount` 兜底。
- 饰品：`data/curios/tags/items/charm.json` 把物品加入护符标签；`CuriosApi.getCuriosInventory(佩戴者)` 读取已装备宝珠（支持玩家和车万女仆等 Curios 生物）。
- 配方：火药 + 铁锭 + 回响碎片（见 `data/gundevourer/recipes/gun_devourer.json`）。

## 性能与安全

- **每 tick 缓存**：`GunDamageHandler.getDevouredCount` 按 `佩戴者 + gameTime` 缓存，霰弹多弹丸/高射速同 tick 只扫描一次 Curios/NBT；吞噬和命令会主动失效缓存。
- **O(1) 数量读取**：`GunDevourerItem.getDevouredCount` 直接读 `ListTag.size()`，不再构造 Set。
- **无逐发日志**：368 种后的真伤命中不写 debug 日志，只在 Apollyon 兼容异常时 `warn`。
- **客户端隔离**：Shift 检测拆到 `com.gundevourer.client.ClientTooltipHelper`，公共代码先用 `FMLEnvironment.dist == Dist.CLIENT` 短接；专用服务端不会加载 `net.minecraft.client.*`。
- **纯服务端逻辑**：伤害、真伤、PvP、Curios、命令均只在服务端执行；没有自定义网络包。
- **软兼容**：Apollyon 相关类通过 `ModList` + `try/catch(Throwable)` 保护，缺少依赖时不会导致加载崩溃。

## 测试命令

```
/gundevourer set <0-4096>
```

把玩家饰品栏（优先）或背包里第一个天顶军械库的吞噬数设为指定值，用 `gundevourer:test_n` 形式的假 ID 填充，方便测试 9 / 368 阈值。`set 0` 清空。需要权限等级 2（单人需开启作弊/对局域网开放）。

## 构建

无 Gradle。使用 JDK 17+ 编译，classpath 需要：

- 带 SRG 名称的 Minecraft/Forge client jar（例如 `libraries/net/minecraftforge/forge/1.20.1-47.4.0/forge-1.20.1-47.4.0-client.jar`）及 `libraries` 下的 Forge 库
- `tacz`、`curios`、`gunsmithlib` 的 mod jar
- 编译 `ApollyonCompat` 还需要 `goety` 与 `GoetyRevelation`；RevelationFix 是后者内嵌的 JarJar，需要解包并把内层 jar 加入 classpath

```bash
javac --release 17 -encoding UTF-8 -cp "$CP" -d build/classes $(find src -name "*.java")
```

将 `resources/`、`assets/`、`build/classes/` 合并后打成 jar 即可。生成的 jar 位于 `build/gundevourer-1.0.0.jar`，复制到整合包 mods 目录。

## 待实测/可扩展

- 目前只统计 Curios 护符槽中的宝珠；若要“放在背包里也生效”，把 `GunDamageHandler#getDevouredCount` 扩展为同时扫描玩家背包。
- 可加入吞噬上限、按枪械类别区分加成、吞噬列表 tooltip 等。
