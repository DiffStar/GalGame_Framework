package net.star.galgame.world.scene.transition

import net.star.galgame.dialogue.visual.TransitionAnimation
import net.star.galgame.dialogue.visual.TransitionType
import net.star.galgame.dialogue.visual.EasingType

object SceneTransition {
    private var currentTransition: TransitionAnimation? = null

    fun playTransition(config: SceneTransitionConfig) {
        val transitionType = when (config.type) {
            TransitionTypeEnum.FADE -> TransitionType.FADE
            TransitionTypeEnum.CROSSFADE -> TransitionType.CROSSFADE
            TransitionTypeEnum.SLIDE_LEFT -> TransitionType.SLIDE_LEFT
            TransitionTypeEnum.SLIDE_RIGHT -> TransitionType.SLIDE_RIGHT
            TransitionTypeEnum.SLIDE_UP -> TransitionType.SLIDE_UP
            TransitionTypeEnum.SLIDE_DOWN -> TransitionType.SLIDE_DOWN
            TransitionTypeEnum.CIRCLE -> TransitionType.CIRCLE
            TransitionTypeEnum.DIAMOND -> TransitionType.DIAMOND
            TransitionTypeEnum.WIPE_LEFT -> TransitionType.WIPE_LEFT
            TransitionTypeEnum.WIPE_RIGHT -> TransitionType.WIPE_RIGHT
            TransitionTypeEnum.WIPE_UP -> TransitionType.WIPE_UP
            TransitionTypeEnum.WIPE_DOWN -> TransitionType.WIPE_DOWN
            TransitionTypeEnum.BLINDS -> TransitionType.BLINDS
            TransitionTypeEnum.PIXELATE -> TransitionType.PIXELATE
        }

        val easingType = when (config.easing) {
            EasingTypeEnum.LINEAR -> EasingType.LINEAR
            EasingTypeEnum.EASE_IN -> EasingType.EASE_IN
            EasingTypeEnum.EASE_OUT -> EasingType.EASE_OUT
            EasingTypeEnum.EASE_IN_OUT -> EasingType.EASE_IN_OUT
            EasingTypeEnum.EASE_OUT_BOUNCE -> EasingType.EASE_OUT_BOUNCE
            EasingTypeEnum.EASE_OUT_ELASTIC -> EasingType.EASE_OUT_ELASTIC
            EasingTypeEnum.EASE_IN_BACK -> EasingType.EASE_IN_BACK
            EasingTypeEnum.EASE_OUT_BACK -> EasingType.EASE_OUT_BACK
        }

        currentTransition = TransitionAnimation(transitionType, config.duration, easingType)
        currentTransition?.start()
    }

    fun update(deltaTime: Float) {
        currentTransition?.update(deltaTime)
        if (currentTransition?.isComplete() == true) {
            currentTransition = null
        }
    }

    fun getCurrentTransition(): TransitionAnimation? = currentTransition
}

