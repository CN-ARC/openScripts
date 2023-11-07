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
    "昼夜交替", "${norm.mode}玩法介绍 \n[acid]By xkldklp&Lucky Clover&blac[]" +
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
      "health": 1000,
      "mineTier": 4,
      "mineRange": 200,
      "weapons.0.bullet.damage": 50,
      "mineSpeed": 0
    },
    "oct": {
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
    "core-shard": {
      "unitType": "alpha",
      "unitCapModifier": 5,
      "itemCapacity": 10000000,
      "requirements": [
        "scrap/10000"
      ],
    },
    "core-foundation": {
      "unitType": "mono",
      "unitCapModifier": 8,
      "itemCapacity": 10000000,
      "requirements": [
        "scrap/1000"
      ],
    },
    "core-bastion": {
      "unitType": "poly",
      "unitCapModifier": 11,
      "itemCapacity": 10000000,
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

val core2label by autoInit { mutableMapOf<CoreBuild, WorldLabel>() }

/** 全局各种资源、难度系数 */
fun globalMultiplier(): Float {
    return Groups.player.size() * 0.33f + 1
}

var timeSpd: Int = 180 //控制时间流逝速度,和现实时间的比值

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
        spawnFort()
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

val debugMode: Boolean = false

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
        UnitTypes.retusa to listOf(Items.scrap to 200),
    ),
    listOf(
        UnitTypes.stell to listOf(Items.copper to 100, Items.lead to 50),
        UnitTypes.elude to listOf(Items.copper to 50, Items.lead to 50),
        UnitTypes.risso to listOf(Items.copper to 200, Items.lead to 200),
        UnitTypes.flare to listOf(Items.copper to 200, Items.lead to 200)
    ),
    listOf(
        UnitTypes.mace to listOf(Items.copper to 100, Items.lead to 50, Items.coal to 20),
        UnitTypes.pulsar to listOf(Items.copper to 50, Items.lead to 100, Items.coal to 40),
        UnitTypes.oxynoe to listOf(Items.copper to 350, Items.lead to 350, Items.coal to 200),
        UnitTypes.horizon to listOf(Items.copper to 350, Items.lead to 350, Items.coal to 200)
    ),
    listOf(
        UnitTypes.cleroi to listOf(Items.copper to 200, Items.lead to 100, Items.beryllium to 150),
        UnitTypes.locus to listOf(Items.copper to 100, Items.lead to 200, Items.beryllium to 150),
        UnitTypes.minke to listOf(Items.copper to 200, Items.lead to 200, Items.titanium to 100),
        UnitTypes.avert to listOf(Items.copper to 200, Items.lead to 200, Items.titanium to 100)
    ),
    listOf(
        UnitTypes.fortress to listOf(
            Items.copper to 300,
            Items.lead to 150,
            Items.titanium to 250,
            Items.thorium to 100
        ),
        UnitTypes.quasar to listOf(Items.copper to 150, Items.lead to 300, Items.titanium to 150, Items.thorium to 100),
        UnitTypes.bryde to listOf(Items.copper to 600, Items.lead to 600, Items.titanium to 400, Items.thorium to 250),
        UnitTypes.zenith to listOf(Items.copper to 600, Items.lead to 600, Items.titanium to 400, Items.thorium to 250)
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
val unitExpE: Float = 1.7f

data class UnitData(
    var exp: Float = 0f,
    var level: Int = 0
) {
    lateinit var unit: mindustry.gen.Unit

    fun nextLevelNeed(): Float {
        return levelNeed(level)
    }

    fun levelNeed(l: Int): Float {
        return (l + 1f).pow(unitExpE) * (unit.type.health).pow(0.5f) * 5
    }
}

val unitData by autoInit { mutableMapOf<mindustry.gen.Unit, UnitData>() }
val mindustry.gen.Unit.data get() = unitData.getOrPut(this) { UnitData() }.also { it.unit = this }

fun spawnSkillUnit(tile: Tile, unitP: Pair<UnitType, Float>) {
    val unit = unitP.first.spawnAround(tile, state.rules.waveTeam)
    if (unit != null) {
        unit.data.exp = unitP.second
        Call.effect(Fx.greenBomb, tile.worldx(), tile.worldy(), 0f, state.rules.waveTeam.color)
    }
}

val enemyTier = listOf(
    listOf(UnitTypes.dagger, UnitTypes.nova),
    listOf(UnitTypes.stell, UnitTypes.elude, UnitTypes.flare),
    listOf(UnitTypes.crawler, UnitTypes.retusa, UnitTypes.risso),
    listOf(UnitTypes.mace, UnitTypes.pulsar, UnitTypes.atrax),
    listOf(UnitTypes.horizon, UnitTypes.avert, UnitTypes.oxynoe),
    listOf(UnitTypes.cleroi, UnitTypes.locus),
    listOf(UnitTypes.fortress, UnitTypes.quasar, UnitTypes.spiroct, UnitTypes.zenith),
    listOf(UnitTypes.obviate, UnitTypes.bryde, UnitTypes.cyerce),
    listOf(UnitTypes.minke, UnitTypes.precept, UnitTypes.anthicus, UnitTypes.obviate),
    listOf(UnitTypes.scepter, UnitTypes.vela, UnitTypes.arkyid),
    listOf(UnitTypes.vanquish, UnitTypes.tecta, UnitTypes.quell),
    listOf(UnitTypes.antumbra, UnitTypes.sei, UnitTypes.aegires),
    listOf(UnitTypes.reign, UnitTypes.corvus, UnitTypes.toxopid),
    listOf(UnitTypes.conquer, UnitTypes.collaris, UnitTypes.disrupt),
    listOf(UnitTypes.eclipse, UnitTypes.omura, UnitTypes.navanax)
)

val difficultTier = listOf(
    "和平" to "[#66ff33]", "简单" to "[#99ff33]", "一般" to "[#ccff33]",
    "进阶" to "[#ffff00]", "稍难" to "[#ffcc00]", "困难" to "[#ff9933]",
    "硬核" to "[#ff6600]", "专家" to "[#ff5050]", "大师" to "[#cc0066]",
    "噩梦" to "[#6600cc]", "疯狂" to "[#9900cc]", "不可能" to "[#660066]",
    "我看到你了" to "[#660033]", "我来找你了" to "[#800000]", "无尽灾厄" to "[#000000]"
)

val difficultRate: Float = 0.005f
val levelPerTier: Int = 3
val expPerTier: Int = 1000

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

    fun ltier(add: Int): Int {
        return (level / levelPerTier + add).toInt().coerceAtMost(difficultTier.size - 1)
    }

    fun process(): Int {
        return ((level % 1) * 100).toInt()
    }

    fun name(): String {
        return "${difficultTier[tier()].second}${difficultTier[tier()].first} ${"I".repeat(subTier() + 1)}  [white]~ ${difficultTier[tier()].second}${process()}%"
    }

    fun randomUnit(Tier: Int): UnitType {
        return enemyTier[Tier].random()
    }

    /**
     * 普通级单位生成
     * 生成subTier个tier单位
     * 随机：subTier ± 1
     * 每次生成单位时，有概率掉一Tier，但单位获得expPerTier
     * */
    fun normalUnit(): List<Pair<UnitType, Float>> {
        val spawn: MutableList<Pair<UnitType, Float>> = mutableListOf()
        for (st in 0..(subTier() + Random.nextInt(1, 2))) {
            var unitTier = tier()
            var exp = Random.nextFloat() * level.pow(unitExpE) * 20f
            while (unitTier > 0) {
                if (Random.nextFloat() < 0.6) {
                    unitTier -= 1
                    exp += expPerTier
                } else break
            }
            spawn.add(randomUnit(unitTier) to exp)
        }
        return spawn
    }

    /**
     * 大型单位生成
     * 生成1个高3 * ltier级的单位
     * */
    fun eliteUnit(ltier: Int): Pair<UnitType, Float> {
        return randomUnit(ltier(ltier)) to Random.nextFloat() * (level + ltier * 3).pow(unitExpE) * 20f
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

class Production(
    var name: String,
    var tier: Int = 0,
    var maxTier: Int = 10
)

val resTier = listOf(
    listOf(Items.scrap),
    listOf(Items.copper, Items.lead),
    listOf(Items.coal),
    listOf(Items.titanium, Items.beryllium),
    listOf(Items.thorium)
)

val blockTier = listOf(
    listOf(Blocks.repairPoint, Blocks.shockMine),
    listOf(Blocks.wave, Blocks.liquidSource, Blocks.segment, Blocks.berylliumWallLarge),
    listOf(Blocks.repairTurret, Blocks.parallax, Blocks.regenProjector),
    listOf(Blocks.tsunami, Blocks.shockwaveTower, Blocks.forceProjector),
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
    return nestTypes[(Random.nextFloat() * worldDifficult.level / levelPerTier).toInt()].block
}

data class NestData(
    var nestType: NestType,
    var tier: Int = 0,
    var bonus: Bonus = Bonus()
) {
    fun mainTier(): Int {
        return tier / levelPerTier
    }

    fun init() {
        tier = nestTypes.indexOf(nestType) * levelPerTier * 2 + Random.nextInt(3)
        bonus.randomBonus(tier)
    }

    fun getLabel(): String {
        return "${nestType.name} Lv${tier} - [cyan]破坏奖励[white]\n ${bonus.label()}"
    }

    fun spawnUnit(tile: Tile) {
        for (i in 0..mainTier())
            spawnSkillUnit(tile, worldDifficult.eliteUnit(mainTier() / 3 + 1))

    }

}

val nestData by autoInit { mutableMapOf<CoreBuild, NestData?>() }
fun CoreBuild.nestData(): NestData? {
    if (this.team != state.rules.waveTeam) return null
    if (nestData[this] == null) {
        val data = NestData(nestType = this.nestType())
        data.init()
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

listen<EventType.BlockDestroyEvent> {
    if (it.tile.build is CoreBuild && it.tile.build.team == state.rules.waveTeam) {
        (it.tile.build as CoreBuild).nestData()?.bonus?.effect((it.tile.build as CoreBuild).lastDamage, it.tile)
    }
}

var bossUnit: mindustry.gen.Unit? = null
val bossSpawned: Boolean get() = bossUnit?.dead() == false

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

onEnable {
    bossUnit = null

    contextScript<coreMindustry.ContentsTweaker>().addPatch("100041", contentPatch)
    state.rules.apply {
        canGameOver = false
        defaultTeam.rules().cheat = true
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
        worldTime.time += timeSpd

        state.rules.lighting = true
        state.rules.ambientLight.a = worldTime.lights()

        worldTime.transTimeType()

        if (bossSpawned && Groups.unit.count { it.team == state.rules.waveTeam && it.hasEffect(StatusEffects.boss) } >= 1)
            state.rules.ambientLight.r = 0.6f
        else
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
                        if (Random.nextFloat() < mineEff) it.statuses.add(
                            StatusEntry().set(
                                StatusEffects.muddy,
                                amount * 20f
                            )
                        )
                        launch(Dispatchers.game) {
                            Call.effect(Fx.unitEnvKill, tile.worldx(), tile.worldy(), 0f, Color.red)
                            repeat(log2(amount.toFloat()).toInt()) {
                                val core =
                                    Geometry.findClosest(tile.worldx(), tile.worldy(), state.rules.defaultTeam.cores())
                                Call.effect(Fx.itemTransfer, tile.worldx(), tile.worldy(), 0f, Color.yellow, core)
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
                else if (!it.closestCore().within(it, state.rules.enemyCoreBuildRadius)) {
                    it.health += it.maxHealth * 0.1f * worldTime.curTimeType.friendly
                    it.apply(StatusEffects.electrified, 5f * 60f)
                    if (it.isFlying) it.apply(StatusEffects.disarmed, 5f * 60f)
                    if (it.controller() is Player) Call.effect(Fx.unitEnvKill, it.x, it.y, 0f, Color.red)
                }
            }
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
            it.health += 200
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

    //生成敌怪
    loop(Dispatchers.game) {
        delay(500)
        if (state.isPaused) return@loop
        if (Random.nextFloat() > 0.1f * globalMultiplier() * (1 - worldTime.curTimeType.friendly)) return@loop
        val tile = getSpawnTiles()
        when (Random.nextInt(100)) {
            in 0..98 -> {
                worldDifficult.normalUnit().forEach {
                    val unit = it.first.spawnAround(tile, state.rules.waveTeam) ?: return@forEach
                    unit.data.exp = it.second
                    Call.effect(Fx.greenBomb, tile.worldx(), tile.worldy(), 0f, state.rules.waveTeam.color)
                }
            }

            else -> {
                tile.setNet(randomNest(), state.rules.waveTeam, 0)
                broadcast(
                    "[orange]在[${tile.x},${tile.y}]处发现感染核心！感染核心会持续生成敌人，可摧毁获得物质！  [red]<Attack>[white](${tile.x},${tile.y})".with(),
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
            if (it.data.exp >= it.data.nextLevelNeed()) {
                it.data.exp -= it.data.nextLevelNeed()
                it.data.level++

                /**
                 * 1 add maxHealth
                 * 2 apply StatusEffect
                 * 3 --
                 * 4 --
                 * 5 --
                 * 6 --
                 * 7 strong StatusEffect
                 */

                if (it.data.level % 8 == 7) {
                    it.statuses.add(
                        StatusEntry().set(
                            listOf(StatusEffects.fast, StatusEffects.shielded).random(),
                            Float.POSITIVE_INFINITY
                        )
                    )
                } else {
                    when (it.data.level % 2) {
                        0 -> {
                            it.statuses.add(
                                StatusEntry().set(
                                    listOf(
                                        StatusEffects.overdrive,
                                        StatusEffects.overclock,
                                        StatusEffects.boss
                                    ).random(), Float.POSITIVE_INFINITY
                                )
                            )
                        }

                        1 -> {
                            it.maxHealth *= 1.2f
                            it.health *= 1.2f
                        }
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
                    appendLine("[white]${it.item.emoji()} ${if (core.items[it.item] >= it.amount) "[green]" else "[lightgray]"}${core.items[it.item]}/${it.amount}")
                }
            }
            append("[yellow]在此进行升级据点,招募兵种等工作")
        }
        lazyOption {
            fun canUpgrade() =
                next.block.requirements.all { core.items[it.item] >= it.amount } && core.isValid && core.team() == player.team() && !core.fortData()
                    .maxTier()
            if (core.fortData().maxTier()) {
                refreshOption("[green]据点已经满级")
            } else if (!canUpgrade()) {
                refreshOption("[lightgray]据点升级资源不足")
            } else {
                option("升级至 ${next.block.emoji()}${next.name}")
                if (canUpgrade()) {
                    next.block.requirements.forEach {
                        core.items.remove(it.item, it.amount)
                    }
                    world.tile(1, 1).setNet(Blocks.coreShard, core.team(), 0)
                    core.tile.setNet(next.block, core.team(), 0)
                    world.tile(1, 1).setNet(Blocks.air, core.team(), 0)
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
        /*
        newRow()
        option("玩法介绍") {
            tab = 4; refresh()
        }*/

        if (debugMode) {
            newRow()
            option("[red] DEBUG ${norm.difficult}+1") {
                worldDifficult.level += 1
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
        }
    }

    suspend fun UnitType.shop() {
        val u = this
        val uc = unitsWithCost.find { it.first == u }
        if (uc != null) {
            fun enough() = uc.second.all { core.items[it.first] >= it.second }
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
                    if (unit != null) unit.data.exp += (1.5f.pow(tech.moreExpInitTier.tier)) * (1.2f.pow(tech.moreExpTier.tier)) * 10
                    uc.second.forEach {
                        core.items.remove(it.first, it.second)
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
                tech.research(it);
                refresh()
            }

            option(tech.msg(it)) {
                tech.research(it);
                refresh()
            }
            newRow()
        }

        if (core.fortData().maxTier()) {
            val finalTechCost = 99999
            option("${if (tech.exp >= finalTechCost) "[green]" else "[lightgray]"}最终科技-重启跃迁\n${finalTechCost}科技点") {
                if (tech.exp >= finalTechCost) {
                    state.gameOver = true
                    Events.fire(EventType.GameOverEvent(state.rules.defaultTeam))
                    launch(Dispatchers.IO) {
                        Groups.player.forEach {
                            it.achievement("[purple][跃迁逃脱]", 200)
                        }
                    }
                }
                refresh()
            }
            newRow()
        }
        if (debugMode) {
            option("[red] DEBUG [white]科技点+100000") {
                tech.exp += 100000
            }
            newRow()
        }
        option("返回主菜单") {
            tab = 0; refresh()
        }
    }

    /*
    fun playInfo() {
        msg = "${norm.mode}玩法介绍 \n[acid]By xkldklp&Lucky Clover&blac[]" +
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
        option("返回主菜单") {
            tab = 0; refresh()
        }
    }*/

    override suspend fun build() {
        title = core.fortData().fortType.name
        when (tab) {
            0 -> mainMenu()
            1 -> unitShop()
            2 -> unitType.shop()
            3 -> techMenu()
        }
        newRow()
        option("[white]退出菜单") { }
    }
}

listen<EventType.TapEvent> {
    if (it.tile.build is CoreBuild && it.tile.team() == it.player.team()) {
        CoreMenu(it.player, it.tile.build as CoreBuild).sendTo()
    }
}

listen<EventType.UnitBulletDestroyEvent> {
    var owner = (it.bullet.owner() as? mindustry.gen.Unit) ?: return@listen
    (owner.controller() as? MissileAI).let {//导弹
        owner = (it ?: return@let).shooter ?: return@listen
    }
    if (owner.spawnedByCore) return@listen
    owner.data.exp += it.unit.maxHealth / 10f *
            it.unit.healthMultiplier *
            1.2f.pow(tech.moreExpTier.tier) *
            1.2f.pow((it.unit.data.level - owner.data.level).coerceAtLeast(0))
}
