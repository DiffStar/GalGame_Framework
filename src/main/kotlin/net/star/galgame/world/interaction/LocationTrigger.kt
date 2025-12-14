package net.star.galgame.world.interaction

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.star.galgame.dialogue.DialogueHelper
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.world.scene.SceneManager

data class LocationTrigger(
    val id: String,
    val scriptId: String,
    val position: BlockPos,
    val range: Double = 5.0,
    val condition: ((Player) -> Boolean)? = null,
    val sceneId: String? = null
) {
    fun isInRange(player: Player, pos: BlockPos): Boolean {
        val targetPos = Vec3(
            position.x + 0.5,
            position.y + 0.5,
            position.z + 0.5
        )
        val distance = player.position().distanceTo(targetPos)
        return distance <= range && pos == position
    }

    fun canTrigger(player: Player): Boolean {
        return condition?.invoke(player) ?: true
    }

    fun trigger(player: Player) {
        val script = DialogueManager.getScript(scriptId) ?: return
        if (sceneId != null) {
            SceneManager.setScene(sceneId)
        }
        DialogueHelper.openDialogue(script)
    }
}

