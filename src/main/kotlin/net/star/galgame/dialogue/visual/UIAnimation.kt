package net.star.galgame.dialogue.visual

enum class AnimationType {
    FADE_IN,
    SLIDE_UP,
    SLIDE_DOWN,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    NONE
}

class UIAnimation(
    private val type: AnimationType,
    private val duration: Float = 0.3f
) {
    private var progress = 0f
    private var isActive = false
    private var isComplete = false

    fun start() {
        progress = 0f
        isActive = true
        isComplete = false
    }

    fun update(deltaTime: Float) {
        if (!isActive) return

        progress += deltaTime / duration
        if (progress >= 1f) {
            progress = 1f
            isActive = false
            isComplete = true
        }
    }

    fun getAlpha(): Float {
        return when (type) {
            AnimationType.FADE_IN -> easeOut(progress)
            AnimationType.SLIDE_UP, AnimationType.SLIDE_DOWN,
            AnimationType.SLIDE_LEFT, AnimationType.SLIDE_RIGHT -> easeOut(progress)
            AnimationType.NONE -> 1f
        }
    }

    fun getOffsetX(width: Int): Int {
        return when (type) {
            AnimationType.SLIDE_LEFT -> ((1f - easeOut(progress)) * width).toInt()
            AnimationType.SLIDE_RIGHT -> ((-(1f - easeOut(progress)) * width)).toInt()
            else -> 0
        }
    }

    fun getOffsetY(height: Int): Int {
        return when (type) {
            AnimationType.SLIDE_UP -> ((1f - easeOut(progress)) * height).toInt()
            AnimationType.SLIDE_DOWN -> ((-(1f - easeOut(progress)) * height)).toInt()
            else -> 0
        }
    }

    fun isActive(): Boolean = isActive
    fun isComplete(): Boolean = isComplete

    private fun easeOut(t: Float): Float {
        return 1f - (1f - t) * (1f - t)
    }
}

class AnimationManager {
    private val animations = mutableMapOf<String, UIAnimation>()

    fun addAnimation(key: String, animation: UIAnimation) {
        animations[key] = animation
    }

    fun startAnimation(key: String) {
        animations[key]?.start()
    }

    fun update(deltaTime: Float) {
        animations.values.forEach { it.update(deltaTime) }
    }

    fun getAnimation(key: String): UIAnimation? = animations[key]

    fun removeAnimation(key: String) {
        animations.remove(key)
    }

    fun clear() {
        animations.clear()
    }
}

