package net.star.galgame.dialogue.menu

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.dialogue.DialogueScreen
import net.star.galgame.dialogue.audio.AudioManager
import net.star.galgame.dialogue.control.DialogueController
import net.star.galgame.dialogue.save.SaveManager
import net.star.galgame.dialogue.save.SaveSlot
import net.star.galgame.dialogue.settings.SettingsManager
import net.star.galgame.contentpack.ContentPackManager
import net.star.galgame.contentpack.ContentPackValidator
import net.star.galgame.contentpack.InstalledPackInfo
import net.star.galgame.contentpack.ValidationReport
import net.star.galgame.developer.ui.Theme
import org.lwjgl.glfw.GLFW
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class MainMenuScreen : Screen(Component.literal("GalGame Main Menu")) {
    private var selectedTab = 0
    private val tabs = listOf("主菜单", "设置", "存档", "读档", "内容包")
    
    private val sidebarWidth = 180
    private val headerHeight = 32
    private val padding = 12
    private val tabButtonHeight = 24
    
    private var settingsCategory = 0
    private val settingsCategories = listOf("显示", "音频", "控制")
    
    private var masterVolume = SettingsManager.getFloat(SettingsManager.Audio.MASTER_VOLUME, 1.0f)
    private var bgmVolume = SettingsManager.getFloat(SettingsManager.Audio.BGM_VOLUME, 1.0f)
    private var seVolume = SettingsManager.getFloat(SettingsManager.Audio.SE_VOLUME, 1.0f)
    private var voiceVolume = SettingsManager.getFloat(SettingsManager.Audio.VOICE_VOLUME, 1.0f)
    private var uiScale = SettingsManager.getFloat(SettingsManager.Display.UI_SCALE, 1.0f)
    private var textSize = SettingsManager.getFloat(SettingsManager.Display.TEXT_SIZE, 1.0f)
    private var autoPlaySpeed = SettingsManager.getFloat(SettingsManager.Control.AUTO_PLAY_SPEED, 1.0f)
    private var isFullscreen = SettingsManager.getBoolean(SettingsManager.Display.FULLSCREEN, false)
    private var mouseEnabled = SettingsManager.getBoolean(SettingsManager.Control.MOUSE_ENABLED, true)
    private var touchEnabled = SettingsManager.getBoolean(SettingsManager.Control.TOUCH_ENABLED, true)
    private var resolutionWidth = 1920
    private var resolutionHeight = 1080
    private var selectedResolutionIndex = 0
    private val resolutions = listOf(
        Pair(1280, 720),
        Pair(1366, 768),
        Pair(1600, 900),
        Pair(1920, 1080),
        Pair(2560, 1440),
        Pair(3840, 2160)
    )
    
    private var saveSlots: List<SaveSlot> = emptyList()
    private var selectedSaveSlot: Int? = null
    private var saveScrollOffset = 0
    private val saveSlotsPerPage = 9
    
    private var loadSlots: List<SaveSlot> = emptyList()
    private var selectedLoadSlot: Int? = null
    private var loadScrollOffset = 0
    private val loadSlotsPerPage = 9
    
    private var packs: List<InstalledPackInfo> = emptyList()
    private var selectedPack: InstalledPackInfo? = null
    private var packScrollOffset = 0
    private val packsPerPage = 8
    private var showingPackDetails = false
    private var packValidationReport: ValidationReport? = null
    
    private var waitingForKey: String? = null
    private var keyNext = SettingsManager.getInt(SettingsManager.Control.KEY_NEXT, GLFW.GLFW_KEY_SPACE)
    private var keySkip = SettingsManager.getInt(SettingsManager.Control.KEY_SKIP, GLFW.GLFW_KEY_S)
    private var keyAuto = SettingsManager.getInt(SettingsManager.Control.KEY_AUTO, GLFW.GLFW_KEY_A)
    private var keySave = SettingsManager.getInt(SettingsManager.Control.KEY_SAVE, GLFW.GLFW_KEY_F5)
    private var keyLoad = SettingsManager.getInt(SettingsManager.Control.KEY_LOAD, GLFW.GLFW_KEY_F9)
    private var keyMenu = SettingsManager.getInt(SettingsManager.Control.KEY_MENU, GLFW.GLFW_KEY_M)
    private var keyHistory = SettingsManager.getInt(SettingsManager.Control.KEY_HISTORY, GLFW.GLFW_KEY_H)
    
    private var draggingScrollBar: String? = null
    
    private val mc = Minecraft.getInstance()
    
    init {
        val currentWidth = mc.window.guiScaledWidth
        val currentHeight = mc.window.guiScaledHeight
        resolutionWidth = SettingsManager.getInt(SettingsManager.Display.RESOLUTION_WIDTH, currentWidth)
        resolutionHeight = SettingsManager.getInt(SettingsManager.Display.RESOLUTION_HEIGHT, currentHeight)
        val currentRes = resolutions.indexOfFirst { it.first == resolutionWidth && it.second == resolutionHeight }
        if (currentRes != -1) {
            selectedResolutionIndex = currentRes
        }
    }
    
    override fun init() {
        super.init()
        refreshData()
    }
    
    private fun refreshData() {
        saveSlots = SaveManager.getAllSaveSlots()
        loadSlots = SaveManager.getAllSaveSlots()
        packs = ContentPackManager.getAllInstalledPacks(Paths.get("contentpacks"))
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        renderHeader(graphics, mouseX, mouseY)
        renderSidebar(graphics, mouseX, mouseY)
        renderContent(graphics, mouseX, mouseY)
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    private fun renderHeader(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, 0, width, headerHeight, Theme.BACKGROUND_DARK)
        Theme.drawDivider(graphics, 0, headerHeight, width)
        
        graphics.drawString(font, "GalGame Framework", padding, (headerHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderSidebar(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, headerHeight, sidebarWidth, height, Theme.BACKGROUND_MEDIUM)
        Theme.drawDivider(graphics, sidebarWidth, headerHeight, 1)
        
        val tabStartY = headerHeight + padding
        for ((index, tab) in tabs.withIndex()) {
            val tabY = tabStartY + index * (tabButtonHeight + 4)
            val isSelected = selectedTab == index
            val isHovered = mouseX >= padding && mouseX < sidebarWidth - padding &&
                    mouseY >= tabY && mouseY <= tabY + tabButtonHeight
            
            if (isSelected || isHovered) {
                Theme.drawButton(
                    graphics,
                    padding, tabY,
                    sidebarWidth - padding * 2, tabButtonHeight,
                    isHovered, isSelected
                )
            }
            
            if (isSelected) {
                Theme.drawSelectionIndicator(graphics, padding, tabY, tabButtonHeight)
            }
            
            val textColor = if (isSelected) Theme.TEXT_PRIMARY else Theme.TEXT_SECONDARY
            graphics.drawString(font, tab, padding + 8, tabY + (tabButtonHeight - font.lineHeight) / 2, textColor, false)
        }
    }
    
    private fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(sidebarWidth, headerHeight, width, height, Theme.BACKGROUND_LIGHT)
        
        val contentX = sidebarWidth + padding
        val contentY = headerHeight + padding
        
        when (selectedTab) {
            0 -> renderMainMenuTab(graphics, mouseX, mouseY, contentX, contentY)
            1 -> renderSettingsTab(graphics, mouseX, mouseY, contentX, contentY)
            2 -> renderSaveTab(graphics, mouseX, mouseY, contentX, contentY)
            3 -> renderLoadTab(graphics, mouseX, mouseY, contentX, contentY)
            4 -> renderContentPackTab(graphics, mouseX, mouseY, contentX, contentY)
        }
    }
    
    private fun renderMainMenuTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        val scripts = DialogueManager.getAllScripts()
        if (scripts.isEmpty()) {
            graphics.drawString(font, "未找到内容包，请先导入内容包", startX, startY + 40, Theme.TEXT_TERTIARY, false)
            return
        }
        
        val buttonWidth = 200
        val buttonHeight = 30
        val buttonSpacing = 10
        val centerX = width / 2
        var currentY = startY + 40
        
        val hasSave = SaveManager.getAutoSave() != null
        
        val startButtonY = currentY
        val isStartHovered = mouseX >= centerX - buttonWidth / 2 && mouseX <= centerX + buttonWidth / 2 &&
                mouseY >= startButtonY && mouseY <= startButtonY + buttonHeight
        Theme.drawButton(graphics, centerX - buttonWidth / 2, startButtonY, buttonWidth, buttonHeight, isStartHovered)
        graphics.drawString(font, "开始游戏", centerX - font.width("开始游戏") / 2, startButtonY + (buttonHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
        
        currentY += buttonHeight + buttonSpacing
        val continueButtonY = currentY
        val isContinueHovered = mouseX >= centerX - buttonWidth / 2 && mouseX <= centerX + buttonWidth / 2 &&
                mouseY >= continueButtonY && mouseY <= continueButtonY + buttonHeight
        Theme.drawButton(graphics, centerX - buttonWidth / 2, continueButtonY, buttonWidth, buttonHeight, isContinueHovered, false, hasSave)
        val continueTextColor = if (hasSave) Theme.TEXT_PRIMARY else Theme.TEXT_DISABLED
        graphics.drawString(font, "继续游戏", centerX - font.width("继续游戏") / 2, continueButtonY + (buttonHeight - font.lineHeight) / 2, continueTextColor, false)
        
        currentY += buttonHeight + buttonSpacing
        val exitButtonY = currentY
        val isExitHovered = mouseX >= centerX - buttonWidth / 2 && mouseX <= centerX + buttonWidth / 2 &&
                mouseY >= exitButtonY && mouseY <= exitButtonY + buttonHeight
        Theme.drawButton(graphics, centerX - buttonWidth / 2, exitButtonY, buttonWidth, buttonHeight, isExitHovered)
        graphics.drawString(font, "退出游戏", centerX - font.width("退出游戏") / 2, exitButtonY + (buttonHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderSettingsTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        var currentY = startY
        
        val categoryButtonWidth = 100
        val categoryButtonHeight = 24
        val categorySpacing = 8
        val categoryStartX = startX
        var categoryX = categoryStartX
        
        for ((index, category) in settingsCategories.withIndex()) {
            val isSelected = settingsCategory == index
            val isHovered = mouseX >= categoryX && mouseX <= categoryX + categoryButtonWidth &&
                    mouseY >= currentY && mouseY <= currentY + categoryButtonHeight
            
            Theme.drawButton(graphics, categoryX, currentY, categoryButtonWidth, categoryButtonHeight, isHovered, isSelected)
            
            if (isSelected) {
                Theme.drawSelectionIndicator(graphics, categoryX, currentY, categoryButtonHeight)
            }
            
            val textColor = if (isSelected) Theme.TEXT_PRIMARY else Theme.TEXT_SECONDARY
            graphics.drawString(font, category, categoryX + categoryButtonWidth / 2 - font.width(category) / 2, currentY + (categoryButtonHeight - font.lineHeight) / 2, textColor, false)
            
            categoryX += categoryButtonWidth + categorySpacing
        }
        
        currentY += categoryButtonHeight + padding + 8
        Theme.drawDivider(graphics, startX, currentY - 4, width - sidebarWidth - padding * 2)
        currentY += 8
        
        when (settingsCategory) {
            0 -> renderDisplaySettings(graphics, mouseX, mouseY, startX, currentY)
            1 -> renderAudioSettings(graphics, mouseX, mouseY, startX, currentY)
            2 -> renderControlSettings(graphics, mouseX, mouseY, startX, currentY)
        }
        
        val saveButtonY = height - padding - 24
        val saveButtonX = width - padding - 100
        val isSaveHovered = mouseX >= saveButtonX && mouseX <= saveButtonX + 100 &&
                mouseY >= saveButtonY && mouseY <= saveButtonY + 24
        Theme.drawButton(graphics, saveButtonX, saveButtonY, 100, 24, isSaveHovered)
        graphics.drawString(font, "保存设置", saveButtonX + 50 - font.width("保存设置") / 2, saveButtonY + 12 - font.lineHeight / 2, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderDisplaySettings(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        var currentY = startY
        val sliderWidth = 300
        val sliderHeight = 20
        val spacing = 30
        
        val resolutionButtonY = currentY
        val resolutionButtonWidth = 300
        val isResolutionHovered = mouseX >= startX && mouseX <= startX + resolutionButtonWidth &&
                mouseY >= resolutionButtonY && mouseY <= resolutionButtonY + sliderHeight
        Theme.drawButton(graphics, startX, resolutionButtonY, resolutionButtonWidth, sliderHeight, isResolutionHovered)
        val resText = "分辨率: ${resolutions[selectedResolutionIndex].first}x${resolutions[selectedResolutionIndex].second}"
        graphics.drawString(font, resText, startX + 10, resolutionButtonY + (sliderHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
        currentY += spacing
        
        renderSlider(graphics, mouseX, mouseY, startX, currentY, sliderWidth, sliderHeight, "UI缩放", uiScale, 0.5f, 2.0f) { value ->
            uiScale = value
        }
        currentY += spacing
        
        renderSlider(graphics, mouseX, mouseY, startX, currentY, sliderWidth, sliderHeight, "文字大小", textSize, 0.5f, 2.0f) { value ->
            textSize = value
        }
        currentY += spacing
        
        val fullscreenButtonY = currentY
        val fullscreenButtonWidth = 200
        val isFullscreenHovered = mouseX >= startX && mouseX <= startX + fullscreenButtonWidth &&
                mouseY >= fullscreenButtonY && mouseY <= fullscreenButtonY + sliderHeight
        Theme.drawButton(graphics, startX, fullscreenButtonY, fullscreenButtonWidth, sliderHeight, isFullscreenHovered, isFullscreen)
        graphics.drawString(font, "全屏: ${if (isFullscreen) "开启" else "关闭"}", startX + 10, fullscreenButtonY + (sliderHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderAudioSettings(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        var currentY = startY
        val sliderWidth = 300
        val sliderHeight = 20
        val spacing = 30
        
        renderSlider(graphics, mouseX, mouseY, startX, currentY, sliderWidth, sliderHeight, "主音量", masterVolume, 0.0f, 1.0f) { value ->
            masterVolume = value
            updateAudioVolumes()
        }
        currentY += spacing
        
        renderSlider(graphics, mouseX, mouseY, startX, currentY, sliderWidth, sliderHeight, "BGM音量", bgmVolume, 0.0f, 1.0f) { value ->
            bgmVolume = value
            updateAudioVolumes()
        }
        currentY += spacing
        
        renderSlider(graphics, mouseX, mouseY, startX, currentY, sliderWidth, sliderHeight, "SE音量", seVolume, 0.0f, 1.0f) { value ->
            seVolume = value
            updateAudioVolumes()
        }
        currentY += spacing
        
        renderSlider(graphics, mouseX, mouseY, startX, currentY, sliderWidth, sliderHeight, "语音音量", voiceVolume, 0.0f, 1.0f) { value ->
            voiceVolume = value
            updateAudioVolumes()
        }
    }
    
    private fun renderControlSettings(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        var currentY = startY
        val buttonWidth = 250
        val buttonHeight = 24
        val spacing = 28
        
        renderKeyButton(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "下一句", keyNext, "next")
        currentY += spacing
        
        renderKeyButton(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "跳过", keySkip, "skip")
        currentY += spacing
        
        renderKeyButton(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "自动播放", keyAuto, "auto")
        currentY += spacing
        
        renderKeyButton(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "保存", keySave, "save")
        currentY += spacing
        
        renderKeyButton(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "读取", keyLoad, "load")
        currentY += spacing
        
        renderKeyButton(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "菜单", keyMenu, "menu")
        currentY += spacing
        
        renderKeyButton(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "历史", keyHistory, "history")
        currentY += spacing + 4
        
        val mouseButtonY = currentY
        val isMouseHovered = mouseX >= startX && mouseX <= startX + buttonWidth &&
                mouseY >= mouseButtonY && mouseY <= mouseButtonY + buttonHeight
        Theme.drawButton(graphics, startX, mouseButtonY, buttonWidth, buttonHeight, isMouseHovered, mouseEnabled)
        graphics.drawString(font, "鼠标控制: ${if (mouseEnabled) "开启" else "关闭"}", startX + 10, mouseButtonY + (buttonHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
        currentY += spacing
        
        val touchButtonY = currentY
        val isTouchHovered = mouseX >= startX && mouseX <= startX + buttonWidth &&
                mouseY >= touchButtonY && mouseY <= touchButtonY + buttonHeight
        Theme.drawButton(graphics, startX, touchButtonY, buttonWidth, buttonHeight, isTouchHovered, touchEnabled)
        graphics.drawString(font, "触摸控制: ${if (touchEnabled) "开启" else "关闭"}", startX + 10, touchButtonY + (buttonHeight - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
        currentY += spacing
        
        renderSlider(graphics, mouseX, mouseY, startX, currentY, buttonWidth, buttonHeight, "自动播放速度", autoPlaySpeed, 0.5f, 3.0f) { value ->
            autoPlaySpeed = value
        }
    }
    
    private fun renderKeyButton(graphics: GuiGraphics, mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int, label: String, keyCode: Int, keyId: String) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        val isWaiting = waitingForKey == keyId
        Theme.drawButton(graphics, x, y, width, height, isHovered, isWaiting)
        val keyName = getKeyName(keyCode)
        val text = if (isWaiting) "$label: 按下按键..." else "$label: $keyName"
        graphics.drawString(font, text, x + 10, y + (height - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderSlider(graphics: GuiGraphics, mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int, label: String, value: Float, min: Float, max: Float) {
        renderSlider(graphics, mouseX, mouseY, x, y, width, height, label, value, min, max) {}
    }
    
    private fun renderSlider(graphics: GuiGraphics, mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int, label: String, value: Float, min: Float, max: Float, onValueChanged: (Float) -> Unit) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        Theme.drawButton(graphics, x, y, width, height, isHovered)
        
        val trackX = x + 10
        val trackY = y + height / 2 - 2
        val trackWidth = width - 20
        val trackHeight = 4
        
        graphics.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, Theme.BUTTON_BG)
        
        val normalizedValue = (value - min) / (max - min)
        val thumbX = trackX + (normalizedValue * trackWidth).toInt() - 6
        val thumbY = trackY - 6
        val thumbWidth = 12
        val thumbHeight = 16
        
        val thumbColor = if (isHovered) Theme.ACCENT_PRIMARY else 0xFFFFFFFF.toInt()
        graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, thumbColor)
        graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + 1, Theme.BUTTON_BORDER)
        graphics.fill(thumbX, thumbY + thumbHeight - 1, thumbX + thumbWidth, thumbY + thumbHeight, Theme.BUTTON_BORDER)
        graphics.fill(thumbX, thumbY, thumbX + 1, thumbY + thumbHeight, Theme.BUTTON_BORDER)
        graphics.fill(thumbX + thumbWidth - 1, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, Theme.BUTTON_BORDER)
        
        val displayValue = if (max <= 1.0f) "${(value * 100).toInt()}%" else String.format("%.1f", value)
        val text = "$label: $displayValue"
        graphics.drawString(font, text, x + 10, y + (height - font.lineHeight) / 2, Theme.TEXT_PRIMARY, false)
    }
    
    private fun renderSaveTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        var currentY = startY
        
        graphics.drawString(font, "选择存档槽位", startX, currentY, Theme.TEXT_PRIMARY, false)
        currentY += font.lineHeight + 12
        
        val visibleSlots = saveSlots.drop(saveScrollOffset).take(saveSlotsPerPage)
        val slotWidth = 200
        val slotHeight = 90
        val slotSpacing = 10
        val slotsPerRow = 3
        
        for ((index, slot) in visibleSlots.withIndex()) {
            val row = index / slotsPerRow
            val col = index % slotsPerRow
            val slotX = startX + col * (slotWidth + slotSpacing)
            val slotY = currentY + row * (slotHeight + slotSpacing)
            
            val isSelected = selectedSaveSlot == saveScrollOffset + index
            val isHovered = mouseX >= slotX && mouseX <= slotX + slotWidth &&
                    mouseY >= slotY && mouseY <= slotY + slotHeight
            
            Theme.drawPanel(graphics, slotX, slotY, slotWidth, slotHeight)
            
            if (isSelected) {
                Theme.drawSelectionIndicator(graphics, slotX, slotY, slotHeight)
            }
            
            val slotText = if (slot.slotId == 0) "自动存档" else "槽位 ${slot.slotId}"
            graphics.drawString(font, slotText, slotX + 8, slotY + 8, Theme.TEXT_PRIMARY, false)
            
            if (slot.isValid && slot.saveData != null) {
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                val dateText = dateFormat.format(Date(slot.saveData!!.timestamp))
                graphics.drawString(font, dateText, slotX + 8, slotY + 24, Theme.TEXT_SECONDARY, false)
                val progressText = slot.saveData!!.progress.take(20)
                graphics.drawString(font, progressText, slotX + 8, slotY + 40, Theme.TEXT_TERTIARY, false)
            } else {
                graphics.drawString(font, "空槽位", slotX + 8, slotY + 24, Theme.TEXT_TERTIARY, false)
            }
        }
        
        if (saveSlots.size > saveSlotsPerPage) {
            val scrollBarX = width - padding - 10
            val scrollBarY = startY
            val scrollBarHeight = height - startY - padding - 40
            val scrollBarThumbHeight = (scrollBarHeight * saveSlotsPerPage / saveSlots.size.toFloat()).toInt().coerceAtLeast(20)
            val scrollBarThumbY = scrollBarY + (saveScrollOffset * scrollBarHeight / saveSlots.size)
            val isScrollHovered = mouseX >= scrollBarX && mouseX <= scrollBarX + 8 &&
                    mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight
            val isThumbHovered = mouseX >= scrollBarX && mouseX <= scrollBarX + 8 &&
                    mouseY >= scrollBarThumbY && mouseY <= scrollBarThumbY + scrollBarThumbHeight
            val isDragging = draggingScrollBar == "save"
            
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 8, scrollBarY + scrollBarHeight, Theme.BUTTON_BG)
            val thumbColor = if (isDragging || isThumbHovered) Theme.ACCENT_PRIMARY else Theme.BUTTON_BG_HOVER
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 8, scrollBarThumbY + scrollBarThumbHeight, thumbColor)
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 8, scrollBarThumbY + 1, Theme.BUTTON_BORDER)
            graphics.fill(scrollBarX, scrollBarThumbY + scrollBarThumbHeight - 1, scrollBarX + 8, scrollBarThumbY + scrollBarThumbHeight, Theme.BUTTON_BORDER)
        }
        
        val saveButtonY = height - padding - 24
        val saveButtonX = width - padding - 100
        val isSaveHovered = mouseX >= saveButtonX && mouseX <= saveButtonX + 100 &&
                mouseY >= saveButtonY && mouseY <= saveButtonY + 24
        val canSave = selectedSaveSlot != null && mc.screen is DialogueScreen
        Theme.drawButton(graphics, saveButtonX, saveButtonY, 100, 24, isSaveHovered, false, canSave)
        val saveTextColor = if (canSave) Theme.TEXT_PRIMARY else Theme.TEXT_DISABLED
        graphics.drawString(font, "保存", saveButtonX + 50 - font.width("保存") / 2, saveButtonY + 12 - font.lineHeight / 2, saveTextColor, false)
    }
    
    private fun renderLoadTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        var currentY = startY
        
        graphics.drawString(font, "选择存档", startX, currentY, Theme.TEXT_PRIMARY, false)
        currentY += font.lineHeight + 12
        
        val visibleSlots = loadSlots.drop(loadScrollOffset).take(loadSlotsPerPage)
        val slotWidth = 200
        val slotHeight = 90
        val slotSpacing = 10
        val slotsPerRow = 3
        
        for ((index, slot) in visibleSlots.withIndex()) {
            val row = index / slotsPerRow
            val col = index % slotsPerRow
            val slotX = startX + col * (slotWidth + slotSpacing)
            val slotY = currentY + row * (slotHeight + slotSpacing)
            
            val isSelected = selectedLoadSlot == loadScrollOffset + index
            val isHovered = mouseX >= slotX && mouseX <= slotX + slotWidth &&
                    mouseY >= slotY && mouseY <= slotY + slotHeight
            
            Theme.drawPanel(graphics, slotX, slotY, slotWidth, slotHeight)
            
            if (isSelected) {
                Theme.drawSelectionIndicator(graphics, slotX, slotY, slotHeight)
            }
            
            val slotText = if (slot.slotId == 0) "自动存档" else "槽位 ${slot.slotId}"
            graphics.drawString(font, slotText, slotX + 8, slotY + 8, Theme.TEXT_PRIMARY, false)
            
            if (slot.isValid && slot.saveData != null) {
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                val dateText = dateFormat.format(Date(slot.saveData!!.timestamp))
                graphics.drawString(font, dateText, slotX + 8, slotY + 24, Theme.TEXT_SECONDARY, false)
                val progressText = slot.saveData!!.progress.take(20)
                graphics.drawString(font, progressText, slotX + 8, slotY + 40, Theme.TEXT_TERTIARY, false)
            } else {
                val emptyText = if (slot.isCorrupted) "已损坏" else "空槽位"
                val emptyColor = if (slot.isCorrupted) Theme.STATUS_ERROR else Theme.TEXT_TERTIARY
                graphics.drawString(font, emptyText, slotX + 8, slotY + 24, emptyColor, false)
            }
        }
        
        if (loadSlots.size > loadSlotsPerPage) {
            val scrollBarX = width - padding - 10
            val scrollBarY = startY
            val scrollBarHeight = height - startY - padding - 40
            val scrollBarThumbHeight = (scrollBarHeight * loadSlotsPerPage / loadSlots.size.toFloat()).toInt().coerceAtLeast(20)
            val scrollBarThumbY = scrollBarY + (loadScrollOffset * scrollBarHeight / loadSlots.size)
            val isScrollHovered = mouseX >= scrollBarX && mouseX <= scrollBarX + 8 &&
                    mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight
            val isThumbHovered = mouseX >= scrollBarX && mouseX <= scrollBarX + 8 &&
                    mouseY >= scrollBarThumbY && mouseY <= scrollBarThumbY + scrollBarThumbHeight
            val isDragging = draggingScrollBar == "load"
            
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 8, scrollBarY + scrollBarHeight, Theme.BUTTON_BG)
            val thumbColor = if (isDragging || isThumbHovered) Theme.ACCENT_PRIMARY else Theme.BUTTON_BG_HOVER
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 8, scrollBarThumbY + scrollBarThumbHeight, thumbColor)
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 8, scrollBarThumbY + 1, Theme.BUTTON_BORDER)
            graphics.fill(scrollBarX, scrollBarThumbY + scrollBarThumbHeight - 1, scrollBarX + 8, scrollBarThumbY + scrollBarThumbHeight, Theme.BUTTON_BORDER)
        }
        
        val loadButtonY = height - padding - 24
        val loadButtonX = width - padding - 100
        val isLoadHovered = mouseX >= loadButtonX && mouseX <= loadButtonX + 100 &&
                mouseY >= loadButtonY && mouseY <= loadButtonY + 24
        val canLoad = selectedLoadSlot != null && selectedLoadSlot!! < loadSlots.size && loadSlots[selectedLoadSlot!!].isValid && loadSlots[selectedLoadSlot!!].saveData != null
        Theme.drawButton(graphics, loadButtonX, loadButtonY, 100, 24, isLoadHovered, false, canLoad)
        val loadTextColor = if (canLoad) Theme.TEXT_PRIMARY else Theme.TEXT_DISABLED
        graphics.drawString(font, "加载", loadButtonX + 50 - font.width("加载") / 2, loadButtonY + 12 - font.lineHeight / 2, loadTextColor, false)
    }
    
    private fun renderContentPackTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        if (showingPackDetails && selectedPack != null) {
            renderPackDetails(graphics, mouseX, mouseY, startX, startY)
        } else {
            renderPackList(graphics, mouseX, mouseY, startX, startY)
        }
    }
    
    private fun renderPackList(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        var currentY = startY
        
        graphics.drawString(font, "内容包管理", startX, currentY, Theme.TEXT_PRIMARY, false)
        currentY += font.lineHeight + 12
        
        val visiblePacks = packs.drop(packScrollOffset).take(packsPerPage)
        val packHeight = 60
        val packSpacing = 8
        
        for ((index, pack) in visiblePacks.withIndex()) {
            val packY = currentY + index * (packHeight + packSpacing)
            val isSelected = selectedPack == pack
            val isHovered = mouseX >= startX && mouseX <= width - padding &&
                    mouseY >= packY && mouseY <= packY + packHeight
            
            Theme.drawPanel(graphics, startX, packY, width - startX - padding, packHeight)
            
            if (isSelected) {
                Theme.drawSelectionIndicator(graphics, startX, packY, packHeight)
            }
            
            graphics.drawString(font, pack.name, startX + 8, packY + 8, Theme.TEXT_PRIMARY, false)
            graphics.drawString(font, "版本: ${pack.version}", startX + 8, packY + 24, Theme.TEXT_SECONDARY, false)
            
            val statusText = when {
                !pack.isLoaded -> "未加载"
                pack.isEnabled -> "已启用"
                else -> "已禁用"
            }
            val statusColor = when {
                !pack.isLoaded -> Theme.TEXT_TERTIARY
                pack.isEnabled -> Theme.STATUS_SUCCESS
                else -> Theme.STATUS_ERROR
            }
            graphics.drawString(font, statusText, width - padding - 100, packY + 8, statusColor, false)
            
            if (pack.author != null) {
                graphics.drawString(font, "作者: ${pack.author}", startX + 8, packY + 40, Theme.TEXT_TERTIARY, false)
            }
        }
        
        if (packs.size > packsPerPage) {
            val scrollBarX = width - padding - 10
            val scrollBarY = startY
            val scrollBarHeight = height - startY - padding - 40
            val scrollBarThumbHeight = (scrollBarHeight * packsPerPage / packs.size.toFloat()).toInt().coerceAtLeast(20)
            val scrollBarThumbY = scrollBarY + (packScrollOffset * scrollBarHeight / packs.size)
            
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 8, scrollBarY + scrollBarHeight, Theme.BUTTON_BG)
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 8, scrollBarThumbY + scrollBarThumbHeight, Theme.BUTTON_BG_HOVER)
        }
        
        val refreshButtonY = height - padding - 24
        val refreshButtonX = width - padding - 200
        val isRefreshHovered = mouseX >= refreshButtonX && mouseX <= refreshButtonX + 80 &&
                mouseY >= refreshButtonY && mouseY <= refreshButtonY + 24
        Theme.drawButton(graphics, refreshButtonX, refreshButtonY, 80, 24, isRefreshHovered)
        graphics.drawString(font, "刷新", refreshButtonX + 40 - font.width("刷新") / 2, refreshButtonY + 12 - font.lineHeight / 2, Theme.TEXT_PRIMARY, false)
        
        val detailsButtonX = width - padding - 100
        val isDetailsHovered = mouseX >= detailsButtonX && mouseX <= detailsButtonX + 80 &&
                mouseY >= refreshButtonY && mouseY <= refreshButtonY + 24
        val canShowDetails = selectedPack != null
        Theme.drawButton(graphics, detailsButtonX, refreshButtonY, 80, 24, isDetailsHovered, false, canShowDetails)
        val detailsTextColor = if (canShowDetails) Theme.TEXT_PRIMARY else Theme.TEXT_DISABLED
        graphics.drawString(font, "详情", detailsButtonX + 40 - font.width("详情") / 2, refreshButtonY + 12 - font.lineHeight / 2, detailsTextColor, false)
    }
    
    private fun renderPackDetails(graphics: GuiGraphics, mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        val pack = selectedPack ?: return
        var currentY = startY
        
        graphics.drawString(font, "内容包信息", startX, currentY, Theme.TEXT_PRIMARY, false)
        currentY += font.lineHeight + 12
        
        Theme.drawPanel(graphics, startX, currentY, width - startX - padding, height - currentY - padding - 40)
        
        var infoY = currentY + 8
        graphics.drawString(font, "名称: ${pack.name}", startX + 8, infoY, Theme.TEXT_SECONDARY, false)
        infoY += font.lineHeight + 4
        graphics.drawString(font, "ID: ${pack.packId}", startX + 8, infoY, Theme.TEXT_SECONDARY, false)
        infoY += font.lineHeight + 4
        graphics.drawString(font, "版本: ${pack.version}", startX + 8, infoY, Theme.TEXT_SECONDARY, false)
        infoY += font.lineHeight + 4
        
        if (pack.author != null) {
            graphics.drawString(font, "作者: ${pack.author}", startX + 8, infoY, Theme.TEXT_SECONDARY, false)
            infoY += font.lineHeight + 4
        }
        
        val statusText = when {
            !pack.isLoaded -> "状态: 未加载"
            pack.isEnabled -> "状态: 已启用"
            else -> "状态: 已禁用"
        }
        graphics.drawString(font, statusText, startX + 8, infoY, Theme.TEXT_SECONDARY, false)
        infoY += font.lineHeight + 8
        
        if (pack.loadErrors.isNotEmpty()) {
            graphics.drawString(font, "加载错误:", startX + 8, infoY, Theme.STATUS_WARNING, false)
            infoY += font.lineHeight + 4
            pack.loadErrors.take(5).forEach { error ->
                graphics.drawString(font, "  - $error", startX + 8, infoY, Theme.STATUS_ERROR, false)
                infoY += font.lineHeight + 2
            }
        }
        
        if (packValidationReport != null) {
            infoY += 8
            graphics.drawString(font, "验证报告:", startX + 8, infoY, Theme.TEXT_PRIMARY, false)
            infoY += font.lineHeight + 4
            
            val report = packValidationReport!!
            val reportColor = if (report.isValid) Theme.STATUS_SUCCESS else Theme.STATUS_ERROR
            graphics.drawString(font, "状态: ${if (report.isValid) "通过" else "失败"}", startX + 8, infoY, reportColor, false)
            infoY += font.lineHeight + 4
            
            if (report.errors.isNotEmpty()) {
                graphics.drawString(font, "错误 (${report.errors.size}):", startX + 8, infoY, Theme.STATUS_WARNING, false)
                infoY += font.lineHeight + 2
                report.errors.take(5).forEach { error ->
                    graphics.drawString(font, "  - $error", startX + 8, infoY, Theme.STATUS_ERROR, false)
                    infoY += font.lineHeight + 2
                }
            }
        }
        
        val backButtonY = height - padding - 24
        val backButtonX = width - padding - 100
        val isBackHovered = mouseX >= backButtonX && mouseX <= backButtonX + 100 &&
                mouseY >= backButtonY && mouseY <= backButtonY + 24
        Theme.drawButton(graphics, backButtonX, backButtonY, 100, 24, isBackHovered)
        graphics.drawString(font, "返回", backButtonX + 50 - font.width("返回") / 2, backButtonY + 12 - font.lineHeight / 2, Theme.TEXT_PRIMARY, false)
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, captured)
        
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        
        if (mouseX < sidebarWidth) {
            val tabStartY = headerHeight + padding
            for ((index, _) in tabs.withIndex()) {
                val tabY = tabStartY + index * (tabButtonHeight + 4)
                if (mouseY >= tabY && mouseY <= tabY + tabButtonHeight) {
                    selectedTab = index
                    return true
                }
            }
        }
        
        when (selectedTab) {
            0 -> handleMainMenuClick(mouseX, mouseY)
            1 -> handleSettingsClick(mouseX, mouseY)
            2 -> handleSaveClick(mouseX, mouseY)
            3 -> handleLoadClick(mouseX, mouseY)
            4 -> handleContentPackClick(mouseX, mouseY)
        }
        
        return super.mouseClicked(event, captured)
    }
    
    private fun handleMainMenuClick(mouseX: Int, mouseY: Int) {
        val scripts = DialogueManager.getAllScripts()
        if (scripts.isEmpty()) return
        
        val buttonWidth = 200
        val buttonHeight = 30
        val buttonSpacing = 10
        val centerX = width / 2
        val startY = headerHeight + padding + 40
        
        val startButtonY = startY
        if (mouseX >= centerX - buttonWidth / 2 && mouseX <= centerX + buttonWidth / 2 &&
            mouseY >= startButtonY && mouseY <= startButtonY + buttonHeight) {
                startNewGame()
            return
        }
        
        val continueButtonY = startY + buttonHeight + buttonSpacing
        if (mouseX >= centerX - buttonWidth / 2 && mouseX <= centerX + buttonWidth / 2 &&
            mouseY >= continueButtonY && mouseY <= continueButtonY + buttonHeight) {
            continueGame()
            return
        }
        
        val exitButtonY = startY + (buttonHeight + buttonSpacing) * 2
        if (mouseX >= centerX - buttonWidth / 2 && mouseX <= centerX + buttonWidth / 2 &&
            mouseY >= exitButtonY && mouseY <= exitButtonY + buttonHeight) {
            mc.stop()
            return
        }
    }
    
    private fun handleSettingsClick(mouseX: Int, mouseY: Int) {
        val startX = sidebarWidth + padding
        var currentY = headerHeight + padding
        
        val categoryButtonWidth = 100
        val categoryButtonHeight = 24
        val categorySpacing = 8
        var categoryX = startX
        
        for ((index, _) in settingsCategories.withIndex()) {
            if (mouseX >= categoryX && mouseX <= categoryX + categoryButtonWidth &&
                mouseY >= currentY && mouseY <= currentY + categoryButtonHeight) {
                settingsCategory = index
                return
            }
            categoryX += categoryButtonWidth + categorySpacing
        }
        
        currentY += categoryButtonHeight + padding + 8
        
        when (settingsCategory) {
            0 -> handleDisplaySettingsClick(mouseX, mouseY, startX, currentY)
            1 -> handleAudioSettingsClick(mouseX, mouseY, startX, currentY)
            2 -> handleControlSettingsClick(mouseX, mouseY, startX, currentY)
        }
        
        val saveButtonY = height - padding - 24
        val saveButtonX = width - padding - 100
        if (mouseX >= saveButtonX && mouseX <= saveButtonX + 100 &&
            mouseY >= saveButtonY && mouseY <= saveButtonY + 24) {
            saveSettings()
        }
    }
    
    private fun handleDisplaySettingsClick(mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        val sliderWidth = 300
        val sliderHeight = 20
        val spacing = 30
        
        val resolutionButtonY = startY
        val resolutionButtonWidth = 300
        if (mouseX >= startX && mouseX <= startX + resolutionButtonWidth &&
            mouseY >= resolutionButtonY && mouseY <= resolutionButtonY + sliderHeight) {
            selectedResolutionIndex = (selectedResolutionIndex + 1) % resolutions.size
            val newRes = resolutions[selectedResolutionIndex]
            resolutionWidth = newRes.first
            resolutionHeight = newRes.second
        }
        
        handleSliderClick(mouseX, mouseY, startX, startY + spacing, sliderWidth, sliderHeight, uiScale, 0.5f, 2.0f) { value ->
            uiScale = value
        }
        
        handleSliderClick(mouseX, mouseY, startX, startY + spacing * 2, sliderWidth, sliderHeight, textSize, 0.5f, 2.0f) { value ->
            textSize = value
        }
        
        val fullscreenButtonY = startY + spacing * 3
        val fullscreenButtonWidth = 200
        if (mouseX >= startX && mouseX <= startX + fullscreenButtonWidth &&
            mouseY >= fullscreenButtonY && mouseY <= fullscreenButtonY + sliderHeight) {
            isFullscreen = !isFullscreen
        }
    }
    
    private fun handleAudioSettingsClick(mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        val sliderWidth = 300
        val sliderHeight = 20
        val spacing = 30
        
        handleSliderClick(mouseX, mouseY, startX, startY, sliderWidth, sliderHeight, masterVolume, 0.0f, 1.0f) { value ->
            masterVolume = value
            updateAudioVolumes()
        }
        
        handleSliderClick(mouseX, mouseY, startX, startY + spacing, sliderWidth, sliderHeight, bgmVolume, 0.0f, 1.0f) { value ->
            bgmVolume = value
            updateAudioVolumes()
        }
        
        handleSliderClick(mouseX, mouseY, startX, startY + spacing * 2, sliderWidth, sliderHeight, seVolume, 0.0f, 1.0f) { value ->
            seVolume = value
            updateAudioVolumes()
        }
        
        handleSliderClick(mouseX, mouseY, startX, startY + spacing * 3, sliderWidth, sliderHeight, voiceVolume, 0.0f, 1.0f) { value ->
            voiceVolume = value
            updateAudioVolumes()
        }
    }
    
    private fun handleControlSettingsClick(mouseX: Int, mouseY: Int, startX: Int, startY: Int) {
        val buttonWidth = 250
        val buttonHeight = 24
        val spacing = 28
        
        handleKeyButtonClick(mouseX, mouseY, startX, startY, buttonWidth, buttonHeight, "next")
        handleKeyButtonClick(mouseX, mouseY, startX, startY + spacing, buttonWidth, buttonHeight, "skip")
        handleKeyButtonClick(mouseX, mouseY, startX, startY + spacing * 2, buttonWidth, buttonHeight, "auto")
        handleKeyButtonClick(mouseX, mouseY, startX, startY + spacing * 3, buttonWidth, buttonHeight, "save")
        handleKeyButtonClick(mouseX, mouseY, startX, startY + spacing * 4, buttonWidth, buttonHeight, "load")
        handleKeyButtonClick(mouseX, mouseY, startX, startY + spacing * 5, buttonWidth, buttonHeight, "menu")
        handleKeyButtonClick(mouseX, mouseY, startX, startY + spacing * 6, buttonWidth, buttonHeight, "history")
        
        val mouseButtonY = startY + spacing * 7 + 4
        if (mouseX >= startX && mouseX <= startX + buttonWidth &&
            mouseY >= mouseButtonY && mouseY <= mouseButtonY + buttonHeight) {
            mouseEnabled = !mouseEnabled
        }
        
        val touchButtonY = startY + spacing * 8 + 4
        if (mouseX >= startX && mouseX <= startX + buttonWidth &&
            mouseY >= touchButtonY && mouseY <= touchButtonY + buttonHeight) {
            touchEnabled = !touchEnabled
        }
        
        handleSliderClick(mouseX, mouseY, startX, startY + spacing * 9 + 4, buttonWidth, buttonHeight, autoPlaySpeed, 0.5f, 3.0f) { value ->
            autoPlaySpeed = value
        }
    }
    
    private fun handleKeyButtonClick(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int, keyId: String) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            waitingForKey = keyId
        }
    }
    
    private fun handleSliderClick(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int, currentValue: Float, min: Float, max: Float, onValueChanged: (Float) -> Unit) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            val trackX = x + 10
            val trackY = y + height / 2 - 2
            val trackWidth = width - 20
            val normalizedValue = (currentValue - min) / (max - min)
            val thumbX = trackX + (normalizedValue * trackWidth).toInt() - 6
            val thumbY = trackY - 6
            val thumbWidth = 12
            val thumbHeight = 16
            
            if (mouseX < thumbX || mouseX > thumbX + thumbWidth || mouseY < thumbY || mouseY > thumbY + thumbHeight) {
                val relativeX = (mouseX - trackX).coerceIn(0, trackWidth)
                val normalizedValue = relativeX.toFloat() / trackWidth
                val newValue = (min + normalizedValue * (max - min)).coerceIn(min, max)
                onValueChanged(newValue)
            }
        }
    }
    
    private fun handleSaveClick(mouseX: Int, mouseY: Int) {
        val startX = sidebarWidth + padding
        val startY = headerHeight + padding + font.lineHeight + 12
        
        if (saveSlots.size > saveSlotsPerPage) {
            val scrollBarX = width - padding - 10
            val scrollBarY = startY
            val scrollBarHeight = height - startY - padding - 40
            val scrollBarThumbHeight = (scrollBarHeight * saveSlotsPerPage / saveSlots.size.toFloat()).toInt().coerceAtLeast(20)
            val scrollBarThumbY = scrollBarY + (saveScrollOffset * scrollBarHeight / saveSlots.size)
            
            if (mouseX >= scrollBarX && mouseX <= scrollBarX + 8 &&
                mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight) {
                if (mouseY >= scrollBarThumbY && mouseY <= scrollBarThumbY + scrollBarThumbHeight) {
                    draggingScrollBar = "save"
                } else {
                    val relativeY = mouseY - scrollBarY
                    val normalizedY = relativeY.toFloat() / scrollBarHeight
                    saveScrollOffset = (normalizedY * (saveSlots.size - saveSlotsPerPage)).toInt().coerceIn(0, saveSlots.size - saveSlotsPerPage)
                }
                return
            }
        }
        
        val slotWidth = 200
        val slotHeight = 90
        val slotSpacing = 10
        val slotsPerRow = 3
        
        val visibleSlots = saveSlots.drop(saveScrollOffset).take(saveSlotsPerPage)
        for ((index, slot) in visibleSlots.withIndex()) {
            val row = index / slotsPerRow
            val col = index % slotsPerRow
            val slotX = startX + col * (slotWidth + slotSpacing)
            val slotY = startY + row * (slotHeight + slotSpacing)
            
            if (mouseX >= slotX && mouseX <= slotX + slotWidth &&
                mouseY >= slotY && mouseY <= slotY + slotHeight) {
                selectedSaveSlot = saveScrollOffset + index
                return
            }
        }
        
        val saveButtonY = height - padding - 24
        val saveButtonX = width - padding - 100
        if (mouseX >= saveButtonX && mouseX <= saveButtonX + 100 &&
            mouseY >= saveButtonY && mouseY <= saveButtonY + 24) {
            performSave()
        }
    }
    
    private fun handleLoadClick(mouseX: Int, mouseY: Int) {
        val startX = sidebarWidth + padding
        val startY = headerHeight + padding + font.lineHeight + 12
        
        if (loadSlots.size > loadSlotsPerPage) {
            val scrollBarX = width - padding - 10
            val scrollBarY = startY
            val scrollBarHeight = height - startY - padding - 40
            val scrollBarThumbHeight = (scrollBarHeight * loadSlotsPerPage / loadSlots.size.toFloat()).toInt().coerceAtLeast(20)
            val scrollBarThumbY = scrollBarY + (loadScrollOffset * scrollBarHeight / loadSlots.size)
            
            if (mouseX >= scrollBarX && mouseX <= scrollBarX + 8 &&
                mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight) {
                if (mouseY >= scrollBarThumbY && mouseY <= scrollBarThumbY + scrollBarThumbHeight) {
                    draggingScrollBar = "load"
                } else {
                    val relativeY = mouseY - scrollBarY
                    val normalizedY = relativeY.toFloat() / scrollBarHeight
                    loadScrollOffset = (normalizedY * (loadSlots.size - loadSlotsPerPage)).toInt().coerceIn(0, loadSlots.size - loadSlotsPerPage)
                }
                return
            }
        }
        
        val slotWidth = 200
        val slotHeight = 90
        val slotSpacing = 10
        val slotsPerRow = 3
        
        val visibleSlots = loadSlots.drop(loadScrollOffset).take(loadSlotsPerPage)
        for ((index, slot) in visibleSlots.withIndex()) {
            val row = index / slotsPerRow
            val col = index % slotsPerRow
            val slotX = startX + col * (slotWidth + slotSpacing)
            val slotY = startY + row * (slotHeight + slotSpacing)
            
            if (mouseX >= slotX && mouseX <= slotX + slotWidth &&
                mouseY >= slotY && mouseY <= slotY + slotHeight) {
                selectedLoadSlot = loadScrollOffset + index
                return
            }
        }
        
        val loadButtonY = height - padding - 24
        val loadButtonX = width - padding - 100
        if (mouseX >= loadButtonX && mouseX <= loadButtonX + 100 &&
            mouseY >= loadButtonY && mouseY <= loadButtonY + 24) {
            performLoad()
        }
    }
    
    private fun handleContentPackClick(mouseX: Int, mouseY: Int) {
        if (showingPackDetails) {
            val backButtonY = height - padding - 24
            val backButtonX = width - padding - 100
            if (mouseX >= backButtonX && mouseX <= backButtonX + 100 &&
                mouseY >= backButtonY && mouseY <= backButtonY + 24) {
                showingPackDetails = false
                packValidationReport = null
            }
        } else {
            val startX = sidebarWidth + padding
            val startY = headerHeight + padding + font.lineHeight + 12
            val packHeight = 60
            val packSpacing = 8
            
            val visiblePacks = packs.drop(packScrollOffset).take(packsPerPage)
            for ((index, pack) in visiblePacks.withIndex()) {
                val packY = startY + index * (packHeight + packSpacing)
                if (mouseX >= startX && mouseX <= width - padding &&
                    mouseY >= packY && mouseY <= packY + packHeight) {
                    selectedPack = pack
                    return
                }
            }
            
            val refreshButtonY = height - padding - 24
            val refreshButtonX = width - padding - 200
            if (mouseX >= refreshButtonX && mouseX <= refreshButtonX + 80 &&
                mouseY >= refreshButtonY && mouseY <= refreshButtonY + 24) {
                refreshData()
            }
            
            val detailsButtonX = width - padding - 100
            if (mouseX >= detailsButtonX && mouseX <= detailsButtonX + 80 &&
                mouseY >= refreshButtonY && mouseY <= refreshButtonY + 24 && selectedPack != null) {
                val pack = selectedPack!!
                val manifest = ContentPackManager.getPack(pack.packId)?.manifest
                    ?: ContentPackManager.discoverPacks(Paths.get("contentpacks"))
                        .firstOrNull { it.pack.manifest.id == pack.packId }
                        ?.pack?.manifest
                    ?: return
                packValidationReport = ContentPackValidator.validatePack(pack.packPath, manifest)
                showingPackDetails = true
            }
        }
    }
    
    override fun mouseDragged(event: MouseButtonEvent, mouseX: Double, mouseY: Double): Boolean {
        if (event.button() == 0) {
            if (draggingScrollBar != null) {
                val startX = sidebarWidth + padding
                val startY = headerHeight + padding + font.lineHeight + 12
                val scrollBarX = width - padding - 10
                val scrollBarY = startY
                val scrollBarHeight = height - startY - padding - 40
                
                when (draggingScrollBar) {
                    "save" -> {
                        if (saveSlots.size > saveSlotsPerPage) {
                            val relativeY = (mouseY - scrollBarY).coerceIn(0.0, scrollBarHeight.toDouble())
                            val normalizedY = relativeY / scrollBarHeight
                            saveScrollOffset = (normalizedY * (saveSlots.size - saveSlotsPerPage)).toInt().coerceIn(0, saveSlots.size - saveSlotsPerPage)
                        }
                        return true
                    }
                    "load" -> {
                        if (loadSlots.size > loadSlotsPerPage) {
                            val relativeY = (mouseY - scrollBarY).coerceIn(0.0, scrollBarHeight.toDouble())
                            val normalizedY = relativeY / scrollBarHeight
                            loadScrollOffset = (normalizedY * (loadSlots.size - loadSlotsPerPage)).toInt().coerceIn(0, loadSlots.size - loadSlotsPerPage)
                        }
                        return true
                    }
                }
            }
        }
        
        return super.mouseDragged(event, mouseX, mouseY)
    }
    
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0) {
            draggingScrollBar = null
        }
        return super.mouseReleased(event)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        when (selectedTab) {
            2 -> {
                if (saveSlots.size > saveSlotsPerPage) {
                    saveScrollOffset = (saveScrollOffset - scrollY.toInt()).coerceIn(0, saveSlots.size - saveSlotsPerPage)
                    return true
                }
            }
            3 -> {
                if (loadSlots.size > loadSlotsPerPage) {
                    loadScrollOffset = (loadScrollOffset - scrollY.toInt()).coerceIn(0, loadSlots.size - loadSlotsPerPage)
                    return true
                }
            }
            4 -> {
                if (!showingPackDetails && packs.size > packsPerPage) {
                    packScrollOffset = (packScrollOffset - scrollY.toInt()).coerceIn(0, packs.size - packsPerPage)
                    return true
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (waitingForKey != null) {
            if (event.key != GLFW.GLFW_KEY_ESCAPE) {
                when (waitingForKey) {
                    "next" -> keyNext = event.key
                    "skip" -> keySkip = event.key
                    "auto" -> keyAuto = event.key
                    "save" -> keySave = event.key
                    "load" -> keyLoad = event.key
                    "menu" -> keyMenu = event.key
                    "history" -> keyHistory = event.key
                }
            }
            waitingForKey = null
            return true
        }
        
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            if (showingPackDetails) {
                showingPackDetails = false
                packValidationReport = null
                return true
            }
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
    
    private fun startNewGame() {
        val scripts = DialogueManager.getAllScripts()
        if (scripts.isEmpty()) return
        val script = scripts.first()
        val screen = DialogueScreen(script)
        mc.setScreen(screen)
    }
    
    private fun continueGame() {
        val autoSave = SaveManager.getAutoSave() ?: return
        val script = DialogueManager.getScript(autoSave.scriptId) ?: return
        val controller = DialogueController(script)
        SaveManager.applySaveData(autoSave, controller)
        val screen = DialogueScreen(script, controller)
        mc.setScreen(screen)
    }
    
    private fun performSave() {
        if (selectedSaveSlot == null) return
        val dialogueScreen = mc.screen as? DialogueScreen ?: return
        val slotId = saveSlots[selectedSaveSlot!!].slotId
        val script = dialogueScreen.script
        val controller = dialogueScreen.controller
        val progress = controller.getCurrentEntry()?.text?.take(50) ?: ""
        if (SaveManager.saveGame(slotId, script.id, controller, progress)) {
            refreshData()
        }
    }
    
    private fun performLoad() {
        if (selectedLoadSlot == null || selectedLoadSlot!! >= loadSlots.size) return
        val slot = loadSlots[selectedLoadSlot!!]
        if (!slot.isValid || slot.saveData == null) return
        
        val saveData = SaveManager.loadGame(slot.slotId) ?: return
        val script = DialogueManager.getScript(saveData.scriptId) ?: return
        val controller = DialogueController(script)
        SaveManager.applySaveData(saveData, controller)
        val screen = DialogueScreen(script, controller)
        mc.setScreen(screen)
    }
    
    private fun saveSettings() {
        SettingsManager.setFloat(SettingsManager.Audio.MASTER_VOLUME, masterVolume)
        SettingsManager.setFloat(SettingsManager.Audio.BGM_VOLUME, bgmVolume)
        SettingsManager.setFloat(SettingsManager.Audio.SE_VOLUME, seVolume)
        SettingsManager.setFloat(SettingsManager.Audio.VOICE_VOLUME, voiceVolume)
        SettingsManager.setInt(SettingsManager.Display.RESOLUTION_WIDTH, resolutionWidth)
        SettingsManager.setInt(SettingsManager.Display.RESOLUTION_HEIGHT, resolutionHeight)
        SettingsManager.setFloat(SettingsManager.Display.UI_SCALE, uiScale)
        SettingsManager.setFloat(SettingsManager.Display.TEXT_SIZE, textSize)
        SettingsManager.setBoolean(SettingsManager.Display.FULLSCREEN, isFullscreen)
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
        
        if (isFullscreen) {
            mc.options.fullscreen().set(true)
        } else {
            mc.options.fullscreen().set(false)
        }
    }
    
    private fun updateAudioVolumes() {
        val audioManager = AudioManager()
        audioManager.bgm.setVolume(bgmVolume * masterVolume)
        audioManager.se.setVolume(seVolume * masterVolume)
        audioManager.voice.setVolume(voiceVolume * masterVolume)
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
}
