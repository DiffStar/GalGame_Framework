package net.star.galgame.client

import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.ClientTickEvent.Post
import net.star.galgame.GalGameFramework
import net.star.galgame.developer.ui.DevToolsScreen
import net.star.galgame.dialogue.menu.MainMenuScreen
import org.lwjgl.glfw.GLFW

@EventBusSubscriber(modid = GalGameFramework.MODID, value = [Dist.CLIENT])
object DevToolsKeyBinding {
    private val OPEN_DEV_TOOLS = KeyMapping(
        "key.galgame.dev_tools",
        GLFW.GLFW_KEY_HOME,
        net.minecraft.client.KeyMapping.Category.MISC
    )
    
    private val OPEN_MAIN_MENU = KeyMapping(
        "key.galgame.main_menu",
        GLFW.GLFW_KEY_G,
        net.minecraft.client.KeyMapping.Category.MISC
    )
    
    @SubscribeEvent
    @JvmStatic
    fun onRegisterKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(OPEN_DEV_TOOLS)
        event.register(OPEN_MAIN_MENU)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onClientTick(event: Post) {
        val mc = Minecraft.getInstance()
        while (OPEN_DEV_TOOLS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(DevToolsScreen())
            }
        }
        while (OPEN_MAIN_MENU.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(MainMenuScreen())
            }
        }
    }
}

