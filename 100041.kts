@file:Depends("coreMindustry/menu", "调用菜单")
@file:Depends("coreMindustry/contentsTweaker", "修改核心单位,单位属性")
@file:Depends("wayzer/user/achievement", "成就")
@file:Import("@coreMindustry/util/spawnAround.kt", sourceFile = true)

package mapScript

//就当是为了我，离开那个优化import
import arc.Events
import arc.graphics.Color
import arc.math.geom.Geometry
import arc.struct.*
import coreLibrary.lib.util.loop
import coreLibrary.lib.with
import coreMindustry.MenuBuilder
import coreMindustry.lib.broadcast
import coreMindustry.lib.game
import coreMindustry.lib.listen
import coreMindustry.util.spawnAround
import mapScript.lib.modeIntroduce
import mindustry.Vars.*
import mindustry.ai.types.MissileAI
import mindustry.content.*
import mindustry.entities.Units
import mindustry.entities.bullet.BulletType
import mindustry.entities.units.StatusEntry
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.*
import mindustry.type.ItemStack
import mindustry.type.StatusEffect
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.campaign.Accelerator.AcceleratorBuild
import mindustry.world.blocks.payloads.BuildPayload
import mindustry.world.blocks.storage.CoreBlock.CoreBuild
import org.intellij.lang.annotations.Language
import wayzer.lib.dao.PlayerData
import kotlin.math.*
import kotlin.random.Random


/**@author xkldklp & Lucky Clover */
/**
 * 末日模式
 * 在尸潮中生存
 * */

name = "Daynight"

val achievement = contextScript<wayzer.user.Achievement>()
fun Player.achievement(name: String, exp: Int) {
    val profile = PlayerData[uuid()].profile
    if (profile != null)
        achievement.finishAchievement(profile, name, exp, false)
}

fun Float.buildLineBar(
    length: Int = 20,
    max: Float = 20f,
    color: Pair<Pair<String, String>, String> = Pair(Pair("[yellow]", "[green]"), "[red]")
): String {
    val num = this
    return buildString {
        repeat(length) {
            append(
                "${
                    when {
                        num > it * (max / length) + max -> color.first.first
                        num > it * (max / length) -> color.first.second
                        else -> color.second
                    }
                }|"
            )
        }
    }
}

class Norm(
    val mode: String = "[cyan]昼夜交替[white]",
    val tech: String = "[sky]科技${Iconc.teamSharded}[white]",
    val unitExp: String = "[acid]经验\uE809[white]",
    val difficult: String = "[orange]难度\uE865[white]",
    val health: String = "[green]最大血量\uE813[white]",
    val fort: String = "[gold]据点\uE86B[white]"
)

val norm = Norm()

modeIntroduce(
    "昼夜交替", "${norm.mode}玩法介绍 \n[acid]By Lucky Clover&xkldklp&blac[]" +
            "\n安装ctmod(各大群都有)以同步属性，推荐使用学术以支持标记系统和快捷挖矿(控制-自动吸附)" +
            "\n每个${norm.fort}产出对应等级的核心机" +
            "\n核心机挖掘矿可以通过核心数据库查看" +
            "\n挖矿无视核心距离，挖掘后矿床需要时间再生" +
            "\n点击${norm.fort}，矿物可升级核心和购买兵种" +
            "\n核心同时产出${norm.tech}，${norm.tech}可购买获得各种全局增益" +
            "\n单位可以升级，${norm.unitExp}主要由击杀敌方单位\uE86D[white]获取" +
            "\n每2级交替依次获得${norm.health}和buff，每8级获得强力buff。" +
            "\n简单来说，单位${norm.unitExp}获取多少主要与单位${norm.health}、被击杀单位${norm.health}和buff、[sky]科技${Iconc.teamSharded}[]有关" +
            "\n${norm.difficult}会随着时间增加" +
            "\n高${norm.difficult}会生成高级敌人，同时赋予敌人${norm.unitExp}" +
            "\n每天存在昼夜循环，夜越深敌人越强大" +
            "\n请留意切换时间事件时的全图播报" +
            "\n玩家数增加->增加${norm.difficult}速度、${norm.fort}生成、敌人生成"
)

val contentPatch
    @Language("JSON5")
    get() = """
{
  "unit": {
    "alpha": {
      "mineTier": 0,
      "mineSpeed": 0,
      "speed": 1.8
    },
    "mono": {
      "mineSpeed": 0,
      "speed": 2.2
    },
    "poly": {
      "mineSpeed": 0,
      "mineRange": 100
    },
    "mega": {
      "mineSpeed": 0,
      "mineRange": 150,
      "weapons.0.bullet.damage": 5,
      "weapons.2.bullet.damage": 5
    },
    "quad": {
      "speed": 2.4,
      "health": 1000,
      "mineTier": 4,
      "mineRange": 200,
      "weapons.0.bullet.damage": 50,
      "mineSpeed": 0
    },
    "oct": {
      "speed": 2.4,
      "health": 2500,
      "mineTier": 5,
      "mineSpeed": 0,
      "mineRange": 250,
      "abilities.0.regen": 1,
      "abilities.0.max": 1000,
    },
    "retusa": {
      "health": 250,
      "flying": true,
      "armor": 2
    },
    "stell": {
     "health": 200,
     "armor": 5
    },
    "elude": {
      "health": 200,
      "armor": 2
    },
    "risso": {
      "health": 300,
      "flying": true,
      "armor": 2
    },
    "mace": {
      "armor": 8
    },
    "pulsar": {
      "mineTier": -1,
      "armor": 5
    },
    "oxynoe": {
      "armor": 3,
      "flying": true
    },
    "locus": {
      "health": 750,
      "armor": 11
    },
    "cleroi": {
      "health": 750,
      "armor": 8
    },
    "minke": {
      "health": 750,
      "flying": true,
      "armor": 8
    },
    "fortress": {
      "health": 1200,
      "armor": 14
    },
    "quasar": {
      "mineTier": -1,
      "health": 1000,
      "armor": 11
    },
    "bryde": {
      "health": 1000,
      "flying": true,
      "armor": 8
    },
    "precept": {
      "health": 1500,
      "armor": 17
    },
    "anthicus": {
      "health": 1500,
      "armor": 14
    },
    "cyerce": {
      "health": 1500,
      "flying": true,
      "armor": 10
    },
    "obviate": {
      "health": 1500,
      "armor": 10
    },
    "sei": {
      "flying": true,
    },
    "aegires": {
      "flying": true,
    },
    "omura": {
      "flying": true,
    },
    "navanax": {
      "flying": true,
    },
  },
  "block": {
    "interplanetary-accelerator": {
      "health" : 100000,
      "solid": false,
      "underBullets": true
    },
    "core-shard": {
      "unitType": "alpha",
      "unitCapModifier": 5,
      "itemCapacity": 10000000,
      "health": 1500,
      "armor" : 0,
      "requirements": [
        "scrap/10000"
      ],
    },
    "core-foundation": {
      "unitType": "mono",
      "unitCapModifier": 8,
      "itemCapacity": 10000000,
      "health": 3000,
      "armor" : 5,
      "requirements": [
        "scrap/1000"
      ],
    },
    "core-bastion": {
      "unitType": "poly",
      "unitCapModifier": 11,
      "itemCapacity": 10000000,
      "health": 7500,
      "armor" : 10,
      "incinerateNonBuildable": false,
      "requirements": [
        "copper/2000",
        "lead/2000",
        "scrap/2000"
      ],
    },
    "core-nucleus": {
      "unitType": "mega",
      "unitCapModifier": 14,
      "itemCapacity": 10000000,
      "health": 15000,
      "armor" : 15,
      "requirements": [
        "copper/4000",
        "lead/4000",
        "scrap/4000",
        "coal/4000"
      ],
    },
    "core-citadel": {
      "unitType": "quad",
      "unitCapModifier": 17,
      "itemCapacity": 10000000,
      "incinerateNonBuildable": false,
      "health": 25000,
      "armor" : 20,
      "requirements": [
        "copper/10000",
        "lead/10000",
        "scrap/10000",
        "coal/10000",
        "titanium/5000",
        "beryllium/5000"
      ],
    },
    "core-acropolis": {
      "unitType": "oct",
      "unitCapModifier": 20,
      "itemCapacity": 10000000,
      "incinerateNonBuildable": false,
      "health": 40000,
      "armor" : 25,
      "requirements": [
        "copper/10000",
        "lead/10000",
        "scrap/10000",
        "coal/10000",
        "titanium/10000",
        "beryllium/10000",
        "thorium/5000"
      ],
    },
  }
}
"""

