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
import mindustry.game.EventType
import mindustry.gen.*
import mindustry.type.Category
import mindustry.type.ItemStack
import mindustry.world.Block
import mindustry.world.blocks.storage.CoreBlock
import mindustry.world.blocks.storage.CoreBlock.CoreBuild
import org.intellij.lang.annotations.Language
import wayzer.lib.dao.PlayerData
import kotlin.math.*
import kotlin.random.Random

/**@author Lucky Clover */
name = "战役模式"
modeIntroduce(
    "战役模式", "玩法介绍 \n[acid]Made By [vioet]Lucky Clover[]" +
            "\n类似于战役，你的绝大部分建筑需要使用[cyan]资源[white]解锁\n点击核心进行解锁\n\n[cyan]作图说明[white]\n参考16774简介打上地图标签和设置科技消耗即可"
)

val debugMode = false

val techCostMultiplier = state.rules.tags.getInt("@techCost", 5)

onEnable {

}

fun Category.name(): String{
    return when(this){
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

fun Block.resCost(): List<ItemStack> {
    val techCost: MutableList<ItemStack> = mutableListOf()
    this.requirements.forEach {
        techCost.add(ItemStack(it.item, it.amount * techCostMultiplier))
    }
    return techCost
}

fun Block.unlockTech() {
    if (this.techNode.parent != null && state.rules.bannedBlocks.contains(this.techNode.parent.content as Block)) return
    var canBuild: Boolean = true
    this.resCost().forEach { if (state.rules.defaultTeam.core().items.get(it.item) < it.amount) canBuild = false }
    if (!canBuild) return
    this.resCost().forEach {
        state.rules.defaultTeam.core().items.remove(it)
    }
    state.rules.bannedBlocks.remove(this)
    Call.setRules(state.rules)
    Call.sendMessage("[acid]已解锁科技：[sky]<Content>[white]${this.emoji()} ${this.name}")
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

fun Block.buttonName(): String {
    if (!state.rules.bannedBlocks.contains(this)) return "${this.emoji()}[cyan]已解锁"
    if (this.techNode.parent != null && this.techNode.parent.content is Block && state.rules.bannedBlocks.contains(this.techNode.parent.content as Block)) {
        return "${this.emoji()} \n[gray]前置科技 ${this.techNode.parent.content.emoji()}"
    }
    var canBuild: Boolean = true
    this.resCost().forEach { if (state.rules.defaultTeam.core().items.get(it.item) > it.amount) canBuild = false }
    if (canBuild) return "${this.emoji()}\n${this.costName()}"
    else return "${this.emoji()}\n${this.costName()}"
}

val blocksList: Seq<Block> = content.blocks().select { block -> block.isVisible && block.environmentBuildable() }

class CoreMenu(private val player: Player, private val core: CoreBlock.CoreBuild) : MenuBuilder<Unit>() {
    private var category: Category? = null

    fun sendTo() {
        launch(Dispatchers.game) {
            sendTo(player, 60_000)
        }
    }

    private suspend fun mainMenu() {
        msg = "[cyan]科技类别"
        Category.all.forEach {
            val lsBlockList = blocksList.select{ block -> block.category == it }
            if (lsBlockList.isEmpty) return
            option("[cyan]${it.name()}[white] ~ ${lsBlockList.first().emoji()}${lsBlockList.get(lsBlockList.size/2) .emoji()}${lsBlockList.last().emoji()}...") {
                category = it
                refresh()
            }
            newRow()
        }

        if (debugMode) {
            option("[red] DEBUG ") {}
        }
    }

    private suspend fun unlockMenu() {
        msg =
            "当前科技类型：[cyan]${category?.name()}"
        var count = 0
        blocksList.select { block -> block.category == category }.forEach {
            if (count > 0 && count % 2 == 0) newRow()
            count += 1
            option(it.buttonName()) {
                it.unlockTech();
                refresh()
            }
        }
        newRow()
    }

    override suspend fun build() {
        title = "[cyan]科技解锁器"
        when (category) {
            null -> mainMenu()
            else -> unlockMenu()
        }
        //newRow()
        option("[white]退出菜单") { }
    }
}

listen<EventType.TapEvent> {
    if (it.tile.build is CoreBlock.CoreBuild && it.tile.team() == it.player.team()) {
        CoreMenu(it.player, it.tile.build as CoreBlock.CoreBuild).sendTo()
    }
}