package net.star.galgame.developer.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.developer.ResourceManager
import org.lwjgl.glfw.GLFW

class ResourceManagerScreen(
    private val resourceManager: ResourceManager
) : Screen(Component.literal("资源管理器")) {
    private var selectedType: ResourceManager.ResourceType? = null
    private var resources: Map<ResourceManager.ResourceType, List<ResourceManager.ResourceInfo>> = emptyMap()
    private var scrollOffset = 0
    
    private val headerHeight = 32
    private val padding = 12
    private val buttonHeight = 24
    private val buttonSpacing = 8
    
    override fun init() {
        super.init()
        resources = resourceManager.scanResources()
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
        
        graphics.drawString(font, "资源管理器", padding, (headerHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
        
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
        
        val filterY = headerHeight + padding
        var filterX = padding
        
        for (type in ResourceManager.ResourceType.values()) {
            val buttonWidth = font.width(type.name) + 16
            val isSelected = selectedType == type
            val isHovered = mouseX >= filterX && mouseX <= filterX + buttonWidth &&
                    mouseY >= filterY && mouseY <= filterY + buttonHeight
            
            Theme.drawButton(graphics, filterX, filterY, buttonWidth, buttonHeight, isHovered, isSelected)
            
            if (isSelected) {
                Theme.drawSelectionIndicator(graphics, filterX, filterY, buttonHeight)
            }
            
            val textColor = if (isSelected) Theme.TEXT_PRIMARY else Theme.TEXT_SECONDARY
            val textX = filterX + buttonWidth / 2 - font.width(type.name) / 2
            val textY = filterY + buttonHeight / 2 - font.lineHeight / 2
            graphics.drawString(font, type.name, textX, textY, textColor, false)
            
            filterX += buttonWidth + buttonSpacing
        }
        
        val contentY = filterY + buttonHeight + padding + 8
        Theme.drawDivider(graphics, padding, contentY - 4, width - padding * 2)
        
        val filteredResources = if (selectedType != null) {
            resources[selectedType] ?: emptyList()
        } else {
            resources.values.flatten()
        }
        
        var currentY = contentY + 8
        for (resource in filteredResources.drop(scrollOffset)) {
            if (currentY > height - padding - 20) break
            
            Theme.drawPanel(graphics, padding, currentY, width - padding * 2, font.lineHeight + 8)
            
            val sizeText = formatSize(resource.size)
            val text = "${resource.path.fileName} ($sizeText)"
            graphics.drawString(font, text, padding + 8, currentY + 4, Theme.TEXT_PRIMARY, false)
            
            currentY += font.lineHeight + 12
        }
        
        val totalSize = filteredResources.sumOf { it.size }
        val footerText = "总计: ${formatSize(totalSize)} (${filteredResources.size} 个文件)"
        graphics.drawString(font, footerText, padding, height - padding - font.lineHeight, Theme.TEXT_SECONDARY, false)
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, captured)
        
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        
        val filterY = headerHeight + padding
        var filterX = padding
        
        for (type in ResourceManager.ResourceType.values()) {
            val buttonWidth = font.width(type.name) + 16
            if (mouseX >= filterX && mouseX <= filterX + buttonWidth &&
                mouseY >= filterY && mouseY <= filterY + buttonHeight) {
                selectedType = if (selectedType == type) null else type
                return true
            }
            filterX += buttonWidth + buttonSpacing
        }
        
        val refreshButtonX = width - 100 - padding
        val refreshButtonY = (headerHeight - 20) / 2
        if (mouseX >= refreshButtonX && mouseX <= refreshButtonX + 100 &&
            mouseY >= refreshButtonY && mouseY <= refreshButtonY + 20) {
            resources = resourceManager.scanResources()
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

