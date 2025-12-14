package net.star.galgame.world.scene

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.star.galgame.world.scene.transition.SceneTransitionConfig

data class Scene(
    val id: String,
    val background2D: ResourceLocation? = null,
    val cameraPosition: BlockPos? = null,
    val cameraRotation: CameraRotation? = null,
    val transition: SceneTransitionConfig? = null
)

data class CameraRotation(
    val yaw: Float = 0f,
    val pitch: Float = 0f
)

