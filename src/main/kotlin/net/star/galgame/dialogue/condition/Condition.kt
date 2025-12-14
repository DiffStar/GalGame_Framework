package net.star.galgame.dialogue.condition

import net.star.galgame.dialogue.variable.VariableManager
import net.star.galgame.dialogue.variable.VariableValue

sealed class Condition {
    abstract fun evaluate(): Boolean

    data class Compare(
        val variable: String,
        val operator: CompareOperator,
        val value: VariableValue
    ) : Condition() {
        override fun evaluate(): Boolean {
            val varValue = VariableManager.get(variable) ?: return false
            return when (operator) {
                CompareOperator.EQUALS -> varValue == value
                CompareOperator.NOT_EQUALS -> varValue != value
                CompareOperator.GREATER_THAN -> compareNumbers(varValue, value) { a, b -> a > b }
                CompareOperator.LESS_THAN -> compareNumbers(varValue, value) { a, b -> a < b }
                CompareOperator.GREATER_EQUAL -> compareNumbers(varValue, value) { a, b -> a >= b }
                CompareOperator.LESS_EQUAL -> compareNumbers(varValue, value) { a, b -> a <= b }
            }
        }

        private fun compareNumbers(a: VariableValue, b: VariableValue, op: (Double, Double) -> Boolean): Boolean {
            val numA = when (a) {
                is VariableValue.Integer -> a.value.toDouble()
                is VariableValue.Long -> a.value.toDouble()
                is VariableValue.Float -> a.value.toDouble()
                is VariableValue.Number -> a.value
                is VariableValue.String -> a.value.toDoubleOrNull()
                is VariableValue.Boolean -> if (a.value) 1.0 else 0.0
            } ?: return false

            val numB = when (b) {
                is VariableValue.Integer -> b.value.toDouble()
                is VariableValue.Long -> b.value.toDouble()
                is VariableValue.Float -> b.value.toDouble()
                is VariableValue.Number -> b.value
                is VariableValue.String -> b.value.toDoubleOrNull()
                is VariableValue.Boolean -> if (b.value) 1.0 else 0.0
            } ?: return false

            return op(numA, numB)
        }
    }

    data class And(val conditions: List<Condition>) : Condition() {
        override fun evaluate(): Boolean {
            return conditions.all { it.evaluate() }
        }
    }

    data class Or(val conditions: List<Condition>) : Condition() {
        override fun evaluate(): Boolean {
            return conditions.any { it.evaluate() }
        }
    }

    data class Not(val condition: Condition) : Condition() {
        override fun evaluate(): Boolean {
            return !condition.evaluate()
        }
    }

    data class HasVariable(val variable: String) : Condition() {
        override fun evaluate(): Boolean {
            return VariableManager.has(variable)
        }
    }
}

enum class CompareOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_EQUAL,
    LESS_EQUAL
}

