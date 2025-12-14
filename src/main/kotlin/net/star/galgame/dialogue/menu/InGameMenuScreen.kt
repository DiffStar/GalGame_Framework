package net.star.galgame.dialogue.menu

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.star.galgame.dialogue.DialogueScreen
import net.star.galgame.dialogue.control.DialogueController
import net.star.galgame.dialogue.save.LoadScreen
import net.star.galgame.dialogue.save.SaveScreen
import org.lwjgl.glfw.GLFW

class InGameMenuScreen(
    private val dialogueScreen: DialogueScreen,
    private val scriptId: String,
    private val controller: DialogueController
) : Screen(Component.literal("Game Menu")) {
    override fun init() {
        super.init()
        
        val buttonWidth = 200
        val buttonHeight = 30
        val buttonSpacing = 10
        val startY = height / 2 - 60
        
        addRenderableWidget(
            Button.builder(Component.literal("存档")) {
                minecraft?.setScreen(SaveScreen(scriptId, controller) {
                    minecraft?.setScreen(this@InGameMenuScreen)
                })
            }.bounds(width / 2 - buttonWidth / 2, startY, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("读档")) {
                minecraft?.setScreen(LoadScreen(
                    onLoadComplete = { loadedScreen ->
                        minecraft?.setScreen(loadedScreen)
                    },
                    onCancel = {
                        minecraft?.setScreen(this@InGameMenuScreen)
                    }
                ))
            }.bounds(width / 2 - buttonWidth / 2, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("设置")) {
                minecraft?.setScreen(SettingsScreen {
                    minecraft?.setScreen(this@InGameMenuScreen)
                })
            }.bounds(width / 2 - buttonWidth / 2, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("返回游戏")) {
                onClose()
            }.bounds(width / 2 - buttonWidth / 2, startY + (buttonHeight + buttonSpacing) * 3, buttonWidth, buttonHeight).build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        val title = Component.literal("游戏菜单")
        graphics.drawCenteredString(font, title, width / 2, height / 4, 0xFFFFFF)
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
    
    override fun onClose() {
        minecraft?.setScreen(dialogueScreen)
    }
}

