package net.star.galgame.world.interaction

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.star.galgame.dialogue.DialogueHelper
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.world.scene.SceneManager

data class EntityInteraction(
    val id: String,
    val scriptId: String,
    val entityType: EntityType<*>? = null,
    val entityId: String? = null,
    val condition: ((Player, Entity) -> Boolean)? = null,
    val sceneId: String? = null,
    val interactionRange: Double = 5.0
) {
    fun matches(entity: Entity): Boolean {
        if (entityType != null && entity.type != entityType) return false
        if (entityId != null && entity.uuid.toString() != entityId) return false
        return true
    }

    fun canInteract(player: Player, entity: Entity): Boolean {
        val distance = player.position().distanceTo(entity.position())
        if (distance > interactionRange) return false
        return condition?.invoke(player, entity) ?: true
    }

    fun interact(player: Player, entity: Entity) {
        val script = DialogueManager.getScript(scriptId) ?: return
        if (sceneId != null) {
            SceneManager.setScene(sceneId)
        }
        DialogueHelper.openDialogue(script)
    }
}

