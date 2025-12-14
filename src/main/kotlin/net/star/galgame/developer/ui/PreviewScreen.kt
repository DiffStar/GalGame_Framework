package net.star.galgame.developer.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.developer.PreviewTool
import org.lwjgl.glfw.GLFW

class PreviewScreen(
    private val previewTool: PreviewTool,
    private val scriptId: String
) : Screen(Component.literal("预览: $scriptId")) {
    private var preview: PreviewTool.ScriptPreview? = null
    private var errorText: String? = null
    private var scrollOffset = 0
    
    private val headerHeight = 32
    private val padding = 12
    
    override fun init() {
        super.init()
        
        val result = previewTool.previewScript(scriptId)
        if (result.errors.isEmpty() && result.preview != null) {
            preview = result.preview
            errorText = null
        } else {
            errorText = result.errors.joinToString("\n")
        }
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        renderHeader(graphics, mouseX, mouseY)
        renderContent(graphics, mouseX, mouseY)
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    private fun renderHeader(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, 0, width, headerHeight, Theme.BACKGROUND_DARK)
        Theme.drawDivider(graphics, 0, headerHeight, width)
        
        graphics.drawString(font, "脚本预览: $scriptId", padding, (headerHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
        
        val refreshButtonX = width - 100 - padding
        val refreshButtonY = (headerHeight - 20) / 2
        val isRefreshHovered = mouseX >= refreshButtonX && mouseX <= refreshButtonX + 100 &&
                mouseY >= refreshButtonY && mouseY <= refreshButtonY + 20
        
        Theme.drawButton(graphics, refreshButtonX, refreshButtonY, 100, 20, isRefreshHovered)
        val refreshTextX = refreshButtonX + 50 - font.width("刷新") / 2
        val refreshTextY = refreshButtonY + 10 - font.lineHeight / 2
        graphics.drawString(font, "刷新", refreshTextX, refreshTextY, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, headerHeight, width, height, Theme.BACKGROUND_LIGHT)
        
        val contentX = padding
        var contentY = headerHeight + padding
        
        if (errorText != null) {
            graphics.drawString(font, errorText!!, contentX, contentY, Theme.STATUS_ERROR, false)
            return
        }
        
        val preview = this.preview ?: return
        
        graphics.drawString(font, "脚本ID: ${preview.scriptId}", contentX, contentY, Theme.TEXT_PRIMARY, false)
        contentY += font.lineHeight + 8
        
        Theme.drawDivider(graphics, contentX, contentY, width - padding * 2)
        contentY += 12
        
        graphics.drawString(font, "总条目数: ${preview.totalEntries}", contentX, contentY, Theme.TEXT_SECONDARY, false)
        contentY += font.lineHeight + 4
        
        graphics.drawString(font, "总选择数: ${preview.totalChoices}", contentX, contentY, Theme.TEXT_SECONDARY, false)
        contentY += font.lineHeight + 8
        
        graphics.drawString(font, "角色: ${preview.characters.joinToString(", ")}", contentX, contentY, Theme.TEXT_SECONDARY, false)
        contentY += font.lineHeight + 4
        
        graphics.drawString(font, "标签: ${preview.labels.joinToString(", ")}", contentX, contentY, Theme.TEXT_SECONDARY, false)
        contentY += font.lineHeight + 12
        
        Theme.drawDivider(graphics, contentX, contentY, width - padding * 2)
        contentY += 12
        
        graphics.drawString(font, "条目预览:", contentX, contentY, Theme.TEXT_PRIMARY, false)
        contentY += font.lineHeight + 8
        
        val visibleEntries = preview.entryPreviews.drop(scrollOffset)
        for (entryPreview in visibleEntries) {
            if (contentY > height - padding) break
            
            Theme.drawPanel(graphics, contentX, contentY, width - padding * 2, font.lineHeight * 3 + 8)
            
            val entryText = "[${entryPreview.id}] ${entryPreview.textPreview}"
            graphics.drawString(font, entryText, contentX + 8, contentY + 4, Theme.TEXT_PRIMARY, false)
            var entryY = contentY + font.lineHeight + 6
            
            if (entryPreview.hasChoices) {
                graphics.drawString(font, "  - 包含选择", contentX + 16, entryY, Theme.STATUS_SUCCESS, false)
                entryY += font.lineHeight + 2
            }
            
            if (entryPreview.hasCondition) {
                graphics.drawString(font, "  - 包含条件", contentX + 16, entryY, Theme.STATUS_WARNING, false)
            }
            
            contentY += font.lineHeight * 3 + 12
        }
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, captured)
        
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        
        val refreshButtonX = width - 100 - padding
        val refreshButtonY = (headerHeight - 20) / 2
        if (mouseX >= refreshButtonX && mouseX <= refreshButtonX + 100 &&
            mouseY >= refreshButtonY && mouseY <= refreshButtonY + 20) {
            val newResult = previewTool.previewScript(scriptId)
            if (newResult.errors.isEmpty() && newResult.preview != null) {
                preview = newResult.preview
                errorText = null
            } else {
                errorText = newResult.errors.joinToString("\n")
            }
            return true
        }
        
        return super.mouseClicked(event, captured)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        scrollOffset = (scrollOffset - scrollY.toInt()).coerceAtLeast(0)
        return true
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
}

