package net.star.galgame.dialogue.state

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object GameStateManager {
    private var currentScene: String? = null
    private var currentChapter: String? = null
    private val readFlags = ConcurrentHashMap<String, Boolean>()
    private val achievements = ConcurrentHashMap<String, AchievementProgress>()
    private val statistics = ConcurrentHashMap<String, AtomicLong>()
    private val sceneHistory = mutableListOf<SceneRecord>()

    fun setCurrentScene(sceneId: String) {
        if (currentScene != sceneId) {
            currentScene?.let { recordSceneExit(it) }
            currentScene = sceneId
            recordSceneEnter(sceneId)
            net.star.galgame.dialogue.statistics.StatisticsManager.incrementSceneCount()
        }
    }

    fun getCurrentScene(): String? = currentScene

    fun setCurrentChapter(chapterId: String) {
        if (currentChapter != chapterId) {
            currentChapter = chapterId
            net.star.galgame.dialogue.statistics.StatisticsManager.incrementChapterCount()
        }
    }

    fun getCurrentChapter(): String? = currentChapter

    fun markAsRead(entryId: String) {
        readFlags[entryId] = true
    }

    fun isRead(entryId: String): Boolean {
        return readFlags[entryId] == true
    }

    fun clearReadFlags() {
        readFlags.clear()
    }

    fun getAllReadFlags(): Map<String, Boolean> {
        return readFlags.toMap()
    }

    fun setReadFlags(flags: Map<String, Boolean>) {
        readFlags.clear()
        readFlags.putAll(flags)
    }

    fun unlockAchievement(achievementId: String) {
        achievements.getOrPut(achievementId) { AchievementProgress() }.unlock()
    }

    fun getAchievementProgress(achievementId: String): AchievementProgress? {
        return achievements[achievementId]
    }

    fun isAchievementUnlocked(achievementId: String): Boolean {
        return achievements[achievementId]?.isUnlocked == true
    }

    fun getAllAchievements(): Map<String, AchievementProgress> {
        return achievements.toMap()
    }

    fun setAchievements(achievements: Map<String, AchievementProgress>) {
        this.achievements.clear()
        this.achievements.putAll(achievements)
    }

    fun incrementStatistic(statId: String, amount: Long = 1) {
        statistics.getOrPut(statId) { AtomicLong(0) }.addAndGet(amount)
    }

    fun setStatistic(statId: String, value: Long) {
        statistics[statId] = AtomicLong(value)
    }

    fun getStatistic(statId: String): Long {
        return statistics[statId]?.get() ?: 0
    }

    fun getAllStatistics(): Map<String, Long> {
        return statistics.mapValues { it.value.get() }
    }

    fun setStatistics(stats: Map<String, Long>) {
        statistics.clear()
        stats.forEach { (id, value) ->
            statistics[id] = AtomicLong(value)
        }
    }

    fun getSceneHistory(): List<SceneRecord> {
        return sceneHistory.toList()
    }

    fun clearSceneHistory() {
        sceneHistory.clear()
    }

    fun reset() {
        currentScene = null
        currentChapter = null
        readFlags.clear()
        achievements.clear()
        statistics.clear()
        sceneHistory.clear()
    }

    fun getState(): GameState {
        return GameState(
            currentScene = currentScene,
            currentChapter = currentChapter,
            readFlags = readFlags.toMap(),
            achievements = achievements.mapValues { it.value.copy() },
            statistics = statistics.mapValues { it.value.get() },
            sceneHistory = sceneHistory.toList()
        )
    }

    fun applyState(state: GameState) {
        currentScene = state.currentScene
        currentChapter = state.currentChapter
        setReadFlags(state.readFlags)
        setAchievements(state.achievements)
        setStatistics(state.statistics)
        sceneHistory.clear()
        sceneHistory.addAll(state.sceneHistory)
    }

    private fun recordSceneEnter(sceneId: String) {
        sceneHistory.add(SceneRecord(sceneId, System.currentTimeMillis(), SceneAction.ENTER))
    }

    private fun recordSceneExit(sceneId: String) {
        sceneHistory.add(SceneRecord(sceneId, System.currentTimeMillis(), SceneAction.EXIT))
    }
}

data class GameState(
    val currentScene: String?,
    val currentChapter: String?,
    val readFlags: Map<String, Boolean>,
    val achievements: Map<String, AchievementProgress>,
    val statistics: Map<String, Long>,
    val sceneHistory: List<SceneRecord>
)

data class AchievementProgress(
    var isUnlocked: Boolean = false,
    var unlockTime: Long = 0
) {
    private var _progress: Double = 0.0
    
    var progress: Double
        get() = _progress
        set(value) {
            _progress = value.coerceIn(0.0, 1.0)
            if (_progress >= 1.0 && !isUnlocked) {
                unlock()
            }
        }
    
    fun unlock() {
        if (!isUnlocked) {
            isUnlocked = true
            unlockTime = System.currentTimeMillis()
        }
    }
}

data class SceneRecord(
    val sceneId: String,
    val timestamp: Long,
    val action: SceneAction
)

enum class SceneAction {
    ENTER, EXIT
}

