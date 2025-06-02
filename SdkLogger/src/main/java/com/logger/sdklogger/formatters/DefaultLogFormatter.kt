package com.logger.sdklogger.formatters

import com.logger.sdklogger.core.interfaces.LogFormatter
import com.logger.sdklogger.core.models.LogEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Default log formatter
 */
class DefaultLogFormatter : LogFormatter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun format(logEntry: LogEntry): String {
        val timestamp = dateFormat.format(Date(logEntry.timestamp))
        val level = logEntry.level.tag
        val sdk = logEntry.sdkName
        val tag = logEntry.tag
        val thread = logEntry.threadName
        val message = logEntry.message

        val baseLog = "$timestamp [$level][$sdk][$thread] $tag: $message"

        val metadata = if (logEntry.metadata.isNotEmpty()) {
            "\n  Metadata: ${logEntry.metadata.entries.joinToString { "${it.key}=${it.value}" }}"
        } else ""

        val location = if (logEntry.className != null) {
            "\n  Location: ${logEntry.className}.${logEntry.methodName}:${logEntry.lineNumber}"
        } else ""

        val throwable = logEntry.throwable?.let {
            "\n  Exception: ${it.javaClass.simpleName}: ${it.message}\n${it.stackTraceToString()}"
        } ?: ""

        return "$baseLog$metadata$location$throwable"
    }
}