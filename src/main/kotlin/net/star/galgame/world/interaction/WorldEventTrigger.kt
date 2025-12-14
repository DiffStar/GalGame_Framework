package net.star.galgame.world.interaction

import net.minecraft.world.entity.player.Player
import net.star.galgame.dialogue.DialogueHelper
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.world.scene.SceneManager

data class WorldEventTrigger(
    val id: String,
    val scriptId: String,
    val eventType: String,
    val condition: ((Player, Map<String, Any>?) -> Boolean)? = null,
    val sceneId: String? = null
) {
    fun matches(eventType: String): Boolean {
        return this.eventType == eventType
    }

    fun canTrigger(player: Player, data: Map<String, Any>?): Boolean {
        return condition?.invoke(player, data) ?: true
    }

    fun trigger(player: Player, data: Map<String, Any>?) {
        val script = DialogueManager.getScript(scriptId) ?: return
        if (sceneId != null) {
            SceneManager.setScene(sceneId)
        }
        DialogueHelper.openDialogue(script)
    }
}

