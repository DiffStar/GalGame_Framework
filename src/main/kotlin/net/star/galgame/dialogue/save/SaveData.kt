package net.star.galgame.dialogue.save

import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.variable.VariableValue
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
    val variables: Map<String, SerializableVariableValue>,
    val timestamp: Long,
    val worldName: String?,
    val progress: String,
    val screenshotPath: String?
) : Serializable

sealed class SerializableVariableValue : Serializable {
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