fun setRules() {
    Call.setRules(state.rules)
}

fun Float.format(i: Int = 2): String {
    return "%.${i}f".format(this)
}

val tiles by autoInit { world.tiles.filter { !it.floor().isLiquid } }
fun getSpawnTiles(): Tile {
    return tiles.filter {
        !it.floor().isDeep && it.passable() && Team.sharded.cores()
            .all { c -> !it.within(c, state.rules.enemyCoreBuildRadius) }
    }.random()
}

val core2label by autoInit { mutableMapOf<Building, WorldLabel>() }

/** 全局系数 */
val difficultRate: Float = 0.005f
var timeSpd: Int = 180 //控制时间流逝速度,和现实时间的比值
var mutatorChoice: Int = 3
/** 单位升级 */
var unitExpE: Float = 1.7f
var unitExpCE: Float = 1.3f
var unitExpMultiplier = 5f
var unitInitExpMultiplier = 100f
/** 敌人降级 */
val levelPerTier: Int = 3
val expPerTier: Int = 500
/** 价格 */
var fortUpgradeCostMultiplier: Float = 1f
var unitCostMultiplier: Float = 1f
/** 科技 */
val finalTechCost = 99999
/** 其他 */
val maxSpeedMultiplier = 20

/** 全局各种资源、难度系数 */
fun globalMultiplier(): Float {
    return Groups.player.filter{it.team() == state.rules.defaultTeam }.size * 0.33f + 1
}

class TimeType(
    val name: String,
    val desc: String,
    val initEffect: (() -> Unit)? = null,
    val friendly: Int = 0,
    val buffFriendly: StatusEffect
) {
    fun active() {
        initEffect?.invoke()
    }
}

//世界时间
class WorldTime(
    // second，但并不是实际速度
    var time: Int = 10 * 60 * 60,

    private var midnight: TimeType = TimeType("[#660033]午夜", "灾厄来袭，敌军获得增幅，野外单位惩罚+禁飞", fun() {
        state.rules.waveTeam.rules().unitDamageMultiplier = 1.25f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1.25f
    }, -2, StatusEffects.unmoving),
    private var dawn: TimeType = TimeType("[#ff00ff]黎明", "黎明将至，敌军获得加强，野外单位惩罚+禁飞", fun() {
        state.rules.waveTeam.rules().unitDamageMultiplier = 1.1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1.1f
    }, -1, StatusEffects.electrified),
    private var morning: TimeType = TimeType("[#99ff33]早晨", "阳光笼罩，又活过了新的一天", fun() {
        state.rules.waveTeam.rules().unitDamageMultiplier = 1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1f
        worldMutator.spawnMutator()
    }, 0, StatusEffects.none),
    private var midday: TimeType = TimeType(
        "[#ffff00]中午",
        "烈日当空，敌军获得削弱，不再出现新的敌人，友方单位加强",
        fun() {
            state.rules.waveTeam.rules().unitDamageMultiplier = 0.75f
            state.rules.waveTeam.rules().unitHealthMultiplier = 0.75f
        },
        1,
        StatusEffects.fast
    ),
    private var afternoon: TimeType = TimeType("[#ff9933]下午", "太阳西沉，远方传来阵阵踩踏大地的脚步声", fun() {
        state.rules.waveTeam.rules().unitDamageMultiplier = 1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1f
    }, 0, StatusEffects.none),
    private var night: TimeType = TimeType("[#0033cc]夜晚", "夜幕将至，敌军获得加强，野外单位惩罚+禁飞", fun() {
        state.rules.waveTeam.rules().unitDamageMultiplier = 1.1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1.1f
    }, -1, StatusEffects.electrified),

    var curTimeType: TimeType = dawn
) {
    private fun getNatureTimeType(): TimeType {
        return when (hours()) {
            in 2..5 -> dawn
            in 6..9 -> morning
            in 10..13 -> midday
            in 14..17 -> afternoon
            in 18..21 -> night
            else -> midnight
        }
    }

    fun transTimeType() {
        if (getNatureTimeType() != curTimeType) {
            curTimeType = getNatureTimeType()
            Call.announce("[orange]${curTimeType.desc}")
            curTimeType.active()
        }
    }

    fun timeString(): String {
        return "第${days()}天 ${hoursFixed()}:${minutesFixed()}"
    }

    fun minutes(): Int {
        return (time / 60) % 60
    }

    fun minutesFixed(): String {
        return (100 + minutes()).toString().removePrefix("1")
    }

    fun hours(): Int {
        return (time / 3600) % 24
    }

    fun hoursFixed(): String {
        return (100 + hours()).toString().removePrefix("1")
    }

    fun days(): Int {
        return (time / 86400) + 1
    }

    fun lights(): Float {
        return abs(0.5 - time % 86400 / 86400f).toFloat() * 2f
    }

}

val worldTime by autoInit { WorldTime() }

var debugMode: Boolean = false

val maxNewFort: Int = 3
fun canSpawnNewFort(): Boolean {
    return state.rules.waveTeam.cores().size <= maxNewFort
}

class FortType(
    var name: String,
    var block: Block,
    var bulletType: BulletType
)

val fortTypes: List<FortType> = listOf(
    FortType("前哨", Blocks.coreShard, UnitTypes.zenith.weapons[0].bullet),
    FortType("卫戍", Blocks.coreFoundation, UnitTypes.fortress.weapons[0].bullet),
    FortType("堡垒", Blocks.coreBastion, UnitTypes.sei.weapons[0].bullet),
    FortType("城市", Blocks.coreNucleus, UnitTypes.toxopid.weapons[2].bullet),
    FortType("省府", Blocks.coreCitadel, UnitTypes.conquer.weapons[0].bullet),
    FortType("王都", Blocks.coreAcropolis, UnitTypes.navanax.weapons[4].bullet)
)

data class FortData(
    var fortType: FortType
) {
    fun tier(): Int {
        return fortTypes.indexOf(fortType)
    }

    fun nextFortType(): FortType {
        return fortTypes[(tier() + 1).coerceAtMost(fortTypes.size - 1)]
    }

    fun maxTier(): Boolean {
        return fortType == fortTypes.last()
    }

}

val fortData by autoInit { mutableMapOf<CoreBuild, FortData?>() }
fun CoreBuild.fortData(): FortData {
    if (fortData[this] == null) {
        val data = FortData(fortType = this.fortType())
        fortData[this] = data
    }
    return fortData[this]!!
}

fun CoreBuild.fortType(): FortType {
    fortTypes.forEach {
        if (it.block == this.block) return it
    }
    return fortTypes.first()
}

