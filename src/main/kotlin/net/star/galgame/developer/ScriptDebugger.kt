package net.star.galgame.developer

import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.dialogue.control.DialogueController
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ScriptDebugger(private val script: DialogueScript, private val controller: DialogueController) {
    private val breakpoints = ConcurrentHashMap<String, Boolean>()
    private val watchExpressions = ConcurrentLinkedQueue<String>()
    private val executionHistory = ConcurrentLinkedQueue<ExecutionStep>()
    private val isPaused = AtomicBoolean(false)
    private val isStepping = AtomicBoolean(false)
    private val currentStep = AtomicReference<ExecutionStep?>(null)
    private val stepCount = AtomicInteger(0)
    
    data class ExecutionStep(
        val entryId: String,
        val entry: DialogueEntry,
        val timestamp: Long,
        val variables: Map<String, Any>,
        val stepNumber: Int
    )
    
    fun setBreakpoint(entryId: String, enabled: Boolean = true) {
        breakpoints[entryId] = enabled
    }
    
    fun removeBreakpoint(entryId: String) {
        breakpoints.remove(entryId)
    }
    
    fun hasBreakpoint(entryId: String): Boolean {
        return breakpoints[entryId] == true
    }
    
    fun addWatchExpression(expression: String) {
        watchExpressions.offer(expression)
    }
    
    fun removeWatchExpression(expression: String) {
        watchExpressions.remove(expression)
    }
    
    fun getWatchExpressions(): List<String> = watchExpressions.toList()
    
    fun pause() {
        isPaused.set(true)
        DevLogger.info("ScriptDebugger", "脚本执行已暂停")
    }
    
    fun resume() {
        isPaused.set(false)
        isStepping.set(false)
        DevLogger.info("ScriptDebugger", "脚本执行已恢复")
    }
    
    fun step() {
        isStepping.set(true)
        isPaused.set(false)
    }
    
    fun isPaused(): Boolean = isPaused.get()
    
    fun isStepping(): Boolean = isStepping.get()
    
    fun checkBreakpoint(entry: DialogueEntry): Boolean {
        if (!DevModeManager.isEnabled()) return false
        
        val shouldBreak = breakpoints[entry.id] == true || isPaused.get()
        
        if (shouldBreak) {
            pause()
            recordStep(entry)
            DevLogger.debug("ScriptDebugger", "在断点处暂停: ${entry.id}")
        }
        
        return shouldBreak
    }
    
    fun recordStep(entry: DialogueEntry) {
        val step = ExecutionStep(
            entryId = entry.id,
            entry = entry,
            timestamp = System.currentTimeMillis(),
            variables = getCurrentVariables(),
            stepNumber = stepCount.incrementAndGet()
        )
        executionHistory.offer(step)
        currentStep.set(step)
        
        while (executionHistory.size > 100) {
            executionHistory.poll()
        }
    }
    
    fun getCurrentStep(): ExecutionStep? = currentStep.get()
    
    fun getExecutionHistory(limit: Int = 50): List<ExecutionStep> {
        val list = executionHistory.toList()
        return list.takeLast(limit)
    }
    
    fun clearHistory() {
        executionHistory.clear()
        stepCount.set(0)
        currentStep.set(null)
    }
    
    fun evaluateWatchExpressions(): Map<String, Any?> {
        val results = mutableMapOf<String, Any?>()
        for (expr in watchExpressions) {
            try {
                results[expr] = evaluateExpression(expr)
            } catch (e: Exception) {
                results[expr] = "错误: ${e.message}"
            }
        }
        return results
    }
    
    private fun evaluateExpression(expression: String): Any? {
        return when {
            expression.contains(".") -> {
                val parts = expression.split(".")
                when (parts[0]) {
                    "entry" -> {
                        val current = controller.getCurrentEntry()
                        when (parts[1]) {
                            "id" -> current?.id
                            "text" -> current?.text
                            "characterId" -> current?.characterId
                            else -> null
                        }
                    }
                    "variable" -> {
                        if (parts.size > 1) {
                            net.star.galgame.dialogue.variable.VariableManager.get(parts[1])?.let {
                                when (it) {
                                    is net.star.galgame.dialogue.variable.VariableValue.Integer -> it.value
                                    is net.star.galgame.dialogue.variable.VariableValue.Number -> it.value
                                    is net.star.galgame.dialogue.variable.VariableValue.Boolean -> it.value
                                    is net.star.galgame.dialogue.variable.VariableValue.String -> it.value
                                    else -> it.toString()
                                }
                            }
                        } else null
                    }
                    else -> null
                }
            }
            else -> {
                net.star.galgame.dialogue.variable.VariableManager.get(expression)?.let {
                    when (it) {
                        is net.star.galgame.dialogue.variable.VariableValue.Integer -> it.value
                        is net.star.galgame.dialogue.variable.VariableValue.Number -> it.value
                        is net.star.galgame.dialogue.variable.VariableValue.Boolean -> it.value
                        is net.star.galgame.dialogue.variable.VariableValue.String -> it.value
                        else -> it.toString()
                    }
                }
            }
        }
    }
    
    private fun getCurrentVariables(): Map<String, Any> {
        val vars = mutableMapOf<String, Any>()
        net.star.galgame.dialogue.variable.VariableManager.getAll().forEach { (key, value) ->
            vars[key] = when (value) {
                is net.star.galgame.dialogue.variable.VariableValue.Integer -> value.value
                is net.star.galgame.dialogue.variable.VariableValue.Number -> value.value
                is net.star.galgame.dialogue.variable.VariableValue.Boolean -> value.value
                is net.star.galgame.dialogue.variable.VariableValue.String -> value.value
                else -> value.toString()
            }
        }
        return vars
    }
    
    fun reset() {
        breakpoints.clear()
        watchExpressions.clear()
        clearHistory()
        isPaused.set(false)
        isStepping.set(false)
    }
}

