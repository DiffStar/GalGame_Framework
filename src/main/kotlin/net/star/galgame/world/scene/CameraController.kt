package net.star.galgame.world.scene

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

class CameraController {
    private var targetPosition: BlockPos? = null
    private var targetRotation: CameraRotation? = null
    private var isActive = false
    private var transitionProgress = 0f
    private val transitionDuration = 1.0f
    private var startYaw = 0f
    private var startPitch = 0f
    private var startPos: Vec3? = null

    fun setTargetPosition(position: BlockPos, rotation: CameraRotation?) {
        val player = Minecraft.getInstance().player ?: return
        targetPosition = position
        targetRotation = rotation
        isActive = true
        transitionProgress = 0f
        startYaw = player.yRot
        startPitch = player.xRot
        startPos = player.position()
    }

    fun update(deltaTime: Float) {
        if (!isActive) return

        val player = Minecraft.getInstance().player as? LocalPlayer ?: return
        val targetPos = targetPosition ?: return

        transitionProgress += deltaTime / transitionDuration
        if (transitionProgress > 1f) {
            transitionProgress = 1f
        }

        val t = smoothStep(transitionProgress)
        val start = startPos ?: player.position()
        val end = Vec3(
            targetPos.x + 0.5,
            targetPos.y + 1.0,
            targetPos.z + 0.5
        )

        val currentPos = start.lerp(end, t.toDouble())
        player.setPos(currentPos.x, currentPos.y, currentPos.z)

        if (targetRotation != null) {
            val targetYaw = Mth.lerp(t, startYaw, targetRotation!!.yaw)
            val targetPitch = Mth.lerp(t, startPitch, targetRotation!!.pitch)
            player.setYRot(targetYaw)
            player.setXRot(targetPitch)
        }
    }

    fun reset() {
        isActive = false
        targetPosition = null
        targetRotation = null
        transitionProgress = 0f
    }

    fun isActive(): Boolean = isActive

    private fun smoothStep(t: Float): Float {
        return t * t * (3f - 2f * t)
    }
}

