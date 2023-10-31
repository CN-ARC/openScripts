@file:Depends("coreMindustry/menu", "调用菜单")
@file:Depends("coreMindustry/contentsTweaker", "修改核心单位,单位属性")
@file:Depends("wayzer/user/achievement", "成就")
@file:Depends("wayzer/voteService", "投票实现")
@file:Import("@coreMindustry/util/spawnAround.kt", sourceFile = true)

package mapScript

import arc.Events
import arc.graphics.Color
import arc.math.geom.Geometry
import arc.math.geom.Vec2
import arc.util.Time
import coreLibrary.lib.util.loop
import coreLibrary.lib.with
import coreMindustry.MenuBuilder
import coreMindustry.lib.broadcast
import coreMindustry.lib.game
import coreMindustry.lib.listen
import coreMindustry.lib.player
import coreMindustry.util.spawnAround
import mindustry.Vars
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
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.VoteService
import wayzer.lib.dao.PlayerData
import wayzer.user.AchievementEntity
import java.text.DecimalFormat
import java.util.Date
import kotlin.math.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


/**@author xkldklp & Lucky Clover */
/**
 * 末日模式
 * 在尸潮中生存
 * */

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
      mineSpeed: 0
    },
    "mono": {
      "mineSpeed": 0
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
    "stell": {
     "health": 200,
     "armor": 5
    },
    "elude": {
      "health": 200,
      "armor": 2
    },
    "mace": {
      "armor": 8
    },
    "pulsar": {
      "mineTier": -1,
      "armor": 5
    },
    "locus": {
      "health": 750,
      "armor": 11
    },
    "cleroi": {
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
    "precept": {
      "health": 1500,
      "armor": 17
    },
    "anthicus": {
      "health": 1500,
      "armor": 14
    }
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

val startTime by autoInit { Time.millis() }
var timeOffset = 0L

//0 - 23
fun hours(): Int {
    return ((Time.timeSinceMillis(startTime - timeOffset) / 1000 / 60 + 16) % 24).toInt()
}

fun hoursFixed(): String {
    return (100 + hours()).toString().removePrefix("1")
}

//0 - 59
fun minutes(): Int {
    return (Time.timeSinceMillis(startTime - timeOffset) / 1000 % 60).toInt()
}

fun minutesFixed(): String {
    return (100 + minutes()).toString().removePrefix("1")
}


fun days(): Int {
    return ((Time.timeSinceMillis(startTime - timeOffset) / 1000 / 60 + 16) / 24 + 1).toInt()
}

fun timeName(): String {
    return when (hours()) {
        in 5..7 -> "清晨"
        in 7..9 -> "早上"
        in 9..12 -> "上午"
        in 12..14 -> "中午"
        in 14..18 -> "下午"
        else -> "夜晚"
    }
}

fun Block.level():Int {
    return when(this){
        Blocks.coreShard -> 1
        Blocks.coreFoundation -> 2
        Blocks.coreBastion -> 3
        Blocks.coreNucleus -> 4
        Blocks.coreCitadel -> 5
        Blocks.coreAcropolis -> 6
        else -> 0
    }
}

fun Block.coreName():String {
    return when(this){
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
        UnitTypes.nova to listOf(Items.scrap to 20)),
    listOf(
        UnitTypes.stell to listOf(Items.copper to 100, Items.lead to 50),
        UnitTypes.elude to listOf(Items.copper to 50, Items.lead to 50)
    ),
    listOf(
        UnitTypes.mace to listOf(Items.copper to 100, Items.lead to 50, Items.coal to 20),
        UnitTypes.pulsar to listOf(Items.copper to 50, Items.lead to 100, Items.coal to 40)
    ),
    listOf(
        UnitTypes.cleroi to listOf(Items.copper to 200, Items.lead to 100, Items.titanium to 150),
        UnitTypes.locus to listOf(Items.copper to 100, Items.lead to 200, Items.beryllium to 150)
    ),
    listOf(
        UnitTypes.fortress to listOf(Items.copper to 300, Items.lead to 150, Items.titanium to 250, Items.thorium to 100),
        UnitTypes.quasar to listOf(Items.copper to 150, Items.lead to 300, Items.titanium to 150, Items.thorium to 100)
    ),
    listOf(
        UnitTypes.precept to listOf(Items.copper to 1000, Items.lead to 500, Items.beryllium to 500, Items.thorium to 300),
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
            timeOffset -= 20_000
            delay(30_000)
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

    fun msg(): String{
        return "[green]|".repeat(tier) + "[red]|".repeat(maxTier - tier)
    }

}

class TechInfo(
    var exp: Int = 0,

    var mineTier: Tech = Tech("挖掘效率","减少挖矿损血", 0),
    var moreExpTier: Tech = Tech("经验效率","增加单位经验", 0),
    var moreExpInitTier: Tech = Tech("预训练","单位初始经验", 0),
    var turretsTier: Tech = Tech("核心炮台","减少核炮CD", 0),
    var unitRepairTier: Tech = Tech("单位修复","定期回复单位", 0),

    var techList: List<Tech> = listOf(mineTier, moreExpTier, moreExpInitTier, turretsTier, unitRepairTier)
) {
    private fun canResearch(tech: Tech): Boolean {
        return exp > tech.cost() && tech.tier < tech.maxTier
    }

    fun buttonName(tech: Tech): String {
        return "[gold]${tech.name} [cyan]${tech.desc} \n ${if (canResearch(tech)) "[green]" else "[lightgray]"} ${tech.cost()}"
    }

    fun research(tech: Tech){
        if (!canResearch(tech)) return
        exp -= tech.cost()
        tech.tier += 1
    }

    fun techIncreased(): Int{
        var expIncreased: Float = 0f
        Team.sharded.cores().forEach {
            expIncreased += 2f.pow(it.block.level())
        }
        if (hours() in 5..18) {
            expIncreased /= 8
        }else{
            expIncreased /= 2
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
            if (hours() !in 5..18) returnReply("[red]无法跳过夜晚".with())
        }

        canVote = canVote.let { default -> { default(it) && it.team() == team } }
        start(player, "跳过漫长的白天".with("team" to team)) {
            _100041.apply {
                launch(Dispatchers.game) {
                    while (hours() in 5..18) {
                        timeOffset += 10_000
                        delay(250)
                    }
                }
            }
        }
    }
}

onEnable {
    voteService.register()
    timeOffset = 0L
    bossUnit = null

    contextScript<coreMindustry.ContentsTweaker>().addPatch("100041", contentPatch)
    Vars.state.rules.apply {
        canGameOver = false
    }
    setRules()
    Team.sharded.cores().forEach { it.tile.setNet(Blocks.air) }

    val landedTile = tiles.random()
    landedTile.setNet(Blocks.coreShard, Team.sharded, 0)

    launch(Dispatchers.game) {
        Vars.state.rules.apply {
            canGameOver = true
        }
        setRules()
        delay(500)
    }

    loop(Dispatchers.game) {
        repeat(10) {
            Vars.state.rules.modeName = "D${days()}${timeName()}|${hoursFixed()}:${minutesFixed()}"
            delay(100)
        }
        Vars.state.rules.lighting = true

        Vars.state.rules.ambientLight.a = when (hours()) {
            in 5..7 -> 0.5f
            in 7..9 -> 0.3f
            in 9..12 -> 0.1f
            in 12..14 -> 0.01f
            in 14..18 -> 0.55f
            else -> 0.8f
        }
        if (bossSpawned && Groups.unit.count { it.team == Team.crux && it.hasEffect(StatusEffects.boss) } >= 1)
            Vars.state.rules.ambientLight.r = 0.6f
        else
            Vars.state.rules.ambientLight.r = 0.01f
        setRules()
    }

    // 天数显示
    loop(Dispatchers.game) {
        Groups.player.forEach {
            Call.setHudText(it.con, buildString {
                appendLine("第${days()}天 ${hoursFixed()}:${minutesFixed()}")
                append("时段：${timeName()}")
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
                Geometry.circle(mineTile.x.toInt(), mineTile.y.toInt(), Vars.world.width(), Vars.world.height(), (tech.mineTier.tier / 2).toInt()) { x, y ->
                        add(Vars.world.tile(x,y))
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
                        Team.sharded.core().items.add(tile.drop(), (amount).toInt())
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
        if (Vars.state.rules.ambientLight.a >= 0.8) {
            if (Random.nextFloat() <= 0.99f || Team.crux.cores().size >= 4) {
                var enemy = unitsWithDays[(days() - 1).coerceAtMost(unitsWithDays.size - 1)]
                val tile = getSpawnTiles()
                enemy.forEach {
                    if (it.second == 0 && !bossSpawned) {
                        if ((Random.nextFloat() >= 0.99f && hours() in 0..2) || hours() in 2..3) {
                            it.first.spawnAround(tile, Team.crux)?.apply {
                                Call.effect(Fx.greenBomb, x, y, 0f, team.color)
                                Call.soundAt(Sounds.explosionbig, x, y, 114514f, 0f)
                                Call.effect(Fx.impactReactorExplosion, x, y, 0f, team.color)
                                broadcast("[yellow]boss已经生成！  [red]<Attack>[white](${x},${y})".with(), quite = true)
                                bossUnit = this

                                statuses.add(StatusEntry().set(StatusEffects.boss, Float.POSITIVE_INFINITY))

                                val all = bossAbilities.toMutableList()
                                repeat(days()) {
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
                broadcast("[yellow]在[${tile.x},${tile.y}]处发现废弃的前哨站！  [#eab678]<Mark>[white](${tile.x},${tile.y})".with(), quite = true)
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

                if (it.data.level == 8) {
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
                    var unit = spawnAround(core, core.team)
                    if (unit!=null) unit.data.exp += (1.5f.pow(tech.moreExpInitTier.tier)) * (1.2f.pow(tech.moreExpTier.tier)) * 10
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
            "[yellow]在此研发科技,满级科技过后即可研究最终科技\n[cyan]科技点: ${tech.exp} [white]+[acid]${tech.techIncreased()}/s"  +
                    "\n[yellow]科技点增长速度与据点数量与等级有关"
        tech.techList.forEach {
            option(tech.buttonName(it)) {
                tech.research(it);
                refresh()
            }

            option(it.msg()){

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
    val owner = (it.bullet.owner() as? mindustry.gen.Unit) ?: return@listen
    owner.data.exp += it.unit.type.health / 10f * 1 + 1.2f.pow(tech.moreExpTier.tier)
}

