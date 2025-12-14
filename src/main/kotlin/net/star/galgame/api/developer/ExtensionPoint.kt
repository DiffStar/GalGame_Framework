package net.star.galgame.api.developer

interface ExtensionPoint {
    val id: String
    val version: String
}

interface IExtensionRegistry {
    fun <T : ExtensionPoint> register(extension: T)
    fun <T : ExtensionPoint> unregister(id: String, type: Class<T>)
    fun <T : ExtensionPoint> get(id: String, type: Class<T>): T?
    fun <T : ExtensionPoint> getAll(type: Class<T>): List<T>
}

object ExtensionRegistry : IExtensionRegistry {
    private val extensions = mutableMapOf<Class<*>, MutableMap<String, ExtensionPoint>>()

    override fun <T : ExtensionPoint> register(extension: T) {
        val type = extension.javaClass
        extensions.getOrPut(type) { mutableMapOf() }[extension.id] = extension
    }

    override fun <T : ExtensionPoint> unregister(id: String, type: Class<T>) {
        extensions[type]?.remove(id)
    }

    override fun <T : ExtensionPoint> get(id: String, type: Class<T>): T? {
        return extensions[type]?.get(id) as? T
    }

    override fun <T : ExtensionPoint> getAll(type: Class<T>): List<T> {
        return extensions[type]?.values?.filter { type.isInstance(it) }?.map { type.cast(it) } ?: emptyList()
    }
}

