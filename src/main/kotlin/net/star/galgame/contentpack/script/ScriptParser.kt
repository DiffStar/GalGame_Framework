package net.star.galgame.contentpack.script

import net.star.galgame.dialogue.*
import net.star.galgame.dialogue.condition.CompareOperator
import net.star.galgame.dialogue.condition.Condition
import net.star.galgame.dialogue.variable.VariableValue
import net.star.galgame.contentpack.ScriptFormat

class ScriptParser {
    fun parse(content: String, format: ScriptFormat): ParseResult {
        return when (format) {
            ScriptFormat.JSON -> parseJson(content)
            ScriptFormat.YAML -> parseYaml(content)
            ScriptFormat.DSL -> parseDsl(content)
        }
    }

    private fun parseJson(content: String): ParseResult {
        try {
            val json = parseJsonString(content)
            return parseScriptObject(json)
        } catch (e: Exception) {
            return ParseResult(null, listOf("JSON解析错误: ${e.message}"))
        }
    }

    private fun parseYaml(content: String): ParseResult {
        try {
            val lines = content.lines()
            val script = mutableMapOf<String, Any>()
            parseYamlLines(lines, script, 0)
            return parseScriptObject(script)
        } catch (e: Exception) {
            return ParseResult(null, listOf("YAML解析错误: ${e.message}"))
        }
    }

