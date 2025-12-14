package net.star.galgame.dialogue.statistics

import net.star.galgame.dialogue.state.GameStateManager
import java.util.concurrent.atomic.AtomicLong

object StatisticsManager {
    private const val STAT_GAME_TIME = "game_time"
    private const val STAT_DIALOGUE_COUNT = "dialogue_count"
    private const val STAT_CHOICE_COUNT = "choice_count"
    private const val STAT_SCENE_COUNT = "scene_count"
    private const val STAT_CHAPTER_COUNT = "chapter_count"
    
    private var gameStartTime: Long = 0
    private var lastUpdateTime: Long = 0
    private var isGameActive = false
    
    fun startGame() {
        gameStartTime = System.currentTimeMillis()
        lastUpdateTime = gameStartTime
        isGameActive = true
    }
    
    fun stopGame() {
        if (isGameActive) {
            updateGameTime()
            isGameActive = false
        }
    }
    
    fun update(deltaTime: Float) {
        if (isGameActive) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime >= 1000) {
                updateGameTime()
                lastUpdateTime = currentTime
            }
        }
    }
    
    private fun updateGameTime() {
        if (gameStartTime > 0) {
            val elapsed = (System.currentTimeMillis() - gameStartTime) / 1000
            val currentTime = GameStateManager.getStatistic(STAT_GAME_TIME)
            GameStateManager.setStatistic(STAT_GAME_TIME, currentTime + elapsed)
            gameStartTime = System.currentTimeMillis()
        }
    }
    
    fun incrementDialogueCount() {
        GameStateManager.incrementStatistic(STAT_DIALOGUE_COUNT)
    }
    
    fun incrementChoiceCount() {
        GameStateManager.incrementStatistic(STAT_CHOICE_COUNT)
    }
    
    fun incrementSceneCount() {
        GameStateManager.incrementStatistic(STAT_SCENE_COUNT)
    }
    
    fun incrementChapterCount() {
        GameStateManager.incrementStatistic(STAT_CHAPTER_COUNT)
    }
    
    fun getGameTime(): Long {
        return GameStateManager.getStatistic(STAT_GAME_TIME)
    }
    
    fun getDialogueCount(): Long {
        return GameStateManager.getStatistic(STAT_DIALOGUE_COUNT)
    }
    
    fun getChoiceCount(): Long {
        return GameStateManager.getStatistic(STAT_CHOICE_COUNT)
    }
    
    fun getSceneCount(): Long {
        return GameStateManager.getStatistic(STAT_SCENE_COUNT)
    }
    
    fun getChapterCount(): Long {
        return GameStateManager.getStatistic(STAT_CHAPTER_COUNT)
    }
    
    fun getCompletionRate(totalDialogues: Int, totalChoices: Int, totalScenes: Int, totalChapters: Int): Double {
        if (totalDialogues == 0 && totalChoices == 0 && totalScenes == 0 && totalChapters == 0) {
            return 0.0
        }
        
        val dialogueRate = if (totalDialogues > 0) {
            getDialogueCount().toDouble() / totalDialogues
        } else 0.0
        
        val choiceRate = if (totalChoices > 0) {
            getChoiceCount().toDouble() / totalChoices
        } else 0.0
        
        val sceneRate = if (totalScenes > 0) {
            getSceneCount().toDouble() / totalScenes
        } else 0.0
        
        val chapterRate = if (totalChapters > 0) {
            getChapterCount().toDouble() / totalChapters
        } else 0.0
        
        return (dialogueRate + choiceRate + sceneRate + chapterRate) / 4.0
    }
    
    fun getFormattedGameTime(): String {
        val seconds = getGameTime()
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
    
    fun reset() {
        GameStateManager.setStatistic(STAT_GAME_TIME, 0)
        GameStateManager.setStatistic(STAT_DIALOGUE_COUNT, 0)
        GameStateManager.setStatistic(STAT_CHOICE_COUNT, 0)
        GameStateManager.setStatistic(STAT_SCENE_COUNT, 0)
        GameStateManager.setStatistic(STAT_CHAPTER_COUNT, 0)
        gameStartTime = 0
        lastUpdateTime = 0
        isGameActive = false
    }
    
    fun getAllStatistics(): Map<String, Long> {
        return mapOf(
            STAT_GAME_TIME to getGameTime(),
            STAT_DIALOGUE_COUNT to getDialogueCount(),
            STAT_CHOICE_COUNT to getChoiceCount(),
            STAT_SCENE_COUNT to getSceneCount(),
            STAT_CHAPTER_COUNT to getChapterCount()
        )
    }
}

