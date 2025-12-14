package net.star.galgame.dialogue.save

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.dialogue.DialogueScreen
import net.star.galgame.dialogue.i18n.I18nHelper
import org.lwjgl.glfw.GLFW
import java.text.SimpleDateFormat
import java.util.*

class LoadScreen(
    private val onLoadComplete: (DialogueScreen) -> Unit,
    private val onCancel: () -> Unit
) : Screen(Component.literal("Load Game")) {
    private var slots: List<SaveSlot> = emptyList()
    private var selectedSlot: Int? = null
    private var scrollOffset = 0
    private val slotsPerPage = 10
    
    override fun init() {
        super.init()
        slots = SaveManager.getAllSaveSlots()
        
        addRenderableWidget(
            Button.builder(Component.literal("返回")) { onCancel() }
                .bounds(width / 2 - 100, height - 30, 200, 20)
                .build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        val title = if (I18nHelper.hasTranslation("galgame.load.title")) {
            Component.translatable("galgame.load.title")
        } else {
            Component.literal("选择存档")
        }
        graphics.drawString(font, title, width / 2 - font.width(title) / 2, 20, 0xFFFFFF, false)
        
        val startIndex = scrollOffset
        val endIndex = (startIndex + slotsPerPage).coerceAtMost(slots.size)
        
        var yOffset = 60
        for (i in startIndex until endIndex) {
            val slot = slots[i]
            val slotY = yOffset + (i - startIndex) * 80
            
            renderSlot(graphics, slot, 50, slotY, width - 100, 70, mouseX, mouseY, i == selectedSlot)
        }
        
        if (slots.size > slotsPerPage) {
            val scrollBarX = width - 30
            val scrollBarY = 60
            val scrollBarHeight = slotsPerPage * 80
            val scrollBarThumbHeight = (scrollBarHeight * slotsPerPage / slots.size.toFloat()).toInt().coerceAtLeast(20)
            val scrollBarThumbY = scrollBarY + (scrollOffset * scrollBarHeight / slots.size)
            
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 10, scrollBarY + scrollBarHeight, 0x80000000.toInt())
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 10, scrollBarThumbY + scrollBarThumbHeight, 0xFF808080.toInt())
        }
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    private fun renderSlot(
        graphics: GuiGraphics,
        slot: SaveSlot,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        isSelected: Boolean
    ) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        
        val bgColor = when {
            isSelected -> 0xFF4A90E2.toInt()
            isHovered && slot.isValid -> 0xFF555555.toInt()
            slot.isValid -> 0xFF333333.toInt()
            else -> 0xFF222222.toInt()
        }
        
        graphics.fill(x, y, x + width, y + height, bgColor)
        graphics.fill(x, y, x + width, y + 2, 0xFFFFFFFF.toInt())
        
        val slotText = if (slot.slotId == 0) {
            if (I18nHelper.hasTranslation("galgame.load.auto_save")) {
                Component.translatable("galgame.load.auto_save")
            } else {
                Component.literal("自动存档")
            }
        } else {
            Component.literal("存档槽位 ${slot.slotId}")
        }
        
        graphics.drawString(font, slotText, x + 10, y + 10, 0xFFFFFF, false)
        
        if (slot.isValid && slot.saveData != null) {
            val saveData = slot.saveData
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateText = dateFormat.format(Date(saveData.timestamp))
            graphics.drawString(font, dateText, x + 10, y + 25, 0xCCCCCC, false)
            
            if (saveData.progress.isNotEmpty()) {
                val progressText = saveData.progress.take(30)
                graphics.drawString(font, progressText, x + 10, y + 40, 0xAAAAAA, false)
            }
            
            if (saveData.worldName != null) {
                val worldText = "世界: ${saveData.worldName}"
                graphics.drawString(font, worldText, x + 10, y + 55, 0x999999, false)
            }
        } else if (slot.isCorrupted) {
            val corruptedText = if (I18nHelper.hasTranslation("galgame.load.corrupted")) {
                Component.translatable("galgame.load.corrupted")
            } else {
                Component.literal("存档已损坏")
            }
            graphics.drawString(font, corruptedText, x + 10, y + 25, 0xFF0000, false)
            
            val recoverText = if (I18nHelper.hasTranslation("galgame.load.try_recover")) {
                Component.translatable("galgame.load.try_recover")
            } else {
                Component.literal("尝试恢复")
            }
            graphics.drawString(font, recoverText, x + 10, y + 40, 0xFFFF00, false)
        } else {
            val emptyText = if (I18nHelper.hasTranslation("galgame.load.empty")) {
                Component.translatable("galgame.load.empty")
            } else {
                Component.literal("空槽位")
            }
            graphics.drawString(font, emptyText, x + 10, y + 25, 0x888888, false)
        }
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() == 0) {
            val startIndex = scrollOffset
            val endIndex = (startIndex + slotsPerPage).coerceAtMost(slots.size)
            val mouseX = event.x()
            val mouseY = event.y()
            
            for (i in startIndex until endIndex) {
                val slot = slots[i]
                val slotY = 60 + (i - startIndex) * 80
                
                if (mouseX >= 50.0 && mouseX <= width - 50.0 &&
                    mouseY >= slotY.toDouble() && mouseY <= slotY + 70.0) {
                    
                    if (slot.isValid && slot.saveData != null) {
                        selectedSlot = i
                        val confirmText = if (I18nHelper.hasTranslation("galgame.load.confirm")) {
                            Component.translatable("galgame.load.confirm")
                        } else {
                            Component.literal("加载此存档？")
                        }
                        
                        minecraft?.setScreen(ConfirmScreen(
                            confirmText,
                            { confirmed ->
                                if (confirmed) {
                                    performLoad(slot.slotId)
                                } else {
                                    minecraft?.setScreen(this)
                                }
                            }
                        ))
                    } else if (slot.isCorrupted) {
                        tryRecover(slot.slotId)
                    }
                    return true
                }
            }
        }
        
        return super.mouseClicked(event, captured)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (slots.size > slotsPerPage) {
            scrollOffset = (scrollOffset - scrollY.toInt()).coerceIn(0, slots.size - slotsPerPage)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            onCancel()
            return true
        }
        return super.keyPressed(event)
    }
    
    private fun performLoad(slotId: Int) {
        val saveData = SaveManager.loadGame(slotId) ?: return
        
        val script = DialogueManager.getScript(saveData.scriptId) ?: return
        
        val controller = net.star.galgame.dialogue.control.DialogueController(script)
        SaveManager.applySaveData(saveData, controller)
        
        val screen = DialogueScreen(script, controller)
        onLoadComplete(screen)
        minecraft?.setScreen(screen)
    }
    
    private fun tryRecover(slotId: Int) {
        val saveData = SaveManager.loadGame(slotId)
        if (saveData != null) {
            performLoad(slotId)
        } else {
            val errorText = if (I18nHelper.hasTranslation("galgame.load.recover_failed")) {
                Component.translatable("galgame.load.recover_failed")
            } else {
                Component.literal("无法恢复存档")
            }
            
            minecraft?.setScreen(ErrorScreen(errorText) {
                minecraft?.setScreen(this)
            })
        }
    }
}

class ErrorScreen(
    private val message: Component,
    private val onClose: () -> Unit
) : Screen(Component.literal("Error")) {
    override fun init() {
        super.init()
        
        addRenderableWidget(
            Button.builder(Component.literal("确定")) { onClose() }
                .bounds(width / 2 - 100, height / 2 + 20, 200, 20)
                .build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, message, width / 2, height / 2 - 20, 0xFF0000)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE || event.key == GLFW.GLFW_KEY_ENTER) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
}

