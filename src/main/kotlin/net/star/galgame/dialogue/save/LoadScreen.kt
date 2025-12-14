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
    private val slotsPerRow = 4
    private val slotsPerColumn = 3
    private val slotsPerPage = slotsPerRow * slotsPerColumn
    private val slotWidth = 180
    private val slotHeight = 220
    private val slotSpacing = 10
    
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
        
        val totalSlots = slots.size
        val totalPages = (totalSlots + slotsPerPage - 1) / slotsPerPage
        val currentPage = scrollOffset / slotsPerPage
        val startIndex = currentPage * slotsPerPage
        val endIndex = (startIndex + slotsPerPage).coerceAtMost(totalSlots)
        
        val gridStartX = (width - (slotsPerRow * (slotWidth + slotSpacing) - slotSpacing)) / 2
        val gridStartY = 60
        
        for (i in startIndex until endIndex) {
            val slot = slots[i]
            val relativeIndex = i - startIndex
            val row = relativeIndex / slotsPerRow
            val col = relativeIndex % slotsPerRow
            
            val slotX = gridStartX + col * (slotWidth + slotSpacing)
            val slotY = gridStartY + row * (slotHeight + slotSpacing)
            
            renderSlot(graphics, slot, slotX, slotY, slotWidth, slotHeight, mouseX, mouseY, i == selectedSlot)
        }
        
        if (totalPages > 1) {
            val pageInfo = "第 ${currentPage + 1} / $totalPages 页"
            graphics.drawString(font, pageInfo, width / 2 - font.width(pageInfo) / 2, height - 50, 0xCCCCCC, false)
        }
        
        if (selectedSlot != null && selectedSlot!! < slots.size) {
            val slot = slots[selectedSlot!!]
            if (slot.isValid && slot.saveData != null) {
                renderSlotInfo(graphics, slot, mouseX, mouseY)
            }
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
        graphics.fill(x, y + height - 2, x + width, y + height, 0xFFFFFFFF.toInt())
        graphics.fill(x, y, x + 2, y + height, 0xFFFFFFFF.toInt())
        graphics.fill(x + width - 2, y, x + width, y + height, 0xFFFFFFFF.toInt())
        
        val previewHeight = 120
        val previewY = y + 5
        
        if (slot.isValid && slot.saveData != null && slot.saveData!!.screenshotPath != null) {
            val screenshotPath = java.nio.file.Paths.get(slot.saveData!!.screenshotPath!!)
            if (java.nio.file.Files.exists(screenshotPath)) {
                try {
                    val inputStream = java.nio.file.Files.newInputStream(screenshotPath)
                    val image = com.mojang.blaze3d.platform.NativeImage.read(inputStream)
                    inputStream.close()
                    val texture = net.minecraft.client.renderer.texture.DynamicTexture(
                        java.util.function.Supplier { "load_preview_${slot.slotId}" },
                        image
                    )
                    val resourceLocation = net.minecraft.resources.ResourceLocation.parse("galgame:load_preview_${slot.slotId}")
                    minecraft?.textureManager?.register(resourceLocation, texture)
                    graphics.blitInscribed(resourceLocation, x + 5, previewY, width - 10, previewHeight, width - 10, previewHeight, false, false)
                } catch (e: Exception) {
                    graphics.fill(x + 5, previewY, x + width - 5, previewY + previewHeight, 0xFF000000.toInt())
                }
            } else {
                graphics.fill(x + 5, previewY, x + width - 5, previewY + previewHeight, 0xFF000000.toInt())
            }
        } else {
            graphics.fill(x + 5, previewY, x + width - 5, previewY + previewHeight, 0xFF000000.toInt())
            val emptyPreviewText = "无预览"
            graphics.drawString(font, emptyPreviewText, 
                x + width / 2 - font.width(emptyPreviewText) / 2,
                previewY + previewHeight / 2 - font.lineHeight / 2,
                0x888888, false)
        }
        
        val infoY = previewY + previewHeight + 5
        val slotText = if (slot.slotId == 0) {
            if (I18nHelper.hasTranslation("galgame.load.auto_save")) {
                Component.translatable("galgame.load.auto_save")
            } else {
                Component.literal("自动存档")
            }
        } else {
            Component.literal("槽位 ${slot.slotId}")
        }
        
        graphics.drawString(font, slotText, x + 10, infoY, 0xFFFFFF, false)
        
        if (slot.isValid && slot.saveData != null) {
            val saveData = slot.saveData
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val dateText = dateFormat.format(Date(saveData.timestamp))
            graphics.drawString(font, dateText, x + 10, infoY + 15, 0xCCCCCC, false)
            
            if (saveData.progress.isNotEmpty()) {
                val progressText = saveData.progress.take(20)
                graphics.drawString(font, progressText, x + 10, infoY + 30, 0xAAAAAA, false)
            }
        } else if (slot.isCorrupted) {
            val corruptedText = if (I18nHelper.hasTranslation("galgame.load.corrupted")) {
                Component.translatable("galgame.load.corrupted")
            } else {
                Component.literal("已损坏")
            }
            graphics.drawString(font, corruptedText, x + 10, infoY + 15, 0xFF0000, false)
        } else {
            val emptyText = if (I18nHelper.hasTranslation("galgame.load.empty")) {
                Component.translatable("galgame.load.empty")
            } else {
                Component.literal("空槽位")
            }
            graphics.drawString(font, emptyText, x + 10, infoY + 15, 0x888888, false)
        }
    }
    
    private fun renderSlotInfo(graphics: GuiGraphics, slot: SaveSlot, mouseX: Int, mouseY: Int) {
        if (slot.saveData == null) return
        
        val infoWidth = 300
        val infoHeight = 150
        val infoX = width - infoWidth - 20
        val infoY = 60
        
        graphics.fill(infoX, infoY, infoX + infoWidth, infoY + infoHeight, 0xE0000000.toInt())
        graphics.fill(infoX, infoY, infoX + infoWidth, infoY + 2, 0xFFFFFFFF.toInt())
        graphics.fill(infoX, infoY + infoHeight - 2, infoX + infoWidth, infoY + infoHeight, 0xFFFFFFFF.toInt())
        graphics.fill(infoX, infoY, infoX + 2, infoY + infoHeight, 0xFFFFFFFF.toInt())
        graphics.fill(infoX + infoWidth - 2, infoY, infoX + infoWidth, infoY + infoHeight, 0xFFFFFFFF.toInt())
        
        val saveData = slot.saveData!!
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateText = dateFormat.format(Date(saveData.timestamp))
        
        graphics.drawString(font, "存档信息", infoX + 10, infoY + 10, 0xFFFFFF, false)
        graphics.drawString(font, "时间: $dateText", infoX + 10, infoY + 30, 0xCCCCCC, false)
        
        if (saveData.progress.isNotEmpty()) {
            graphics.drawString(font, "进度:", infoX + 10, infoY + 50, 0xCCCCCC, false)
            val progressComponent = net.minecraft.network.chat.Component.literal(saveData.progress)
            val progressLines = font.split(progressComponent, infoWidth - 20)
            var lineY = infoY + 70
            for (line in progressLines.take(3)) {
                graphics.drawString(font, line, infoX + 10, lineY, 0xAAAAAA, false)
                lineY += font.lineHeight + 2
            }
        }
        
        if (saveData.worldName != null) {
            graphics.drawString(font, "世界: ${saveData.worldName}", infoX + 10, infoY + 120, 0x999999, false)
        }
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() == 0) {
            val totalSlots = slots.size
            val currentPage = scrollOffset / slotsPerPage
            val startIndex = currentPage * slotsPerPage
            val endIndex = (startIndex + slotsPerPage).coerceAtMost(totalSlots)
            val mouseX = event.x().toInt()
            val mouseY = event.y().toInt()
            
            val gridStartX = (width - (slotsPerRow * (slotWidth + slotSpacing) - slotSpacing)) / 2
            val gridStartY = 60
            
            for (i in startIndex until endIndex) {
                val slot = slots[i]
                val relativeIndex = i - startIndex
                val row = relativeIndex / slotsPerRow
                val col = relativeIndex % slotsPerRow
                
                val slotX = gridStartX + col * (slotWidth + slotSpacing)
                val slotY = gridStartY + row * (slotHeight + slotSpacing)
                
                if (mouseX >= slotX && mouseX <= slotX + slotWidth &&
                    mouseY >= slotY && mouseY <= slotY + slotHeight) {
                    
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
        val totalSlots = slots.size
        val totalPages = (totalSlots + slotsPerPage - 1) / slotsPerPage
        if (totalPages > 1) {
            val currentPage = scrollOffset / slotsPerPage
            val newPage = (currentPage - scrollY.toInt()).coerceIn(0, totalPages - 1)
            scrollOffset = newPage * slotsPerPage
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