val unitsWithTier = listOf(
    listOf(
        UnitTypes.dagger to listOf(Items.scrap to 20),
        UnitTypes.nova to listOf(Items.scrap to 25),
        UnitTypes.retusa to listOf(Items.scrap to 300),
        UnitTypes.flare to listOf(Items.scrap to 100),
    ),
    listOf(
        UnitTypes.stell to listOf(Items.copper to 100, Items.lead to 50),
        UnitTypes.elude to listOf(Items.copper to 50, Items.lead to 50),
        UnitTypes.risso to listOf(Items.copper to 200, Items.lead to 200),
        UnitTypes.avert to listOf(Items.copper to 25, Items.lead to 25)
    ),
    listOf(
        UnitTypes.mace to listOf(Items.copper to 100, Items.lead to 50, Items.coal to 20),
        UnitTypes.pulsar to listOf(Items.copper to 50, Items.lead to 100, Items.coal to 40),
        UnitTypes.atrax to listOf(Items.copper to 75, Items.lead to 75, Items.coal to 40),
        UnitTypes.oxynoe to listOf(Items.copper to 350, Items.lead to 350, Items.coal to 400)
    ),
    listOf(
        UnitTypes.cleroi to listOf(Items.copper to 200, Items.lead to 100, Items.beryllium to 150),
        UnitTypes.locus to listOf(Items.copper to 100, Items.lead to 200, Items.beryllium to 150),
        UnitTypes.minke to listOf(Items.copper to 200, Items.lead to 200, Items.titanium to 100),
        UnitTypes.zenith to listOf(Items.copper to 200, Items.lead to 200, Items.titanium to 100)
    ),
    listOf(
        UnitTypes.fortress to listOf(
            Items.copper to 300,
            Items.lead to 150,
            Items.titanium to 250,
            Items.thorium to 100
        ),
        UnitTypes.quasar to listOf(Items.copper to 150, Items.lead to 300, Items.titanium to 150, Items.thorium to 100),
        UnitTypes.spiroct to listOf(
            Items.copper to 600,
            Items.lead to 600,
            Items.titanium to 400,
            Items.thorium to 250
        ),
        UnitTypes.bryde to listOf(Items.copper to 600, Items.lead to 600, Items.titanium to 400, Items.thorium to 250)
    ),
    listOf(
        UnitTypes.precept to listOf(
            Items.copper to 1000,
            Items.lead to 500,
            Items.beryllium to 500,
            Items.thorium to 300
        ),
        UnitTypes.anthicus to listOf(
            Items.copper to 500,
            Items.lead to 1000,
            Items.beryllium to 350,
            Items.thorium to 300
        ),
        UnitTypes.cyerce to listOf(
            Items.copper to 1500,
            Items.lead to 1500,
            Items.titanium to 500,
            Items.thorium to 350
        ),
        UnitTypes.obviate to listOf(
            Items.copper to 1500,
            Items.lead to 1500,
            Items.titanium to 500,
            Items.thorium to 350
        )
    )
)
val unitsWithCost = buildList { unitsWithTier.forEach { it.forEach { add(it) } } }

data class UnitData(
    var exp: Float = 0f,
    var level: Int = 0
) {
    lateinit var unit: mindustry.gen.Unit

    fun nextLevelNeed(): Float {
        return levelNeed(level)
    }

    fun levelNeed(l: Int): Float {
        return (l + 1f).pow(unitExpE) * (unit.type.health).pow(0.5f) * unitExpMultiplier + unitExpCE.pow(l)
    }
}

val unitData by autoInit { mutableMapOf<mindustry.gen.Unit, UnitData>() }
val mindustry.gen.Unit.data get() = unitData.getOrPut(this) { UnitData() }.also { it.unit = this }

fun mindustry.gen.Unit.addEffect(effect: StatusEffect, time: Float = Float.POSITIVE_INFINITY){
    this.statuses.add(StatusEntry().set(effect, time))
}

/** 给一个单位增加x级 */
fun mindustry.gen.Unit.addLevel(level: Int){
    this.data.level += level
    this.maxHealth *= 1.1f.pow(level)
    this.health *= 1.1f.pow(level)
    repeat(level/2) {
        this.addEffect(listOf(
            StatusEffects.overdrive,
            StatusEffects.overclock,
            StatusEffects.boss
        ).random(), Float.POSITIVE_INFINITY)
    }
}

fun Team.maxUnitLevel(): Int{
    var maxLevel = 0
    Groups.unit.filter { it.team == this }.forEach {
        maxLevel = maxLevel.coerceAtLeast(it.data.level)
    }
    return maxLevel
}

val enemyTier = listOf(
    listOf(UnitTypes.dagger, UnitTypes.nova),
    listOf(UnitTypes.stell, UnitTypes.elude, UnitTypes.flare),
    listOf(UnitTypes.crawler, UnitTypes.retusa, UnitTypes.risso),
    listOf(UnitTypes.mace, UnitTypes.pulsar, UnitTypes.atrax),
    listOf(UnitTypes.horizon, UnitTypes.avert, UnitTypes.poly),
    listOf(UnitTypes.cleroi, UnitTypes.locus, UnitTypes.oxynoe),
    listOf(UnitTypes.fortress, UnitTypes.quasar, UnitTypes.spiroct),
    listOf(UnitTypes.zenith, UnitTypes.bryde, UnitTypes.mega),
    listOf(UnitTypes.precept, UnitTypes.anthicus, UnitTypes.cyerce),
    listOf(UnitTypes.minke, UnitTypes.obviate, UnitTypes.quell)
)

val T4Enemy: List<UnitType> = listOf(
    UnitTypes.scepter,
    UnitTypes.antumbra,
    UnitTypes.arkyid,
    UnitTypes.vanquish,
    UnitTypes.tecta,
    UnitTypes.vela,
    UnitTypes.sei,
    UnitTypes.aegires
)

val T5Enemy: List<UnitType> = listOf(
    UnitTypes.reign,
    UnitTypes.corvus,
    UnitTypes.toxopid,
    UnitTypes.conquer,
    UnitTypes.collaris,
    UnitTypes.disrupt,
    UnitTypes.eclipse,
    UnitTypes.omura,
    UnitTypes.navanax
)

val difficultTier = listOf(
    "和平" to "[#66ff33]", "简单" to "[#99ff33]", "一般" to "[#ccff33]",
    "进阶" to "[#ffff00]", "稍难" to "[#ffcc00]", "困难" to "[#ff9933]",
    "硬核" to "[#ff6600]", "专家" to "[#ff5050]", "大师" to "[#cc0066]",
    "噩梦" to "[#6600cc]", "疯狂" to "[#9900cc]", "不可能" to "[#660066]",
    "终焉之时" to "[#660033]", "深渊之地" to "[#800000]", "无尽灾厄" to "[#000000]"
)

// 世界难度与单位生成控制器
class WorldDifficult(
    var level: Float = 0f
) {
    fun update() {
        level += difficultRate * globalMultiplier()
    }

    fun tier(): Int {
        return (level / levelPerTier).toInt().coerceAtMost(difficultTier.size - 1)
    }

    fun subTier(): Int {
        return (level % levelPerTier).toInt()
    }

    fun process(): Int {
        return ((level % 1) * 100).toInt()
    }

    fun name(): String {
        return "${difficultTier[tier()].second}${difficultTier[tier()].first} ${"I".repeat(subTier() + 1)}  [white]~ ${difficultTier[tier()].second}${process()}%"
    }

    fun basicUnit(tier: Int): UnitType {
        return enemyTier[tier.coerceAtMost(enemyTier.size - 1)].random()
    }

    /**
     * 单位生成通用模式
     * 生成subTier个tier单位
     * 随机：subTier ± 1
     * 每次生成单位时，有概率掉一Tier，但单位获得expPerTier
     * */
    fun spawnUnit(levels: Float, tile: Tile) {
        val tiers = (levels / levelPerTier).toInt()
        val highTierChance = ((levels - enemyTier.size * 3) * 0.015f).coerceAtMost(0.5f)
        if (tiers < enemyTier.size || Random.nextFloat() < 1 - highTierChance) {
            //生成普通兵种
            for (st in 0..(subTier() + Random.nextInt(1, 2))) {
                var unitTier = tiers
                var exp = Random.nextFloat() * levels.pow(unitExpE) * 20f
                var extraShield = 1f
                while (unitTier > 0) {
                    if (unitTier >= enemyTier.size) { // 超强度的强力降级
                        unitTier -= 1
                        exp += expPerTier * 2f
                        extraShield *= 1.15f
                    } else if (Random.nextFloat() < 0.7) { // 普通随机降级
                        unitTier -= 1
                        exp += expPerTier
                        extraShield *= 1.05f
                    } else break
                }
                val unit = basicUnit(unitTier).spawnAround(tile, state.rules.waveTeam) ?: continue
                unit.data.exp = exp
                unit.shield += unit.maxHealth * (extraShield - 1)
            }
        } else {
            val hardChance = (((levels - enemyTier.size + 5) * 3) * 0.015f).coerceAtMost(0.5f)
            val unitList = if (Random.nextFloat() < 1 - hardChance) T4Enemy else T5Enemy
            while (true) {
                var exp = Random.nextFloat() * levels.pow(unitExpE) * 20f
                var extraShield = 1f
                if (Random.nextFloat() < (highTierChance * 2).coerceAtMost(0.75f)) {
                    exp += expPerTier * 2f
                    extraShield *= 1.25f
                } else {
                    val unit = unitList.random().spawnAround(tile, state.rules.waveTeam) ?: continue
                    unit.data.exp = exp
                    unit.shield += unit.maxHealth * (extraShield - 1)
                    break
                }
            }
        }
    }
}

