package net.star.galgame.developer.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.developer.DevModeManager
import net.star.galgame.developer.DevLogger
import org.lwjgl.glfw.GLFW

class DevToolsScreen : Screen(Component.literal("开发工具")) {
    private var selectedTab = 0
    private val tabs = listOf("日志", "调试器", "变量", "性能", "编辑器")
    
    private val sidebarWidth = 180
    private val headerHeight = 32
    private val padding = 12
    private val tabButtonHeight = 24
    
    override fun init() {
        super.init()
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, captured)
        
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        
        if (mouseX < sidebarWidth) {
            val tabStartY = headerHeight + padding
            for ((index, _) in tabs.withIndex()) {
                val tabY = tabStartY + index * (tabButtonHeight + 4)
                if (mouseY >= tabY && mouseY <= tabY + tabButtonHeight) {
                    selectedTab = index
                    return true
                }
            }
        }
        
        val toggleButtonX = width - 100 - padding
        val toggleButtonY = headerHeight - 24
        if (mouseX >= toggleButtonX && mouseX <= toggleButtonX + 100 &&
            mouseY >= toggleButtonY && mouseY <= toggleButtonY + 20) {
            DevModeManager.toggle()
            return true
        }
        
        return super.mouseClicked(event, captured)
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        
        renderHeader(graphics, mouseX, mouseY)
        renderSidebar(graphics, mouseX, mouseY)
        renderContent(graphics, mouseX, mouseY)
    }
    
    private fun renderHeader(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, 0, width, headerHeight, Theme.BACKGROUND_DARK)
        Theme.drawDivider(graphics, 0, headerHeight, width)
        
        graphics.drawString(font, "开发工具", padding, (headerHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
        
        val toggleButtonX = width - 100 - padding
        val toggleButtonY = (headerHeight - 20) / 2
        val toggleText = if (DevModeManager.isEnabled()) "开发模式: 开启" else "开发模式: 关闭"
        val isToggleHovered = mouseX >= toggleButtonX && mouseX <= toggleButtonX + 100 &&
                mouseY >= toggleButtonY && mouseY <= toggleButtonY + 20
        
        Theme.drawButton(graphics, toggleButtonX, toggleButtonY, 100, 20, isToggleHovered, DevModeManager.isEnabled())
        
        val textColor = if (DevModeManager.isEnabled()) Theme.STATUS_SUCCESS else Theme.TEXT_TERTIARY
        val toggleTextX = toggleButtonX + 50 - font.width(toggleText) / 2
        val toggleTextY = toggleButtonY + 10 - font.lineHeight / 2
        graphics.drawString(font, toggleText, toggleTextX, toggleTextY, textColor, false)
    }
    
    private fun renderSidebar(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, headerHeight, sidebarWidth, height, Theme.BACKGROUND_MEDIUM)
        Theme.drawDivider(graphics, sidebarWidth, headerHeight, 1)
        
        val tabStartY = headerHeight + padding
        for ((index, tab) in tabs.withIndex()) {
            val tabY = tabStartY + index * (tabButtonHeight + 4)
            val isSelected = selectedTab == index
            val isHovered = mouseX >= padding && mouseX < sidebarWidth - padding &&
                    mouseY >= tabY && mouseY <= tabY + tabButtonHeight
            
            if (isSelected || isHovered) {
                Theme.drawButton(
                    graphics,
                    padding, tabY,
                    sidebarWidth - padding * 2, tabButtonHeight,
                    isHovered, isSelected
                )
            }
            
            if (isSelected) {
                Theme.drawSelectionIndicator(graphics, padding, tabY, tabButtonHeight)
            }
            
            val textColor = if (isSelected) Theme.TEXT_PRIMARY else Theme.TEXT_SECONDARY
            graphics.drawString(font, tab, padding + 8, tabY + (tabButtonHeight - font.lineHeight) / 2, textColor, false)
        }
    }
    
    private fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(sidebarWidth, headerHeight, width, height, Theme.BACKGROUND_LIGHT)
        
        val contentX = sidebarWidth + padding
        val contentY = headerHeight + padding
        
        when (selectedTab) {
            0 -> renderLogsTab(graphics, mouseX, mouseY, contentX, contentY)
            1 -> renderDebuggerTab(graphics, mouseX, mouseY, contentX, contentY)
            2 -> renderVariablesTab(graphics, mouseX, mouseY, contentX, contentY)
            3 -> renderPerformanceTab(graphics, mouseX, mouseY, contentX, contentY)
            4 -> renderEditorTab(graphics, mouseX, mouseY, contentX, contentY)
        }
    }
    
    private fun renderLogsTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        graphics.drawString(font, "日志", startX, startY, Theme.TEXT_PRIMARY, false)
        Theme.drawDivider(graphics, startX, startY + font.lineHeight + 8, width - sidebarWidth - padding * 2)
        
        val logs = DevLogger.getLogs(limit = 50)
        var currentY = startY + font.lineHeight + 20
        
        for (log in logs.reversed()) {
            val color = when (log.level) {
                DevLogger.LogLevel.DEBUG -> Theme.TEXT_TERTIARY
                DevLogger.LogLevel.INFO -> Theme.TEXT_PRIMARY
                DevLogger.LogLevel.WARN -> Theme.STATUS_WARNING
                DevLogger.LogLevel.ERROR -> Theme.STATUS_ERROR
            }
            
            val logText = "[${log.timestamp}] [${log.level.displayName}] [${log.category}] ${log.message}"
            graphics.drawString(font, logText, startX, currentY, color, false)
            currentY += font.lineHeight + 4
            
            if (currentY > height - padding) break
        }
    }
    
    private fun renderDebuggerTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        graphics.drawString(font, "脚本调试器", startX, startY, Theme.TEXT_PRIMARY, false)
        graphics.drawString(font, "功能: 断点、单步执行、变量监视", startX, startY + font.lineHeight + 8, Theme.TEXT_SECONDARY, false)
    }
    
    private fun renderVariablesTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        graphics.drawString(font, "变量监视器", startX, startY, Theme.TEXT_PRIMARY, false)
        
        var currentY = startY + font.lineHeight + 16
        
        if (DevModeManager.isEnabled()) {
            val variables = net.star.galgame.developer.VariableWatcher.getAllVariables()
            for ((name, value) in variables) {
                val text = "$name = $value"
                graphics.drawString(font, text, startX, currentY, Theme.TEXT_PRIMARY, false)
                currentY += font.lineHeight + 4
                if (currentY > height - padding) break
            }
        } else {
            graphics.drawString(font, "请先启用开发模式", startX, currentY, Theme.STATUS_WARNING, false)
        }
    }
    
    private fun renderPerformanceTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        graphics.drawString(font, "性能分析", startX, startY, Theme.TEXT_PRIMARY, false)
        
        var currentY = startY + font.lineHeight + 16
        
        if (DevModeManager.isEnabled()) {
            val profiles = net.star.galgame.developer.PerformanceProfiler.getTopProfiles(15)
            for (profile in profiles) {
                val text = "${profile.name}: ${String.format("%.2f", profile.averageTime / 1_000_000.0)} ms (${profile.callCount} 次)"
                graphics.drawString(font, text, startX, currentY, Theme.TEXT_PRIMARY, false)
                currentY += font.lineHeight + 4
                if (currentY > height - padding) break
            }
        } else {
            graphics.drawString(font, "请先启用开发模式", startX, currentY, Theme.STATUS_WARNING, false)
        }
    }
    
    private fun renderEditorTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        graphics.drawString(font, "内容包编辑器", startX, startY, Theme.TEXT_PRIMARY, false)
        graphics.drawString(font, "功能: 脚本编辑、资源管理、预览", startX, startY + font.lineHeight + 8, Theme.TEXT_SECONDARY, false)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
}
