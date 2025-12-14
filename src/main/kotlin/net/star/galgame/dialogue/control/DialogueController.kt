package net.star.galgame.dialogue.control

import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.DialogueScript

class DialogueController(private val script: DialogueScript) {
    private var currentIndex = 0
    private var isFastForwarding = false
    private val history = mutableListOf<DialogueEntry>()

    fun getCurrentEntry(): DialogueEntry? {
        return if (currentIndex < script.entries.size) {
            script.entries[currentIndex]
        } else {
            null
        }
    }

    fun next(): Boolean {
        val current = getCurrentEntry()
        if (current != null) {
            history.add(current.copy(read = true))
        }
        
        currentIndex++
        return currentIndex < script.entries.size
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
}

