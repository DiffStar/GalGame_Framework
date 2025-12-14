package net.star.galgame.dialogue.visual

import net.minecraft.client.gui.GuiGraphics
import java.lang.Math

enum class TransitionType {
    FADE,
    CROSSFADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    CIRCLE,
    DIAMOND,
    WIPE_LEFT,
    WIPE_RIGHT,
    WIPE_UP,
    WIPE_DOWN,
    BLINDS,
    PIXELATE
}

class TransitionAnimation(
    private val type: TransitionType,
    private val duration: Float = 0.5f,
    private val easing: EasingType = EasingType.EASE_IN_OUT
) : IAnimation {
    private var progress = 0f
    private var isActive = false
    private var isComplete = false

    override fun start() {
        progress = 0f
        isActive = true
        isComplete = false
    }

    override fun update(deltaTime: Float) {
        if (!isActive) return
        progress += deltaTime / duration
        if (progress >= 1f) {
            progress = 1f
            isActive = false
            isComplete = true
        }
    }

    override fun reset() {
        progress = 0f
        isActive = false
        isComplete = false
    }

    override fun isActive(): Boolean = isActive
    override fun isComplete(): Boolean = isComplete

    fun getProgress(): Float = applyEasing(progress)

    fun renderTransition(
        graphics: GuiGraphics,
        width: Int,
        height: Int,
        renderOld: (GuiGraphics) -> Unit,
        renderNew: (GuiGraphics) -> Unit
    ) {
        val t = getProgress()
        when (type) {
            TransitionType.FADE -> {
                val oldAlpha = ((255 * (1f - t)).toInt() shl 24) or 0x00FFFFFF
                val newAlpha = ((255 * t).toInt() shl 24) or 0x00FFFFFF
                renderOld(graphics)
                renderNew(graphics)
            }
            TransitionType.CROSSFADE -> {
                renderOld(graphics)
                renderNew(graphics)
            }
            TransitionType.SLIDE_LEFT -> {
                renderOld(graphics)
                val wipeX = (width * (1f - t)).toInt()
                graphics.enableScissor(wipeX, 0, width, height)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.SLIDE_RIGHT -> {
                renderOld(graphics)
                val wipeX = (width * t).toInt()
                graphics.enableScissor(0, 0, wipeX, height)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.SLIDE_UP -> {
                renderOld(graphics)
                val wipeY = (height * (1f - t)).toInt()
                graphics.enableScissor(0, wipeY, width, height)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.SLIDE_DOWN -> {
                renderOld(graphics)
                val wipeY = (height * t).toInt()
                graphics.enableScissor(0, 0, width, wipeY)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.CIRCLE -> {
                renderOld(graphics)
                val radius = (kotlin.math.sqrt((width * width + height * height).toDouble()) * t).toInt()
                val centerX = width / 2
                val centerY = height / 2
                graphics.enableScissor(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.DIAMOND -> {
                renderOld(graphics)
                val size = (kotlin.math.sqrt((width * width + height * height).toDouble()) * t).toInt()
                val centerX = width / 2
                val centerY = height / 2
                graphics.enableScissor(centerX - size, centerY - size, centerX + size, centerY + size)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.WIPE_LEFT -> {
                renderOld(graphics)
                val wipeX = (width * (1f - t)).toInt()
                graphics.enableScissor(wipeX, 0, width, height)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.WIPE_RIGHT -> {
                renderOld(graphics)
                val wipeX = (width * t).toInt()
                graphics.enableScissor(0, 0, wipeX, height)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.WIPE_UP -> {
                renderOld(graphics)
                val wipeY = (height * (1f - t)).toInt()
                graphics.enableScissor(0, wipeY, width, height)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.WIPE_DOWN -> {
                renderOld(graphics)
                val wipeY = (height * t).toInt()
                graphics.enableScissor(0, 0, width, wipeY)
                renderNew(graphics)
                graphics.disableScissor()
            }
            TransitionType.BLINDS -> {
                val blinds = 10
                val blindHeight = height / blinds
                for (i in 0 until blinds) {
                    val blindT = ((t * blinds) - i).coerceIn(0f, 1f)
                    val blindY = i * blindHeight
                    val blindH = (blindHeight * blindT).toInt()
                    graphics.enableScissor(0, blindY, width, blindY + blindH)
                    renderNew(graphics)
                    graphics.disableScissor()
                }
                renderOld(graphics)
            }
            TransitionType.PIXELATE -> {
                if (t < 0.5f) {
                    renderOld(graphics)
                } else {
                    renderNew(graphics)
                }
            }
        }
    }

    private fun applyEasing(t: Float): Float {
        return when (easing) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN -> t * t
            EasingType.EASE_OUT -> 1f - (1f - t) * (1f - t)
            EasingType.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).let { it * it } / 2f
            EasingType.EASE_OUT_BOUNCE -> bounceOut(t)
            EasingType.EASE_OUT_ELASTIC -> elasticOut(t)
            EasingType.EASE_IN_BACK -> backIn(t)
            EasingType.EASE_OUT_BACK -> backOut(t)
        }
    }

    private fun bounceOut(t: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            t < 1f / d1 -> n1 * t * t
            t < 2f / d1 -> n1 * (t - 1.5f / d1) * (t - 1.5f / d1) + 0.75f
            t < 2.5f / d1 -> n1 * (t - 2.25f / d1) * (t - 2.25f / d1) + 0.9375f
            else -> n1 * (t - 2.625f / d1) * (t - 2.625f / d1) + 0.984375f
        }
    }

    private fun elasticOut(t: Float): Float {
        val c4 = (2f * kotlin.math.PI) / 3f
        return if (t == 0f) 0f else if (t == 1f) 1f else
            Math.pow(2.0, (-10.0 * t).toDouble()).toFloat() * kotlin.math.sin((t * 10f - 0.75f) * c4).toFloat() + 1f
    }

    private fun backIn(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return c3 * t * t * t - c1 * t * t
    }

    private fun backOut(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val t1 = (t - 1.0).toDouble()
        return 1f + c3 * Math.pow(t1, 3.0).toFloat() + c1 * Math.pow(t1, 2.0).toFloat()
    }
}