val worldDifficult by autoInit { WorldDifficult() }

class Abilitiy(
    val name: String,
    val desc: String,

    val effect: suspend mindustry.gen.Unit.() -> Unit
)

val bossAbilities by autoInit {
    listOf(
        Abilitiy("[purple]蚀时", "[lightgray]愈加漫长的时间") {
            worldTime.time -= 100
            delay(150)
        },
        Abilitiy("[gold]进化", "[lightgray]吞噬..进化") {
            val target = Units.closest(null, x, y, range()) { it != this }
            if (target == null) {
                delay(1_000)
            } else {
                Call.effect(Fx.sparkExplosion, target.x, target.y, target.hitSize * 5f, Color.red)
                data.exp += target.data.nextLevelNeed() * 0.5f

                if (team == target.team()) {
                    target.kill()
                    delay(7_500)
                } else {
                    target.data.exp -= target.data.nextLevelNeed() * 0.5f
                    target.health -= target.maxHealth * 0.5f
                    delay(15_000)
                }

            }

        },
        Abilitiy("[maroon]领域", "[gold]随我出征!") {
            val near = buildList { Units.nearby(team(), x, y, range() * 0.4f) { if (it != this) add(it) } }
            val far =
                buildList { Units.nearby(team(), x, y, range() * 1.2f) { if (it != this && it !in near) add(it) } }

            far.forEach {
                it.apply(StatusEffects.fast, 0.2f * 60f)

            }
            near.forEach {
                it.apply(StatusEffects.slow, 0.2f * 60f)
                it.apply(StatusEffects.shielded, 0.2f * 60f)
            }

            delay(100)
        },
        Abilitiy("[accent]共进", "[lightgray]共同进步") {
            Groups.unit.filter { it.team == state.rules.waveTeam && it != this }.forEach {
                it.data.exp += worldDifficult.level.pow(unitExpE).coerceAtLeast(12f).coerceAtMost(500f)
            }
            delay(1000)
        },
        Abilitiy("[forest]龟壳", "[lightgray]壳。") {
            val u = buildList { Units.nearby(team(), x, y, range() * 0.2f) { add(it) } }
            u.forEach {
                if (this == it) {
                    repeat(1) { _ ->
                        it.statuses.add(StatusEntry().set(StatusEffects.shielded, 1f * 60f))
                    }
                } else {
                    repeat(2) { _ ->
                        it.statuses.add(StatusEntry().set(StatusEffects.shielded, 1f * 60f))
                    }
                    it.apply(StatusEffects.slow, 1f * 60f)
                }

            }
            delay(1000)
        },
        Abilitiy("[lime]电磁", "[lightgray]麻了") {
            val u = buildList { Units.nearby(null, x, y, range()) { if (it.team != team) add(it) } }
            u.forEach {
                it.statuses.add(StatusEntry().set(StatusEffects.electrified, 1f * 60f))
            }
            delay(500)
        },
    )
}

class Tech(
    var name: String,
    var desc: String,
    var tier: Int = 0,
    var maxTier: Int = 10
) {
    fun cost(): Int {
        return (1.5f.pow(tier) * 500).toInt()
    }
}

class TechInfo(
    var exp: Int = 0,

    var mineTier: Tech = Tech("挖掘效率", "减少挖矿损血|增加单次范围", 0),
    var mineEffTier: Tech = Tech("精准采集", "矿物更快恢复|增加挖矿效率", 0),
    var moreExpTier: Tech = Tech("经验效率", "增加单位经验", 0),
    var moreExpInitTier: Tech = Tech("预训练", "单位初始经验", 0),
    var unitRepairTier: Tech = Tech("单位修复", "定期回复单位|赋予单位护盾", 0),
    var turretsTier: Tech = Tech("核心防御", "减少核炮CD|核心血量增加", 0),

    var techList: List<Tech> = listOf(mineTier, mineEffTier, moreExpTier, moreExpInitTier, turretsTier, unitRepairTier)
) {
    private fun canResearch(tech: Tech): Boolean {
        return exp > tech.cost() && tech.tier < tech.maxTier
    }

    fun buttonName(tech: Tech): String {
        return "[gold]${tech.name} \n[cyan]${tech.desc}"
    }

    fun msg(tech: Tech): String {
        return "${if (canResearch(tech)) "[green]" else "[lightgray]"}\n ${tech.cost()} ~ " + "[green]|".repeat(tech.tier) + "[red]|".repeat(
            tech.maxTier - tech.tier
        )
    }

    fun research(tech: Tech) {
        if (!canResearch(tech)) return
        exp -= tech.cost()
        tech.tier += 1
    }

    fun update() {
        tech.exp += tech.techIncreased()
    }

    fun techIncreased(): Int {
        var expIncreased = 0f
        state.rules.defaultTeam.cores().forEach {
            expIncreased += 1.7f.pow(it.fortData().tier())
        }
        return expIncreased.toInt()
    }

}

val tech by autoInit { TechInfo() }

val resTier = listOf(
    listOf(Items.scrap),
    listOf(Items.copper, Items.lead),
    listOf(Items.coal),
    listOf(Items.titanium, Items.beryllium),
    listOf(Items.thorium)
)

val blockTier = listOf(
    listOf(Blocks.repairPoint, Blocks.wave, Blocks.liquidSource),
    listOf(Blocks.repairTurret, Blocks.segment, Blocks.berylliumWallLarge),
    listOf(Blocks.tsunami, Blocks.parallax, Blocks.regenProjector),
    listOf(Blocks.mender, Blocks.shockwaveTower, Blocks.forceProjector),
    listOf(Blocks.unitRepairTower, Blocks.logicProcessor, Blocks.shieldedWall)
)

class Bonus(
    var expBonus: Int = 0,
    var items: MutableList<ItemStack> = mutableListOf(),
    var blocks: MutableList<Block> = mutableListOf()
) {
    fun effect(team: Team, tile: Tile) {
        Call.effect(Fx.reactorExplosion, tile.worldx(), tile.worldy(), 0f, Color.red)
        tech.exp += expBonus
        team.core().items.add(items)
        if (blocks.isNotEmpty()) {
            val unit: mindustry.gen.Unit? = UnitTypes.emanate.spawnAround(tile, team, 10)?.apply {
                if (this is Payloadc) {
                    for (block in blocks)
                        addPayload(BuildPayload(block, team))
                }
                apply(StatusEffects.shielded, 99999f)
                apply(StatusEffects.fast, 99999f)
            }
            unit?.add()
            launch(Dispatchers.game) {
                delay(60000)
                unit?.kill()
            }
            broadcast(
                "[yellow]在[${tile.x},${tile.y}]获得奖励建筑(已装载在单位上)，你有60秒时间释放其中的建筑！  [#eab678]<Mark>[white](${tile.x},${tile.y})".with(),
                quite = true
            )
        }
    }

    fun randomBonus(tier: Int) {
        expBonus += (tier * 50)
        when (Random.nextInt(100)) {
            in 0..50 -> {
                val multi: Float = 1.2f.pow(tier - resTier.size * levelPerTier).coerceAtLeast(1f)
                for (subTier in 0..tier.coerceAtMost(resTier.size * levelPerTier - 1)) {
                    items.add(
                        ItemStack(
                            resTier[(subTier / levelPerTier)].random(),
                            ((subTier % levelPerTier + 1) * Random.nextInt(200) * multi).toInt()
                        )
                    )
                }
            }

            else -> {
                for (blockIndex in 0..(tier % levelPerTier + 1)) {
                    var count: Int = 1
                    var level: Int = tier / levelPerTier
                    while (level > 0) {
                        if (level < blockTier.size - 1 && Random.nextFloat() < 0.5) break
                        level -= 1
                        count += 2
                    }
                    for (i in 1..count + 1) blocks.add(blockTier[level].random())
                }
            }
        }
    }

    fun label(): String {
        var text: String = ""
        if (expBonus > 0) text += "${norm.tech} $expBonus"
        if (items.isNotEmpty()) items.forEach {
            text += "\n${it.item.emoji()} ${it.amount}"
        }
        if (blocks.isNotEmpty()) {
            text += "\n"
            blocks.forEach {
                text += "${it.emoji()} "
            }
        }
        if (text == "") return "[red]无(已占领)"
        return text
    }
}

