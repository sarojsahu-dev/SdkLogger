package com.logger.sdklogger.core.models

import java.util.Date
import java.util.UUID

/**
 * Represents a single log entry with all necessary metadata
 */
data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val sdkName: String,
    val sdkVersion: String,
    val threadName: String = Thread.currentThread().name,
    val className: String? = null,
    val methodName: String? = null,
    val lineNumber: Int? = null
) {
    fun toFormattedString(): String {
        val date = Date(timestamp)
        val throwableStr = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
        val metadataStr = if (metadata.isNotEmpty()) {
            "\nMetadata: ${metadata.entries.joinToString { "${it.key}=${it.value}" }}"
        } else ""

        return "$date [${level.tag}][$sdkName] $tag: $message$metadataStr$throwableStr"
    }
}