package com.logger.sdklogger.core.models

/**
 * Filter configuration for logs
 */
data class LogFilter(
    val minLevel: LogLevel = LogLevel.VERBOSE,
    val maxLevel: LogLevel = LogLevel.ASSERT,
    val includeTags: Set<String> = emptySet(),
    val excludeTags: Set<String> = emptySet(),
    val includeSDKs: Set<String> = emptySet(),
    val excludeSDKs: Set<String> = emptySet(),
    val timeRange: Pair<Long, Long>? = null
) {
    fun matches(logEntry: LogEntry): Boolean {
        // Check level range
        if (logEntry.level.priority < minLevel.priority ||
            logEntry.level.priority > maxLevel.priority) {
            return false
        }

        // Check tag filters
        if (includeTags.isNotEmpty() && !includeTags.contains(logEntry.tag)) {
            return false
        }
        if (excludeTags.contains(logEntry.tag)) {
            return false
        }

        // Check SDK filters
        if (includeSDKs.isNotEmpty() && !includeSDKs.contains(logEntry.sdkName)) {
            return false
        }
        if (excludeSDKs.contains(logEntry.sdkName)) {
            return false
        }

        // Check time range
        timeRange?.let { (start, end) ->
            if (logEntry.timestamp < start || logEntry.timestamp > end) {
                return false
            }
        }

        return true
    }
}
