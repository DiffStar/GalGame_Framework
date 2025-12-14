package net.star.galgame.dialogue.choice

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.Font
import net.star.galgame.dialogue.ChoiceEntry
import kotlin.math.sin

class ChoiceRenderer {
    private var hoveredIndex = -1
    private var animationTime = 0f
    private val animationSpeed = 0.05f

    fun update(deltaTime: Float) {
        animationTime += deltaTime * animationSpeed
        if (animationTime > 1f) animationTime -= 1f
    }

    fun setHovered(index: Int) {
        hoveredIndex = index
    }

    fun render(
        graphics: GuiGraphics,
        font: Font,
        choices: List<ChoiceEntry>,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int
    ) {
        val visibleChoices = choices.filter { it.visible }
        if (visibleChoices.isEmpty()) return

        val choiceHeight = 30
        val spacing = 5
        var currentY = y

        visibleChoices.forEachIndexed { index, choice ->
            val choiceY = currentY
            val isHovered = hoveredIndex == index || 
                (mouseX >= x && mouseX <= x + width && 
                 mouseY >= choiceY && mouseY <= choiceY + choiceHeight)

            if (isHovered) {
                hoveredIndex = index
            }

            val highlightAlpha = if (isHovered) {
                (sin(animationTime * Math.PI * 2) * 0.3 + 0.7).toFloat()
            } else {
                0.3f
            }

            val bgColor = (0xFF * highlightAlpha).toInt() shl 24 or 0x000000
            graphics.fill(x, choiceY, x + width, choiceY + choiceHeight, bgColor)

            val textColor = if (isHovered) 0xFFFFFF else 0xCCCCCC
            val textX = x + 10
            val textY = choiceY + (choiceHeight - font.lineHeight) / 2

            graphics.drawString(
                font,
                "${index + 1}. ${choice.text}",
                textX,
                textY,
                textColor,
                false
            )

            if (isHovered) {
                graphics.fill(x, choiceY, x + 3, choiceY + choiceHeight, 0xFFFFFFFF.toInt())
            }

            currentY += choiceHeight + spacing
        }
    }

    fun getChoiceAt(
        choices: List<ChoiceEntry>,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int
    ): Int? {
        val visibleChoices = choices.filter { it.visible }
        if (visibleChoices.isEmpty()) return null

        val choiceHeight = 30
        val spacing = 5
        var currentY = y

        visibleChoices.forEachIndexed { index, _ ->
            val choiceY = currentY
            if (mouseX >= x && mouseX <= x + width && 
                mouseY >= choiceY && mouseY <= choiceY + choiceHeight) {
                return index
            }
            currentY += choiceHeight + spacing
        }

        return null
    }

    fun reset() {
        hoveredIndex = -1
        animationTime = 0f
    }
}

