package net.star.galgame.dialogue.achievement

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.ConcurrentLinkedQueue

object AchievementNotification {
    private val notificationQueue = ConcurrentLinkedQueue<NotificationData>()
    private var currentNotification: NotificationData? = null
    private var notificationTimer = 0f
    private val notificationDuration = 3.0f
    private val fadeInDuration = 0.3f
    private val fadeOutDuration = 0.3f
    
    private data class NotificationData(
        val achievementId: String,
        val name: Component,
        val description: Component,
        val icon: ResourceLocation?
    )
    
    fun showNotification(achievementId: String) {
        val definition = AchievementManager.getAchievement(achievementId) ?: return
        val notification = NotificationData(
            achievementId = achievementId,
            name = definition.name,
            description = definition.description,
            icon = definition.icon
        )
        notificationQueue.offer(notification)
    }
    
    fun update(deltaTime: Float) {
        if (currentNotification == null && notificationQueue.isNotEmpty()) {
            currentNotification = notificationQueue.poll()
            notificationTimer = 0f
        }
        
        if (currentNotification != null) {
            notificationTimer += deltaTime
            if (notificationTimer >= notificationDuration) {
                currentNotification = null
                notificationTimer = 0f
            }
        }
    }
    
    fun render(graphics: GuiGraphics, width: Int, height: Int) {
        val notification = currentNotification ?: return
        val mc = Minecraft.getInstance()
        val font = mc.font
        
        val alpha = calculateAlpha()
        if (alpha <= 0f) return
        
        val notificationWidth = 300
        val notificationHeight = 80
        val x = width - notificationWidth - 20
        val y = 20
        
        val bgColor = (alpha * 0.9f * 255).toInt() shl 24 or 0x000000
        graphics.fill(x, y, x + notificationWidth, y + notificationHeight, bgColor)
        
        val borderColor = (alpha * 255).toInt() shl 24 or 0xFFFF00
        graphics.fill(x, y, x + notificationWidth, y + 2, borderColor)
        graphics.fill(x, y, x + 2, y + notificationHeight, borderColor)
        graphics.fill(x + notificationWidth - 2, y, x + notificationWidth, y + notificationHeight, borderColor)
        graphics.fill(x, y + notificationHeight - 2, x + notificationWidth, y + notificationHeight, borderColor)
        
        val iconSize = 48
        val iconX = x + 16
        val iconY = y + 16
        
        if (notification.icon != null) {
            graphics.blitInscribed(notification.icon, iconX, iconY, iconSize, iconSize, iconSize, iconSize, false, false)
        } else {
            val defaultIconColor = (alpha * 255).toInt() shl 24 or 0xFFD700
            graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, defaultIconColor)
        }
        
        val textX = iconX + iconSize + 16
        val textY = y + 20
        val textColor = (alpha * 255).toInt() shl 24 or 0xFFFFFF
        
        graphics.drawString(font, notification.name, textX, textY, textColor, false)
        
        val descY = textY + font.lineHeight + 4
        val descWidth = notificationWidth - textX + x - 16
        val wrappedDescription = font.split(notification.description, descWidth)
        val maxLines = 2
        wrappedDescription.take(maxLines).forEachIndexed { index, line ->
            if (index < maxLines) {
                graphics.drawString(font, line, textX, descY + index * font.lineHeight, textColor, false)
            }
        }
    }
    
    private fun calculateAlpha(): Float {
        if (currentNotification == null) return 0f
        
        val elapsed = notificationTimer
        return when {
            elapsed < fadeInDuration -> elapsed / fadeInDuration
            elapsed > notificationDuration - fadeOutDuration -> {
                val fadeOutElapsed = elapsed - (notificationDuration - fadeOutDuration)
                1f - (fadeOutElapsed / fadeOutDuration)
            }
            else -> 1f
        }.coerceIn(0f, 1f)
    }
    
    fun clear() {
        notificationQueue.clear()
        currentNotification = null
        notificationTimer = 0f
    }
}

