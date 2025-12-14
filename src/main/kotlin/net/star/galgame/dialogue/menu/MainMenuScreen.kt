package net.star.galgame.dialogue.menu

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.dialogue.DialogueScreen
import net.star.galgame.dialogue.save.SaveManager
import org.lwjgl.glfw.GLFW

class MainMenuScreen : Screen(Component.literal("GalGame Main Menu")) {
    override fun init() {
        super.init()
        
        val buttonWidth = 200
        val buttonHeight = 30
        val buttonSpacing = 10
        val startY = height / 2 - 60
        
        addRenderableWidget(
            Button.builder(Component.literal("开始游戏")) {
                startNewGame()
            }.bounds(width / 2 - buttonWidth / 2, startY, buttonWidth, buttonHeight).build()
        )
        
        val continueButton = Button.builder(Component.literal("继续游戏")) {
            continueGame()
        }.bounds(width / 2 - buttonWidth / 2, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight).build()
        
        val hasSave = SaveManager.getAutoSave() != null
        continueButton.active = hasSave
        addRenderableWidget(continueButton)
        
        addRenderableWidget(
            Button.builder(Component.literal("设置")) {
                minecraft?.setScreen(SettingsScreen {
                    minecraft?.setScreen(this@MainMenuScreen)
                })
            }.bounds(width / 2 - buttonWidth / 2, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("退出游戏")) {
                minecraft?.stop()
            }.bounds(width / 2 - buttonWidth / 2, startY + (buttonHeight + buttonSpacing) * 3, buttonWidth, buttonHeight).build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        val title = Component.literal("GalGame Framework")
        graphics.drawCenteredString(font, title, width / 2, height / 4, 0xFFFFFF)
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            minecraft?.stop()
            return true
        }
        return super.keyPressed(event)
    }
    
    private fun startNewGame() {
        val scripts = DialogueManager.getAllScripts()
        if (scripts.isEmpty()) {
            return
        }
        val script = scripts.first()
        val screen = DialogueScreen(script)
        minecraft?.setScreen(screen)
    }
    
    private fun continueGame() {
        val autoSave = SaveManager.getAutoSave() ?: return
        val script = DialogueManager.getScript(autoSave.scriptId) ?: return
        
        val controller = net.star.galgame.dialogue.control.DialogueController(script)
        SaveManager.applySaveData(autoSave, controller)
        
        val screen = DialogueScreen(script, controller)
        minecraft?.setScreen(screen)
    }
}

