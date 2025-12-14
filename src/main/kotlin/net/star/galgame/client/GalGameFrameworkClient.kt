package net.star.galgame.client

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.star.galgame.GalGameFramework

@Mod(value = GalGameFramework.Companion.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GalGameFramework.Companion.MODID, value = Dist.CLIENT)
class GalGameFrameworkClient(container: ModContainer) {
    init {
        container.registerExtensionPoint(IConfigScreenFactory::class.java, ConfigurationScreen::new)
    }

    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        GalGameFramework.Companion.LOGGER.info("Ciallo～(∠・ω<)⌒☆ Client")
    }
}