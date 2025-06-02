package com.logger.sdklogger.formatters

import com.logger.sdklogger.core.interfaces.LogFormatter
import com.logger.sdklogger.core.models.LogEntry
import org.json.JSONObject

/**
 * JSON log formatter for structured logging
 */
class JsonLogFormatter : LogFormatter {

    override fun format(logEntry: LogEntry): String {
        val json = JSONObject().apply {
            put("id", logEntry.id)
            put("timestamp", logEntry.timestamp)
            put("level", logEntry.level.name)
            put("tag", logEntry.tag)
            put("message", logEntry.message)
            put("sdkName", logEntry.sdkName)
            put("sdkVersion", logEntry.sdkVersion)
            put("threadName", logEntry.threadName)

            logEntry.className?.let { put("className", it) }
            logEntry.methodName?.let { put("methodName", it) }
            logEntry.lineNumber?.let { put("lineNumber", it) }

            if (logEntry.metadata.isNotEmpty()) {
                put("metadata", JSONObject(logEntry.metadata))
            }

            logEntry.throwable?.let { throwable ->
                put("exception", JSONObject().apply {
                    put("type", throwable.javaClass.simpleName)
                    put("message", throwable.message)
                    put("stackTrace", throwable.stackTraceToString())
                })
            }
        }

        return json.toString()
    }
}