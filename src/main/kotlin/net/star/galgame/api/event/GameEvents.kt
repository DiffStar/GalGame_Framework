package net.star.galgame.api.event

data class DialogueStartEvent(
    override val id: String,
    val scriptId: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

data class DialogueEndEvent(
    override val id: String,
    val scriptId: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

data class DialogueEntryEvent(
    override val id: String,
    val scriptId: String,
    val entryId: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

data class ChoiceSelectEvent(
    override val id: String,
    val scriptId: String,
    val choiceId: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

data class ContentPackLoadEvent(
    override val id: String,
    val packId: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

data class ContentPackUnloadEvent(
    override val id: String,
    val packId: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

data class VariableChangeEvent(
    override val id: String,
    val variableName: String,
    val oldValue: Any?,
    val newValue: Any?,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