class NestType(
    var name: String,
    var block: Block,
)

val nestTypes: List<NestType> = listOf(
    NestType("感染区", Blocks.coreShard),
    NestType("孵化室", Blocks.coreFoundation),
    NestType("控制节点", Blocks.coreBastion),
    NestType("分支节点", Blocks.coreFoundation),
    NestType("核心节点", Blocks.coreCitadel),
    NestType("母巢", Blocks.coreAcropolis)
)

fun randomNest(): Block {
    return nestTypes[(Random.nextFloat() * worldDifficult.level / levelPerTier).toInt()
        .coerceAtMost(nestTypes.size - 1)].block
}

data class NestData(
    var nestType: NestType,
    var tier: Int = 0,
    var bonus: Bonus = Bonus()
) {
    fun mainTier(): Int {
        return tier / levelPerTier
    }

    fun init(hasReward: Boolean) {
        tier = nestTypes.indexOf(nestType) * levelPerTier * 2 + Random.nextInt(3)
        if (hasReward) bonus.randomBonus(tier)
    }

    fun getLabel(): String {
        return "${nestType.name} Lv${tier} - [cyan]占领奖励[white]\n ${bonus.label()}"
    }

    fun spawnUnit(tile: Tile) {
        worldDifficult.spawnUnit(tier / 6 + worldDifficult.level, tile)
    }

}

val nestBonusGotten: MutableList<Tile> = mutableListOf()

val nestData by autoInit { mutableMapOf<CoreBuild, NestData?>() }
fun CoreBuild.nestData(): NestData? {
    if (this.team != state.rules.waveTeam) return null
    if (nestData[this] == null) {
        val data = NestData(nestType = this.nestType())
        if (nestBonusGotten.contains(this.tile)) data.init(false)
        else {
            data.init(true)
            nestBonusGotten.add(this.tile)
        }
        nestData[this] = data
    }
    return nestData[this]!!
}

fun CoreBuild.nestType(): NestType {
    nestTypes.forEach {
        if (it.block == this.block) return it
    }
    return nestTypes.first()
}

val bossList: List<UnitType> = listOf(
    UnitTypes.reign,
    UnitTypes.corvus,
    UnitTypes.toxopid,
    UnitTypes.conquer,
    UnitTypes.collaris,
    UnitTypes.disrupt,
    UnitTypes.eclipse,
    UnitTypes.omura,
    UnitTypes.navanax
)

class BossUnit(
    var spawned: Boolean = false,
    var bossUnits: mindustry.gen.Unit = UnitTypes.gamma.spawn(world.tile(1,1),Team.derelict)
){

    fun spawnBoss(){
        var tile: Tile? = null
        var lsBossUnits: mindustry.gen.Unit? = null

        while (lsBossUnits == null || tile == null){
            tile = getSpawnTiles()
            lsBossUnits = bossList.random().spawnAround(tile, state.rules.waveTeam)
        }

        bossUnits = lsBossUnits

        broadcast(
            "[orange]警告！！！最终BOSS已在[${tile.x},${tile.y}]处生成！  [red]<Attack>[white](${tile.x},${tile.y})".with(),
            quite = true
        )

        bossUnits.addLevel((state.rules.defaultTeam.maxUnitLevel() * 0.75f).toInt())
        repeat(2){
            bossUnits.addEffect(StatusEffects.boss)
            bossUnits.addEffect(StatusEffects.shielded)
        }
        repeat((bossUnits.speedMultiplier * 0.4).toInt() + 1){
            bossUnits.addEffect(StatusEffects.slow)
        }

        bossUnits.shield = bossUnits.maxHealth * globalMultiplier()

        spawned = true
    }

    fun finished(): Boolean{
        return spawned && bossUnits.dead
    }
}
val bossUnit by autoInit { BossUnit() }

fun spawnFort() {
    val tile = getSpawnTiles()
    tile.setNet(Blocks.coreShard, Team.derelict, 0)
    broadcast(
        "[yellow]在[${tile.x},${tile.y}]处发现废弃的前哨站！  [#eab678]<Mark>[white](${tile.x},${tile.y})".with(),
        quite = true
    )
    if (!canSpawnNewFort()) broadcast(
        "[orange]前哨站已达到上限！占领更多的前哨站以允许新前哨生成".with(),
        quite = true
    )
}

class Mutator(
    private val name: ((type: Boolean) -> String),
    private val effect: ((type: Boolean) -> Unit),
    var type: Boolean = true
) {
    fun name(): String {
        return (if (type) "[green]" else "[red]") + name(type)
    }

    fun active() {
        active(type)
    }

    fun new(): Mutator {
        return Mutator(name = name, effect = effect)
    }

    fun setType(boolean: Boolean): Mutator {
        type = boolean
        return this
    }

    private fun name(type: Boolean): String {
        return name.invoke(type)
    }

    private fun active(type: Boolean) {
        effect.invoke(type)
    }
}

val mutatorList: List<Mutator> = listOf(
    Mutator(fun(type: Boolean): String { return "单位升级需求指数 ${(if (type) "-0.05" else "+0.04")}" },
        fun(type: Boolean) { unitExpE += if (type) -0.05f else 0.04f }),
    Mutator(fun(type: Boolean): String { return "单位经验需求倍率 ${(if (type) "÷1.2" else "×1.15")}" },
        fun(type: Boolean) { unitExpMultiplier *= if (type) 1 / 1.2f else 1.15f }),
    Mutator(fun(type: Boolean): String { return "玩家单位初始经验 ${(if (type) "×1.5" else "÷1.4")}" },
        fun(type: Boolean) { unitInitExpMultiplier *= if (type) 1.5f else 1 / 1.4f }),
    Mutator(fun(type: Boolean): String { return "友方单位血量 ${(if (type) "×1.25" else "÷1.2")}" },
        fun(type: Boolean) { state.rules.defaultTeam.rules().unitHealthMultiplier *= if (type) 1.25f else 1 / 1.2f }),
    Mutator(fun(type: Boolean): String { return "友方单位伤害 ${(if (type) "×1.2" else "÷1.15")}" },
        fun(type: Boolean) { state.rules.defaultTeam.rules().unitDamageMultiplier *= if (type) 1.2f else 1 / 1.15f }),
    Mutator(fun(type: Boolean): String { return "单位购买花费 ${(if (type) "÷1.5" else "×1.4")}" },
        fun(type: Boolean) { unitCostMultiplier *= if (type) 1 / 1.5f else 1.4f }),
    Mutator(fun(type: Boolean): String { return "友方建筑血量 ${(if (type) "×1.5" else "÷1.4")}" },
        fun(type: Boolean) { state.rules.defaultTeam.rules().blockHealthMultiplier *= if (type) 1.5f else 1 / 1.4f }),
    Mutator(fun(type: Boolean): String { return "核心升级花费 ${(if (type) "÷1.5" else "×1.4")} " },
        fun(type: Boolean) { fortUpgradeCostMultiplier *= if (type) 1 / 1.5f else 1.4f }),
    Mutator(fun(type: Boolean): String { return "畸变枢纽选项 ${(if (type) "+" else "-")} 1" },
        fun(type: Boolean) { mutatorChoice += if (type) 1 else -1 })
)

