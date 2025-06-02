package com.logger.sdklogger.destinations

import android.util.Log
import com.logger.sdklogger.core.interfaces.LogDestination
import com.logger.sdklogger.core.models.LogEntry
import com.logger.sdklogger.core.models.LogLevel

/**
 * Console/Logcat destination for Android
 */
class ConsoleDestination : LogDestination {

    override suspend fun writeLog(logEntry: LogEntry) {
        val tag = "[${logEntry.sdkName}] ${logEntry.tag}"
        val message = logEntry.message

        when (logEntry.level) {
            LogLevel.VERBOSE -> Log.v(tag, message, logEntry.throwable)
            LogLevel.DEBUG -> Log.d(tag, message, logEntry.throwable)
            LogLevel.INFO -> Log.i(tag, message, logEntry.throwable)
            LogLevel.WARNING -> Log.w(tag, message, logEntry.throwable)
            LogLevel.ERROR -> Log.e(tag, message, logEntry.throwable)
            LogLevel.ASSERT -> Log.wtf(tag, message, logEntry.throwable)
        }
    }

    override suspend fun flush() {
        // No need to flush console
    }

    override suspend fun close() {
        // No resources to close
    }

    override fun getName(): String = "Console"
}