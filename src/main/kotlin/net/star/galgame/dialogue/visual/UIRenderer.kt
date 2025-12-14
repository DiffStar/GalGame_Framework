package net.star.galgame.dialogue.visual

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component

class UIRenderer {
    fun renderDialogBox(
        graphics: GuiGraphics,
        theme: UITheme,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        animation: UIAnimation? = null
    ) {
        val style = theme.dialogBox
        val offsetX = animation?.getOffsetX(width) ?: 0
        val offsetY = animation?.getOffsetY(height) ?: 0
        val alpha = animation?.getAlpha() ?: 1f

        val actualX = x + offsetX
        val actualY = y + offsetY

        val bgAlpha = ((style.backgroundColor shr 24) and 0xFF) / 255f * alpha
        val bgColor = ((bgAlpha * 255).toInt() shl 24) or (style.backgroundColor and 0x00FFFFFF)

        if (style.cornerRadius > 0) {
            renderRoundedBox(graphics, actualX, actualY, width, height, style.cornerRadius, bgColor)
        } else {
            graphics.fill(actualX, actualY, actualX + width, actualY + height, bgColor)
        }

        if (style.borderThickness > 0) {
            val borderAlpha = ((style.borderColor shr 24) and 0xFF) / 255f * alpha
            val borderColor = ((borderAlpha * 255).toInt() shl 24) or (style.borderColor and 0x00FFFFFF)
            renderBorder(graphics, actualX, actualY, width, height, style.borderThickness, borderColor)
        }

        if (style.blur > 0f) {
            val blurAlpha = (style.blur * alpha * 0.2f).coerceIn(0f, 1f)
            graphics.fill(actualX, actualY, actualX + width, actualY + height, ((blurAlpha * 255).toInt() shl 24) or 0x000000)
        }
    }

    fun renderNameBox(
        graphics: GuiGraphics,
        font: Font,
        theme: UITheme,
        text: String,
        x: Int,
        y: Int,
        animation: UIAnimation? = null
    ) {
        val style = theme.nameBox
        val offsetX = animation?.getOffsetX(100) ?: 0
        val offsetY = animation?.getOffsetY(30) ?: 0
        val alpha = animation?.getAlpha() ?: 1f

        val actualX = x + offsetX
        val actualY = y + offsetY

        val textWidth = font.width(text) + style.padding * 2
        val textHeight = font.lineHeight + style.padding * 2

        val bgAlpha = ((style.backgroundColor shr 24) and 0xFF) / 255f * alpha
        val bgColor = ((bgAlpha * 255).toInt() shl 24) or (style.backgroundColor and 0x00FFFFFF)

        graphics.fill(actualX, actualY, actualX + textWidth, actualY + textHeight, bgColor)

        if (style.borderColor != 0) {
            val borderAlpha = ((style.borderColor shr 24) and 0xFF) / 255f * alpha
            val borderColor = ((borderAlpha * 255).toInt() shl 24) or (style.borderColor and 0x00FFFFFF)
            graphics.fill(actualX, actualY, actualX + textWidth, actualY + 2, borderColor)
            graphics.fill(actualX, actualY + textHeight - 2, actualX + textWidth, actualY + textHeight, borderColor)
            graphics.fill(actualX, actualY, actualX + 2, actualY + textHeight, borderColor)
            graphics.fill(actualX + textWidth - 2, actualY, actualX + textWidth, actualY + textHeight, borderColor)
        }

        val textAlpha = ((style.textColor shr 24) and 0xFF) / 255f * alpha
        val textColor = ((textAlpha * 255).toInt() shl 24) or (style.textColor and 0x00FFFFFF)

        graphics.drawString(
            font,
            text,
            actualX + style.padding,
            actualY + style.padding,
            textColor,
            false
        )
    }

    fun renderText(
        graphics: GuiGraphics,
        font: Font,
        theme: UITheme,
        text: Component,
        x: Int,
        y: Int,
        width: Int,
        animation: UIAnimation? = null
    ) {
        val style = theme.text
        val offsetX = animation?.getOffsetX(width) ?: 0
        val offsetY = animation?.getOffsetY(50) ?: 0
        val alpha = animation?.getAlpha() ?: 1f

        val actualX = x + offsetX
        val actualY = y + offsetY

        val textAlpha = ((style.color shr 24) and 0xFF) / 255f * alpha
        val textColor = ((textAlpha * 255).toInt() shl 24) or (style.color and 0x00FFFFFF)

        if (style.shadow) {
            graphics.drawString(font, text, actualX + 1, actualY + 1, 0x000000, false)
        }
        graphics.drawString(font, text, actualX, actualY, textColor, false)
    }

    private fun renderRoundedBox(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        color: Int
    ) {
        graphics.fill(x + radius, y, x + width - radius, y + height, color)
        graphics.fill(x, y + radius, x + width, y + height - radius, color)
        graphics.fill(x + radius, y, x + width - radius, y + radius, color)
        graphics.fill(x + radius, y + height - radius, x + width - radius, y + height, color)
    }

    private fun renderBorder(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        thickness: Int,
        color: Int
    ) {
        graphics.fill(x, y, x + width, y + thickness, color)
        graphics.fill(x, y + height - thickness, x + width, y + height, color)
        graphics.fill(x, y, x + thickness, y + height, color)
        graphics.fill(x + width - thickness, y, x + width, y + height, color)
    }
}

