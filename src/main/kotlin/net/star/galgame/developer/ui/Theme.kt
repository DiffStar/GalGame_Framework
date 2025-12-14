package net.star.galgame.developer.ui

import net.minecraft.client.gui.GuiGraphics

object Theme {
    const val BACKGROUND_DARK = 0xE0000000.toInt()
    const val BACKGROUND_MEDIUM = 0xD0000000.toInt()
    const val BACKGROUND_LIGHT = 0xC0000000.toInt()
    
    const val PANEL_BG = 0xF01A1A1A.toInt()
    const val PANEL_BORDER = 0xFF2A2A2A.toInt()
    
    const val ACCENT_PRIMARY = 0xFF4A90E2.toInt()
    const val ACCENT_HOVER = 0xFF5AA0F2.toInt()
    const val ACCENT_DISABLED = 0xFF3A3A3A.toInt()
    
    const val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    const val TEXT_SECONDARY = 0xFFCCCCCC.toInt()
    const val TEXT_TERTIARY = 0xFF888888.toInt()
    const val TEXT_DISABLED = 0xFF666666.toInt()
    
    const val STATUS_SUCCESS = 0xFF4CAF50.toInt()
    const val STATUS_WARNING = 0xFFFFFF00.toInt()
    const val STATUS_ERROR = 0xFFFF4444.toInt()
    const val STATUS_INFO = 0xFF2196F3.toInt()
    
    const val BUTTON_BG = 0xFF2A2A2A.toInt()
    const val BUTTON_BG_HOVER = 0xFF3A3A3A.toInt()
    const val BUTTON_BG_ACTIVE = 0xFF4A4A4A.toInt()
    const val BUTTON_BORDER = 0xFF3A3A3A.toInt()
    
    const val DIVIDER = 0xFF2A2A2A.toInt()
    
    fun drawButton(
        graphics: GuiGraphics,
        x: Int, y: Int, width: Int, height: Int,
        isHovered: Boolean = false,
        isActive: Boolean = false,
        hasBorder: Boolean = true
    ) {
        val bgColor = when {
            isActive -> BUTTON_BG_ACTIVE
            isHovered -> BUTTON_BG_HOVER
            else -> BUTTON_BG
        }
        
        graphics.fill(x, y, x + width, y + height, bgColor)
        
        if (hasBorder) {
            graphics.fill(x, y, x + width, y + 1, BUTTON_BORDER)
            graphics.fill(x, y + height - 1, x + width, y + height, BUTTON_BORDER)
            graphics.fill(x, y, x + 1, y + height, BUTTON_BORDER)
            graphics.fill(x + width - 1, y, x + width, y + height, BUTTON_BORDER)
        }
    }
    
    fun drawPanel(
        graphics: GuiGraphics,
        x: Int, y: Int, width: Int, height: Int,
        hasBorder: Boolean = true
    ) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG)
        
        if (hasBorder) {
            graphics.fill(x, y, x + width, y + 1, PANEL_BORDER)
            graphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER)
            graphics.fill(x, y, x + 1, y + height, PANEL_BORDER)
            graphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER)
        }
    }
    
    fun drawDivider(graphics: GuiGraphics, x: Int, y: Int, width: Int) {
        graphics.fill(x, y, x + width, y + 1, DIVIDER)
    }
    
    fun drawSelectionIndicator(graphics: GuiGraphics, x: Int, y: Int, height: Int) {
        graphics.fill(x, y, x + 3, y + height, ACCENT_PRIMARY)
    }
}

