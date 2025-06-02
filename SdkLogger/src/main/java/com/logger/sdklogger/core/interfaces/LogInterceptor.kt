package com.logger.sdklogger.core.interfaces

import com.logger.sdklogger.core.models.LogEntry

/**
 * Interface for intercepting and modifying log entries before they're processed
 */
interface LogInterceptor {
    /**
     * Intercept and potentially modify log entry
     * @return modified log entry or null to skip logging
     */
    suspend fun intercept(logEntry: LogEntry): LogEntry?
}