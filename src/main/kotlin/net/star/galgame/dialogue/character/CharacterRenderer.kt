package net.star.galgame.dialogue.character

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.star.galgame.dialogue.visual.CharacterAnimation
import net.star.galgame.dialogue.visual.CharacterAnimationType
import net.star.galgame.dialogue.visual.EasingType

class CharacterRenderer {
    private var currentAnimation: CharacterAnimation? = null
    private var baseX = 0
    private var baseY = 0
    private var baseWidth = 0
    private var baseHeight = 0

    fun setAnimation(type: CharacterAnimationType, duration: Float = 0.5f, easing: EasingType = EasingType.EASE_OUT) {
        currentAnimation = CharacterAnimation(type, duration, easing)
        currentAnimation?.start()
    }

    fun updateAnimation(deltaTime: Float) {
        currentAnimation?.update(deltaTime)
    }

    fun fadeIn(duration: Float = 0.5f) {
        setAnimation(CharacterAnimationType.FADE_IN, duration)
    }

    fun fadeOut(duration: Float = 0.5f) {
        setAnimation(CharacterAnimationType.FADE_OUT, duration)
    }

    fun slideInLeft(duration: Float = 0.5f) {
        setAnimation(CharacterAnimationType.SLIDE_IN_LEFT, duration)
    }

    fun slideInRight(duration: Float = 0.5f) {
        setAnimation(CharacterAnimationType.SLIDE_IN_RIGHT, duration)
    }

    fun slideInUp(duration: Float = 0.5f) {
        setAnimation(CharacterAnimationType.SLIDE_IN_UP, duration)
    }

    fun slideInDown(duration: Float = 0.5f) {
        setAnimation(CharacterAnimationType.SLIDE_IN_DOWN, duration)
    }

    fun scaleIn(duration: Float = 0.5f) {
        setAnimation(CharacterAnimationType.SCALE_IN, duration)
    }

    fun bounceIn(duration: Float = 0.8f) {
        setAnimation(CharacterAnimationType.BOUNCE_IN, duration, EasingType.EASE_OUT_BOUNCE)
    }

    fun shake(duration: Float = 0.3f) {
        setAnimation(CharacterAnimationType.SHAKE, duration)
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
        baseX = x
        baseY = y
        baseWidth = width
        baseHeight = height

        val anim = currentAnimation
        val alpha = anim?.getAlpha() ?: 1f
        if (alpha <= 0f) return

        val animScale = anim?.getScale() ?: 1f
        val finalScale = scale * animScale
        val animX = anim?.getOffsetX(x, width) ?: x
        val animY = anim?.getOffsetY(y, height) ?: y

        val actualX = when (position) {
            net.star.galgame.dialogue.CharacterPosition.LEFT -> animX
            net.star.galgame.dialogue.CharacterPosition.CENTER -> animX - (width * finalScale / 2).toInt()
            net.star.galgame.dialogue.CharacterPosition.RIGHT -> animX - (width * finalScale).toInt()
        }
        
        val actualY = animY - (height * finalScale).toInt()
        val actualWidth = (width * finalScale).toInt()
        val actualHeight = (height * finalScale).toInt()

        graphics.blitInscribed(texture, actualX, actualY, actualWidth, actualHeight, width, height, false, false)
    }

    fun isAnimationComplete(): Boolean {
        return currentAnimation?.isComplete() ?: true
    }

    fun hasActiveAnimation(): Boolean {
        return currentAnimation?.isActive() ?: false
    }
}

