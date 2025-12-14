package net.star.galgame.dialogue.control

import net.star.galgame.dialogue.ChoiceEntry
import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.DialogueScript

class DialogueController(private val script: DialogueScript) {
    private var currentIndex = 0
    private var isFastForwarding = false
    private val history = mutableListOf<DialogueEntry>()
    private val labelMap = mutableMapOf<String, Int>()

    init {
        script.entries.forEachIndexed { index, entry ->
            if (entry.label != null) {
                labelMap[entry.label] = index
            }
        }
    }

    fun getCurrentEntry(): DialogueEntry? {
        var entry = getEntryAt(currentIndex)
        while (entry != null && !shouldShowEntry(entry)) {
            currentIndex++
            if (entry.jumpTo != null) {
                jumpToLabel(entry.jumpTo)
                entry = getEntryAt(currentIndex)
            } else {
                entry = getEntryAt(currentIndex)
            }
        }
        return entry
    }

    private fun getEntryAt(index: Int): DialogueEntry? {
        return if (index < script.entries.size) {
            script.entries[index]
        } else {
            null
        }
    }

    private fun shouldShowEntry(entry: DialogueEntry): Boolean {
        if (entry.condition != null) {
            return entry.condition.evaluate()
        }
        return true
    }

    fun jumpToLabel(label: String): Boolean {
        val targetIndex = labelMap[label]
        if (targetIndex != null) {
            currentIndex = targetIndex
            return true
        }
        return false
    }

    fun next(): Boolean {
        val current = getCurrentEntry()
        if (current != null) {
            history.add(current.copy(read = true))
        }
        
        if (current?.jumpTo != null) {
            return jumpToLabel(current.jumpTo)
        }
        
        currentIndex++
        return currentIndex < script.entries.size
    }

    fun selectChoice(choiceIndex: Int): Boolean {
        val current = getCurrentEntry() ?: return false
        val visibleChoices = current.choices.filter { it.visible && (it.condition == null || it.condition.evaluate()) }
        
        if (choiceIndex < 0 || choiceIndex >= visibleChoices.size) {
            return false
        }
        
        val selectedChoice = visibleChoices[choiceIndex]
        history.add(current.copy(read = true))
        
        return jumpToLabel(selectedChoice.jumpTo)
    }

    fun getVisibleChoices(): List<ChoiceEntry> {
        val current = getCurrentEntry() ?: return emptyList()
        return current.choices.filter { 
            it.visible && (it.condition == null || it.condition.evaluate())
        }
    }

    fun hasChoices(): Boolean {
        return getVisibleChoices().isNotEmpty()
    }

    fun previous(): Boolean {
        if (currentIndex > 0) {
            currentIndex--
            return true
        }
        return false
    }

    fun fastForward() {
        isFastForwarding = true
    }

    fun stopFastForward() {
        isFastForwarding = false
    }

    fun isFastForwarding(): Boolean = isFastForwarding

    fun skip() {
        while (currentIndex < script.entries.size) {
            val entry = script.entries[currentIndex]
            if (!entry.read) {
                break
            }
            currentIndex++
        }
    }

    fun getHistory(): List<DialogueEntry> = history.toList()

    fun reset() {
        currentIndex = 0
        isFastForwarding = false
        history.clear()
    }

    fun isComplete(): Boolean = currentIndex >= script.entries.size
    
    fun getCurrentIndex(): Int = currentIndex
    
    fun setCurrentIndex(index: Int) {
        currentIndex = index.coerceIn(0, script.entries.size)
    }
}

