package net.star.galgame.dialogue.visual

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import java.lang.Math.pow
import kotlin.math.pow

enum class CharacterAnimationType {
    FADE_IN,
    FADE_OUT,
    SLIDE_IN_LEFT,
    SLIDE_IN_RIGHT,
    SLIDE_IN_UP,
    SLIDE_IN_DOWN,
    SLIDE_OUT_LEFT,
    SLIDE_OUT_RIGHT,
    SLIDE_OUT_UP,
    SLIDE_OUT_DOWN,
    SCALE_IN,
    SCALE_OUT,
    BOUNCE_IN,
    SHAKE,
    NONE
}

class CharacterAnimation(
    private val type: CharacterAnimationType,
    private val duration: Float = 0.5f,
    private val easing: EasingType = EasingType.EASE_OUT
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

    fun getAlpha(): Float {
        val eased = applyEasing(progress)
        return when (type) {
            CharacterAnimationType.FADE_IN -> eased
            CharacterAnimationType.FADE_OUT -> 1f - eased
            CharacterAnimationType.SLIDE_IN_LEFT, CharacterAnimationType.SLIDE_IN_RIGHT,
            CharacterAnimationType.SLIDE_IN_UP, CharacterAnimationType.SLIDE_IN_DOWN,
            CharacterAnimationType.SLIDE_OUT_LEFT, CharacterAnimationType.SLIDE_OUT_RIGHT,
            CharacterAnimationType.SLIDE_OUT_UP, CharacterAnimationType.SLIDE_OUT_DOWN,
            CharacterAnimationType.SCALE_IN, CharacterAnimationType.SCALE_OUT,
            CharacterAnimationType.BOUNCE_IN, CharacterAnimationType.SHAKE -> 1f
            CharacterAnimationType.NONE -> 1f
        }
    }

    fun getOffsetX(baseX: Int, width: Int): Int {
        val eased = applyEasing(progress)
        return when (type) {
            CharacterAnimationType.SLIDE_IN_LEFT -> baseX - ((1f - eased) * width).toInt()
            CharacterAnimationType.SLIDE_IN_RIGHT -> baseX + ((1f - eased) * width).toInt()
            CharacterAnimationType.SLIDE_OUT_LEFT -> baseX - (eased * width).toInt()
            CharacterAnimationType.SLIDE_OUT_RIGHT -> baseX + (eased * width).toInt()
            CharacterAnimationType.SHAKE -> baseX + (kotlin.math.sin(progress * 50f) * 10f * (1f - eased)).toInt()
            else -> baseX
        }
    }

    fun getOffsetY(baseY: Int, height: Int): Int {
        val eased = applyEasing(progress)
        return when (type) {
            CharacterAnimationType.SLIDE_IN_UP -> baseY + ((1f - eased) * height).toInt()
            CharacterAnimationType.SLIDE_IN_DOWN -> baseY - ((1f - eased) * height).toInt()
            CharacterAnimationType.SLIDE_OUT_UP -> baseY + (eased * height).toInt()
            CharacterAnimationType.SLIDE_OUT_DOWN -> baseY - (eased * height).toInt()
            CharacterAnimationType.SHAKE -> baseY + (kotlin.math.cos(progress * 50f) * 10f * (1f - eased)).toInt()
            else -> baseY
        }
    }

    fun getScale(): Float {
        val eased = applyEasing(progress)
        return when (type) {
            CharacterAnimationType.SCALE_IN -> eased
            CharacterAnimationType.SCALE_OUT -> 1f - eased * 0.5f
            CharacterAnimationType.BOUNCE_IN -> bounceIn(eased)
            else -> 1f
        }
    }

    fun getRotation(): Float {
        return when (type) {
            CharacterAnimationType.SHAKE -> kotlin.math.sin(progress * 30f) * 5f * (1f - applyEasing(progress))
            else -> 0f
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

    private fun bounceIn(t: Float): Float = 1f - bounceOut(1f - t)

    private fun elasticOut(t: Float): Float {
        val c4 = (2f * kotlin.math.PI) / 3f
        return if (t == 0f) 0f else if (t == 1f) 1f else
            pow(2.0, (-10.0 * t).toDouble()).toFloat() * kotlin.math.sin((t * 10f - 0.75f) * c4).toFloat() + 1f
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
        return 1f + c3 * pow(t1, 3.0).toFloat() + c1 * pow(t1, 2.0).toFloat()
    }
}

class CustomAnimation(
    private val duration: Float,
    private val updateFn: (Float) -> Unit,
    private val onComplete: (() -> Unit)? = null
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
            onComplete?.invoke()
        } else {
            updateFn(progress)
        }
    }

    override fun reset() {
        progress = 0f
        isActive = false
        isComplete = false
    }

    override fun isActive(): Boolean = isActive
    override fun isComplete(): Boolean = isComplete

    fun getProgress(): Float = progress
}

