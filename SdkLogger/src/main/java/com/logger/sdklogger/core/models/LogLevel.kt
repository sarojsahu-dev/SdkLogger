package com.logger.sdklogger.core.models

/**
 * Represents different log levels in order of severity
 */
enum class LogLevel(val priority: Int, val tag: String) {
    VERBOSE(2, "V"),
    DEBUG(3, "D"),
    INFO(4, "I"),
    WARNING(5, "W"),
    ERROR(6, "E"),
    ASSERT(7, "A");

    companion object {
        fun fromPriority(priority: Int): LogLevel? {
            return LogLevel.entries.find { it.priority == priority }
        }
    }
}