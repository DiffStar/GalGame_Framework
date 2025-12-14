package net.star.galgame.dialogue.save

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.dialogue.i18n.I18nHelper
import org.lwjgl.glfw.GLFW
import java.text.SimpleDateFormat
import java.util.*

class SaveScreen(
    private val scriptId: String,
    private val controller: net.star.galgame.dialogue.control.DialogueController,
    private val onSaveComplete: () -> Unit
) : Screen(Component.literal("Save Game")) {
    private var slots: List<SaveSlot> = emptyList()
    private var selectedSlot: Int? = null
    private var scrollOffset = 0
    private val slotsPerPage = 10
    
    override fun init() {
        super.init()
        slots = SaveManager.getAllSaveSlots()
        
        addRenderableWidget(
            Button.builder(Component.literal("返回")) { onClose() }
                .bounds(width / 2 - 100, height - 30, 200, 20)
                .build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        val title = if (I18nHelper.hasTranslation("galgame.save.title")) {
            Component.translatable("galgame.save.title")
        } else {
            Component.literal("选择存档槽位")
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
            isHovered -> 0xFF555555.toInt()
            else -> 0xFF333333.toInt()
        }
        
        graphics.fill(x, y, x + width, y + height, bgColor)
        graphics.fill(x, y, x + width, y + 2, 0xFFFFFFFF.toInt())
        
        val slotText = if (slot.slotId == 0) {
            if (I18nHelper.hasTranslation("galgame.save.auto_save")) {
                Component.translatable("galgame.save.auto_save")
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
        } else if (slot.isCorrupted) {
            val corruptedText = if (I18nHelper.hasTranslation("galgame.save.corrupted")) {
                Component.translatable("galgame.save.corrupted")
            } else {
                Component.literal("存档已损坏")
            }
            graphics.drawString(font, corruptedText, x + 10, y + 25, 0xFF0000, false)
        } else {
            val emptyText = if (I18nHelper.hasTranslation("galgame.save.empty")) {
                Component.translatable("galgame.save.empty")
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
                    selectedSlot = i
                    
                    if (slot.isValid && slot.saveData != null) {
                        val confirmText = if (I18nHelper.hasTranslation("galgame.save.confirm_overwrite")) {
                            Component.translatable("galgame.save.confirm_overwrite")
                        } else {
                            Component.literal("覆盖此存档？")
                        }
                        
                        minecraft?.setScreen(ConfirmScreen(
                            confirmText,
                            { confirmed ->
                                if (confirmed) {
                                    performSave(slot.slotId)
                                } else {
                                    minecraft?.setScreen(this)
                                }
                            }
                        ))
                    } else {
                        performSave(slot.slotId)
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
            onClose()
            return true
        }
        return super.keyPressed(event)
    }
    
    private fun performSave(slotId: Int) {
        val progress = controller.getCurrentEntry()?.text?.take(50) ?: ""
        if (SaveManager.saveGame(slotId, scriptId, controller, progress)) {
            onSaveComplete()
            onClose()
        }
    }
}

class ConfirmScreen(
    private val message: Component,
    private val onConfirm: (Boolean) -> Unit
) : Screen(Component.literal("Confirm")) {
    override fun init() {
        super.init()
        
        addRenderableWidget(
            Button.builder(Component.literal("确认")) { onConfirm(true) }
                .bounds(width / 2 - 105, height / 2 + 20, 100, 20)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("取消")) { onConfirm(false) }
                .bounds(width / 2 + 5, height / 2 + 20, 100, 20)
                .build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, message, width / 2, height / 2 - 20, 0xFFFFFF)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            onConfirm(false)
            return true
        }
        return super.keyPressed(event)
    }
}

