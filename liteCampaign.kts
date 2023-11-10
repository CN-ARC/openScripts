@file:Depends("coreMindustry/menu", "调用菜单")
@file:Depends("coreMindustry/contentsTweaker", "修改核心单位,单位属性")
@file:Depends("wayzer/user/achievement", "成就")
@file:Import("@coreMindustry/util/spawnAround.kt", sourceFile = true)

package mapScript

import arc.struct.*
import cf.wayzer.scriptAgent.define.annotations.Depends
import cf.wayzer.scriptAgent.define.annotations.Import
import coreMindustry.MenuBuilder
import coreMindustry.lib.game
import coreMindustry.lib.listen
import mindustry.Vars.*
import mindustry.ctype.UnlockableContent
import mindustry.game.EventType
import mindustry.gen.*
import mindustry.type.Category
import mindustry.type.ItemStack
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.blocks.storage.CoreBlock
import kotlin.math.abs

/**@author Lucky Clover */
name = "战役模式"
registerMapTag("liteCampaign")
modeIntroduce(
    "战役模式", "玩法介绍 \n[acid]Made By [violet]Lucky Clover[]" +
            "\n类似于战役，你的绝大部分建筑和兵种需要使用[cyan]资源[white]解锁\n点击核心进行解锁\n\n[cyan]作图说明[white]\n参考16774简介打上地图标签和设置科技消耗即可\n默认倍率(1倍花费)与原版科技相同"
)

val techCostMultiplier = state.rules.tags.getInt("@techCost", 1)

fun Category.name(): String {
    return when (this) {
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

fun UnlockableContent.resCost(): List<ItemStack> {
    val techCost: MutableList<ItemStack> = mutableListOf()
    this.researchRequirements().forEach {
        techCost.add(ItemStack(it.item, it.amount * techCostMultiplier))
    }
    return techCost
}

fun UnlockableContent.checkParents(): Boolean{
    if (this.techNode.parent == null) return true
    return when (this.techNode.parent.content) {
        is Block -> state.rules.bannedBlocks.contains(this.techNode.parent.content)
        is UnitType -> state.rules.bannedUnits.contains(this.techNode.parent.content)
        else -> true
    }

}

fun UnlockableContent.checkItems(): Boolean{
    for (i in this.resCost()){
        if (state.rules.defaultTeam.core().items.get(i.item) < i.amount) return false
    }
    return true
}

fun UnlockableContent.showItems(): Boolean{
    for (i in this.resCost()){
        if (state.rules.defaultTeam.core().items.get(i.item) < i.amount) return false
    }
    return true
}

fun UnlockableContent.unlockTech(){
    this.resCost().forEach {
        state.rules.defaultTeam.core().items.remove(it)
    }
    Call.setRules(state.rules)
    Call.sendMessage("[acid]已解锁科技：[sky]<Content>[white]${this.emoji()} ${this.name}")
}

fun UnlockableContent.costName(): String {
    if (this.resCost().isEmpty()) return "0元购"
    var name: String = ""
    this.resCost().forEach {
        name += it.item.emoji()
        name += if (state.rules.defaultTeam.core().items.get(it.item) > it.amount) "[green]" else "[red]"
        name += "${it.amount}[white] "
    }
    return "${this.emoji()}\n${name.dropLast(1)}"
}


fun Block.costName(): String {
    if (this.resCost().isEmpty()) return "0元购"
    var name: String = ""
    this.resCost().forEach {
        name += it.item.emoji()
        name += if (state.rules.defaultTeam.core().items.get(it.item) > it.amount) "[green]" else "[red]"
        name += "${it.amount}[white] "
    }
    return name.dropLast(1)
}

val blocksList: Seq<Block> = content.blocks().select { block -> block.isVisible && block.environmentBuildable() }

class CoreMenu(private val player: Player, private val core: CoreBlock.CoreBuild) : MenuBuilder<Unit>() {
    private var category: Category? = null
    private var buyUnit: Boolean = false

    fun sendTo() {
        launch(Dispatchers.game) {
            sendTo(player, 60_000)
        }
    }

    private suspend fun mainMenu() {
        msg = "[cyan]科技类别"
        var count = 0
        Category.all.forEach {
            val lsBlockList = blocksList.select { block -> block.category == it }
            if (lsBlockList.isEmpty) return
            if (count > 0 && count % 2 == 0) newRow()
            count += 1
            option(
                "[cyan]${it.name()}[white] ~ ${lsBlockList.first().emoji()}${
                    lsBlockList.get(lsBlockList.size / 2).emoji()
                }${lsBlockList.last().emoji()}..."
            ) {
                category = it
                refresh()
            }
            newRow()
        }
        option("[cyan]兵种[white] ~ ${UnitTypes.mono.emoji()}${UnitTypes.toxopid.emoji()}${UnitTypes.conquer.emoji()}...") {
            buyUnit = true
            refresh()
        }
        newRow()
    }

    private suspend fun unlockMenu() {
        msg =
            "当前科技类型：[cyan]${category?.name()}"
        var count = 0
        blocksList.select { block -> block.category == category }.forEach {
            if (count > 0 && count % 2 == 0) newRow()
            count += 1
            lazyOption {
                if (it.checkParents()) refreshOption("${it.emoji()} \n[gray]前置科技 ${it.techNode.parent.content.emoji()}")
                else if (it.checkItems()) refreshOption(it.costName())
                option("${it.emoji()}[cyan]已解锁")
                state.rules.bannedBlocks.remove(it)
                it.unlockTech()
                refresh()
            }
        }
        newRow()
    }

    private suspend fun unlockUnitMenu() {
        msg =
            "当前科技类型：[cyan]兵种"
        var count = 0
        content.units().select { !it.hidden }.forEach {
            if (count > 0 && count % 2 == 0) newRow()
            count += 1
            lazyOption {
                if (it.checkParents()) refreshOption("${it.emoji()} \n[gray]前置科技 ${it.techNode.parent.content.emoji()}")
                else if (it.checkItems()) refreshOption(it.costName())
                option("${it.emoji()}[cyan]已解锁")
                state.rules.bannedUnits.remove(it)
                it.unlockTech()
                refresh()
            }
        }
        newRow()
    }

    override suspend fun build() {
        title = "[cyan]科技解锁器"
        if (buyUnit) unlockUnitMenu()
        else if (category == null) mainMenu()
        else unlockMenu()
        //newRow()
        option("[white]退出菜单") { }
    }
}

listen<EventType.TapEvent> {
    if (it.tile.build is CoreBlock.CoreBuild && it.tile.team() == it.player.team()) {
        CoreMenu(it.player, it.tile.build as CoreBlock.CoreBuild).sendTo()
    }
}

var timeSpd: Int = 180 //控制时间流逝速度,和现实时间的比值
//世界时间
class WorldTime(
    // second，但并不是实际速度
    var time: Int = 10 * 60 * 60,
) {
    fun lights(): Float {
        return abs(0.5 - time % 86400 / 86400f).toFloat() * 2f
    }
}

val worldTime by autoInit { WorldTime() }

onEnable {
    //时间和模式显示
    loop(Dispatchers.game) {
        state.rules.modeName = "战役模式"
        worldTime.time += timeSpd * 2

        state.rules.lighting = true
        state.rules.ambientLight.a = worldTime.lights()

        Call.setRules(state.rules)
        delay(2000)
    }
}