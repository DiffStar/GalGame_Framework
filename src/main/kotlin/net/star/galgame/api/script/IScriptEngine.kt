package net.star.galgame.api.script

import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.contentpack.ScriptFormat

interface IScriptEngine {
    fun parse(content: String, format: ScriptFormat): ScriptParseResult
    fun validate(script: DialogueScript): ScriptValidationResult
    fun execute(script: DialogueScript, context: ScriptExecutionContext): ScriptExecutionResult
}

interface IScriptRegistry {
    fun registerEngine(format: ScriptFormat, engine: IScriptEngine)
    fun unregisterEngine(format: ScriptFormat)
    fun getEngine(format: ScriptFormat): IScriptEngine?
}

object ScriptEngineRegistry : IScriptRegistry {
    private val engines = mutableMapOf<ScriptFormat, IScriptEngine>()

    override fun registerEngine(format: ScriptFormat, engine: IScriptEngine) {
        engines[format] = engine
    }

    override fun unregisterEngine(format: ScriptFormat) {
        engines.remove(format)
    }

    override fun getEngine(format: ScriptFormat): IScriptEngine? {
        return engines[format]
    }
}

data class ScriptParseResult(
    val script: DialogueScript?,
    val errors: List<String>,
    val warnings: List<String> = emptyList()
)

data class ScriptValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String> = emptyList()
)

data class ScriptExecutionContext(
    val variables: Map<String, Any> = emptyMap(),
    val metadata: Map<String, Any> = emptyMap()
)

data class ScriptExecutionResult(
    val success: Boolean,
    val result: Any? = null,
    val errors: List<String> = emptyList()
)

