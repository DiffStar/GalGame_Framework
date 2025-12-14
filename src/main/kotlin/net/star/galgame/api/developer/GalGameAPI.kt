package net.star.galgame.api.developer

import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.dialogue.CharacterData
import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.contentpack.ContentPackManager
import net.star.galgame.contentpack.ContentPack
import java.nio.file.Path

object GalGameAPI {
    fun registerCharacter(id: String, name: Component, portraitPath: ResourceLocation, expressions: Map<String, ResourceLocation> = emptyMap()) {
        val character = DialogueManager.createCharacter(id, name, portraitPath, expressions)
        DialogueManager.registerCharacter(character)
    }

    fun getCharacter(id: String): CharacterData? {
        return DialogueManager.getCharacter(id)
    }

    fun registerScript(script: DialogueScript) {
        DialogueManager.registerScript(script)
    }

    fun getScript(id: String): DialogueScript? {
        return DialogueManager.getScript(id)
    }

    fun unregisterScript(id: String) {
        DialogueManager.unregisterScript(id)
    }

    fun getAllScripts(): List<DialogueScript> {
        return DialogueManager.getAllScripts()
    }

    fun loadContentPack(packPath: Path): ContentPack? {
        return ContentPackManager.loadPack(packPath)
    }

    fun unloadContentPack(packId: String) {
        ContentPackManager.unloadPack(packId)
    }

    fun getContentPack(packId: String): ContentPack? {
        return ContentPackManager.getPack(packId)
    }

    fun getAllContentPacks(): Map<String, ContentPack> {
        return ContentPackManager.getAllPacks()
    }

    fun isContentPackEnabled(packId: String): Boolean {
        return ContentPackManager.isPackEnabled(packId)
    }

    fun setContentPackEnabled(packId: String, enabled: Boolean) {
        ContentPackManager.setPackEnabled(packId, enabled)
    }

    fun reloadContentPack(packId: String): ContentPack? {
        return ContentPackManager.reloadPack(packId)
    }
}

