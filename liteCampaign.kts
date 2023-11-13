@file:Depends("coreMindustry/menu", "调用菜单")
@file:Depends("coreMindustry/contentsTweaker", "修改核心单位,单位属性")

package mapScript.tags

import coreMindustry.MenuBuilder
import mindustry.ctype.UnlockableContent
import mindustry.type.Category
import mindustry.type.ItemStack
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.blocks.storage.CoreBlock
import mindustry.world.meta.BuildVisibility
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**@author Lucky Clover
 * WayZer 整理并规范化代码*/
name = "战役模式"
registerMapTag("@liteCampaign")
modeIntroduce(
    "战役模式", "玩法介绍 \n[acid]Made By [violet]Lucky Clover[]" +
            "\n类似于战役，你的绝大部分建筑和兵种需要使用[cyan]资源[white]解锁\n点击核心进行解锁\n\n[cyan]作图说明[white]\n参考16774简介打上地图标签和设置科技消耗即可\n默认倍率(1倍花费)与原版科技相同"
)

val techCostMultiplier = state.rules.tags.getFloat("@techCost", 1f)

val allUnlock = mapOf<String, List<UnlockableContent>>("*单位*" to content.units().asIterable().filter { !it.hidden }) +
        content.blocks().asIterable()
            .filter { it.buildVisibility == BuildVisibility.shown && it.environmentBuildable() }
            .groupBy {
                when (it.category) {
                    Category.crafting -> "加工"
                    Category.defense -> "防御"
                    Category.distribution -> "物流"
                    Category.effect -> "其他"
                    Category.liquid -> "流体"
                    Category.logic -> "逻辑"
                    Category.power -> "电力"
                    Category.production -> "生产"
                    Category.turret -> "炮台"
                    Category.units -> "单位"
                    else -> "x"
                }
            }

class CoreMenu : MenuBuilder<Unit>() {
    private val UnlockableContent.unlocked
        get() = state.rules.let { !it.bannedBlocks.contains(this) && !it.bannedUnits.contains(this) }
    private val ItemStack.coreHas
        get() = state.rules.defaultTeam.core().items[item] >= amount

    @MenuBuilderDsl
    private suspend fun unlockOption(content: UnlockableContent) = lazyOption {
        if (content.unlocked) refreshOption("${content.emoji()}[cyan]已解锁")
        val c = content.techNode?.parent?.content
        if (c != null && !c.unlocked)
            refreshOption("${content.emoji()} \n[gray]前置科技 ${c.emoji()}")

        val cost = content.researchRequirements().map {
            it.copy().apply { amount = (amount * techCostMultiplier).roundToInt() }
        }
        val costName = cost.joinToString(" ") {
            "${it.item.emoji()}${if (it.coreHas) "[green]" else "[red]"}${it.amount}[]"
        }
        if (cost.any { !it.coreHas }) refreshOption("${content.emoji()} $costName")
        option("${content.emoji()} $costName")

        state.rules.defaultTeam.core().items.remove(cost)
        if (content is UnitType) state.rules.bannedUnits.remove(content)
        else if (content is Block) state.rules.bannedBlocks.remove(content)
        broadcast("[acid]已解锁科技：[sky]<Content>[white]{content}".with("content" to content))
        refresh()
    }

    private var category: String? = null
    override suspend fun build() {
        title = "[cyan]科技解锁器"
        allUnlock[category]?.let { list ->
            msg = "当前科技类型：[cyan]${category}"
            var count = 0
            list.forEach {
                if (count > 0 && count % 2 == 0) newRow()
                count += 1
                unlockOption(it)
            }
            newRow()
        } ?: let {
            msg = "[cyan]科技类别"
            allUnlock.forEach { (it, list) ->
                option(
                    "[cyan]${it}[white] ~ " +
                            "${list.first().emoji()}${list[list.size / 2].emoji()}${list.last().emoji()}..."
                ) {
                    category = it
                    refresh()
                }
                newRow()
            }
        }
        option("[white]退出菜单") { }
    }
}

listen<EventType.TapEvent> {
    if (it.tile.build is CoreBlock.CoreBuild && it.tile.team() == it.player.team()) {
        launch(Dispatchers.game) {
            CoreMenu().sendTo(it.player, 60_000)
        }
    }
}

var timeSpd: Int = 180 //控制时间流逝速度,和现实时间的比值
var time = 10.0.hours
val lights get() = abs(0.5 - time.toDouble(DurationUnit.DAYS) % 1f).toFloat() * 2f

onEnable {
    time = 10.0.hours
    state.rules.modeName = "战役模式"
    state.rules.lighting = true
    //时间和模式显示
    loop(Dispatchers.game) {
        time += 2.0.seconds * timeSpd

        state.rules.ambientLight.a = lights
        Call.setRules(state.rules)
        delay(2000)
    }
}