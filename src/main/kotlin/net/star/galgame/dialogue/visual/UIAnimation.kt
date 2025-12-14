package net.star.galgame.dialogue.visual

import java.lang.Math.pow
import kotlin.math.pow

enum class AnimationType {
    FADE_IN,
    FADE_OUT,
    SLIDE_UP,
    SLIDE_DOWN,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SCALE_IN,
    SCALE_OUT,
    ROTATE_IN,
    BOUNCE_IN,
    ELASTIC_IN,
    NONE
}

enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    EASE_OUT_BOUNCE,
    EASE_OUT_ELASTIC,
    EASE_IN_BACK,
    EASE_OUT_BACK
}

interface IAnimation {
    fun start()
    fun update(deltaTime: Float)
    fun isActive(): Boolean
    fun isComplete(): Boolean
    fun reset()
}

class UIAnimation(
    private val type: AnimationType,
    private val duration: Float = 0.3f,
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

    fun getAlpha(): Float {
        val eased = applyEasing(progress)
        return when (type) {
            AnimationType.FADE_IN -> eased
            AnimationType.FADE_OUT -> 1f - eased
            AnimationType.SLIDE_UP, AnimationType.SLIDE_DOWN,
            AnimationType.SLIDE_LEFT, AnimationType.SLIDE_RIGHT,
            AnimationType.SCALE_IN, AnimationType.SCALE_OUT,
            AnimationType.ROTATE_IN, AnimationType.BOUNCE_IN,
            AnimationType.ELASTIC_IN -> eased
            AnimationType.NONE -> 1f
        }
    }

    fun getOffsetX(width: Int): Int {
        val eased = applyEasing(progress)
        return when (type) {
            AnimationType.SLIDE_LEFT -> ((1f - eased) * width).toInt()
            AnimationType.SLIDE_RIGHT -> ((-(1f - eased) * width)).toInt()
            else -> 0
        }
    }

    fun getOffsetY(height: Int): Int {
        val eased = applyEasing(progress)
        return when (type) {
            AnimationType.SLIDE_UP -> ((1f - eased) * height).toInt()
            AnimationType.SLIDE_DOWN -> ((-(1f - eased) * height)).toInt()
            else -> 0
        }
    }

    fun getScale(): Float {
        val eased = applyEasing(progress)
        return when (type) {
            AnimationType.SCALE_IN -> eased
            AnimationType.SCALE_OUT -> 1f - eased * 0.5f
            AnimationType.BOUNCE_IN -> bounceIn(eased)
            AnimationType.ELASTIC_IN -> elasticIn(eased)
            else -> 1f
        }
    }

    fun getRotation(): Float {
        val eased = applyEasing(progress)
        return when (type) {
            AnimationType.ROTATE_IN -> (1f - eased) * 360f
            else -> 0f
        }
    }

    override fun isActive(): Boolean = isActive
    override fun isComplete(): Boolean = isComplete

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

    private fun elasticIn(t: Float): Float {
        val c4 = (2f * kotlin.math.PI) / 3f
        return if (t == 0f) 0f else if (t == 1f) 1f else
            -pow(2.0, (10.0 * (t - 1.0)).toDouble()).toFloat() * kotlin.math.sin((t * 10f - 10.75f) * c4).toFloat()
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

class AnimationManager {
    private val animations = mutableMapOf<String, IAnimation>()

    fun addAnimation(key: String, animation: IAnimation) {
        animations[key] = animation
    }

    fun startAnimation(key: String) {
        animations[key]?.start()
    }

    fun update(deltaTime: Float) {
        animations.values.forEach { it.update(deltaTime) }
    }

    fun getAnimation(key: String): IAnimation? = animations[key]

    fun <T : IAnimation> getAnimationAs(key: String, clazz: Class<T>): T? {
        return animations[key]?.let { clazz.cast(it) }
    }

    fun removeAnimation(key: String) {
        animations.remove(key)
    }

    fun clear() {
        animations.values.forEach { it.reset() }
        animations.clear()
    }

    fun pauseAnimation(key: String) {
        animations[key]?.let {
            if (it is UIAnimation) {
                it.reset()
            }
        }
    }
}

