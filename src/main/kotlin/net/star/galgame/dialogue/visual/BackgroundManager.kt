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
    private var transitionAnimation: TransitionAnimation? = null
    private var animationTime = 0f
    private var screenWidth = 0
    private var screenHeight = 0

    fun setBackground(config: BackgroundConfig, transitionType: TransitionType = TransitionType.FADE, duration: Float = 0.5f) {
        if (currentBackground == null) {
            currentBackground = config
        } else {
            nextBackground = config
            transitionAnimation = TransitionAnimation(transitionType, duration)
            transitionAnimation?.start()
        }
    }

    fun update(deltaTime: Float) {
        animationTime += deltaTime
        transitionAnimation?.update(deltaTime)
        if (transitionAnimation?.isComplete() == true && nextBackground != null) {
            currentBackground = nextBackground
            nextBackground = null
            transitionAnimation = null
        }
    }

    fun render(graphics: GuiGraphics, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        val current = currentBackground
        if (current == null) {
            graphics.fill(0, 0, width, height, 0xFF000000.toInt())
            return
        }

        val transition = transitionAnimation
        if (transition != null && transition.isActive() && nextBackground != null) {
            transition.renderTransition(
                graphics,
                width,
                height,
                { renderBackgroundLayer(it, current, width, height, 1f) },
                { renderBackgroundLayer(it, nextBackground!!, width, height, 1f) }
            )
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
    fun isTransitioning(): Boolean = transitionAnimation?.isActive() ?: false
}

