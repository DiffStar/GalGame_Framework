package net.star.galgame.world

import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.star.galgame.GalGameFramework
import net.star.galgame.world.interaction.WorldInteractionManager

@EventBusSubscriber(modid = GalGameFramework.MODID, value = [Dist.CLIENT])
object WorldInteractionHandler {
    @SubscribeEvent
    @JvmStatic
    fun onPlayerInteractEntity(event: PlayerInteractEvent.EntityInteract) {
        if (event.hand != InteractionHand.MAIN_HAND) return
        val player = event.entity as? Player ?: return
        if (player.level().isClientSide) {
            WorldInteractionManager.checkEntityInteraction(player, event.target)
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onPlayerInteractBlock(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entity as? Player ?: return
        if (player.level().isClientSide) {
            val pos = event.pos
            val blockState = event.level.getBlockState(pos)
            val itemStack = player.getItemInHand(event.hand)

            if (WorldInteractionManager.checkItemBlockTrigger(player, itemStack, blockState, pos)) {
                event.isCanceled = true
                return
            }

            if (WorldInteractionManager.checkLocationTrigger(player, pos)) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val player = event.player as? Player ?: return
        if (player.level().isClientSide) {
            val pos = event.pos
            val blockState = event.state
            val itemStack = player.mainHandItem

            if (WorldInteractionManager.checkItemBlockTrigger(player, itemStack, blockState, pos)) {
                event.isCanceled = true
            }
        }
    }

}

