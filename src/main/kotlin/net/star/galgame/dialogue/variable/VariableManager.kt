package net.star.galgame.dialogue.variable

import java.util.concurrent.ConcurrentHashMap

object VariableManager {
    private val globalVariables = ConcurrentHashMap<String, VariableValue>()
    private val localVariableScopes = ConcurrentHashMap<String, ConcurrentHashMap<String, VariableValue>>()
    private var currentScope: String? = null

    fun setGlobal(name: String, value: Any) {
        globalVariables[name] = createValue(value)
    }

    fun setLocal(scope: String, name: String, value: Any) {
        localVariableScopes.getOrPut(scope) { ConcurrentHashMap() }[name] = createValue(value)
    }

    fun set(name: String, value: Any) {
        if (currentScope != null) {
            setLocal(currentScope!!, name, value)
        } else {
            setGlobal(name, value)
        }
    }

    fun getGlobal(name: String): VariableValue? {
        return globalVariables[name]
    }

    fun getLocal(scope: String, name: String): VariableValue? {
        return localVariableScopes[scope]?.get(name)
    }

    fun get(name: String): VariableValue? {
        if (currentScope != null) {
            return getLocal(currentScope!!, name) ?: getGlobal(name)
        }
        return getGlobal(name)
    }

    fun getInt(name: String): Int? {
        return getNumber(name)?.toInt()
    }

    fun getLong(name: String): Long? {
        return getNumber(name)?.toLong()
    }

    fun getFloat(name: String): Float? {
        return getNumber(name)?.toFloat()
    }

    fun getNumber(name: String): Double? {
        return (get(name) as? VariableValue.Number)?.value
    }

    fun getBoolean(name: String): Boolean? {
        return (get(name) as? VariableValue.Boolean)?.value
    }

    fun getString(name: String): String? {
        return (get(name) as? VariableValue.String)?.value
    }

    fun hasGlobal(name: String): Boolean {
        return globalVariables.containsKey(name)
    }

    fun hasLocal(scope: String, name: String): Boolean {
        return localVariableScopes[scope]?.containsKey(name) == true
    }

    fun has(name: String): Boolean {
        if (currentScope != null) {
            return hasLocal(currentScope!!, name) || hasGlobal(name)
        }
        return hasGlobal(name)
    }

    fun enterScope(scope: String) {
        currentScope = scope
        localVariableScopes.getOrPut(scope) { ConcurrentHashMap() }
    }

    fun exitScope() {
        currentScope = null
    }

    fun clearScope(scope: String) {
        localVariableScopes.remove(scope)
    }

    fun clearGlobal() {
        globalVariables.clear()
    }

    fun clear() {
        globalVariables.clear()
        localVariableScopes.clear()
        currentScope = null
    }

    fun removeGlobal(name: String) {
        globalVariables.remove(name)
    }

    fun removeLocal(scope: String, name: String) {
        localVariableScopes[scope]?.remove(name)
    }

    fun remove(name: String) {
        if (currentScope != null) {
            removeLocal(currentScope!!, name)
        } else {
            removeGlobal(name)
        }
    }

    fun getAllGlobal(): Map<String, VariableValue> {
        return globalVariables.toMap()
    }

    fun getAllLocal(scope: String): Map<String, VariableValue> {
        return localVariableScopes[scope]?.toMap() ?: emptyMap()
    }

    fun getAll(): Map<String, VariableValue> {
        val result = mutableMapOf<String, VariableValue>()
        result.putAll(globalVariables)
        if (currentScope != null) {
            result.putAll(localVariableScopes[currentScope] ?: emptyMap())
        }
        return result
    }

    private fun createValue(value: Any): VariableValue {
        return when (value) {
            is Int -> VariableValue.Integer(value)
            is Long -> VariableValue.Long(value)
            is Float -> VariableValue.Float(value)
            is Double -> VariableValue.Number(value)
            is Boolean -> VariableValue.Boolean(value)
            is String -> VariableValue.String(value)
            else -> VariableValue.String(value.toString())
        }
    }
}

sealed class VariableValue {
    data class Integer(val value: Int) : VariableValue()
    data class Long(val value: kotlin.Long) : VariableValue()
    data class Float(val value: kotlin.Float) : VariableValue()
    data class Number(val value: Double) : VariableValue()
    data class Boolean(val value: kotlin.Boolean) : VariableValue()
    data class String(val value: kotlin.String) : VariableValue()
}

