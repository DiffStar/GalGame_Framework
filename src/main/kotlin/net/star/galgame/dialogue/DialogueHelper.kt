package net.star.galgame.dialogue

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

object DialogueHelper {
    fun openDialogue(script: DialogueScript) {
        val minecraft = Minecraft.getInstance()
        minecraft.execute {
            minecraft.setScreen(DialogueScreen(script))
        }
    }

    fun createSimpleDialogue(
        id: String,
        entries: List<DialogueEntry>
    ): DialogueScript {
        return DialogueScript(id, entries)
    }

    fun createDialogueEntry(
        id: String,
        text: String,
        characterId: String? = null,
        expression: String = "normal",
        position: CharacterPosition = CharacterPosition.LEFT
    ): DialogueEntry {
        return DialogueEntry(id, characterId, text, expression, position, false)
    }

    fun registerCharacter(
        id: String,
        name: Component,
        portraitPath: ResourceLocation,
        expressions: Map<String, ResourceLocation> = emptyMap()
    ) {
        val character = DialogueManager.createCharacter(id, name, portraitPath, expressions)
        DialogueManager.registerCharacter(character)
    }
}

