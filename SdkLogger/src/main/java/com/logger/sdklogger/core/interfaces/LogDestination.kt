package com.logger.sdklogger.core.interfaces

import com.logger.sdklogger.core.models.LogEntry

/**
 * Interface for different log destinations (Console, File, Network, etc.)
 */
interface LogDestination {
    /**
     * Write log entry to this destination
     */
    suspend fun writeLog(logEntry: LogEntry)

    /**
     * Flush any pending logs
     */
    suspend fun flush()

    /**
     * Close and cleanup resources
     */
    suspend fun close()

    /**
     * Get destination name/identifier
     */
    fun getName(): String
}