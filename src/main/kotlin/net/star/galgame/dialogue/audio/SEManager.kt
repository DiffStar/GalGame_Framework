package net.star.galgame.dialogue.audio

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3

class SEManager {
    private val minecraft = Minecraft.getInstance()
    private var seVolume = 1.0f
    private val activeSounds = mutableMapOf<ResourceLocation, SimpleSoundInstance>()

    fun playClick(resource: ResourceLocation) {
        play(resource, 0.8f)
    }

    fun playChoice(resource: ResourceLocation) {
        play(resource, 0.9f)
    }

    fun playSceneChange(resource: ResourceLocation) {
        play(resource, 1.0f)
    }

    fun play(resource: ResourceLocation, pitch: Float = 1.0f, volume: Float = 1.0f, position: Vec3? = null) {
        val soundEvent = SoundEvent.createVariableRangeEvent(resource)
        val finalVolume = volume * seVolume
        
        val soundInstance = if (position != null) {
            SimpleSoundInstance(
                soundEvent,
                SoundSource.MASTER,
                finalVolume,
                pitch,
                minecraft.level?.random,
                position.x, position.y, position.z
            )
        } else {
            val player = minecraft.player
            if (player != null) {
                SimpleSoundInstance(
                    soundEvent,
                    SoundSource.MASTER,
                    finalVolume,
                    pitch,
                    minecraft.level?.random,
                    player.x, player.y, player.z
                )
            } else {
                SimpleSoundInstance(
                    soundEvent,
                    SoundSource.MASTER,
                    finalVolume,
                    pitch,
                    minecraft.level?.random,
                    0.0, 0.0, 0.0
                )
            }
        }
        
        minecraft.soundManager.play(soundInstance)
        activeSounds[resource] = soundInstance
    }

    fun play3D(resource: ResourceLocation, x: Double, y: Double, z: Double, pitch: Float = 1.0f, volume: Float = 1.0f) {
        play(resource, pitch, volume, Vec3(x, y, z))
    }

    fun stop(resource: ResourceLocation) {
        activeSounds[resource]?.let {
            minecraft.soundManager.stop(it)
            activeSounds.remove(resource)
        }
    }

    fun stopAll() {
        activeSounds.values.forEach {
            minecraft.soundManager.stop(it)
        }
        activeSounds.clear()
    }

    fun setVolume(volume: Float) {
        seVolume = volume.coerceIn(0.0f, 1.0f)
    }

    fun getVolume(): Float = seVolume

    fun isPlaying(resource: ResourceLocation): Boolean {
        return activeSounds[resource]?.let { minecraft.soundManager.isActive(it) } ?: false
    }

    fun update() {
        activeSounds.entries.removeAll { (_, sound) ->
            !minecraft.soundManager.isActive(sound)
        }
    }
}

