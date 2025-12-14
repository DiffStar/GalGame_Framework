package net.star.galgame.developer

import net.star.galgame.dialogue.variable.VariableManager
import net.star.galgame.dialogue.variable.VariableValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

object VariableWatcher {
    private val watchedVariables = ConcurrentHashMap<String, WatchConfig>()
    private val changeHistory = ConcurrentLinkedQueue<VariableChange>()
    private val isEnabled = AtomicBoolean(false)
    private val maxHistory = 500
    
    data class WatchConfig(
        val name: String,
        val enabled: Boolean = true,
        val notifyOnChange: Boolean = true,
        val breakOnChange: Boolean = false
    )
    
    data class VariableChange(
        val name: String,
        val oldValue: Any?,
        val newValue: Any?,
        val timestamp: Long
    )
    
    fun enable() {
        isEnabled.set(true)
        DevLogger.info("VariableWatcher", "变量监视器已启用")
    }
    
    fun disable() {
        isEnabled.set(false)
        DevLogger.info("VariableWatcher", "变量监视器已禁用")
    }
    
    fun isEnabled(): Boolean = isEnabled.get()
    
    fun watch(name: String, notifyOnChange: Boolean = true, breakOnChange: Boolean = false) {
        watchedVariables[name] = WatchConfig(name, true, notifyOnChange, breakOnChange)
        DevLogger.debug("VariableWatcher", "开始监视变量: $name")
    }
    
    fun unwatch(name: String) {
        watchedVariables.remove(name)
        DevLogger.debug("VariableWatcher", "停止监视变量: $name")
    }
    
    fun getWatchedVariables(): Set<String> = watchedVariables.keys
    
    fun update() {
        if (!isEnabled.get() || !DevModeManager.isEnabled()) return
        
        for ((name, config) in watchedVariables) {
            if (!config.enabled) continue
            
            val currentValue = VariableManager.get(name)
            val lastChange = changeHistory.lastOrNull { it.name == name }
            val lastValue = lastChange?.newValue
            
            if (hasChanged(currentValue, lastValue)) {
                recordChange(name, lastValue, currentValue)
                
                if (config.notifyOnChange) {
                    DevLogger.info("VariableWatcher", "变量 $name 已更改: $lastValue -> $currentValue")
                }
                
                if (config.breakOnChange) {
                    DevLogger.warn("VariableWatcher", "变量 $name 更改触发断点")
                }
            }
        }
    }
    
    private fun hasChanged(current: VariableValue?, last: Any?): Boolean {
        if (current == null && last == null) return false
        if (current == null || last == null) return true
        
        val currentValue = when (current) {
            is VariableValue.Integer -> current.value
            is VariableValue.Number -> current.value
            is VariableValue.Boolean -> current.value
            is VariableValue.String -> current.value
            else -> current.toString()
        }
        
        return currentValue != last
    }
    
    private fun recordChange(name: String, oldValue: Any?, newValue: VariableValue?) {
        val change = VariableChange(
            name = name,
            oldValue = oldValue,
            newValue = newValue?.let {
                when (it) {
                    is VariableValue.Integer -> it.value
                    is VariableValue.Number -> it.value
                    is VariableValue.Boolean -> it.value
                    is VariableValue.String -> it.value
                    else -> it.toString()
                }
            },
            timestamp = System.currentTimeMillis()
        )
        
        changeHistory.offer(change)
        while (changeHistory.size > maxHistory) {
            changeHistory.poll()
        }
    }
    
    fun getChangeHistory(name: String? = null, limit: Int = 100): List<VariableChange> {
        val filtered = changeHistory.asSequence()
            .filter { name == null || it.name == name }
            .toList()
        return filtered.takeLast(limit)
    }
    
    fun getCurrentValues(): Map<String, Any?> {
        val values = mutableMapOf<String, Any?>()
        for (name in watchedVariables.keys) {
            val value = VariableManager.get(name)
            values[name] = value?.let {
                when (it) {
                    is VariableValue.Integer -> it.value
                    is VariableValue.Number -> it.value
                    is VariableValue.Boolean -> it.value
                    is VariableValue.String -> it.value
                    else -> it.toString()
                }
            }
        }
        return values
    }
    
    fun getAllVariables(): Map<String, Any?> {
        val all = VariableManager.getAll()
        return all.mapValues { (_, value) ->
            when (value) {
                is VariableValue.Integer -> value.value
                is VariableValue.Number -> value.value
                is VariableValue.Boolean -> value.value
                is VariableValue.String -> value.value
                else -> value.toString()
            }
        }
    }
    
    fun clearHistory() {
        changeHistory.clear()
    }
    
    fun reset() {
        watchedVariables.clear()
        clearHistory()
    }
}

