package net.star.galgame.dialogue.character

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

class CharacterRenderer {
    private var currentAlpha = 0f
    private var targetAlpha = 1f
    private val fadeSpeed = 0.1f

    fun updateFade() {
        if (currentAlpha < targetAlpha) {
            currentAlpha = (currentAlpha + fadeSpeed).coerceAtMost(targetAlpha)
        } else if (currentAlpha > targetAlpha) {
            currentAlpha = (currentAlpha - fadeSpeed).coerceAtLeast(targetAlpha)
        }
    }

    fun fadeIn() {
        targetAlpha = 1f
    }

    fun fadeOut() {
        targetAlpha = 0f
    }

    fun render(
        graphics: GuiGraphics,
        texture: ResourceLocation,
        position: net.star.galgame.dialogue.CharacterPosition,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        scale: Float = 1f
    ) {
        updateFade()
        
        if (currentAlpha <= 0f) return

        val actualX = when (position) {
            net.star.galgame.dialogue.CharacterPosition.LEFT -> x
            net.star.galgame.dialogue.CharacterPosition.CENTER -> x - (width * scale / 2).toInt()
            net.star.galgame.dialogue.CharacterPosition.RIGHT -> x - (width * scale).toInt()
        }
        
        val actualY = y - (height * scale).toInt()
        val actualWidth = (width * scale).toInt()
        val actualHeight = (height * scale).toInt()
        
        graphics.blitInscribed(texture, actualX, actualY, actualWidth, actualHeight, width, height, false, false)
    }

    fun isFadeComplete(): Boolean {
        return kotlin.math.abs(currentAlpha - targetAlpha) < 0.01f
    }
}

