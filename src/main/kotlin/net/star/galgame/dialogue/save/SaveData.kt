package net.star.galgame.dialogue.save

import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.variable.VariableValue
import net.star.galgame.dialogue.state.AchievementProgress
import net.star.galgame.dialogue.state.SceneRecord
import java.io.Serializable

data class SerializableDialogueEntry(
    val id: String,
    val characterId: String?,
    val text: String,
    val expression: String,
    val position: String,
    val read: Boolean
) : Serializable

data class SaveData(
    val slotId: Int,
    val scriptId: String,
    val currentIndex: Int,
    val history: List<SerializableDialogueEntry>,
    val globalVariables: Map<String, SerializableVariableValue>,
    val localVariables: Map<String, Map<String, SerializableVariableValue>>,
    val gameState: SerializableGameState,
    val timestamp: Long,
    val worldName: String?,
    val progress: String,
    val screenshotPath: String?
) : Serializable {
    @Deprecated("Use globalVariables instead", ReplaceWith("globalVariables"))
    val variables: Map<String, SerializableVariableValue> = globalVariables
}

data class SerializableGameState(
    val currentScene: String?,
    val currentChapter: String?,
    val readFlags: Map<String, Boolean>,
    val achievements: Map<String, SerializableAchievementProgress>,
    val statistics: Map<String, Long>,
    val sceneHistory: List<SerializableSceneRecord>
) : Serializable

data class SerializableAchievementProgress(
    val isUnlocked: Boolean,
    val unlockTime: Long,
    val progress: Double
) : Serializable

data class SerializableSceneRecord(
    val sceneId: String,
    val timestamp: Long,
    val action: String
) : Serializable

sealed class SerializableVariableValue : Serializable {
    data class Integer(val value: Int) : SerializableVariableValue()
    data class Long(val value: kotlin.Long) : SerializableVariableValue()
    data class Float(val value: kotlin.Float) : SerializableVariableValue()
    data class Number(val value: Double) : SerializableVariableValue()
    data class Boolean(val value: kotlin.Boolean) : SerializableVariableValue()
    data class String(val value: kotlin.String) : SerializableVariableValue()
}

data class SaveSlot(
    val slotId: Int,
    val saveData: SaveData?,
    val isValid: Boolean,
    val isCorrupted: Boolean
)