class WorldMutator(
    val curMutator: MutableList<Mutator> = mutableListOf<Mutator>(),
    val selectMutator: MutableList<List<Mutator>> = mutableListOf(listOf<Mutator>()),
    var mutatorCount: Int = 0
) {
    fun hasMutator(): Boolean {
        return mutatorChoice > 0 || selectMutator.isEmpty()
    }

    fun randomMutator() {
        if (mutatorCount == 0) return
        for (i in 1..mutatorChoice) {
            selectMutator.add(
                listOf(
                    mutatorList.random().new().setType(true),
                    mutatorList.random().new().setType(false)
                )
            )
        }
        mutatorCount -= 1
    }

    fun spawnMutator() {
        val tile = getSpawnTiles()
        tile.setNet(Blocks.interplanetaryAccelerator, state.rules.waveTeam, 0)
        tile.build.health = 2000f * worldDifficult.level
        broadcast(
            "[yellow]发现新的世界畸变枢纽！摧毁以选择世界畸变  [cyan]<Gather>[white](${tile.x},${tile.y})".with(),
            quite = true
        )
        core2label.getOrPut(tile.build) {
            WorldLabel.create().apply {
                set(tile.build)
                snapInterpolation()
                fontSize = 2f
                add()
                core2label[tile.build] = this
                text = "[red]世界畸变枢纽（已污染）\n 血量：${tile.build.health}"
            }
        }
    }
}

val worldMutator by autoInit { WorldMutator() }

class MutatorMenu(private val player: Player) : MenuBuilder<Unit>() {
    var showCur: Boolean = false
    fun sendTo() {
        launch(Dispatchers.game) {
            sendTo(player, 60_000)
        }
    }

    override suspend fun build() {
        title = "畸变枢纽-在此选择和查看世界畸变"
        msg = buildString {
            append("当前世界畸变\n")
            worldMutator.curMutator.forEach {
                append(it.name())
                appendLine()
            }
        }
        worldMutator.randomMutator()
        if (worldMutator.hasMutator()) {
            worldMutator.selectMutator.forEach {
                if (it.isNotEmpty()) {
                    option("获得 ${it.first().name()}\n[white]但是 ${it.last().name()}") {
                        worldMutator.curMutator.add(it.first())
                        worldMutator.curMutator.add(it.last())
                        it.first().active()
                        it.last().active()
                        worldMutator.selectMutator.clear()
                        refresh()
                    }
                    newRow()
                }
            }
        } else {
            option("当前已无畸变点") {}
        }
        option("[white]退出畸变枢纽") { }
    }
}

listen<EventType.BlockDestroyEvent> {
    if (it.tile.build.team == state.rules.waveTeam) {
        when (it.tile.build) {
            is CoreBuild -> (it.tile.build as CoreBuild).nestData()?.bonus?.effect(
                (it.tile.build as CoreBuild).lastDamage,
                it.tile
            )

            is AcceleratorBuild -> {
                launch(Dispatchers.game) {
                    delay(2000)
                    it.tile.setNet(Blocks.interplanetaryAccelerator, state.rules.defaultTeam, 0)
                    worldMutator.mutatorCount += 1
                    worldMutator.randomMutator()
                    core2label.getOrPut(it.tile.build) {
                        WorldLabel.create().apply {
                            set(it.tile.build)
                            snapInterpolation()
                            fontSize = 2f
                            add()
                            core2label[it.tile.build] = this
                            text = "世界畸变枢纽"
                        }
                    }
                    broadcast(
                        "[yellow]已占领畸变枢纽！畸变点+1，点击以选择世界畸变  [cyan]<Gather>[white](${it.tile.x},${it.tile.y})".with(),
                        quite = true
                    )
                }
            }
        }
    }
}

