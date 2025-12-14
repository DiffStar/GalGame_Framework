package net.star.galgame.api.developer

import java.nio.file.Path

interface IPlugin {
    val id: String
    val name: String
    val version: String
    fun onLoad()
    fun onEnable()
    fun onDisable()
}

interface IPluginManager {
    fun loadPlugin(pluginPath: Path): IPlugin?
    fun unloadPlugin(pluginId: String)
    fun getPlugin(pluginId: String): IPlugin?
    fun getAllPlugins(): Map<String, IPlugin>
    fun isPluginEnabled(pluginId: String): Boolean
    fun setPluginEnabled(pluginId: String, enabled: Boolean)
}

object PluginManager : IPluginManager {
    private val plugins = mutableMapOf<String, IPlugin>()
    private val enabledPlugins = mutableSetOf<String>()

    override fun loadPlugin(pluginPath: Path): IPlugin? {
        return try {
            val pluginClass = loadPluginClass(pluginPath)
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as IPlugin
            plugin.onLoad()
            plugins[plugin.id] = plugin
            if (enabledPlugins.contains(plugin.id)) {
                plugin.onEnable()
            }
            plugin
        } catch (e: Exception) {
            null
        }
    }

    override fun unloadPlugin(pluginId: String) {
        plugins[pluginId]?.onDisable()
        plugins.remove(pluginId)
        enabledPlugins.remove(pluginId)
    }

    override fun getPlugin(pluginId: String): IPlugin? {
        return plugins[pluginId]
    }

    override fun getAllPlugins(): Map<String, IPlugin> {
        return plugins.toMap()
    }

    override fun isPluginEnabled(pluginId: String): Boolean {
        return enabledPlugins.contains(pluginId)
    }

    override fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        val plugin = plugins[pluginId] ?: return
        if (enabled && !enabledPlugins.contains(pluginId)) {
            enabledPlugins.add(pluginId)
            plugin.onEnable()
        } else if (!enabled && enabledPlugins.contains(pluginId)) {
            enabledPlugins.remove(pluginId)
            plugin.onDisable()
        }
    }

    private fun loadPluginClass(pluginPath: Path): Class<*> {
        val classLoader = Thread.currentThread().contextClassLoader
        val className = extractClassName(pluginPath)
        return classLoader.loadClass(className)
    }

    private fun extractClassName(pluginPath: Path): String {
        return pluginPath.fileName.toString().removeSuffix(".class").replace("/", ".")
    }
}

