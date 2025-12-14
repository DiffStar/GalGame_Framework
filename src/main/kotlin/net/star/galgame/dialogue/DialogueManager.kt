package net.star.galgame.dialogue

import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import java.util.concurrent.ConcurrentHashMap

object DialogueManager {
    private val characters = ConcurrentHashMap<String, CharacterData>()
    private val scripts = ConcurrentHashMap<String, DialogueScript>()

    fun registerCharacter(character: CharacterData) {
        characters[character.id] = character
    }

    fun getCharacter(id: String): CharacterData? {
        return characters[id]
    }

    fun registerScript(script: DialogueScript) {
        scripts[script.id] = script
    }
    
    fun unregisterScript(scriptId: String) {
        scripts.remove(scriptId)
    }

    fun getScript(id: String): DialogueScript? {
        return scripts[id]
    }

    fun getAllScripts(): List<DialogueScript> {
        return scripts.values.toList()
    }

    fun createCharacter(
        id: String,
        name: Component,
        portraitPath: ResourceLocation,
        expressions: Map<String, ResourceLocation> = emptyMap()
    ): CharacterData {
        return CharacterData(id, name, portraitPath, expressions)
    }
}

