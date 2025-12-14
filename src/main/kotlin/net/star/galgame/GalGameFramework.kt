package net.star.galgame

import com.mojang.logging.LogUtils
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.ModContainer
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStartingEvent
import org.slf4j.Logger

@Mod(GalGameFramework.MODID)
class GalGameFramework(modEventBus: IEventBus, modContainer: ModContainer) {
    companion object {
        const val MODID = "galgame"
        val LOGGER: Logger = LogUtils.getLogger()
    }

    init {
        modEventBus.addListener(this::commonSetup)
        NeoForge.EVENT_BUS.register(this)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        LOGGER.info("Ciallo～(∠・ω< )⌒★ Server")
    }
}

