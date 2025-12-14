package net.star.galgame.api.ui

import java.util.concurrent.ConcurrentHashMap

interface IUIComponentRegistry {
    fun register(component: IUIComponent)
    fun unregister(id: String)
    fun get(id: String): IUIComponent?
    fun getAll(): Map<String, IUIComponent>
    fun getByType(type: Class<out IUIComponent>): List<IUIComponent>
}

object UIComponentRegistry : IUIComponentRegistry {
    private val components = ConcurrentHashMap<String, IUIComponent>()

    override fun register(component: IUIComponent) {
        components[component.id] = component
    }

    override fun unregister(id: String) {
        components.remove(id)
    }

    override fun get(id: String): IUIComponent? {
        return components[id]
    }

    override fun getAll(): Map<String, IUIComponent> {
        return components.toMap()
    }

    override fun getByType(type: Class<out IUIComponent>): List<IUIComponent> {
        return components.values.filter { type.isInstance(it) }
    }
}