onEnable {
    contextScript<coreMindustry.ContentsTweaker>().addPatch("100041", contentPatch)
    state.rules.apply {
        canGameOver = false
        defaultTeam.rules().cheat = true
        waveTeam.rules().cheat = true
        deconstructRefundMultiplier = 0f
    }
    setRules()
    state.rules.defaultTeam.cores().forEach { it.tile.setNet(Blocks.air) }

    val landedTile = tiles.random()
    landedTile.setNet(Blocks.coreShard, state.rules.defaultTeam, 0)

    launch(Dispatchers.game) {
        state.rules.apply {
            canGameOver = true
        }
        setRules()
        delay(500)
    }

    //时间和模式显示
    loop(Dispatchers.game) {
        state.rules.modeName = worldTime.timeString()
        if (!debugMode) worldTime.time += timeSpd

        state.rules.lighting = true
        state.rules.ambientLight.a = worldTime.lights()

        worldTime.transTimeType()

        state.rules.ambientLight.r = 0.01f
        setRules()
        delay(1000)
    }

    //科技与世界难度
    loop(Dispatchers.game) {
        tech.update()
        worldDifficult.update()
        delay(1000)
    }

    //天数显示
    loop(Dispatchers.game) {
        Groups.player.forEach {
            Call.setHudText(it.con, buildString {
                appendLine("时间 - ${worldTime.timeString()} ${worldTime.curTimeType.name}[white]")
                append("${norm.difficult} - ${worldDifficult.name()}")
                if (!it.unit().spawnedByCore && !it.dead()) {
                    appendLine()
                    appendLine(
                        "[white]${it.unit().type.emoji()} LV.${it.unit().data.level} ${
                            it.unit().data.exp.buildLineBar(
                                10,
                                it.unit().data.nextLevelNeed()
                            )
                        } [white]${it.unit().data.exp.format(1)}/${it.unit().data.nextLevelNeed().format(1)}"
                    )
                    append(
                        "[green]${Iconc.add} ${
                            it.unit().health.buildLineBar(
                                10,
                                it.unit().type.health
                            )
                        } [white]${it.unit().health.format(1)}/${it.unit().maxHealth.format(1)} ${
                            it.unit().statuses().joinToString("") { it.effect.emoji() }
                        }"
                    )
                }
            })
        }
        delay(100)
    }

    //核心信息版
    loop(Dispatchers.game) {
        state.rules.defaultTeam.cores().forEach {
            val label = core2label.getOrPut(it) {
                WorldLabel.create().apply {
                    set(it)
                    snapInterpolation()
                    fontSize = 2f
                    add()
                    core2label[it] = this
                }
            }
            label.apply {
                text = "[#${it.team.color}]" + it.fortType().name
            }
        }
        state.rules.waveTeam.cores().forEach {
            val label = core2label.getOrPut(it) {
                WorldLabel.create().apply {
                    set(it)
                    snapInterpolation()
                    fontSize = 2f
                    add()
                    core2label[it] = this
                }
            }
            label.apply {
                text = "[#${it.team.color}]" + (it.nestData()?.getLabel() ?: "")
            }
        }
        val need2Remove = core2label.filter { !it.key.isValid }
        need2Remove.forEach { (t, u) ->
            core2label.remove(t, u)
            Call.removeWorldLabel(u.id)
            u.remove()
        }
        delay(1000)
    }

    //单位挖矿
    loop(Dispatchers.game) {
        Groups.unit.filter { it.mining() && it.health >= 0 }.forEach {
            val tiles = buildSet {
                val mineTile = it.mineTile
                add(mineTile)
                Geometry.circle(
                    mineTile.x.toInt(),
                    mineTile.y.toInt(),
                    world.width(),
                    world.height(),
                    tech.mineTier.tier
                ) { x, y ->
                    add(world.tile(x, y))
                }
            }

            tiles.forEach { tile ->
                if (it.health < 0) return@forEach
                if (tile.drop() != null && tile.drop() != Items.sand) {
                    val overlay = tile.overlay()
                    if (tile.drop().hardness <= it.type.mineTier) {
                        val amount = (Random.nextInt(
                            5,
                            10
                        ) / (tile.drop().hardness - (it.type.mineTier - 1).coerceAtLeast(0)).toFloat()
                            .coerceAtLeast(0.5f)).toInt()
                        val mineEff = 0.75.pow(tech.mineTier.tier)
                        it.health -= (amount * tile.drop().hardness * mineEff).toFloat()
                        if (Random.nextFloat() < mineEff) it.addEffect(StatusEffects.muddy, amount * 20f)
                        launch(Dispatchers.game) {
                            Call.effect(Fx.unitEnvKill, tile.worldx(), tile.worldy(), 0f, Color.red)
                            repeat(log2(amount.toFloat()).toInt()) {
                                val core =
                                    Geometry.findClosest(
                                        tile.worldx(),
                                        tile.worldy(),
                                        state.rules.defaultTeam.cores()
                                    )
                                Call.effect(
                                    Fx.itemTransfer,
                                    tile.worldx(),
                                    tile.worldy(),
                                    0f,
                                    Color.yellow,
                                    core
                                )
                                delay(100)
                            }
                        }
                        state.rules.defaultTeam.core().items.add(
                            tile.drop(),
                            (amount * 1.2f.pow(tech.mineEffTier.tier)).toInt()
                        )
                        launch(Dispatchers.game) {
                            tile.setOverlayNet(Blocks.pebbles)
                            delay((600_000 * 0.9f.pow(tech.mineEffTier.tier)).toLong())
                            tile.setOverlayNet(overlay)
                        }
                    }
                }
            }
        }
        yield()
    }

    //核心单位离核处理
    loop(Dispatchers.game) {
        if (worldTime.curTimeType.friendly != 0) {
            Groups.unit.filter { it.team == state.rules.defaultTeam }.forEach {
                if (worldTime.curTimeType.friendly > 0) it.apply(worldTime.curTimeType.buffFriendly, 5f * 60f)
                else if (!it.closestCore().within(it, state.rules.enemyCoreBuildRadius) && it.data.level < 20) {
                    it.health += it.maxHealth * 0.1f * worldTime.curTimeType.friendly
                    it.apply(StatusEffects.electrified, 5f * 60f)
                    if (it.isFlying) it.apply(StatusEffects.disarmed, 5f * 60f)
                    if (it.controller() is Player) Call.effect(Fx.unitEnvKill, it.x, it.y, 0f, Color.red)

                }
            }
        }
        delay(5000)
    }

    //玩家控制单位获得buff
    loop(Dispatchers.game) {
        Groups.player.filter { it.team() == state.rules.defaultTeam }.forEach {
            if (it.unit().spawnedByCore) return@forEach
            it.unit().addEffect(StatusEffects.overclock, 5*60f)
            it.unit().addEffect(StatusEffects.overclock, 5*60f)
        }
        delay(5000)
    }

    //单位维修
    loop(Dispatchers.game) {
        Groups.unit.filter { it.team == state.rules.defaultTeam }.forEach {
            it.health =
                (it.health + it.maxHealth * (tech.unitRepairTier.tier / 50f)).coerceAtMost(it.maxHealth * (tech.unitRepairTier.tier / 5f + 1))
            it.shield =
                (it.shield + it.maxHealth * (tech.unitRepairTier.tier / 50f)).coerceAtMost(it.maxHealth * (tech.unitRepairTier.tier / 5f + 1))
        }
        delay(1000)
    }

    //核心炮台
    loop(Dispatchers.game) {
        state.rules.defaultTeam.cores().forEach {
            val bullet = it.fortData().fortType.bulletType
            val e = Units.closestEnemy(it.team, it.x, it.y, state.rules.enemyCoreBuildRadius / 2f) { true }
            if (e != null) {
                Call.createBullet(bullet, it.team, it.x, it.y, it.angleTo(e), bullet.damage * 0.5f, 1f, 2f)
            }
            it.health += 75 * it.fortData().tier()
        }
        delay((tech.turretsTier.maxTier - tech.turretsTier.tier / 2) * 500L)
    }

    //生成核心，默认平均5分钟一次
    /*
    loop(Dispatchers.game) {
        delay(3000)
        if (worldTime.curTimeType.friendly <= 0 && Random.nextFloat() < 0.001 * globalMultiplier() * (maxNewFort - Team.derelict.cores().size)) {
            spawnFort()
        }
    }*/

    //生成敌怪及巢穴
    loop(Dispatchers.game) {
        delay(500)
        if (state.isPaused) return@loop
        if (Random.nextFloat() > 0.1f * globalMultiplier() * (1 - worldTime.curTimeType.friendly)) return@loop
        val tile = getSpawnTiles()
        when (Random.nextInt(100)) {
            in 0..98 -> {
                worldDifficult.spawnUnit(worldDifficult.level, tile)
                Call.effect(Fx.greenBomb, tile.worldx(), tile.worldy(), 0f, state.rules.waveTeam.color)
            }

            else -> {
                tile.setNet(randomNest(), state.rules.waveTeam, 0)
                broadcast(
                    "[orange]在[${tile.x},${tile.y}]处发现感染核心！感染核心会生成高級敌人，摧毁可获得奖励！  [red]<Attack>[white](${tile.x},${tile.y})".with(),
                    quite = true
                )
            }
        }
        state.rules.waveTeam.cores().forEach {
            if (Random.nextFloat() < 0.012f) it.nestData()?.spawnUnit(it.tile)
        }

    }

    //单位升级
    loop(Dispatchers.game) {
        Groups.unit.forEach {
            if (it.speedMultiplier > maxSpeedMultiplier && worldTime.curTimeType.friendly <= 0) it.addEffect(StatusEffects.sporeSlowed)
            if (it.data.exp >= it.data.nextLevelNeed()) {
                it.data.exp -= it.data.nextLevelNeed()
                it.data.level++

                /* 导致单位过于难死，移除
                if (it.data.level % 10 == 9)
                    it.addEffect(StatusEffects.shielded, Float.POSITIVE_INFINITY)
                */

                when (it.data.level % 2) {
                    0 -> {
                        it.addEffect(listOf(StatusEffects.overdrive, StatusEffects.overclock, StatusEffects.boss).random())
                    }
                    1 -> {
                        it.maxHealth *= 1.2f
                        it.health *= 1.2f
                    }
                }
            }
        }
        yield()
    }
}

class CoreMenu(private val player: Player, private val core: CoreBuild) : MenuBuilder<Unit>() {
    var tab: Int = 0
    lateinit var unitType: UnitType

    fun sendTo() {
        launch(Dispatchers.game) {
            sendTo(player, 60_000)
        }
    }

    suspend fun mainMenu() {
        val next = core.fortData().nextFortType()
        msg = buildString {
            if (!core.fortData().maxTier()) {
                appendLine("${core.block.emoji()}${core.fortType().name} -> ${next.block.emoji()}${next.name}")
                next.block.requirements.forEach {
                    appendLine("[white]${it.item.emoji()} ${if (core.items[it.item] >= it.amount * fortUpgradeCostMultiplier) "[green]" else "[lightgray]"}${core.items[it.item]}/${(it.amount * fortUpgradeCostMultiplier).toInt()}")
                }
            }
            append("[yellow]在此进行升级据点,招募兵种等工作")
        }
        lazyOption {
            fun canUpgrade() =
                next.block.requirements.all { core.items[it.item] >= it.amount * fortUpgradeCostMultiplier } && core.isValid && core.team() == player.team() && !core.fortData()
                    .maxTier()
            if (core.fortData().maxTier()) {
                refreshOption("[green]据点已经满级")
            } else if (!canUpgrade()) {
                refreshOption("[lightgray]据点升级资源不足")
            } else {
                option("升级至 ${next.block.emoji()}${next.name}")
                if (canUpgrade()) {
                    next.block.requirements.forEach {
                        core.items.remove(it.item, (it.amount * fortUpgradeCostMultiplier).toInt())
                    }
                    world.tile(1, 1).setNet(Blocks.coreShard, core.team(), 0)
                    core.tile.setNet(next.block, core.team(), 0)
                    world.tile(1, 1).setNet(Blocks.air, core.team(), 0)
                    broadcast(
                        "[acid]位于[${core.tileX()},${core.tileY()}]的核心已由${player.name()}[acid]升级！！  [#eab678]<Mark>[white](${core.tileX()},${core.tileY()})".with(),
                        quite = true
                    )
                }
            }
        }
        newRow()
        option("招募兵种") {
            tab = 1; refresh()
        }
        newRow()
        option("研究科技") {
            tab = 3; refresh()
        }
        newRow()
        option("[cyan]更新说明") {
            tab = 4; refresh()
        }
        newRow()
        option("[acid]当前状态") {
            tab = 5; refresh()
        }
        if (player.admin){
            newRow()
            option("[red] DEBUGMODE") {
                debugMode = !debugMode
                refresh()
            }
        }

        if (debugMode) {
            newRow()
            option("[red] DEBUG ${norm.difficult}+1") {
                worldDifficult.level += 1
                refresh()
            }
            newRow()
            option("[red] DEBUG 时间+2000") {
                worldTime.time += 2000
                refresh()
            }
            newRow()
            option("[red] DEBUG 感染巢") {
                val tile = getSpawnTiles()
                tile.setNet(randomNest(), state.rules.waveTeam, 0)
                broadcast(
                    "[orange]在[${tile.x},${tile.y}]处发现感染核心！感染核心会持续生成敌人，可摧毁获得物质！  [red]<Attack>[white](${tile.x},${tile.y})".with(),
                    quite = true
                )
                refresh()
            }
            newRow()
            option("[red] DEBUG 畸变枢纽") {
                worldMutator.spawnMutator()
                refresh()
            }
        }
    }

