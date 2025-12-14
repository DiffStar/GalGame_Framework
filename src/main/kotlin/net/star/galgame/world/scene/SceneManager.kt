package net.star.galgame.world.scene

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.star.galgame.dialogue.visual.BackgroundManager
import net.star.galgame.dialogue.visual.BackgroundConfig
import net.star.galgame.dialogue.visual.BackgroundType
import net.star.galgame.world.scene.transition.SceneTransition
import java.util.concurrent.ConcurrentHashMap

object SceneManager {
    private val scenes = ConcurrentHashMap<String, Scene>()
    private var currentScene: Scene? = null
    private val cameraController = CameraController()
    private val backgroundManager = BackgroundManager()

    fun registerScene(scene: Scene) {
        scenes[scene.id] = scene
    }

    fun unregisterScene(id: String) {
        scenes.remove(id)
        if (currentScene?.id == id) {
            currentScene = null
        }
    }

    fun getScene(id: String): Scene? = scenes[id]

    fun setScene(id: String) {
        val scene = scenes[id] ?: return
        val previousScene = currentScene
        currentScene = scene

        if (scene.background2D != null) {
            backgroundManager.setBackground(BackgroundConfig(
                texture = scene.background2D,
                type = BackgroundType.STATIC
            ))
        }

        if (scene.cameraPosition != null) {
            cameraController.setTargetPosition(scene.cameraPosition, scene.cameraRotation)
        }

        if (previousScene != null && scene.transition != null) {
            SceneTransition.playTransition(scene.transition)
        }
    }

    fun getCurrentScene(): Scene? = currentScene

    fun update(deltaTime: Float) {
        cameraController.update(deltaTime)
    }

    fun resetCamera() {
        cameraController.reset()
    }
}

