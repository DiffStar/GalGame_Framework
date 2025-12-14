package net.star.galgame.api.ui

interface IUIComponentFactory {
    fun <T : IUIComponent> create(type: Class<T>, id: String, vararg args: Any): T?
    fun registerFactory(type: Class<out IUIComponent>, factory: (String, Array<out Any>) -> IUIComponent)
    fun unregisterFactory(type: Class<out IUIComponent>)
}

object UIComponentFactory : IUIComponentFactory {
    private val factories = mutableMapOf<Class<*>, (String, Array<out Any>) -> IUIComponent>()

    override fun <T : IUIComponent> create(type: Class<T>, id: String, vararg args: Any): T? {
        val factory = factories[type] ?: return null
        return try {
            @Suppress("UNCHECKED_CAST")
            factory(id, args) as? T
        } catch (e: Exception) {
            null
        }
    }

    override fun registerFactory(type: Class<out IUIComponent>, factory: (String, Array<out Any>) -> IUIComponent) {
        factories[type] = factory
    }

    override fun unregisterFactory(type: Class<out IUIComponent>) {
        factories.remove(type)
    }
}