    suspend fun UnitType.shop() {
        val u = this
        val uc = unitsWithCost.find { it.first == u }
        if (uc != null) {
            fun enough() = uc.second.all { core.items[it.first] >= it.second * unitCostMultiplier }
            msg = buildString {
                uc.second.forEach {
                    appendLine("[white]${it.first.emoji()} ${if (enough()) "[green]" else "[lightgray]"}${core.items[it.first]}/${it.second}")
                }
                append(uc.first.emoji().repeat(5))
            }
            lazyOption {
                fun canSpawn() = enough() && core.isValid && core.team == player.team()
                if (!enough()) {
                    refreshOption("[lightgray]资源不足")
                } else if (!canSpawn()) {
                    refreshOption("[lightgray]核心丢失")
                } else if (Groups.unit.count { it.team == state.rules.defaultTeam && it.type == u } > state.rules.defaultTeam.data().unitCap) {
                    refreshOption("[lightgray]单位已满")
                } else {
                    option("[green]招募！")
                    val unit = spawnAround(core, core.team)
                    if (unit != null) unit.data.exp += (2f.pow(tech.moreExpInitTier.tier)) * (1.2f.pow(tech.moreExpTier.tier)) * unitInitExpMultiplier
                    uc.second.forEach {
                        core.items.remove(it.first, (it.second * unitCostMultiplier).toInt())
                    }
                    refresh()
                }
            }
            newRow()
        }
        option("返回募兵界面") {
            tab = 1; refresh()
        }
    }

    fun unitShop() {
        msg = "[yellow]在此进行招募兵种,随据点等级解锁"
        repeat(6) {
            if (core.fortData().tier() >= it) {
                unitsWithTier[it].forEach {
                    option(it.first.emoji()) {
                        tab = 2; unitType = it.first; refresh()
                    }
                }
                newRow()
            }
        }
        option("返回主菜单") {
            tab = 0; refresh()
        }
    }

    fun techMenu() {
        msg =
            "[yellow]在此研发科技,逃离星球需要满级核心\n[cyan]科技点: ${tech.exp} [white]+[acid]${tech.techIncreased()}/s" +
                    "\n[yellow]科技点增长速度与据点数量与等级有关"
        tech.techList.forEach {
            option(tech.buttonName(it)) {
                tech.research(it)
                refresh()
            }

            option(tech.msg(it)) {
                tech.research(it)
                refresh()
            }
            newRow()
        }
        if (core.fortData().maxTier()) {
            option("${if (tech.exp >= finalTechCost) "[green]" else "[lightgray]"}最终科技-无尽深渊\n${finalTechCost}科技点\n[red]召唤最终boss，击败以获得胜利") {
                if (tech.exp >= finalTechCost) {
                    bossUnit.spawnBoss()
                    tech.exp -= finalTechCost
                }
                refresh()
            }
            newRow()
        }
        if (debugMode) {
            option("[red] DEBUG [white]科技点+100000") {
                tech.exp += 100000
                refresh()
            }
            newRow()
        }
        option("返回主菜单") {
            tab = 0; refresh()
        }
    }

    fun playInfo() {
        msg = "${norm.mode}更新说明" +
                "\n11/22 更新最终boss机制，击败后通关（但不会游戏结束，奖励一个T4）" +
                "\n移除保护和迅捷等级奖励，调整经验曲线" +
                "\n[cyan]大改出怪机制：\n[white]每个难度为一个级别，每个级别共3级" +
                "\n敌军分为普通(最高T3+quell)和精英(T4T5级)，各分为若干级" +
                "\n前中期只生成对应级别的普通敌人，生成时会随机降低敌人级别但给予经验值和盾补偿" +
                "\n超过普通敌人最大级别时，有低概率生成精英敌人，且难度越高概率越大(1.5%*越级，最大20%)" +
                "\n如果roll失败，会生成带高盾和经验的普通敌人" +
                "\n如果roll成功，再roll一次是否为T4/T5，同样难度越高概率越大(1.5%*(越级-5)，最大20%)" +
                "\n随后随机生成一个T4/T5级单位，难度越高，单位经验和盾越高" +
                "\n如果发现难度过于简单或困难，请及时反馈!"
        option("返回主菜单") {
            tab = 0; refresh()
        }
    }

    fun status() {
        msg = "${norm.mode}当前状态~DUBUG" +
                "\n[acid]全局系数[white] ~ ${globalMultiplier()}" +
                "\n[acid]游戏难度[white] ~ ${worldDifficult.level}" +
                "\n[acid]游戏难度等级[white] ~ ${worldDifficult.tier()}" +
                "\n[acid]玩家最高等级[white] ~ ${state.rules.defaultTeam.maxUnitLevel()}"
        option("返回主菜单") {
            tab = 0; refresh()
        }
    }

    override suspend fun build() {
        title = core.fortData().fortType.name
        when (tab) {
            0 -> mainMenu()
            1 -> unitShop()
            2 -> unitType.shop()
            3 -> techMenu()
            4 -> playInfo()
            5 -> status()
        }
        newRow()
        option("[white]退出菜单") { }
    }
}

listen<EventType.TapEvent> {
    if (it.tile.team() == it.player.team()) {
        when (it.tile.build) {
            is CoreBuild -> CoreMenu(it.player, it.tile.build as CoreBuild).sendTo()
            is AcceleratorBuild -> MutatorMenu(it.player).sendTo()
        }
    }
}

listen<EventType.UnitBulletDestroyEvent> {
    if (bossUnit.spawned && it.unit == bossUnit.bossUnits){
        broadcast(
            "[orange]你已击败最终boss！！救援已到达，你可以随时逃离此星球（请自行换图结束） [cyan]同时奖励一个15级T4单位 [#eab678]<Mark>[white](${it.unit.tileX()},${it.unit.tileY()})".with(),
            quite = true
        )
        Call.effect(Fx.impactReactorExplosion, it.unit.x, it.unit.y, 0f, Color.red)
        var owner = (it.bullet.owner() as? mindustry.gen.Unit) ?: return@listen
        (owner.controller() as? MissileAI).let {//导弹
            owner = (it ?: return@let).shooter ?: return@listen
        }
        T4Enemy.random().spawnAround(it.unit.tileOn(), owner.team)?.apply { addLevel(15) }
        if (owner.spawnedByCore) return@listen
        owner.addLevel(5)
        return@listen
    }

    var owner = (it.bullet.owner() as? mindustry.gen.Unit) ?: return@listen
    (owner.controller() as? MissileAI).let {//导弹
        owner = (it ?: return@let).shooter ?: return@listen
    }
    if (owner.spawnedByCore) return@listen
    owner.data.exp += it.unit.maxHealth / 5f *
            it.unit.healthMultiplier *
            1.2f.pow(tech.moreExpTier.tier) *
            1.2f.pow((it.unit.data.level - owner.data.level).coerceAtLeast(0))
}
