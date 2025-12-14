package net.star.galgame.dialogue

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

data class DialogueEntry(
    val id: String,
    val characterId: String?,
    val text: String,
    val expression: String = "normal",
    val position: CharacterPosition = CharacterPosition.LEFT,
    val read: Boolean = false
)

data class CharacterData(
    val id: String,
    val name: Component,
    val portraitPath: ResourceLocation,
    val expressions: Map<String, ResourceLocation> = emptyMap()
)

enum class CharacterPosition {
    LEFT, CENTER, RIGHT
}

data class DialogueScript(
    val id: String,
    val entries: List<DialogueEntry>
)

