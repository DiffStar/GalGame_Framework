package net.star.galgame.dialogue.variable

import java.util.concurrent.ConcurrentHashMap

object VariableManager {
    private val variables = ConcurrentHashMap<String, VariableValue>()

    fun set(name: String, value: Any) {
        variables[name] = when (value) {
            is Int -> VariableValue.Number(value.toDouble())
            is Long -> VariableValue.Number(value.toDouble())
            is Float -> VariableValue.Number(value.toDouble())
            is Double -> VariableValue.Number(value)
            is Boolean -> VariableValue.Boolean(value)
            is String -> VariableValue.String(value)
            else -> VariableValue.String(value.toString())
        }
    }

    fun get(name: String): VariableValue? {
        return variables[name]
    }

    fun getNumber(name: String): Double? {
        return (variables[name] as? VariableValue.Number)?.value
    }

    fun getBoolean(name: String): Boolean? {
        return (variables[name] as? VariableValue.Boolean)?.value
    }

    fun getString(name: String): String? {
        return (variables[name] as? VariableValue.String)?.value
    }

    fun has(name: String): Boolean {
        return variables.containsKey(name)
    }

    fun clear() {
        variables.clear()
    }

    fun remove(name: String) {
        variables.remove(name)
    }

    fun getAll(): Map<String, VariableValue> {
        return variables.toMap()
    }
}

sealed class VariableValue {
    data class Number(val value: Double) : VariableValue()
    data class Boolean(val value: kotlin.Boolean) : VariableValue()
    data class String(val value: kotlin.String) : VariableValue()
}

