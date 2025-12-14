package net.star.galgame.dialogue.audio

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource



class BGMManager {
    private val minecraft = Minecraft.getInstance()
    private var currentBGM: SimpleSoundInstance? = null
    private var currentSoundEvent: SoundEvent? = null
    private var fadeTargetVolume = 1.0f
    private var fadeCurrentVolume = 1.0f
    private var isFading = false
    private var fadeSpeed = 0.0f
    private var bgmVolume = 1.0f
    private val bgmQueue = mutableListOf<ResourceLocation>()
    private var currentBGMIndex = -1

    fun play(resource: ResourceLocation, fadeIn: Boolean = false, fadeDuration: Float = 1.0f) {
        stop(false, 0f)
        
        currentSoundEvent = SoundEvent.createVariableRangeEvent(resource)
        val initialVolume = if (fadeIn) 0.0f else bgmVolume
        val player = minecraft.player
        currentBGM = if (player != null) {
            SimpleSoundInstance(
                currentSoundEvent!!,
                SoundSource.MUSIC,
                initialVolume,
                1.0f,
                minecraft.level?.random,
                player.x, player.y, player.z
            )
        } else {
            SimpleSoundInstance(
                currentSoundEvent!!,
                SoundSource.MUSIC,
                initialVolume,
                1.0f,
                minecraft.level?.random,
                0.0, 0.0, 0.0
            )
        }
        currentBGMIndex = bgmQueue.indexOf(resource)
        if (currentBGMIndex == -1) {
            bgmQueue.add(resource)
            currentBGMIndex = bgmQueue.size - 1
        }
        
        if (fadeIn) {
            fadeCurrentVolume = 0.0f
            fadeTargetVolume = bgmVolume
            fadeSpeed = bgmVolume / fadeDuration
            isFading = true
        } else {
            fadeCurrentVolume = bgmVolume
            fadeTargetVolume = bgmVolume
            isFading = false
        }
        
        currentBGM?.let {
            minecraft.soundManager.play(it)
        }
    }

    fun pause() {
        currentBGM?.let {
            minecraft.soundManager.stop(it)
        }
    }

    fun resume() {
        if (currentSoundEvent != null && !minecraft.soundManager.isActive(currentBGM!!)) {
            val player = minecraft.player
            currentBGM = if (player != null) {
                SimpleSoundInstance(
                    currentSoundEvent!!,
                    SoundSource.MUSIC,
                    fadeCurrentVolume,
                    1.0f,
                    minecraft.level?.random,
                    player.x, player.y, player.z
                )
            } else {
                SimpleSoundInstance(
                    currentSoundEvent!!,
                    SoundSource.MUSIC,
                    fadeCurrentVolume,
                    1.0f,
                    minecraft.level?.random,
                    0.0, 0.0, 0.0
                )
            }
            minecraft.soundManager.play(currentBGM!!)
        }
    }

    fun stop(fadeOut: Boolean = false, fadeDuration: Float = 1.0f) {
        if (fadeOut && currentBGM != null) {
            fadeTargetVolume = 0.0f
            fadeSpeed = fadeCurrentVolume / fadeDuration
            isFading = true
        } else {
            currentBGM?.let {
                minecraft.soundManager.stop(it)
            }
            currentBGM = null
            currentSoundEvent = null
            currentBGMIndex = -1
            isFading = false
            fadeCurrentVolume = 1.0f
            fadeTargetVolume = 1.0f
        }
    }

    fun switchTrack(resource: ResourceLocation, fadeDuration: Float = 1.0f) {
        stop(fadeOut = true, fadeDuration = fadeDuration / 2.0f)
        play(resource, fadeIn = true, fadeDuration = fadeDuration / 2.0f)
    }

    fun setVolume(volume: Float) {
        bgmVolume = volume.coerceIn(0.0f, 1.0f)
        if (!isFading) {
            fadeCurrentVolume = bgmVolume
            fadeTargetVolume = bgmVolume
        }
        if (currentSoundEvent != null && currentBGM != null) {
            val player = minecraft.player
            val oldInstance = currentBGM!!
            currentBGM = if (player != null) {
                SimpleSoundInstance(
                    currentSoundEvent!!,
                    SoundSource.MUSIC,
                    fadeCurrentVolume,
                    1.0f,
                    minecraft.level?.random,
                    player.x, player.y, player.z
                )
            } else {
                SimpleSoundInstance(
                    currentSoundEvent!!,
                    SoundSource.MUSIC,
                    fadeCurrentVolume,
                    1.0f,
                    minecraft.level?.random,
                    0.0, 0.0, 0.0
                )
            }
            minecraft.soundManager.stop(oldInstance)
            minecraft.soundManager.play(currentBGM!!)
        }
    }

    fun getVolume(): Float = bgmVolume

    fun update(deltaTime: Float) {
        if (isFading && currentBGM != null && currentSoundEvent != null) {
            val direction = if (fadeTargetVolume > fadeCurrentVolume) 1.0f else -1.0f
            fadeCurrentVolume += fadeSpeed * deltaTime * direction
            
            if ((direction > 0 && fadeCurrentVolume >= fadeTargetVolume) || 
                (direction < 0 && fadeCurrentVolume <= fadeTargetVolume)) {
                fadeCurrentVolume = fadeTargetVolume
                isFading = false
                
                if (fadeTargetVolume <= 0.0f) {
                    minecraft.soundManager.stop(currentBGM!!)
                    currentBGM = null
                    currentSoundEvent = null
                    currentBGMIndex = -1
                }
            } else {
                val player = minecraft.player
                val oldInstance = currentBGM!!
                currentBGM = if (player != null) {
                    SimpleSoundInstance(
                        currentSoundEvent!!,
                        SoundSource.MUSIC,
                        fadeCurrentVolume,
                        1.0f,
                        minecraft.level?.random,
                        player.x, player.y, player.z
                    )
                } else {
                    SimpleSoundInstance(
                        currentSoundEvent!!,
                        SoundSource.MUSIC,
                        fadeCurrentVolume,
                        1.0f,
                        minecraft.level?.random,
                        0.0, 0.0, 0.0
                    )
                }
                minecraft.soundManager.stop(oldInstance)
                minecraft.soundManager.play(currentBGM!!)
            }
        }
        
        currentBGM?.let {
            if (!it.isLooping && !minecraft.soundManager.isActive(it)) {
                nextTrack()
            }
        }
    }

    fun nextTrack() {
        if (bgmQueue.isEmpty()) return
        currentBGMIndex = (currentBGMIndex + 1) % bgmQueue.size
        play(bgmQueue[currentBGMIndex], fadeIn = true, fadeDuration = 1.0f)
    }

    fun previousTrack() {
        if (bgmQueue.isEmpty()) return
        currentBGMIndex = if (currentBGMIndex <= 0) bgmQueue.size - 1 else currentBGMIndex - 1
        play(bgmQueue[currentBGMIndex], fadeIn = true, fadeDuration = 1.0f)
    }

    fun addToQueue(resource: ResourceLocation) {
        if (!bgmQueue.contains(resource)) {
            bgmQueue.add(resource)
        }
    }

    fun clearQueue() {
        bgmQueue.clear()
        currentBGMIndex = -1
    }

    fun isPlaying(): Boolean {
        return currentBGM != null && minecraft.soundManager.isActive(currentBGM!!)
    }

    fun getCurrentTrack(): ResourceLocation? {
        return if (currentBGMIndex >= 0 && currentBGMIndex < bgmQueue.size) {
            bgmQueue[currentBGMIndex]
        } else null
    }
}

