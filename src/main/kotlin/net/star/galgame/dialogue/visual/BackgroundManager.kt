package net.star.galgame.dialogue.visual

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import kotlin.math.cos
import kotlin.math.sin

enum class BackgroundType {
    STATIC,
    ANIMATED
}

data class BackgroundConfig(
    val texture: ResourceLocation? = null,
    val type: BackgroundType = BackgroundType.STATIC,
    val blur: Float = 0f,
    val brightness: Float = 1f,
    val animationSpeed: Float = 1f,
    val animationOffsetX: Float = 0f,
    val animationOffsetY: Float = 0f
)

class BackgroundManager {
    private var currentBackground: BackgroundConfig? = null
    private var nextBackground: BackgroundConfig? = null
    private var transitionProgress = 1f
    private var transitionDuration = 0.5f
    private var animationTime = 0f
    private var isTransitioning = false

    fun setBackground(config: BackgroundConfig, duration: Float = 0.5f) {
        if (currentBackground == null) {
            currentBackground = config
            transitionProgress = 1f
            isTransitioning = false
        } else {
            nextBackground = config
            transitionProgress = 0f
            transitionDuration = duration
            isTransitioning = true
        }
    }

    fun update(deltaTime: Float) {
        animationTime += deltaTime

        if (isTransitioning) {
            transitionProgress += deltaTime / transitionDuration
            if (transitionProgress >= 1f) {
                transitionProgress = 1f
                currentBackground = nextBackground
                nextBackground = null
                isTransitioning = false
            }
        }
    }

    fun render(graphics: GuiGraphics, width: Int, height: Int) {
        val current = currentBackground
        if (current == null) {
            graphics.fill(0, 0, width, height, 0xFF000000.toInt())
            return
        }

        if (isTransitioning && nextBackground != null) {
            renderBackgroundLayer(graphics, current, width, height, 1f - transitionProgress)
            renderBackgroundLayer(graphics, nextBackground!!, width, height, transitionProgress)
        } else {
            renderBackgroundLayer(graphics, current, width, height, 1f)
        }
    }

    private fun renderBackgroundLayer(
        graphics: GuiGraphics,
        config: BackgroundConfig,
        width: Int,
        height: Int,
        alpha: Float
    ) {
        val texture = config.texture ?: return

        val brightness = config.brightness * alpha
        val color = ((brightness * 255).toInt() shl 16) or
                   ((brightness * 255).toInt() shl 8) or
                   (brightness * 255).toInt() or
                   ((alpha * 255).toInt() shl 24)

        when (config.type) {
            BackgroundType.STATIC -> {
                graphics.blitInscribed(texture, 0, 0, width, height, width, height, false, false)
            }
            BackgroundType.ANIMATED -> {
                val offsetX = (sin(animationTime * config.animationSpeed) * config.animationOffsetX).toInt()
                val offsetY = (cos(animationTime * config.animationSpeed) * config.animationOffsetY).toInt()
                graphics.blitInscribed(texture, offsetX, offsetY, width, height, width, height, false, false)
            }
        }

        if (config.blur > 0f) {
            val blurAlpha = (config.blur * alpha * 0.3f).coerceIn(0f, 1f)
            graphics.fill(0, 0, width, height, ((blurAlpha * 255).toInt() shl 24) or 0x000000)
        }
    }

    fun getCurrentBackground(): BackgroundConfig? = currentBackground
    fun isTransitioning(): Boolean = isTransitioning
}

