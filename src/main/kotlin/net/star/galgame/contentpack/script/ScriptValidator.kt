package net.star.galgame.contentpack.script

import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.ChoiceEntry

class ScriptValidator {
    fun validate(script: DialogueScript): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (script.id.isBlank()) {
            errors.add("脚本ID不能为空")
        }
        
        if (script.entries.isEmpty()) {
            warnings.add("脚本没有对话条目")
        }
        
        val entryIds = mutableSetOf<String>()
        val labels = mutableSetOf<String>()
        
        script.entries.forEachIndexed { index, entry ->
            validateEntry(entry, index, entryIds, labels, errors, warnings)
        }
        
        val allLabels = labels.toSet()
        script.entries.forEach { entry ->
            if (entry.jumpTo != null && !allLabels.contains(entry.jumpTo)) {
                errors.add("条目 ${entry.id}: 跳转标签 '${entry.jumpTo}' 不存在")
            }
            entry.choices.forEach { choice ->
                if (!allLabels.contains(choice.jumpTo)) {
                    errors.add("选择项 ${choice.id}: 跳转标签 '${choice.jumpTo}' 不存在")
                }
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun validateEntry(
        entry: DialogueEntry,
        index: Int,
        entryIds: MutableSet<String>,
        labels: MutableSet<String>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (entry.id.isBlank()) {
            errors.add("条目 #$index: ID不能为空")
        } else if (entryIds.contains(entry.id)) {
            errors.add("条目 #$index: ID '${entry.id}' 重复")
        } else {
            entryIds.add(entry.id)
        }
        
        if (entry.text.isBlank()) {
            warnings.add("条目 ${entry.id}: 文本为空")
        }
        
        if (entry.label != null) {
            if (labels.contains(entry.label)) {
                errors.add("条目 ${entry.id}: 标签 '${entry.label}' 重复")
            } else {
                labels.add(entry.label)
            }
        }
        
        if (entry.characterId != null && entry.characterId.isBlank()) {
            warnings.add("条目 ${entry.id}: characterId为空字符串")
        }
        
        entry.choices.forEachIndexed { choiceIndex, choice ->
            validateChoice(choice, entry.id, choiceIndex, errors, warnings)
        }
        
        if (entry.choices.isNotEmpty() && entry.jumpTo != null) {
            warnings.add("条目 ${entry.id}: 同时包含选择项和跳转，跳转将被忽略")
        }
    }
    
    private fun validateChoice(
        choice: ChoiceEntry,
        entryId: String,
        choiceIndex: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (choice.id.isBlank()) {
            errors.add("条目 $entryId 的选择项 #$choiceIndex: ID不能为空")
        }
        
        if (choice.text.isBlank()) {
            warnings.add("条目 $entryId 的选择项 ${choice.id}: 文本为空")
        }
        
        if (choice.jumpTo.isBlank()) {
            errors.add("条目 $entryId 的选择项 ${choice.id}: jumpTo不能为空")
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

