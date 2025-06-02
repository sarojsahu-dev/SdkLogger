package com.logger.sdklogger.utils

import com.logger.sdklogger.core.models.LogEntry
import com.logger.sdklogger.core.models.LogLevel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Analytics and metrics for logging system
 */
class LogAnalytics {
    private val logCounts = ConcurrentHashMap<LogLevel, AtomicLong>()
    private val sdkCounts = ConcurrentHashMap<String, AtomicLong>()
    private val tagCounts = ConcurrentHashMap<String, AtomicLong>()
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()

    fun recordLog(logEntry: LogEntry) {
        // Count by level
        logCounts.getOrPut(logEntry.level) { AtomicLong(0) }.incrementAndGet()

        // Count by SDK
        sdkCounts.getOrPut(logEntry.sdkName) { AtomicLong(0) }.incrementAndGet()

        // Count by tag
        tagCounts.getOrPut(logEntry.tag) { AtomicLong(0) }.incrementAndGet()

        // Count errors by exception type
        logEntry.throwable?.let { throwable ->
            errorCounts.getOrPut(throwable.javaClass.simpleName) { AtomicLong(0) }.incrementAndGet()
        }
    }

    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "logCountsByLevel" to logCounts.mapValues { it.value.get() },
            "logCountsBySDK" to sdkCounts.mapValues { it.value.get() },
            "logCountsByTag" to tagCounts.mapValues { it.value.get() },
            "errorCountsByType" to errorCounts.mapValues { it.value.get() },
            "totalLogs" to logCounts.values.sumOf { it.get() }
        )
    }

    fun reset() {
        logCounts.clear()
        sdkCounts.clear()
        tagCounts.clear()
        errorCounts.clear()
    }
}