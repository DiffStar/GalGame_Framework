package net.star.galgame.client

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.star.galgame.GalGameFramework

@Mod(value = GalGameFramework.MODID, dist = [Dist.CLIENT])
@EventBusSubscriber(modid = GalGameFramework.MODID, value = [Dist.CLIENT])
class GalGameFrameworkClient(container: ModContainer) {
    init {
    }

    companion object {
        @SubscribeEvent
        @JvmStatic
        fun onClientSetup(event: FMLClientSetupEvent) {
            GalGameFramework.LOGGER.info("Ciallo～(∠・ω<)⌒☆ Client")
        }
    }
}