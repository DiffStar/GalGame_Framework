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
import org.lwjgl.glfw.GLFW

class SettingsScreen(
    private val onClose: () -> Unit
) : Screen(Component.literal("Settings")) {
    private var bgmVolume = 1.0f
    private var seVolume = 1.0f
    private var voiceVolume = 1.0f
    private var masterVolume = 1.0f
    
    override fun init() {
        super.init()
        
        val buttonWidth = 200
        val buttonHeight = 20
        val startY = 60
        val spacing = 30
        
        addRenderableWidget(
            VolumeSliderButton(width / 2 - buttonWidth / 2, startY, buttonWidth, buttonHeight, 
                Component.literal("主音量"), masterVolume) { value ->
                masterVolume = value
                updateAudioVolumes()
            }
        )
        
        addRenderableWidget(
            VolumeSliderButton(width / 2 - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight,
                Component.literal("BGM音量"), bgmVolume) { value ->
                bgmVolume = value
                updateAudioVolumes()
            }
        )
        
        addRenderableWidget(
            VolumeSliderButton(width / 2 - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight,
                Component.literal("音效音量"), seVolume) { value ->
                seVolume = value
                updateAudioVolumes()
            }
        )
        
        addRenderableWidget(
            VolumeSliderButton(width / 2 - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight,
                Component.literal("语音音量"), voiceVolume) { value ->
                voiceVolume = value
                updateAudioVolumes()
            }
        )
        
        val mc = Minecraft.getInstance()
        val options = mc.options
        
        addRenderableWidget(
            Button.builder(Component.literal("全屏: ${if (options.fullscreen().get()) "开启" else "关闭"}")) {
                options.fullscreen().set(!options.fullscreen().get())
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 4, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("VSync: ${if (options.enableVsync().get()) "开启" else "关闭"}")) {
                options.enableVsync().set(!options.enableVsync().get())
            }.bounds(width / 2 - buttonWidth / 2, startY + spacing * 5, buttonWidth, buttonHeight).build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("返回")) {
                onClose()
            }.bounds(width / 2 - buttonWidth / 2, height - 40, buttonWidth, buttonHeight).build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        val title = Component.literal("设置")
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF)
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
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
}

class VolumeSliderButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val label: Component,
    private var value: Float,
    private val onValueChanged: (Float) -> Unit
) : AbstractWidget(x, y, width, height, Component.literal("${label.string}: ${(value * 100).toInt()}%")) {
    private var dragging = false
    
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val mc = Minecraft.getInstance()
        val font = mc.font
        val isHovered = isHovered
        
        val bgColor = if (isHovered) 0xFF555555.toInt() else 0xFF333333.toInt()
        graphics.fill(x, y, x + width, y + height, bgColor)
        graphics.fill(x, y, x + width, y + 2, 0xFFFFFFFF.toInt())
        
        val sliderWidth = width - 20
        val sliderX = x + 10
        val sliderY = y + height / 2 - 2
        val sliderHeight = 4
        
        graphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF000000.toInt())
        
        val thumbX = sliderX + (value * sliderWidth).toInt()
        val thumbWidth = 8
        val thumbY = sliderY - 4
        val thumbHeight = 12
        
        graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, 0xFFFFFFFF.toInt())
        
        val text = "${label.string}: ${(value * 100).toInt()}%"
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
        val newValue = ((mouseX - sliderX) / sliderWidth).coerceIn(0.0, 1.0).toFloat()
        if (newValue != value) {
            value = newValue
            setMessage(Component.literal("${label.string}: ${(value * 100).toInt()}%"))
            onValueChanged(value)
        }
    }
}

