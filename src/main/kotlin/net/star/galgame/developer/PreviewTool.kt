package net.star.galgame.developer

import net.star.galgame.contentpack.ContentPack
import net.star.galgame.contentpack.script.ScriptParser
import net.star.galgame.dialogue.DialogueScript
import java.nio.file.Files

class PreviewTool(private val contentPack: ContentPack) {
    private val parser = ScriptParser()
    
    fun previewScript(scriptId: String): PreviewResult {
        val script = contentPack.scripts[scriptId] ?: return PreviewResult(null, listOf("脚本不存在: $scriptId"))
        
        val content = try {
            Files.readString(script.path)
        } catch (e: Exception) {
            return PreviewResult(null, listOf("无法读取脚本: ${e.message}"))
        }
        
        val parseResult = parser.parse(content, script.format)
        
        if (parseResult.errors.isNotEmpty()) {
            return PreviewResult(null, parseResult.errors)
        }
        
        val dialogueScript = parseResult.script ?: return PreviewResult(null, listOf("解析结果为空"))
        
        val preview = generatePreview(dialogueScript)
        
        return PreviewResult(dialogueScript, emptyList(), preview)
    }
    
    private fun generatePreview(script: DialogueScript): ScriptPreview {
        val totalEntries = script.entries.size
        val totalChoices = script.entries.sumOf { it.choices.size }
        val characters = script.entries.mapNotNull { it.characterId }.distinct()
        val labels = script.entries.mapNotNull { it.label }
        
        val entryPreviews = script.entries.take(10).map { entry ->
            EntryPreview(
                id = entry.id,
                characterId = entry.characterId,
                textPreview = entry.text.take(50) + if (entry.text.length > 50) "..." else "",
                hasChoices = entry.choices.isNotEmpty(),
                hasCondition = entry.condition != null
            )
        }
        
        return ScriptPreview(
            scriptId = script.id,
            totalEntries = totalEntries,
            totalChoices = totalChoices,
            characters = characters,
            labels = labels,
            entryPreviews = entryPreviews
        )
    }
    
    fun validateContentPack(): ContentPackValidation {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (contentPack.scripts.isEmpty()) {
            warnings.add("内容包没有脚本文件")
        }
        
        for ((id, script) in contentPack.scripts) {
            val result = parser.parse(script.content, script.format)
            if (result.errors.isNotEmpty()) {
                errors.addAll(result.errors.map { "脚本 $id: $it" })
            }
        }
        
        return ContentPackValidation(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    data class PreviewResult(
        val script: DialogueScript?,
        val errors: List<String>,
        val preview: ScriptPreview? = null
    )
    
    data class ScriptPreview(
        val scriptId: String,
        val totalEntries: Int,
        val totalChoices: Int,
        val characters: List<String>,
        val labels: List<String>,
        val entryPreviews: List<EntryPreview>
    )
    
    data class EntryPreview(
        val id: String,
        val characterId: String?,
        val textPreview: String,
        val hasChoices: Boolean,
        val hasCondition: Boolean
    )
    
    data class ContentPackValidation(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    )
}

