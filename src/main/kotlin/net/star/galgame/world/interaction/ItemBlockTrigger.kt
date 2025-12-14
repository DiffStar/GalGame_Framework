package net.star.galgame.world.interaction

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.star.galgame.dialogue.DialogueHelper
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.world.scene.SceneManager

data class ItemBlockTrigger(
    val id: String,
    val scriptId: String,
    val itemId: ResourceLocation? = null,
    val blockId: ResourceLocation? = null,
    val condition: ((Player, BlockPos) -> Boolean)? = null,
    val sceneId: String? = null
) {
    fun matches(item: ItemStack?, block: BlockState?): Boolean {
        if (itemId != null) {
            val item = item?.item ?: return false
            val itemKey = BuiltInRegistries.ITEM.getKey(item) ?: return false
            if (itemKey != itemId) return false
        }
        if (blockId != null) {
            val block = block?.block ?: return false
            val blockKey = BuiltInRegistries.BLOCK.getKey(block) ?: return false
            if (blockKey != blockId) return false
        }
        return true
    }

    fun canTrigger(player: Player, pos: BlockPos): Boolean {
        return condition?.invoke(player, pos) ?: true
    }

    fun trigger(player: Player, pos: BlockPos) {
        val script = DialogueManager.getScript(scriptId) ?: return
        if (sceneId != null) {
            SceneManager.setScene(sceneId)
        }
        DialogueHelper.openDialogue(script)
    }
}

