package net.star.galgame.world.scene.transition

enum class TransitionTypeEnum {
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

enum class EasingTypeEnum {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    EASE_OUT_BOUNCE,
    EASE_OUT_ELASTIC,
    EASE_IN_BACK,
    EASE_OUT_BACK
}

data class SceneTransitionConfig(
    val type: TransitionTypeEnum,
    val duration: Float = 0.5f,
    val easing: EasingTypeEnum = EasingTypeEnum.EASE_IN_OUT
)

