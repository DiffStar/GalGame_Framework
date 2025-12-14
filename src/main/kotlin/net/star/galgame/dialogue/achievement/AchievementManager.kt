package net.star.galgame.dialogue.achievement

import net.star.galgame.dialogue.state.GameStateManager
import java.util.concurrent.ConcurrentHashMap

object AchievementManager {
    private val definitions = ConcurrentHashMap<String, AchievementDefinition>()
    private val unlockListeners = mutableListOf<(String) -> Unit>()
    
    fun registerAchievement(definition: AchievementDefinition) {
        definitions[definition.id] = definition
    }
    
    fun unregisterAchievement(achievementId: String) {
        definitions.remove(achievementId)
    }
    
    fun getAchievement(achievementId: String): AchievementDefinition? {
        return definitions[achievementId]
    }
    
    fun getAllAchievements(): Map<String, AchievementDefinition> {
        return definitions.toMap()
    }
    
    fun checkAchievements() {
        definitions.forEach { (id, definition) ->
            val progress = GameStateManager.getAchievementProgress(id)
            if (progress?.isUnlocked == true) {
                return@forEach
            }
            
            if (definition.condition.evaluate()) {
                unlockAchievement(id)
            } else {
                updateProgress(id, definition)
            }
        }
    }
    
    fun checkAchievement(achievementId: String) {
        val definition = definitions[achievementId] ?: return
        val progress = GameStateManager.getAchievementProgress(achievementId)
        if (progress?.isUnlocked == true) {
            return
        }
        
        if (definition.condition.evaluate()) {
            unlockAchievement(achievementId)
        } else {
            updateProgress(achievementId, definition)
        }
    }
    
    private fun unlockAchievement(achievementId: String) {
        val progress = GameStateManager.getAchievementProgress(achievementId)
        if (progress?.isUnlocked == true) {
            return
        }
        
        GameStateManager.unlockAchievement(achievementId)
        unlockListeners.forEach { it(achievementId) }
    }
    
    private fun updateProgress(achievementId: String, definition: AchievementDefinition) {
        val progress = GameStateManager.getAchievementProgress(achievementId) ?: return
        
        val condition = definition.condition
        val progressValue = calculateProgress(condition)
        
        if (progressValue > progress.progress) {
            progress.progress = progressValue
        }
    }
    
    private fun calculateProgress(condition: net.star.galgame.dialogue.condition.Condition): Double {
        return when (condition) {
            is net.star.galgame.dialogue.condition.Condition.And -> {
                val results = condition.conditions.map { calculateProgress(it) }
                results.average()
            }
            is net.star.galgame.dialogue.condition.Condition.Or -> {
                val results = condition.conditions.map { calculateProgress(it) }
                results.maxOrNull() ?: 0.0
            }
            is net.star.galgame.dialogue.condition.Condition.Not -> {
                1.0 - calculateProgress(condition.condition)
            }
            is net.star.galgame.dialogue.condition.Condition.Compare -> {
                if (condition.evaluate()) 1.0 else 0.0
            }
            is net.star.galgame.dialogue.condition.Condition.HasVariable -> {
                if (condition.evaluate()) 1.0 else 0.0
            }
        }
    }
    
    fun addUnlockListener(listener: (String) -> Unit) {
        unlockListeners.add(listener)
    }
    
    fun removeUnlockListener(listener: (String) -> Unit) {
        unlockListeners.remove(listener)
    }
    
    fun getUnlockedCount(): Int {
        return definitions.keys.count { GameStateManager.isAchievementUnlocked(it) }
    }
    
    fun getTotalCount(): Int {
        return definitions.size
    }
    
    fun getCompletionRate(): Double {
        val total = getTotalCount()
        if (total == 0) return 0.0
        return getUnlockedCount().toDouble() / total
    }
    
    fun getAchievementsByCategory(category: String): Map<String, AchievementDefinition> {
        return definitions.filter { it.value.category == category }
    }
    
    fun getTotalPoints(): Int {
        return definitions.values.sumOf { it.points }
    }
    
    fun getUnlockedPoints(): Int {
        return definitions.values
            .filter { GameStateManager.isAchievementUnlocked(it.id) }
            .sumOf { it.points }
    }
    
    fun clear() {
        definitions.clear()
        unlockListeners.clear()
    }
}

