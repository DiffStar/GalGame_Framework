package net.star.galgame.developer

import net.star.galgame.GalGameFramework
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

object DevLogger {
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val maxLogs = AtomicInteger(1000)
    private val logLevel = AtomicInteger(LogLevel.INFO.ordinal)
    
    enum class LogLevel(val displayName: String) {
        DEBUG("DEBUG"),
        INFO("INFO"),
        WARN("WARN"),
        ERROR("ERROR")
    }
    
    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val category: String,
        val message: String,
        val stackTrace: String? = null
    )
    
    fun setMaxLogs(max: Int) {
        maxLogs.set(max)
        trimLogs()
    }
    
    fun setLogLevel(level: LogLevel) {
        logLevel.set(level.ordinal)
    }
    
    fun debug(category: String, message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            addLog(LogLevel.DEBUG, category, message)
        }
    }
    
    fun info(category: String, message: String) {
        if (shouldLog(LogLevel.INFO)) {
            addLog(LogLevel.INFO, category, message)
        }
    }
    
    fun warn(category: String, message: String) {
        if (shouldLog(LogLevel.WARN)) {
            addLog(LogLevel.WARN, category, message)
        }
    }
    
    fun error(category: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.ERROR)) {
            val stackTrace = throwable?.let {
                it.stackTraceToString()
            }
            addLog(LogLevel.ERROR, category, message, stackTrace)
            GalGameFramework.LOGGER.error("[$category] $message", throwable)
        }
    }
    
    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= logLevel.get()
    }
    
    private fun addLog(level: LogLevel, category: String, message: String, stackTrace: String? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        logs.offer(LogEntry(timestamp, level, category, message, stackTrace))
        trimLogs()
    }
    
    private fun trimLogs() {
        while (logs.size > maxLogs.get()) {
            logs.poll()
        }
    }
    
    fun getLogs(level: LogLevel? = null, category: String? = null, limit: Int = 100): List<LogEntry> {
        val filtered = logs.asSequence()
            .filter { level == null || it.level == level }
            .filter { category == null || it.category == category }
            .toList()
        return filtered.takeLast(limit)
    }
    
    fun clearLogs() {
        logs.clear()
    }
    
    fun exportLogs(): String {
        return logs.joinToString("\n") { log ->
            "[${log.timestamp}] [${log.level.displayName}] [${log.category}] ${log.message}" +
                    if (log.stackTrace != null) "\n${log.stackTrace}" else ""
        }
    }
}

