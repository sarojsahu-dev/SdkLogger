package com.logger.sdklogger.core.interfaces

import com.logger.sdklogger.core.models.LogEntry

/**
 * Interface for formatting log entries
 */
interface LogFormatter {
    fun format(logEntry: LogEntry): String
}