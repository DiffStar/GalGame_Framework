package net.star.galgame.dialogue.text

class TypewriterEffect(
    var fullText: String = "",
    var speed: Float = 0.05f
) {
    private var currentIndex = 0
    private var accumulatedTime = 0f
    var isComplete = false
        private set

    fun update(deltaTime: Float) {
        if (isComplete) return
        
        accumulatedTime += deltaTime
        if (accumulatedTime >= speed) {
            accumulatedTime = 0f
            currentIndex++
            if (currentIndex >= fullText.length) {
                currentIndex = fullText.length
                isComplete = true
            }
        }
    }

    fun getCurrentText(): String {
        return fullText.substring(0, currentIndex)
    }

    fun skip() {
        currentIndex = fullText.length
        isComplete = true
    }

    fun reset() {
        currentIndex = 0
        accumulatedTime = 0f
        isComplete = false
    }
}

