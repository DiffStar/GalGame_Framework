package net.star.galgame.api.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

interface IEvent {
    val id: String
    val timestamp: Long
    val cancelled: Boolean
}

interface IEventBus {
    fun <T : IEvent> subscribe(eventType: Class<T>, handler: (T) -> Unit)
    fun <T : IEvent> unsubscribe(eventType: Class<T>, handler: (T) -> Unit)
    fun <T : IEvent> post(event: T): T
    fun <T : IEvent> postAsync(event: T)
}

object EventBus : IEventBus {
    private val handlers = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<(IEvent) -> Unit>>()

    override fun <T : IEvent> subscribe(eventType: Class<T>, handler: (T) -> Unit) {
        handlers.getOrPut(eventType) { CopyOnWriteArrayList() }.add(handler as (IEvent) -> Unit)
    }

    override fun <T : IEvent> unsubscribe(eventType: Class<T>, handler: (T) -> Unit) {
        handlers[eventType]?.remove(handler as (IEvent) -> Unit)
    }

    override fun <T : IEvent> post(event: T): T {
        val eventHandlers = handlers[event.javaClass] ?: return event
        eventHandlers.forEach { handler ->
            if (!event.cancelled) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    handler(event as IEvent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return event
    }

    override fun <T : IEvent> postAsync(event: T) {
        Thread {
            post(event)
        }.start()
    }
}

