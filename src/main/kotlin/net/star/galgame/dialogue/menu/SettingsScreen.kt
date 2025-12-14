package net.star.galgame.dialogue.menu

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.dialogue.audio.AudioManager
import net.star.galgame.dialogue.settings.SettingsManager
import org.lwjgl.glfw.GLFW

class SettingsScreen(
    private val onClose: () -> Unit
) : Screen(Component.literal("Settings")) {
    private var currentCategory = Category.DISPLAY
    private val mc = Minecraft.getInstance()
    private val options = mc.options
    
    private var masterVolume = SettingsManager.getFloat(SettingsManager.Audio.MASTER_VOLUME, 1.0f)
    private var bgmVolume = SettingsManager.getFloat(SettingsManager.Audio.BGM_VOLUME, 1.0f)
    private var seVolume = SettingsManager.getFloat(SettingsManager.Audio.SE_VOLUME, 1.0f)
    private var voiceVolume = SettingsManager.getFloat(SettingsManager.Audio.VOICE_VOLUME, 1.0f)
    
    private var resolutionWidth = SettingsManager.getInt(SettingsManager.Display.RESOLUTION_WIDTH, width)
    private var resolutionHeight = SettingsManager.getInt(SettingsManager.Display.RESOLUTION_HEIGHT, height)
    private var isFullscreen = SettingsManager.getBoolean(SettingsManager.Display.FULLSCREEN, options.fullscreen().get())
    private var uiScale = SettingsManager.getFloat(SettingsManager.Display.UI_SCALE, 1.0f)
    private var textSize = SettingsManager.getFloat(SettingsManager.Display.TEXT_SIZE, 1.0f)
    
    private var mouseEnabled = SettingsManager.getBoolean(SettingsManager.Control.MOUSE_ENABLED, true)
    private var touchEnabled = SettingsManager.getBoolean(SettingsManager.Control.TOUCH_ENABLED, true)
    private var autoPlaySpeed = SettingsManager.getFloat(SettingsManager.Control.AUTO_PLAY_SPEED, 1.0f)
    
    private var keyNext = SettingsManager.getInt(SettingsManager.Control.KEY_NEXT, GLFW.GLFW_KEY_SPACE)
    private var keySkip = SettingsManager.getInt(SettingsManager.Control.KEY_SKIP, GLFW.GLFW_KEY_S)
    private var keyAuto = SettingsManager.getInt(SettingsManager.Control.KEY_AUTO, GLFW.GLFW_KEY_A)
    private var keySave = SettingsManager.getInt(SettingsManager.Control.KEY_SAVE, GLFW.GLFW_KEY_F5)
    private var keyLoad = SettingsManager.getInt(SettingsManager.Control.KEY_LOAD, GLFW.GLFW_KEY_F9)
    private var keyMenu = SettingsManager.getInt(SettingsManager.Control.KEY_MENU, GLFW.GLFW_KEY_M)
    private var keyHistory = SettingsManager.getInt(SettingsManager.Control.KEY_HISTORY, GLFW.GLFW_KEY_H)
    
    private var waitingForKey: String? = null
    
    private enum class Category {
        DISPLAY, AUDIO, CONTROL
    }
    
    override fun init() {
        super.init()
        clearWidgets()
        setupCategoryButtons()
        setupCategoryContent()
    }
    
    private fun setupCategoryButtons() {
        val buttonWidth = 100
        val buttonHeight = 20
        val startX = width / 2 - (buttonWidth * 3 + 20) / 2
        val startY = 40
        
        addRenderableWidget(
            Button.builder(Component.literal("显示设置")) {
                currentCategory = Category.DISPLAY
                setupCategoryContent()
            }.bounds(startX, startY, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("音频设置")) {
                currentCategory = Category.AUDIO
                setupCategoryContent()
            }.bounds(startX + buttonWidth + 10, startY, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("控制设置")) {
                currentCategory = Category.CONTROL
                setupCategoryContent()
            }.bounds(startX + (buttonWidth + 10) * 2, startY, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("返回")) {
                saveSettings()
                onClose()
            }.bounds(width / 2 - 50, height - 30, 100, 20).build()
        )
    }
    
    private fun setupCategoryContent() {
        val categoryButtons = listOf("显示设置", "音频设置", "控制设置", "返回")
        val widgetsToRemove = children().filter { 
            it is Button && (it as Button).message.string !in categoryButtons
        }
        widgetsToRemove.forEach { removeWidget(it) }
        
        when (currentCategory) {
            Category.DISPLAY -> setupDisplaySettings()
            Category.AUDIO -> setupAudioSettings()
            Category.CONTROL -> setupControlSettings()
        }
    }
    
    private fun setupDisplaySettings() {
        val startY = 80
        val spacing = 30
        val sliderWidth = 200
        val sliderHeight = 20
        
        val resolutions = listOf(
            Pair(1280, 720),
            Pair(1366, 768),
            Pair(1600, 900),
            Pair(1920, 1080),
            Pair(2560, 1440),
            Pair(3840, 2160)
        )
        
        var currentResIndex = resolutions.indexOfFirst { it.first == resolutionWidth && it.second == resolutionHeight }
        if (currentResIndex == -1) currentResIndex = 0
        
        addRenderableWidget(
            Button.builder(Component.literal("分辨率: ${resolutionWidth}x${resolutionHeight}")) {
                val nextIndex = (currentResIndex + 1) % resolutions.size
                val newRes = resolutions[nextIndex]
                resolutionWidth = newRes.first
                resolutionHeight = newRes.second
                SettingsManager.setInt(SettingsManager.Display.RESOLUTION_WIDTH, resolutionWidth)
                SettingsManager.setInt(SettingsManager.Display.RESOLUTION_HEIGHT, resolutionHeight)
                setupCategoryContent()
            }.bounds(width / 2 - sliderWidth / 2, startY, sliderWidth, sliderHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("全屏: ${if (isFullscreen) "开启" else "关闭"}")) {
                isFullscreen = !isFullscreen
                options.fullscreen().set(isFullscreen)
                SettingsManager.setBoolean(SettingsManager.Display.FULLSCREEN, isFullscreen)
                setupCategoryContent()
            }.bounds(width / 2 - sliderWidth / 2, startY + spacing, sliderWidth, sliderHeight).build()
        )
        
        addRenderableWidget(
            SliderWidget(
                width / 2 - sliderWidth / 2,
                startY + spacing * 2,
                sliderWidth,
                sliderHeight,
                Component.literal("UI缩放"),
                uiScale,
                0.5f,
                2.0f
            ) { value ->
                uiScale = value
                SettingsManager.setFloat(SettingsManager.Display.UI_SCALE, uiScale)
            }
        )
        
        addRenderableWidget(
            SliderWidget(
                width / 2 - sliderWidth / 2,
                startY + spacing * 3,
                sliderWidth,
                sliderHeight,
                Component.literal("文字大小"),
                textSize,
                0.5f,
                2.0f
            ) { value ->
                textSize = value
                SettingsManager.setFloat(SettingsManager.Display.TEXT_SIZE, textSize)
            }
        )
    }
    
    private fun setupAudioSettings() {
        val startY = 80
        val spacing = 30
        val sliderWidth = 200
        val sliderHeight = 20
        
        addRenderableWidget(
            SliderWidget(
                width / 2 - sliderWidth / 2,
                startY,
                sliderWidth,
                sliderHeight,
                Component.literal("主音量"),
                masterVolume,
                0.0f,
                1.0f
            ) { value ->
                masterVolume = value
                SettingsManager.setFloat(SettingsManager.Audio.MASTER_VOLUME, masterVolume)
                updateAudioVolumes()
            }
        )
        
        addRenderableWidget(
            SliderWidget(
                width / 2 - sliderWidth / 2,
                startY + spacing,
                sliderWidth,
                sliderHeight,
                Component.literal("BGM音量"),
                bgmVolume,
                0.0f,
                1.0f
            ) { value ->
                bgmVolume = value
                SettingsManager.setFloat(SettingsManager.Audio.BGM_VOLUME, bgmVolume)
                updateAudioVolumes()
            }
        )
        
        addRenderableWidget(
            SliderWidget(
                width / 2 - sliderWidth / 2,
                startY + spacing * 2,
                sliderWidth,
                sliderHeight,
                Component.literal("SE音量"),
                seVolume,
                0.0f,
                1.0f
            ) { value ->
                seVolume = value
                SettingsManager.setFloat(SettingsManager.Audio.SE_VOLUME, seVolume)
                updateAudioVolumes()
            }
        )
        
        addRenderableWidget(
            SliderWidget(
                width / 2 - sliderWidth / 2,
                startY + spacing * 3,
                sliderWidth,
                sliderHeight,
                Component.literal("语音音量"),
                voiceVolume,
                0.0f,
                1.0f
            ) { value ->
                voiceVolume = value
                SettingsManager.setFloat(SettingsManager.Audio.VOICE_VOLUME, voiceVolume)
                updateAudioVolumes()
            }
        )
    }
    
    private fun setupControlSettings() {
        val startY = 80
        val spacing = 30
        val buttonWidth = 200
        val buttonHeight = 20
        
        addRenderableWidget(
            Button.builder(Component.literal("下一句: ${getKeyName(keyNext)}")) {
                waitingForKey = "next"
            }.bounds(width / 2 - buttonWidth / 2, startY, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("跳过: ${getKeyName(keySkip)}")) {
                waitingForKey = "skip"
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("自动播放: ${getKeyName(keyAuto)}")) {
                waitingForKey = "auto"
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("保存: ${getKeyName(keySave)}")) {
                waitingForKey = "save"
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("读取: ${getKeyName(keyLoad)}")) {
                waitingForKey = "load"
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 4, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("菜单: ${getKeyName(keyMenu)}")) {
                waitingForKey = "menu"
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 5, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("历史: ${getKeyName(keyHistory)}")) {
                waitingForKey = "history"
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 6, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("鼠标控制: ${if (mouseEnabled) "开启" else "关闭"}")) {
                mouseEnabled = !mouseEnabled
                SettingsManager.setBoolean(SettingsManager.Control.MOUSE_ENABLED, mouseEnabled)
                setupCategoryContent()
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 7, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("触摸控制: ${if (touchEnabled) "开启" else "关闭"}")) {
                touchEnabled = !touchEnabled
                SettingsManager.setBoolean(SettingsManager.Control.TOUCH_ENABLED, touchEnabled)
                setupCategoryContent()
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 8, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            SliderWidget(
                width / 2 - buttonWidth / 2,
                startY + spacing * 9,
                buttonWidth,
                buttonHeight,
                Component.literal("自动播放速度"),
                autoPlaySpeed,
                0.5f,
                3.0f
            ) { value ->
                autoPlaySpeed = value
                SettingsManager.setFloat(SettingsManager.Control.AUTO_PLAY_SPEED, autoPlaySpeed)
            }
        )
    }
    
    private fun getKeyName(keyCode: Int): String {
        return when (keyCode) {
            GLFW.GLFW_KEY_SPACE -> "空格"
            GLFW.GLFW_KEY_ENTER -> "回车"
            GLFW.GLFW_KEY_ESCAPE -> "ESC"
            GLFW.GLFW_KEY_LEFT_SHIFT -> "左Shift"
            GLFW.GLFW_KEY_RIGHT_SHIFT -> "右Shift"
            GLFW.GLFW_KEY_LEFT_CONTROL -> "左Ctrl"
            GLFW.GLFW_KEY_RIGHT_CONTROL -> "右Ctrl"
            GLFW.GLFW_KEY_LEFT_ALT -> "左Alt"
            GLFW.GLFW_KEY_RIGHT_ALT -> "右Alt"
            GLFW.GLFW_KEY_TAB -> "Tab"
            GLFW.GLFW_KEY_BACKSPACE -> "退格"
            GLFW.GLFW_KEY_UP -> "上"
            GLFW.GLFW_KEY_DOWN -> "下"
            GLFW.GLFW_KEY_LEFT -> "左"
            GLFW.GLFW_KEY_RIGHT -> "右"
            GLFW.GLFW_KEY_F1 -> "F1"
            GLFW.GLFW_KEY_F2 -> "F2"
            GLFW.GLFW_KEY_F3 -> "F3"
            GLFW.GLFW_KEY_F4 -> "F4"
            GLFW.GLFW_KEY_F5 -> "F5"
            GLFW.GLFW_KEY_F6 -> "F6"
            GLFW.GLFW_KEY_F7 -> "F7"
            GLFW.GLFW_KEY_F8 -> "F8"
            GLFW.GLFW_KEY_F9 -> "F9"
            GLFW.GLFW_KEY_F10 -> "F10"
            GLFW.GLFW_KEY_F11 -> "F11"
            GLFW.GLFW_KEY_F12 -> "F12"
            in GLFW.GLFW_KEY_A..GLFW.GLFW_KEY_Z -> (keyCode - GLFW.GLFW_KEY_A + 'A'.code.toInt()).toChar().toString()
            in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9 -> (keyCode - GLFW.GLFW_KEY_0 + '0'.code.toInt()).toChar().toString()
            else -> "键$keyCode"
        }
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, Component.literal("设置"), width / 2, 20, 0xFFFFFF)
        
        if (waitingForKey != null) {
            graphics.drawCenteredString(font, Component.literal("按下要设置的按键..."), width / 2, height / 2, 0xFFFFFF)
        }
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (waitingForKey != null) {
            if (event.key != GLFW.GLFW_KEY_ESCAPE) {
                when (waitingForKey) {
                    "next" -> {
                        keyNext = event.key
                        SettingsManager.setInt(SettingsManager.Control.KEY_NEXT, keyNext)
                    }
                    "skip" -> {
                        keySkip = event.key
                        SettingsManager.setInt(SettingsManager.Control.KEY_SKIP, keySkip)
                    }
                    "auto" -> {
                        keyAuto = event.key
                        SettingsManager.setInt(SettingsManager.Control.KEY_AUTO, keyAuto)
                    }
                    "save" -> {
                        keySave = event.key
                        SettingsManager.setInt(SettingsManager.Control.KEY_SAVE, keySave)
                    }
                    "load" -> {
                        keyLoad = event.key
                        SettingsManager.setInt(SettingsManager.Control.KEY_LOAD, keyLoad)
                    }
                    "menu" -> {
                        keyMenu = event.key
                        SettingsManager.setInt(SettingsManager.Control.KEY_MENU, keyMenu)
                    }
                    "history" -> {
                        keyHistory = event.key
                        SettingsManager.setInt(SettingsManager.Control.KEY_HISTORY, keyHistory)
                    }
                }
            }
            waitingForKey = null
            setupCategoryContent()
            return true
        }
        
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            saveSettings()
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
    
    private fun updateAudioVolumes() {
        val audioManager = AudioManager()
        audioManager.bgm.setVolume(bgmVolume * masterVolume)
        audioManager.se.setVolume(seVolume * masterVolume)
        audioManager.voice.setVolume(voiceVolume * masterVolume)
    }
    
    private fun saveSettings() {
        SettingsManager.setFloat(SettingsManager.Audio.MASTER_VOLUME, masterVolume)
        SettingsManager.setFloat(SettingsManager.Audio.BGM_VOLUME, bgmVolume)
        SettingsManager.setFloat(SettingsManager.Audio.SE_VOLUME, seVolume)
        SettingsManager.setFloat(SettingsManager.Audio.VOICE_VOLUME, voiceVolume)
        SettingsManager.setInt(SettingsManager.Display.RESOLUTION_WIDTH, resolutionWidth)
        SettingsManager.setInt(SettingsManager.Display.RESOLUTION_HEIGHT, resolutionHeight)
        SettingsManager.setBoolean(SettingsManager.Display.FULLSCREEN, isFullscreen)
        SettingsManager.setFloat(SettingsManager.Display.UI_SCALE, uiScale)
        SettingsManager.setFloat(SettingsManager.Display.TEXT_SIZE, textSize)
        SettingsManager.setBoolean(SettingsManager.Control.MOUSE_ENABLED, mouseEnabled)
        SettingsManager.setBoolean(SettingsManager.Control.TOUCH_ENABLED, touchEnabled)
        SettingsManager.setFloat(SettingsManager.Control.AUTO_PLAY_SPEED, autoPlaySpeed)
        SettingsManager.setInt(SettingsManager.Control.KEY_NEXT, keyNext)
        SettingsManager.setInt(SettingsManager.Control.KEY_SKIP, keySkip)
        SettingsManager.setInt(SettingsManager.Control.KEY_AUTO, keyAuto)
        SettingsManager.setInt(SettingsManager.Control.KEY_SAVE, keySave)
        SettingsManager.setInt(SettingsManager.Control.KEY_LOAD, keyLoad)
        SettingsManager.setInt(SettingsManager.Control.KEY_MENU, keyMenu)
        SettingsManager.setInt(SettingsManager.Control.KEY_HISTORY, keyHistory)
    }
    
    inner class SliderWidget(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val label: Component,
        private var value: Float,
        private val min: Float,
        private val max: Float,
        private val onValueChanged: (Float) -> Unit
    ) : AbstractWidget(x, y, width, height, Component.literal("${label.string}: ${(value * 100).toInt()}%")) {
        private var dragging = false
        
        override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val isHovered = isHovered
            val bgColor = if (isHovered) 0xFF555555.toInt() else 0xFF333333.toInt()
            graphics.fill(x, y, x + width, y + height, bgColor)
            graphics.fill(x, y, x + width, y + 2, 0xFFFFFFFF.toInt())
            
            val sliderWidth = width - 20
            val sliderX = x + 10
            val sliderY = y + height / 2 - 2
            val sliderHeight = 4
            
            graphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF000000.toInt())
            
            val normalizedValue = (value - min) / (max - min)
            val thumbX = sliderX + (normalizedValue * sliderWidth).toInt()
            val thumbWidth = 8
            val thumbY = sliderY - 4
            val thumbHeight = 12
            
            graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, 0xFFFFFFFF.toInt())
            
            val displayValue = when {
                max <= 1.0f -> "${(value * 100).toInt()}%"
                else -> String.format("%.1f", value)
            }
            val text = "${label.string}: $displayValue"
            graphics.drawString(font, text, x + 10, y + height / 2 - font.lineHeight / 2, 0xFFFFFF, false)
        }
        
        override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
            if (event.button() == 0 && isHovered) {
                updateValue(event.x().toDouble())
                dragging = true
                return true
            }
            return super.mouseClicked(event, captured)
        }
        
        override fun mouseDragged(event: MouseButtonEvent, mouseX: Double, mouseY: Double): Boolean {
            if (dragging && event.button() == 0) {
                updateValue(mouseX)
                return true
            }
            return super.mouseDragged(event, mouseX, mouseY)
        }
        
        override fun mouseReleased(event: MouseButtonEvent): Boolean {
            if (event.button() == 0) {
                dragging = false
                return true
            }
            return super.mouseReleased(event)
        }
        
        override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
            narrationElementOutput.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, message)
        }
        
        private fun updateValue(mouseX: Double) {
            val sliderX = x + 10
            val sliderWidth = width - 20
            val normalizedValue = ((mouseX - sliderX) / sliderWidth).coerceIn(0.0, 1.0).toFloat()
            val newValue = min + normalizedValue * (max - min)
            if (kotlin.math.abs(newValue - value) > 0.01f) {
                value = newValue.coerceIn(min, max)
                val displayValue = when {
                    max <= 1.0f -> "${(value * 100).toInt()}%"
                    else -> String.format("%.1f", value)
                }
                setMessage(Component.literal("${label.string}: $displayValue"))
                onValueChanged(value)
            }
        }
    }
    
}
