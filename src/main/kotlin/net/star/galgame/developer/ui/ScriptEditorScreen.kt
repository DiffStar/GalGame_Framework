package net.star.galgame.developer.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.developer.ContentPackEditor
import net.star.galgame.developer.DevLogger
import org.lwjgl.glfw.GLFW

class ScriptEditorScreen(
    private val editor: ContentPackEditor,
    private val scriptId: String
) : Screen(Component.literal("脚本编辑器: $scriptId")) {
    private var contentBox: EditBox? = null
    private var errorText: String? = null
    
    private val headerHeight = 32
    private val footerHeight = 40
    private val padding = 12
    
    override fun init() {
        super.init()
        
        val content = editor.loadScriptContent(scriptId) ?: ""
        val contentAreaHeight = height - headerHeight - footerHeight - padding * 2
        
        contentBox = EditBox(font, padding, headerHeight + padding, width - padding * 2, contentAreaHeight, Component.literal("脚本内容"))
        contentBox?.setValue(content)
        contentBox?.setMaxLength(Int.MAX_VALUE)
        addRenderableWidget(contentBox)
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        renderHeader(graphics, mouseX, mouseY)
        renderFooter(graphics, mouseX, mouseY)
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    private fun renderHeader(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, 0, width, headerHeight, Theme.BACKGROUND_DARK)
        Theme.drawDivider(graphics, 0, headerHeight, width)
        
        graphics.drawString(font, "脚本编辑器: $scriptId", padding, (headerHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderFooter(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val footerY = height - footerHeight
        graphics.fill(0, footerY, width, height, Theme.BACKGROUND_DARK)
        Theme.drawDivider(graphics, 0, footerY, width)
        
        val buttonY = footerY + (footerHeight - 20) / 2
        val buttonWidth = 80
        val buttonSpacing = 8
        
        val saveButtonX = width / 2 - buttonWidth * 2 - buttonSpacing
        val validateButtonX = width / 2 - buttonWidth / 2
        val cancelButtonX = width / 2 + buttonWidth + buttonSpacing
        
        val isSaveHovered = mouseX >= saveButtonX && mouseX <= saveButtonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + 20
        val isValidateHovered = mouseX >= validateButtonX && mouseX <= validateButtonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + 20
        val isCancelHovered = mouseX >= cancelButtonX && mouseX <= cancelButtonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + 20
        
        Theme.drawButton(graphics, saveButtonX, buttonY, buttonWidth, 20, isSaveHovered)
        Theme.drawButton(graphics, validateButtonX, buttonY, buttonWidth, 20, isValidateHovered)
        Theme.drawButton(graphics, cancelButtonX, buttonY, buttonWidth, 20, isCancelHovered)
        
        graphics.drawString(font, "保存", saveButtonX + buttonWidth / 2 - font.width("保存") / 2, buttonY + 10 - font.lineHeight / 2, Theme.TEXT_PRIMARY, false)
        graphics.drawString(font, "验证", validateButtonX + buttonWidth / 2 - font.width("验证") / 2, buttonY + 10 - font.lineHeight / 2, Theme.TEXT_PRIMARY, false)
        graphics.drawString(font, "取消", cancelButtonX + buttonWidth / 2 - font.width("取消") / 2, buttonY + 10 - font.lineHeight / 2, Theme.TEXT_PRIMARY, false)
        
        if (errorText != null) {
            val errorColor = if (errorText!!.contains("失败") || errorText!!.contains("错误")) {
                Theme.STATUS_ERROR
            } else {
                Theme.STATUS_SUCCESS
            }
            graphics.drawString(font, errorText!!, padding, footerY - font.lineHeight - 4, errorColor, false)
        }
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, captured)
        
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        
        val footerY = height - footerHeight
        val buttonY = footerY + (footerHeight - 20) / 2
        val buttonWidth = 80
        val buttonSpacing = 8
        
        val saveButtonX = width / 2 - buttonWidth * 2 - buttonSpacing
        val validateButtonX = width / 2 - buttonWidth / 2
        val cancelButtonX = width / 2 + buttonWidth + buttonSpacing
        
        if (mouseX >= saveButtonX && mouseX <= saveButtonX + buttonWidth &&
            mouseY >= buttonY && mouseY <= buttonY + 20) {
            saveScript()
            return true
        }
        
        if (mouseX >= validateButtonX && mouseX <= validateButtonX + buttonWidth &&
            mouseY >= buttonY && mouseY <= buttonY + 20) {
            validateScript()
            return true
        }
        
        if (mouseX >= cancelButtonX && mouseX <= cancelButtonX + buttonWidth &&
            mouseY >= buttonY && mouseY <= buttonY + 20) {
            onClose()
            return true
        }
        
        return super.mouseClicked(event, captured)
    }
    
    private fun saveScript() {
        val content = contentBox?.value ?: return
        if (editor.saveScriptContent(scriptId, content)) {
            errorText = "保存成功"
            DevLogger.info("ScriptEditor", "脚本已保存: $scriptId")
        } else {
            errorText = "保存失败"
        }
    }
    
    private fun validateScript() {
        val result = editor.validateScript(scriptId)
        if (result.isValid) {
            errorText = "验证通过"
        } else {
            errorText = "验证失败: ${result.errors.joinToString(", ")}"
        }
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
}

