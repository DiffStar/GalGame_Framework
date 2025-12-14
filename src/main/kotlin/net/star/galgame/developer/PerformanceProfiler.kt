package net.star.galgame.developer

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object PerformanceProfiler {
    private val isEnabled = AtomicBoolean(false)
    private val profiles = ConcurrentHashMap<String, ProfileData>()
    private val activeProfiles = ConcurrentHashMap<String, Long>()
    
    data class ProfileData(
        val name: String,
        var totalTime: Long = 0,
        var callCount: Long = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0,
        var lastTime: Long = 0
    ) {
        val averageTime: Double
            get() = if (callCount > 0) totalTime.toDouble() / callCount else 0.0
    }
    
    fun enable() {
        isEnabled.set(true)
        DevLogger.info("PerformanceProfiler", "性能分析器已启用")
    }
    
    fun disable() {
        isEnabled.set(false)
        DevLogger.info("PerformanceProfiler", "性能分析器已禁用")
    }
    
    fun isEnabled(): Boolean = isEnabled.get() && DevModeManager.isEnabled()
    
    fun start(name: String) {
        if (!isEnabled()) return
        activeProfiles[name] = System.nanoTime()
    }
    
    fun end(name: String) {
        if (!isEnabled()) return
        val startTime = activeProfiles.remove(name) ?: return
        val duration = System.nanoTime() - startTime
        
        val profile = profiles.getOrPut(name) { ProfileData(name) }
        profile.totalTime += duration
        profile.callCount++
        profile.lastTime = duration
        profile.minTime = minOf(profile.minTime, duration)
        profile.maxTime = maxOf(profile.maxTime, duration)
    }
    
    fun <T> profile(name: String, block: () -> T): T {
        start(name)
        return try {
            block()
        } finally {
            end(name)
        }
    }
    
    fun getProfile(name: String): ProfileData? = profiles[name]
    
    fun getAllProfiles(): Map<String, ProfileData> = profiles.toMap()
    
    fun getTopProfiles(limit: Int = 10): List<ProfileData> {
        return profiles.values
            .sortedByDescending { it.totalTime }
            .take(limit)
    }
    
    fun reset() {
        profiles.clear()
        activeProfiles.clear()
    }
    
    fun resetProfile(name: String) {
        profiles.remove(name)
    }
    
    fun exportReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 性能分析报告 ===")
        sb.appendLine("总记录数: ${profiles.size}")
        sb.appendLine()
        
        val sorted = profiles.values.sortedByDescending { it.totalTime }
        for (profile in sorted) {
            sb.appendLine("--- ${profile.name} ---")
            sb.appendLine("  调用次数: ${profile.callCount}")
            sb.appendLine("  总时间: ${profile.totalTime / 1_000_000.0} ms")
            sb.appendLine("  平均时间: ${profile.averageTime / 1_000_000.0} ms")
            sb.appendLine("  最小时间: ${profile.minTime / 1_000_000.0} ms")
            sb.appendLine("  最大时间: ${profile.maxTime / 1_000_000.0} ms")
            sb.appendLine("  上次时间: ${profile.lastTime / 1_000_000.0} ms")
            sb.appendLine()
        }
        
        return sb.toString()
    }
}

