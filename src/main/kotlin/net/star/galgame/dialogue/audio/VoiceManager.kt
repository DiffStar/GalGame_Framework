package net.star.galgame.dialogue.audio

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

class VoiceManager {
    private val minecraft = Minecraft.getInstance()
    private var currentVoice: SimpleSoundInstance? = null
    private var currentSoundEvent: SoundEvent? = null
    private var voiceVolume = 1.0f
    private var subtitleText: Component? = null
    private var showSubtitle = true
    private var canSkip = true
    private var isSkipping = false

    fun play(resource: ResourceLocation, subtitle: Component? = null) {
        stop()
        
        currentSoundEvent = SoundEvent.createVariableRangeEvent(resource)
        val player = minecraft.player
        currentVoice = if (player != null) {
            SimpleSoundInstance(
                currentSoundEvent!!,
                SoundSource.VOICE,
                voiceVolume,
                1.0f,
                minecraft.level?.random,
                player.x, player.y, player.z
            )
        } else {
            SimpleSoundInstance(
                currentSoundEvent!!,
                SoundSource.VOICE,
                voiceVolume,
                1.0f,
                minecraft.level?.random,
                0.0, 0.0, 0.0
            )
        }
        
        subtitleText = subtitle
        isSkipping = false
        
        minecraft.soundManager.play(currentVoice!!)
    }

    fun stop() {
        currentVoice?.let {
            minecraft.soundManager.stop(it)
        }
        currentVoice = null
        currentSoundEvent = null
        subtitleText = null
        isSkipping = false
    }

    fun skip() {
        if (canSkip && currentVoice != null) {
            isSkipping = true
            stop()
        }
    }

    fun setVolume(volume: Float) {
        voiceVolume = volume.coerceIn(0.0f, 1.0f)
        if (currentSoundEvent != null && currentVoice != null && minecraft.soundManager.isActive(currentVoice!!)) {
            val player = minecraft.player
            val oldInstance = currentVoice!!
            currentVoice = if (player != null) {
                SimpleSoundInstance(
                    currentSoundEvent!!,
                    SoundSource.VOICE,
                    voiceVolume,
                    1.0f,
                    minecraft.level?.random,
                    player.x, player.y, player.z
                )
            } else {
                SimpleSoundInstance(
                    currentSoundEvent!!,
                    SoundSource.VOICE,
                    voiceVolume,
                    1.0f,
                    minecraft.level?.random,
                    0.0, 0.0, 0.0
                )
            }
            minecraft.soundManager.stop(oldInstance)
            minecraft.soundManager.play(currentVoice!!)
        }
    }

    fun getVolume(): Float = voiceVolume

    fun setSubtitleEnabled(enabled: Boolean) {
        showSubtitle = enabled
    }

    fun isSubtitleEnabled(): Boolean = showSubtitle

    fun getSubtitle(): Component? {
        return if (showSubtitle && isPlaying()) subtitleText else null
    }

    fun setSkipEnabled(enabled: Boolean) {
        canSkip = enabled
    }

    fun isSkipEnabled(): Boolean = canSkip

    fun isPlaying(): Boolean {
        return currentVoice != null && minecraft.soundManager.isActive(currentVoice!!)
    }

    fun isSkipping(): Boolean = isSkipping

    fun update() {
        if (currentVoice != null && !minecraft.soundManager.isActive(currentVoice!!)) {
            currentVoice = null
            currentSoundEvent = null
            subtitleText = null
            isSkipping = false
        }
    }
}

