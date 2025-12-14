package net.star.galgame.developer

import net.star.galgame.GalGameFramework
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object DevModeManager {
    private val enabled = AtomicBoolean(false)
    private val devConfig = ConcurrentHashMap<String, Any>()
    
    fun isEnabled(): Boolean = enabled.get()
    
    fun enable() {
        enabled.set(true)
        GalGameFramework.LOGGER.info("开发模式已启用")
    }
    
    fun disable() {
        enabled.set(false)
        GalGameFramework.LOGGER.info("开发模式已禁用")
    }
    
    fun toggle(): Boolean {
        return if (enabled.get()) {
            disable()
            false
        } else {
            enable()
            true
        }
    }
    
    fun setConfig(key: String, value: Any) {
        devConfig[key] = value
    }
    
    fun getConfig(key: String): Any? = devConfig[key]
    
    fun getConfig(key: String, default: Any): Any = devConfig.getOrDefault(key, default)
    
    fun clearConfig() {
        devConfig.clear()
    }
}

