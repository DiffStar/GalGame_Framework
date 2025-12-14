package net.star.galgame.dialogue.audio

class AudioManager {
    val bgm = BGMManager()
    val se = SEManager()
    val voice = VoiceManager()

    fun update(deltaTime: Float) {
        bgm.update(deltaTime)
        se.update()
        voice.update()
    }

    fun stopAll() {
        bgm.stop()
        se.stopAll()
        voice.stop()
    }
}

