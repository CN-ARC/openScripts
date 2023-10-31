@file:Depends("coreMindustry/menu", "调用菜单")
@file:Depends("coreMindustry/contentsTweaker", "修改核心单位,单位属性")
@file:Depends("wayzer/user/achievement", "成就")
@file:Depends("wayzer/voteService", "投票实现")
@file:Import("@coreMindustry/util/spawnAround.kt", sourceFile = true)

package mapScript

import arc.Events
import arc.graphics.Color
import arc.math.geom.Geometry
import arc.struct.*
import arc.util.Time
import coreLibrary.lib.util.loop
import coreLibrary.lib.with
import coreMindustry.MenuBuilder
import coreMindustry.lib.broadcast
import coreMindustry.lib.game
import coreMindustry.lib.listen
import coreMindustry.lib.player
import coreMindustry.util.spawnAround
import coreMindustry.util.spawnAroundLand
import mindustry.Vars
import mindustry.ai.types.MissileAI
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.entities.Units
import mindustry.entities.units.StatusEntry
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.*
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.storage.CoreBlock.CoreBuild
import org.intellij.lang.annotations.Language
import wayzer.VoteService
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
      "armor": 3
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
      "armor": 10
    },
  },
  "block": {
    "core-shard": {
      "unitType": "alpha",
      "itemCapacity": 10000000,
      "requirements": [
        "scrap/10000"
      ],
    },
    "core-foundation": {
      "unitType": "mono",
      "itemCapacity": 10000000,
      "requirements": [
        "scrap/1000"
      ],
    },
    "core-bastion": {
      "unitType": "poly",
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
    Call.setRules(Vars.state.rules)
}

fun Float.format(i: Int = 2): String {
    return "%.${i}f".format(this)
}

val tiles by autoInit { Vars.world.tiles.filter { !it.floor().isLiquid } }
fun getSpawnTiles(): Tile {
    return tiles.filter {
        !it.floor().isDeep && it.passable() && Team.sharded.cores()
            .all { c -> !it.within(c, Vars.state.rules.enemyCoreBuildRadius) }
    }.random()
}

val core2label by autoInit { mutableMapOf<CoreBuild, WorldLabel>() }

class TimeType(
    val name: String,
    val effect: (() -> Unit)? = null
){
    fun active(){
        effect?.invoke()
    }
}

var timeSpd: Int = 400 //控制时间流逝速度,和现实时间的比值
//世界时间
class WorldTime(
    // second，但并不是实际速度
    var time: Int = 12 * 60 * 60,

    var midnight: TimeType = TimeType("午夜",fun(){
        state.rules.waveTeam.rules().unitDamageMultiplier = 1.25f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1.25f
    }),
    var dawn: TimeType = TimeType("黎明",fun(){
        state.rules.waveTeam.rules().unitDamageMultiplier = 1.1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1.1f
    }),
    var morning: TimeType = TimeType("早晨",fun(){
        state.rules.waveTeam.rules().unitDamageMultiplier = 1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1f
    }),
    var midday: TimeType = TimeType("中午",fun(){
        state.rules.waveTeam.rules().unitDamageMultiplier = 0.75f
        state.rules.waveTeam.rules().unitHealthMultiplier = 0.75f
    }),
    var afternoon: TimeType = TimeType("下午",fun(){
        state.rules.waveTeam.rules().unitDamageMultiplier = 1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1f
    }),
    var night: TimeType = TimeType("夜晚",fun(){
        state.rules.waveTeam.rules().unitDamageMultiplier = 1.1f
        state.rules.waveTeam.rules().unitHealthMultiplier = 1.1f
    }),
    ){
    fun getCurTimeType(): TimeType{
        return when (hours()) {
            in 2..6 -> dawn
            in 6..10 -> morning
            in 10..14 -> midday
            in 14..18 -> afternoon
            in 18..22 -> night
            else -> midnight
        }
    }

    fun timeString(): String{
        return "第${days()}天 ${hoursFixed()}:${minutesFixed()}"
    }

    fun minutes(): Int{
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

    fun lights(): Float{
        return abs(0.5 - time % 86400 / 86400f).toFloat() * 2f
    }

}

val worldTime by autoInit { WorldTime() }

val debugMode: Boolean = true

val maxNewFort: Int = 3
fun canSpawnNewFort(): Boolean {
    return Team.crux.cores().size >= maxNewFort
}

fun Block.level(): Int {
    return when (this) {
        Blocks.coreShard -> 1
        Blocks.coreFoundation -> 2
        Blocks.coreBastion -> 3
        Blocks.coreNucleus -> 4
        Blocks.coreCitadel -> 5
        Blocks.coreAcropolis -> 6
        else -> 0
    }
}

fun Block.coreName(): String {
    return when (this) {
        Blocks.coreShard -> "前哨"
        Blocks.coreFoundation -> "卫戍"
        Blocks.coreBastion -> "堡垒"
        Blocks.coreNucleus -> "城市"
        Blocks.coreCitadel -> "省府"
        Blocks.coreAcropolis -> "王都"
        else -> ""
    }
}

fun CoreBuild.coreName(): String {
    return block.coreName()
}


val unitsWithTier = listOf(
    listOf(
        UnitTypes.dagger to listOf(Items.scrap to 20),
        UnitTypes.nova to listOf(Items.scrap to 25),
        UnitTypes.retusa to listOf(Items.scrap to 200)
    ),
    listOf(
        UnitTypes.stell to listOf(Items.copper to 100, Items.lead to 50),
        UnitTypes.elude to listOf(Items.copper to 50, Items.lead to 50),
        UnitTypes.risso to listOf(Items.copper to 200, Items.lead to 200)
    ),
    listOf(
        UnitTypes.mace to listOf(Items.copper to 100, Items.lead to 50, Items.coal to 20),
        UnitTypes.pulsar to listOf(Items.copper to 50, Items.lead to 100, Items.coal to 40),
        UnitTypes.oxynoe to listOf(Items.copper to 350, Items.lead to 350, Items.coal to 200)
    ),
    listOf(
        UnitTypes.cleroi to listOf(Items.copper to 200, Items.lead to 100, Items.titanium to 150),
        UnitTypes.locus to listOf(Items.copper to 100, Items.lead to 200, Items.beryllium to 150),
        UnitTypes.minke to listOf(Items.copper to 200, Items.lead to 200, Items.titanium to 100),
    ),
    listOf(
        UnitTypes.fortress to listOf(Items.copper to 300, Items.lead to 150, Items.titanium to 250, Items.thorium to 100),
        UnitTypes.quasar to listOf(Items.copper to 150, Items.lead to 300, Items.titanium to 150, Items.thorium to 100),
        UnitTypes.bryde to listOf(Items.copper to 600, Items.lead to 600, Items.titanium to 400, Items.thorium to 250)
    ),
    listOf(
        UnitTypes.precept to listOf(Items.copper to 1000, Items.lead to 500, Items.beryllium to 500, Items.thorium to 300),
        UnitTypes.cyerce to listOf(Items.copper to 1500, Items.lead to 1500, Items.titanium to 500, Items.thorium to 350)
        //UnitTypes.anthicus to listOf(Items.copper to 500, Items.lead to 1000, Items.beryllium to 350, Items.thorium to 300)
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
        return (l + 1f).pow(1.7f) * unit.type.health / 10f
    }
}

val unitData by autoInit { mutableMapOf<mindustry.gen.Unit, UnitData>() }
val mindustry.gen.Unit.data get() = unitData.getOrPut(this) { UnitData() }.also { it.unit = this }

//0 to boss
val unitsWithDays by autoInit {
    listOf(
        buildList {
            add(listOf(UnitTypes.dagger to 1, UnitTypes.nova to 1).random())
        },
        buildList {
            add(listOf(UnitTypes.dagger to 3, UnitTypes.nova to 2).random())
            add(listOf(UnitTypes.mace to 1, UnitTypes.pulsar to 1).random())
        },
        buildList {
            add(listOf(UnitTypes.dagger to 2, UnitTypes.nova to 2).random())
            add(listOf(UnitTypes.dagger to 2, UnitTypes.nova to 2).random())
            add(listOf(UnitTypes.mace to 1, UnitTypes.pulsar to 1).random())
            add(listOf(UnitTypes.crawler to 2, UnitTypes.elude to 1).random())
            add(listOf(UnitTypes.fortress to 0, UnitTypes.quasar to 0).random())
        },
        buildList {
            add(listOf(UnitTypes.dagger to 5, UnitTypes.nova to 5).random())
            add(listOf(UnitTypes.mace to 2, UnitTypes.pulsar to 3).random())
            add(listOf(UnitTypes.crawler to 3, UnitTypes.elude to 2).random())
            add(listOf(UnitTypes.fortress to 1, UnitTypes.quasar to 1).random())
            add(listOf(UnitTypes.locus to 0, UnitTypes.cleroi to 0).random())
        },
        buildList {
            add(listOf(UnitTypes.mace to 3, UnitTypes.pulsar to 3).random())
            add(listOf(UnitTypes.mace to 3, UnitTypes.pulsar to 3).random())
            add(listOf(UnitTypes.crawler to 3, UnitTypes.elude to 2).random())
            add(listOf(UnitTypes.fortress to 1, UnitTypes.cleroi to 1).random())
            add(listOf(UnitTypes.locus to 1, UnitTypes.quasar to 1).random())
            add(listOf(UnitTypes.scepter to 0, UnitTypes.vela to 0).random())
        },
        buildList {
            add(listOf(UnitTypes.fortress to 2, UnitTypes.cleroi to 2).random())
            add(listOf(UnitTypes.locus to 2, UnitTypes.quasar to 2).random())
            add(listOf(UnitTypes.scepter to 0, UnitTypes.vela to 0).random())
        },
        buildList {
            add(listOf(UnitTypes.scepter to 1, UnitTypes.vela to 1).random())
            add(listOf(UnitTypes.scepter to 1, UnitTypes.vela to 1).random())
            add(listOf(UnitTypes.reign to 0, UnitTypes.corvus to 0).random())
        },
    )
}

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
            Groups.unit.filter { it.team == Team.crux && it != this }.forEach {
                it.data.exp += data.nextLevelNeed() / (it.dst(this) / 8f).coerceAtLeast(12f).coerceAtMost(1000f)
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

    fun msg(): String {
        return "[green]|".repeat(tier) + "[red]|".repeat(maxTier - tier)
    }

}

class TechInfo(
    var exp: Int = 0,

    var mineTier: Tech = Tech("挖掘效率", "减少挖矿损血", 0),
    var moreExpTier: Tech = Tech("经验效率", "增加单位经验", 0),
    var moreExpInitTier: Tech = Tech("预训练", "单位初始经验", 0),
    var turretsTier: Tech = Tech("核心炮台", "减少核炮CD", 0),
    var unitRepairTier: Tech = Tech("单位修复", "定期回复单位", 0),

    var techList: List<Tech> = listOf(mineTier, moreExpTier, moreExpInitTier, turretsTier, unitRepairTier)
) {
    private fun canResearch(tech: Tech): Boolean {
        return exp > tech.cost() && tech.tier < tech.maxTier
    }

    fun buttonName(tech: Tech): String {
        return "[gold]${tech.name} [cyan]${tech.desc} \n ${if (canResearch(tech)) "[green]" else "[lightgray]"} ${tech.cost()}"
    }

    fun research(tech: Tech) {
        if (!canResearch(tech)) return
        exp -= tech.cost()
        tech.tier += 1
    }

    fun techIncreased(): Int {
        var expIncreased: Float = 0f
        Team.sharded.cores().forEach {
            expIncreased += 1.7f.pow(it.block.level() - 1)
        }
        return expIncreased.toInt()
    }
}


val tech by autoInit { TechInfo() }

var bossUnit: mindustry.gen.Unit? = null
val bossSpawned: Boolean get() = bossUnit?.dead() == false

val voteService = contextScript<VoteService>()

fun VoteService.register() {
    val _100041 = contextScript<_100041>()
    addSubVote("跳过漫长的白天", "", "skip", "跳过百天") {
        val team = player!!.team()
        val player = player!!
        _100041.apply {
            if (worldTime.hours() !in 5..18) returnReply("[red]无法跳过夜晚".with())
        }

        canVote = canVote.let { default -> { default(it) && it.team() == team } }
        start(player, "跳过漫长的白天".with("team" to team)) {
            _100041.apply {
                launch(Dispatchers.game) {
                    while (worldTime.hours() in 6..18) {
                        worldTime.time += 1_000
                        delay(250)
                    }
                }
            }
        }
    }
}

onEnable {
    voteService.register()
    bossUnit = null

    contextScript<coreMindustry.ContentsTweaker>().addPatch("100041", contentPatch)
    state.rules.apply {
        canGameOver = false
    }
    setRules()
    Team.sharded.cores().forEach { it.tile.setNet(Blocks.air) }

    val landedTile = tiles.random()
    landedTile.setNet(Blocks.coreShard, Team.sharded, 0)

    launch(Dispatchers.game) {
        state.rules.apply {
            canGameOver = true
        }
        setRules()
        delay(500)
    }

    //时间和模式显示
    loop(Dispatchers.game) {
        repeat(10) {
            state.rules.modeName = worldTime.timeString()
            worldTime.time += (timeSpd * 0.1f).toInt()
            delay(100)
        }
        state.rules.lighting = true

        state.rules.ambientLight.a = worldTime.lights()
        worldTime.getCurTimeType().active()

        if (bossSpawned && Groups.unit.count { it.team == Team.crux && it.hasEffect(StatusEffects.boss) } >= 1)
            state.rules.ambientLight.r = 0.6f
        else
            state.rules.ambientLight.r = 0.01f
        setRules()
    }

    // 天数显示
    loop(Dispatchers.game) {
        Groups.player.forEach {
            Call.setHudText(it.con, buildString {
                appendLine(worldTime.timeString())
                append("时段：${worldTime.getCurTimeType().name}  ${worldTime.lights()}")
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
        Team.sharded.cores().forEach {
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
                text = "[#${it.team.color}]" + it.coreName()
            }
        }
        val need2Remove = core2label.filter { !it.key.isValid }
        need2Remove.forEach { t, u ->
            core2label.remove(t, u)
            Call.removeWorldLabel(u.id)
            u.remove()
        }
        delay(100)
    }

    //科技增加
    loop(Dispatchers.game) {
        tech.exp += tech.techIncreased()
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
                    Vars.world.width(),
                    Vars.world.height(),
                    (tech.mineTier.tier / 2).toInt()
                ) { x, y ->
                    add(Vars.world.tile(x, y))
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
                        it.health -= (amount * tile.drop().hardness * 0.75.pow(tech.mineTier.tier)).toFloat()
                        it.statuses.add(StatusEntry().set(StatusEffects.muddy, amount * 20f))
                        launch(Dispatchers.game) {
                            Call.effect(Fx.unitEnvKill, tile.worldx(), tile.worldy(), 0f, Color.red)
                            repeat(log2(amount.toFloat()).toInt()) {
                                val core = Geometry.findClosest(tile.worldx(), tile.worldy(), Team.sharded.cores())
                                Call.effect(Fx.itemTransfer, tile.worldx(), tile.worldy(), 0f, Color.yellow, core)
                                delay(100)
                            }
                        }
                        Team.sharded.core().items.add(tile.drop(), amount)
                        launch(Dispatchers.game) {
                            tile.setOverlayNet(Blocks.pebbles)
                            delay(600_000)
                            tile.setOverlayNet(overlay)
                        }
                    }
                }
            }
        }
        yield()
    }

    //核心单位离核惩罚
    loop(Dispatchers.game) {
        Groups.unit.filter { it.spawnedByCore }.forEach {
            if (!it.closestCore().within(it, Vars.state.rules.enemyCoreBuildRadius)) {
                it.health -= it.maxHealth * 0.1f
                it.apply(StatusEffects.electrified, 1.2f * 60f)
            }
        }
        delay(1000)
    }

    //单位维修
    loop(Dispatchers.game) {
        Groups.unit.filter { it.team == Team.sharded }.forEach {
            it.health += it.type.health * (tech.unitRepairTier.tier / 100f)
            it.clampHealth()
        }
        delay(1000)
    }

    //核心炮台
    loop(Dispatchers.game) {
        Team.sharded.cores().forEach {
            val bullet = when (it.block) {
                Blocks.coreShard -> UnitTypes.fortress.weapons[0].bullet
                Blocks.coreFoundation -> UnitTypes.reign.weapons[0].bullet
                else -> UnitTypes.conquer.weapons[0].bullet
            }
            val e = Units.closestEnemy(it.team, it.x, it.y, Vars.state.rules.enemyCoreBuildRadius / 2f) { true }
            if (e != null) {
                Call.createBullet(bullet, it.team, it.x, it.y, it.angleTo(e), bullet.damage * 0.5f, 1f, 2f)
            }
        }
        delay((6 - tech.turretsTier.tier) * 500L)
    }

    //生成敌怪
    loop(Dispatchers.game) {
        delay(1000)
        if (state.rules.ambientLight.a >= 0.3) {
            if (Random.nextFloat() <= 0.99f || !canSpawnNewFort()) {
                var enemy = unitsWithDays[(worldTime.days() - 1).coerceAtMost(unitsWithDays.size - 1)]
                val tile = getSpawnTiles()
                enemy.forEach {
                    if (it.second == 0 && !bossSpawned) {
                        if ((Random.nextFloat() >= 0.99f && worldTime.hours() in 0..2) || worldTime.hours() in 2..3) {
                            it.first.spawnAround(tile, Team.crux)?.apply {
                                Call.effect(Fx.greenBomb, x, y, 0f, team.color)
                                Call.soundAt(Sounds.explosionbig, x, y, 114514f, 0f)
                                Call.effect(Fx.impactReactorExplosion, x, y, 0f, team.color)
                                broadcast("[yellow]boss已经生成！  [red]<Attack>[white](${x},${y})".with(), quite = true)
                                bossUnit = this

                                statuses.add(StatusEntry().set(StatusEffects.boss, Float.POSITIVE_INFINITY))

                                val all = bossAbilities.toMutableList()
                                repeat(worldTime.days()) {
                                    data.exp += data.levelNeed(it)
                                    if (all.isNotEmpty() && it % 2 == 0) {
                                        val ability = all.random()
                                        all.remove(ability)
                                        Call.sendMessage("${ability.name} -- ${ability.desc}")
                                        launch(Dispatchers.game) {
                                            while (!dead) {
                                                ability.effect.invoke(this@apply)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        repeat(it.second) { _ ->
                            it.first.spawnAround(tile, Team.crux)?.apply {
                                Call.effect(Fx.greenBomb, x, y, 0f, team.color)
                            }
                        }
                    }
                }
            } else {
                val tile = getSpawnTiles()
                tile.setNet(Blocks.coreShard, Team.crux, 0)
                broadcast(
                    "[yellow]在[${tile.x},${tile.y}]处发现废弃的前哨站！  [#eab678]<Mark>[white](${tile.x},${tile.y})".with(),
                    quite = true
                )
                if (!canSpawnNewFort()) broadcast(
                    "[orange]前哨站已达到上限！占领更多的前哨站以允许新前哨生成".with(),
                    quite = true
                )
            }
        } else {
            bossUnit = null
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
        val next = when (core.block) {
            Blocks.coreShard -> Blocks.coreFoundation
            Blocks.coreFoundation -> Blocks.coreBastion
            Blocks.coreBastion -> Blocks.coreNucleus
            Blocks.coreNucleus -> Blocks.coreCitadel
            Blocks.coreCitadel -> Blocks.coreAcropolis
            else -> Blocks.coreShard
        }
        msg = buildString {
            if (core.block != Blocks.coreAcropolis) {
                appendLine("${core.block.emoji()}${core.block.coreName()} -> ${next.emoji()}${next.coreName()}")
                next.requirements.forEach {
                    appendLine("[white]${it.item.emoji()} ${if (core.items[it.item] >= it.amount) "[green]" else "[lightgray]"}${core.items[it.item]}/${it.amount}")
                }
            }
            append("[yellow]在此进行升级据点,招募兵种等工作")
        }
        lazyOption {
            fun canUpgrade() =
                next.requirements.all { core.items[it.item] >= it.amount } && core.isValid && core.team() == player.team() && core.block != Blocks.coreAcropolis
            if (core.block == Blocks.coreAcropolis) {
                refreshOption("[green]据点已经满级")
            } else if (!canUpgrade()) {
                refreshOption("[lightgray]据点升级资源不足")
            } else {
                option("升级至 ${next.emoji()} ${next.coreName()}")
                if (canUpgrade()) {
                    next.requirements.forEach {
                        core.items.remove(it.item, it.amount)
                    }
                    Vars.world.tile(1, 1).setNet(Blocks.coreShard, core.team(), 0)
                    core.tile.setNet(next, core.team(), 0)
                    Vars.world.tile(1, 1).setNet(Blocks.air, core.team(), 0)
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
                } else if (Groups.unit.count { it.team == Team.sharded && it.type == u } > Team.sharded.data().unitCap) {
                    refreshOption("[lightgray]单位已满")
                } else {
                    option("[green]招募！")
                    var unit = spawnAroundLand(core, core.team)
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

    suspend fun unitShop() {
        msg = "[yellow]在此进行招募兵种,随据点等级解锁"
        repeat(6) {
            if (core.block.level() >= it + 1) {
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

    suspend fun techMenu() {
        msg =
            "[yellow]在此研发科技,满级科技过后即可研究最终科技\n[cyan]科技点: ${tech.exp} [white]+[acid]${tech.techIncreased()}/s" +
                    "\n[yellow]科技点增长速度与据点数量与等级有关"
        tech.techList.forEach {
            option(tech.buttonName(it)) {
                tech.research(it);
                refresh()
            }

            option(it.msg()) {

            }
            newRow()
        }

        if (core.block == Blocks.coreAcropolis) {
            option("${if (tech.exp >= 16000) "[green]" else "[lightgray]"}最终科技-重启跃迁\n16000科技点") {
                if (tech.exp >= 16000) {
                    Vars.state.gameOver = true
                    Events.fire(EventType.GameOverEvent(Team.sharded))
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
        if (debugMode && player.admin) {
            option("[red] DEBUG [white]科技点+100000") {
                tech.exp += 100000
            }
            newRow()
        }
        option("返回主菜单") {
            tab = 0; refresh()
        }
    }


    override suspend fun build() {
        title = core.coreName()
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
    var owner = (it.bullet.owner() as? mindustry.gen.Unit)
    (owner?.controller() as? MissileAI).let {//导弹
        owner = it?.shooter
    }
    /*
    if (owner.spawnedByCore) owner = null
    (owner ?: Units.closest(it.bullet.team, it.unit.x, it.unit.y) {!owner.spawnedByCore} ?: return@listen)//核心击杀就选最近单位
        .data.exp += it.unit.maxHealth * it.unit.healthMultiplier * 1.2f.pow(tech.moreExpTier.tier)*/
}


