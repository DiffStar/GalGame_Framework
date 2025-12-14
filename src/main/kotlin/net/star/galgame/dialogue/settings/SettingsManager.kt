package net.star.galgame.dialogue.settings

import net.minecraft.client.Minecraft
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object SettingsManager {
    private var settings: Properties = Properties()
    private var settingsFile: Path? = null
    
    init {
        loadSettings()
    }
    
    private fun getSettingsFile(): Path {
        if (settingsFile == null) {
            val mc = Minecraft.getInstance()
            val gameDir = mc.gameDirectory.toPath()
            settingsFile = gameDir.resolve("galgame_settings.properties")
        }
        return settingsFile!!
    }
    
    fun loadSettings() {
        try {
            val file = getSettingsFile()
            if (Files.exists(file)) {
                Files.newInputStream(file).use { input ->
                    settings.load(input)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun saveSettings() {
        try {
            val file = getSettingsFile()
            Files.newOutputStream(file).use { output ->
                settings.store(output, "Galgame Settings")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getInt(key: String, default: Int): Int {
        return settings.getProperty(key, default.toString()).toIntOrNull() ?: default
    }
    
    fun getFloat(key: String, default: Float): Float {
        return settings.getProperty(key, default.toString()).toFloatOrNull() ?: default
    }
    
    fun getBoolean(key: String, default: Boolean): Boolean {
        return settings.getProperty(key, default.toString()).toBooleanStrictOrNull() ?: default
    }
    
    fun getString(key: String, default: String): String {
        return settings.getProperty(key, default)
    }
    
    fun setInt(key: String, value: Int) {
        settings.setProperty(key, value.toString())
        saveSettings()
    }
    
    fun setFloat(key: String, value: Float) {
        settings.setProperty(key, value.toString())
        saveSettings()
    }
    
    fun setBoolean(key: String, value: Boolean) {
        settings.setProperty(key, value.toString())
        saveSettings()
    }
    
    fun setString(key: String, value: String) {
        settings.setProperty(key, value)
        saveSettings()
    }
    
    object Display {
        const val RESOLUTION_WIDTH = "display.resolution.width"
        const val RESOLUTION_HEIGHT = "display.resolution.height"
        const val FULLSCREEN = "display.fullscreen"
        const val UI_SCALE = "display.ui_scale"
        const val TEXT_SIZE = "display.text_size"
    }
    
    object Audio {
        const val MASTER_VOLUME = "audio.master_volume"
        const val BGM_VOLUME = "audio.bgm_volume"
        const val SE_VOLUME = "audio.se_volume"
        const val VOICE_VOLUME = "audio.voice_volume"
    }
    
    object Control {
        const val KEY_NEXT = "control.key.next"
        const val KEY_SKIP = "control.key.skip"
        const val KEY_AUTO = "control.key.auto"
        const val KEY_SAVE = "control.key.save"
        const val KEY_LOAD = "control.key.load"
        const val KEY_MENU = "control.key.menu"
        const val KEY_HISTORY = "control.key.history"
        const val MOUSE_ENABLED = "control.mouse.enabled"
        const val TOUCH_ENABLED = "control.touch.enabled"
        const val AUTO_PLAY_SPEED = "control.auto_play_speed"
    }
}

