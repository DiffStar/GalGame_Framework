package net.star.galgame.world.interaction

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.star.galgame.dialogue.DialogueHelper
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.dialogue.DialogueScript
import java.util.concurrent.ConcurrentHashMap

object WorldInteractionManager {
    private val locationTriggers = ConcurrentHashMap<String, LocationTrigger>()
    private val entityInteractions = ConcurrentHashMap<String, EntityInteraction>()
    private val itemBlockTriggers = ConcurrentHashMap<String, ItemBlockTrigger>()
    private val worldEventTriggers = ConcurrentHashMap<String, WorldEventTrigger>()

    fun registerLocationTrigger(trigger: LocationTrigger) {
        locationTriggers[trigger.id] = trigger
    }

    fun registerEntityInteraction(interaction: EntityInteraction) {
        entityInteractions[interaction.id] = interaction
    }

    fun registerItemBlockTrigger(trigger: ItemBlockTrigger) {
        itemBlockTriggers[trigger.id] = trigger
    }

    fun registerWorldEventTrigger(trigger: WorldEventTrigger) {
        worldEventTriggers[trigger.id] = trigger
    }

    fun unregisterLocationTrigger(id: String) {
        locationTriggers.remove(id)
    }

    fun unregisterEntityInteraction(id: String) {
        entityInteractions.remove(id)
    }

    fun unregisterItemBlockTrigger(id: String) {
        itemBlockTriggers.remove(id)
    }

    fun unregisterWorldEventTrigger(id: String) {
        worldEventTriggers.remove(id)
    }

    fun checkLocationTrigger(player: Player, pos: BlockPos): Boolean {
        locationTriggers.values.forEach { trigger ->
            if (trigger.isInRange(player, pos) && trigger.canTrigger(player)) {
                trigger.trigger(player)
                return true
            }
        }
        return false
    }

    fun checkEntityInteraction(player: Player, entity: Entity): Boolean {
        entityInteractions.values.forEach { interaction ->
            if (interaction.matches(entity) && interaction.canInteract(player, entity)) {
                interaction.interact(player, entity)
                return true
            }
        }
        return false
    }

    fun checkItemBlockTrigger(player: Player, item: ItemStack?, block: BlockState?, pos: BlockPos): Boolean {
        itemBlockTriggers.values.forEach { trigger ->
            if (trigger.matches(item, block) && trigger.canTrigger(player, pos)) {
                trigger.trigger(player, pos)
                return true
            }
        }
        return false
    }

    fun checkWorldEventTrigger(eventType: String, player: Player, data: Map<String, Any>? = null): Boolean {
        worldEventTriggers.values.forEach { trigger ->
            if (trigger.matches(eventType) && trigger.canTrigger(player, data)) {
                trigger.trigger(player, data)
                return true
            }
        }
        return false
    }

    fun getAllLocationTriggers(): List<LocationTrigger> = locationTriggers.values.toList()
    fun getAllEntityInteractions(): List<EntityInteraction> = entityInteractions.values.toList()
    fun getAllItemBlockTriggers(): List<ItemBlockTrigger> = itemBlockTriggers.values.toList()
    fun getAllWorldEventTriggers(): List<WorldEventTrigger> = worldEventTriggers.values.toList()
}