    private fun parseDsl(content: String): ParseResult {
        try {
            val entries = mutableListOf<DialogueEntry>()
            val lines = content.lines()
            var currentEntry: MutableMap<String, Any>? = null
            var entryId = 0

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                when {
                    trimmed.startsWith("label:") -> {
                        if (currentEntry != null) {
                            entries.add(parseEntry(currentEntry, entryId.toString()))
                        }
                        currentEntry = mutableMapOf()
                        currentEntry["label"] = trimmed.substringAfter(":").trim()
                        entryId++
                    }
                    trimmed.startsWith("character:") -> {
                        currentEntry = currentEntry ?: mutableMapOf()
                        currentEntry["characterId"] = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("text:") -> {
                        currentEntry = currentEntry ?: mutableMapOf()
                        currentEntry["text"] = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("expression:") -> {
                        currentEntry = currentEntry ?: mutableMapOf()
                        currentEntry["expression"] = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("position:") -> {
                        currentEntry = currentEntry ?: mutableMapOf()
                        currentEntry["position"] = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("jump:") -> {
                        currentEntry = currentEntry ?: mutableMapOf()
                        currentEntry["jumpTo"] = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("choice:") -> {
                        currentEntry = currentEntry ?: mutableMapOf()
                        val choiceText = trimmed.substringAfter(":").trim()
                        val choiceParts = choiceText.split("->")
                        if (choiceParts.size == 2) {
                            val choices = currentEntry.getOrPut("choices") { mutableListOf<Map<String, Any>>() } as MutableList<Map<String, Any>>
                            choices.add(mapOf(
                                "id" to choices.size.toString(),
                                "text" to choiceParts[0].trim(),
                                "jumpTo" to choiceParts[1].trim()
                            ))
                        }
                    }
                    else -> {
                        if (currentEntry != null && !trimmed.startsWith("-")) {
                            val existingText = currentEntry["text"] as? String ?: ""
                            currentEntry["text"] = if (existingText.isEmpty()) trimmed else "$existingText\n$trimmed"
                        }
                    }
                }
            }

            if (currentEntry != null) {
                entries.add(parseEntry(currentEntry, entryId.toString()))
            }

            val scriptId = extractScriptId(content) ?: "script_${System.currentTimeMillis()}"
            return ParseResult(DialogueScript(scriptId, entries), emptyList())
        } catch (e: Exception) {
            return ParseResult(null, listOf("DSL解析错误: ${e.message}"))
        }
    }

    private fun parseScriptObject(obj: Map<String, Any>): ParseResult {
        try {
            val scriptId = (obj["id"] as? String) ?: "unknown"
            val entriesList = obj["entries"] as? List<*> ?: return ParseResult(null, listOf("缺少entries字段"))

            val entries = entriesList.mapIndexedNotNull { index, entryObj ->
                if (entryObj is Map<*, *>) {
                    parseEntry(entryObj as Map<String, Any>, index.toString())
                } else null
            }

            return ParseResult(DialogueScript(scriptId, entries), emptyList())
        } catch (e: Exception) {
            return ParseResult(null, listOf("脚本对象解析错误: ${e.message}"))
        }
    }

    private fun parseEntry(entryObj: Map<String, Any>, defaultId: String): DialogueEntry {
        val id = (entryObj["id"] as? String) ?: defaultId
        val characterId = entryObj["characterId"] as? String
        val text = (entryObj["text"] as? String) ?: ""
        val expression = (entryObj["expression"] as? String) ?: "normal"
        val positionStr = (entryObj["position"] as? String) ?: "LEFT"
        val position = when (positionStr.uppercase()) {
            "LEFT" -> CharacterPosition.LEFT
            "CENTER" -> CharacterPosition.CENTER
            "RIGHT" -> CharacterPosition.RIGHT
            else -> CharacterPosition.LEFT
        }
        val label = entryObj["label"] as? String
        val jumpTo = entryObj["jumpTo"] as? String
        val condition = (entryObj["condition"] as? Map<*, *>)?.let { parseCondition(it) }
        val choicesList = entryObj["choices"] as? List<*>
        val choices = choicesList?.mapIndexedNotNull { index, choiceObj ->
            if (choiceObj is Map<*, *>) {
                parseChoice(choiceObj as Map<String, Any>, index.toString())
            } else null
        } ?: emptyList()

        return DialogueEntry(
            id = id,
            characterId = characterId,
            text = text,
            expression = expression,
            position = position,
            read = false,
            label = label,
            jumpTo = jumpTo,
            condition = condition,
            choices = choices
        )
    }

    private fun parseChoice(choiceObj: Map<String, Any>, defaultId: String): ChoiceEntry {
        val id = (choiceObj["id"] as? String) ?: defaultId
        val text = (choiceObj["text"] as? String) ?: ""
        val jumpTo = (choiceObj["jumpTo"] as? String) ?: ""
        val condition = (choiceObj["condition"] as? Map<*, *>)?.let { parseCondition(it) }
        val visible = (choiceObj["visible"] as? Boolean) ?: true

        return ChoiceEntry(id, text, jumpTo, condition, visible)
    }

    private fun parseCondition(conditionObj: Map<*, *>): Condition? {
        return when (val type = conditionObj["type"] as? String) {
            "compare" -> {
                val variable = conditionObj["variable"] as? String ?: return null
                val operatorStr = conditionObj["operator"] as? String ?: return null
                val operator = when (operatorStr.uppercase()) {
                    "==", "EQUALS" -> CompareOperator.EQUALS
                    "!=", "NOT_EQUALS" -> CompareOperator.NOT_EQUALS
                    ">", "GREATER_THAN" -> CompareOperator.GREATER_THAN
                    "<", "LESS_THAN" -> CompareOperator.LESS_THAN
                    ">=", "GREATER_EQUAL" -> CompareOperator.GREATER_EQUAL
                    "<=", "LESS_EQUAL" -> CompareOperator.LESS_EQUAL
                    else -> return null
                }
                val value = parseVariableValue(conditionObj["value"])
                if (value != null) {
                    Condition.Compare(variable, operator, value)
                } else null
            }
            "and" -> {
                val conditions = (conditionObj["conditions"] as? List<*>)?.mapNotNull {
                    if (it is Map<*, *>) parseCondition(it as Map<*, *>) else null
                } ?: return null
                Condition.And(conditions)
            }
            "or" -> {
                val conditions = (conditionObj["conditions"] as? List<*>)?.mapNotNull {
                    if (it is Map<*, *>) parseCondition(it as Map<*, *>) else null
                } ?: return null
                Condition.Or(conditions)
            }
            "not" -> {
                val condition = (conditionObj["condition"] as? Map<*, *>)?.let { parseCondition(it) } ?: return null
                Condition.Not(condition)
            }
            "hasVariable" -> {
                val variable = conditionObj["variable"] as? String ?: return null
                Condition.HasVariable(variable)
            }
            else -> null
        }
    }

    private fun parseVariableValue(value: Any?): VariableValue? {
        return when (value) {
            is Number -> VariableValue.Number(value.toDouble())
            is String -> VariableValue.String(value)
            is Boolean -> VariableValue.Boolean(value)
            else -> null
        }
    }

    private fun parseJsonString(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        var i = 0
        skipWhitespace(json, i)
        i = skipChar(json, i, '{')
        
        while (i < json.length) {
            skipWhitespace(json, i)
            if (json[i] == '}') break
            
            val key = parseJsonStringValue(json, i)
            i = key.second
            skipWhitespace(json, i)
            i = skipChar(json, i, ':')
            skipWhitespace(json, i)
            
            val value = parseJsonValue(json, i)
            i = value.second
            result[key.first] = value.first ?: ""
            
            skipWhitespace(json, i)
            if (json[i] == ',') {
                i++
            }
        }
        
        return result
    }

    private fun parseJsonValue(json: String, start: Int): Pair<Any?, Int> {
        var i = start
        skipWhitespace(json, i)
        
        return when (json[i]) {
            '"' -> parseJsonStringValue(json, i)
            '{' -> {
                val obj = mutableMapOf<String, Any>()
                i++
                while (i < json.length && json[i] != '}') {
                    skipWhitespace(json, i)
                    val key = parseJsonStringValue(json, i)
                    i = key.second
                    skipWhitespace(json, i)
                    i = skipChar(json, i, ':')
                    skipWhitespace(json, i)
                    val value = parseJsonValue(json, i)
                    i = value.second
                    obj[key.first] = value.first ?: ""
                    skipWhitespace(json, i)
                    if (i < json.length && json[i] == ',') i++
                }
                if (i < json.length && json[i] == '}') i++
                Pair(obj, i)
            }
            '[' -> {
                val list = mutableListOf<Any>()
                i++
                while (i < json.length && json[i] != ']') {
                    skipWhitespace(json, i)
                    val value = parseJsonValue(json, i)
                    i = value.second
                    list.add(value.first ?: "")
                    skipWhitespace(json, i)
                    if (i < json.length && json[i] == ',') i++
                }
                if (i < json.length && json[i] == ']') i++
                Pair(list, i)
            }
            't' -> {
                if (json.substring(i).startsWith("true")) {
                    Pair(true, i + 4)
                } else {
                    Pair(null, i)
                }
            }
            'f' -> {
                if (json.substring(i).startsWith("false")) {
                    Pair(false, i + 5)
                } else {
                    Pair(null, i)
                }
            }
            'n' -> {
                if (json.substring(i).startsWith("null")) {
                    Pair(null, i + 4)
                } else {
                    Pair(null, i)
                }
            }
            else -> {
                val num = parseJsonNumber(json, i)
                Pair(num.first, num.second)
            }
        }
    }

    private fun parseJsonStringValue(json: String, start: Int): Pair<String, Int> {
        var i = start
        if (json[i] == '"') i++
        val sb = StringBuilder()
        while (i < json.length && json[i] != '"') {
            if (json[i] == '\\' && i + 1 < json.length) {
                when (json[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    else -> { sb.append(json[i]); i++ }
                }
            } else {
                sb.append(json[i])
                i++
            }
        }
        if (i < json.length && json[i] == '"') i++
        return Pair(sb.toString(), i)
    }

    private fun parseJsonNumber(json: String, start: Int): Pair<Double, Int> {
        var i = start
        val sb = StringBuilder()
        while (i < json.length && (json[i].isDigit() || json[i] == '.' || json[i] == '-' || json[i] == '+')) {
            sb.append(json[i])
            i++
        }
        return Pair(sb.toString().toDoubleOrNull() ?: 0.0, i)
    }

    private fun skipWhitespace(json: String, start: Int): Int {
        var i = start
        while (i < json.length && json[i].isWhitespace()) {
            i++
        }
        return i
    }

    private fun skipChar(json: String, start: Int, char: Char): Int {
        var i = start
        while (i < json.length && json[i] != char) {
            if (!json[i].isWhitespace()) throw IllegalArgumentException("期望字符 '$char'")
            i++
        }
        return if (i < json.length && json[i] == char) i + 1 else i
    }

    private fun parseYamlLines(lines: List<String>, obj: MutableMap<String, Any>, startLine: Int): Int {
        var i = startLine
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++
                continue
            }
            
            val indent = line.takeWhile { it == ' ' || it == '\t' }.length
            if (indent == 0 && i > startLine) break
            
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex > 0) {
                val key = trimmed.substring(0, colonIndex).trim()
                val valueStr = trimmed.substring(colonIndex + 1).trim()
                
                if (valueStr.isEmpty() && i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    val nextIndent = nextLine.takeWhile { it == ' ' || it == '\t' }.length
                    if (nextIndent > indent) {
                        val nested = mutableMapOf<String, Any>()
                        i = parseYamlLines(lines, nested, i + 1)
                        obj[key] = nested
                        continue
                    } else if (nextLine.trim().startsWith("-")) {
                        val list = mutableListOf<Any>()
                        var j = i + 1
                        while (j < lines.size) {
                            val listLine = lines[j]
                            val listIndent = listLine.takeWhile { it == ' ' || it == '\t' }.length
                            if (listIndent <= indent) break
                            if (listLine.trim().startsWith("-")) {
                                val itemStr = listLine.trim().substring(1).trim()
                                if (itemStr.contains(":")) {
                                    val item = mutableMapOf<String, Any>()
                                    parseYamlItem(lines, item, j, listIndent)
                                    list.add(item)
                                    while (j < lines.size) {
                                        val checkLine = lines[j]
                                        val checkIndent = checkLine.takeWhile { it == ' ' || it == '\t' }.length
                                        if (checkIndent <= listIndent) break
                                        j++
                                    }
                                } else {
                                    list.add(itemStr)
                                    j++
                                }
                            } else {
                                j++
                            }
                        }
                        obj[key] = list
                        i = j
                        continue
                    }
                }
                
                obj[key] = parseYamlValue(valueStr)
            }
            i++
        }
        return i
    }

    private fun parseYamlItem(lines: List<String>, obj: MutableMap<String, Any>, startLine: Int, baseIndent: Int): Int {
        var i = startLine
        val itemLine = lines[i].trim().substring(1).trim()
        val parts = itemLine.split(":", limit = 2)
        if (parts.size == 2) {
            obj[parts[0].trim()] = parseYamlValue(parts[1].trim())
        }
        i++
        
        while (i < lines.size) {
            val line = lines[i]
            val indent = line.takeWhile { it == ' ' || it == '\t' }.length
            if (indent <= baseIndent) break
            
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++
                continue
            }
            
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex > 0) {
                val key = trimmed.substring(0, colonIndex).trim()
                val valueStr = trimmed.substring(colonIndex + 1).trim()
                obj[key] = parseYamlValue(valueStr)
            }
            i++
        }
        
        return i
    }

    private fun parseYamlValue(value: String): Any {
        return when {
            value == "true" -> true
            value == "false" -> false
            value == "null" -> ""
            value.startsWith("\"") && value.endsWith("\"") -> value.substring(1, value.length - 1)
            value.startsWith("'") && value.endsWith("'") -> value.substring(1, value.length - 1)
            value.toDoubleOrNull() != null -> value.toDouble()
            value.toIntOrNull() != null -> value.toInt()
            else -> value
        }
    }

    private fun extractScriptId(content: String): String? {
        val lines = content.lines()
        for (line in lines) {
            if (line.trim().startsWith("script_id:") || line.trim().startsWith("id:")) {
                return line.substringAfter(":").trim()
            }
        }
        return null
    }
}

data class ParseResult(
    val script: DialogueScript?,
    val errors: List<String>
)

