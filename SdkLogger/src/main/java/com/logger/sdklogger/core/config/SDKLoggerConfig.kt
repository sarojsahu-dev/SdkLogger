package com.logger.sdklogger.core.config

import com.logger.sdklogger.core.interfaces.LogDestination
import com.logger.sdklogger.core.interfaces.LogFormatter
import com.logger.sdklogger.core.interfaces.LogInterceptor
import com.logger.sdklogger.core.models.LogLevel
import com.logger.sdklogger.destinations.ConsoleDestination
import com.logger.sdklogger.formatters.DefaultLogFormatter

/**
 * Configuration class for SDKLogger
 */
data class SDKLoggerConfig(
    val isEnabled: Boolean = true,
    val minLogLevel: LogLevel = LogLevel.VERBOSE,
    val destinations: MutableList<LogDestination> = mutableListOf(ConsoleDestination()),
    val formatter: LogFormatter = DefaultLogFormatter(),
    val interceptors: MutableList<LogInterceptor> = mutableListOf(),
    val enableMetadataCollection: Boolean = true,
    val enableStackTrace: Boolean = true,
    val bufferSize: Int = 100,
    val flushInterval: Long = 5000L, // 5 seconds
    val enableAsync: Boolean = true,
    val enableCrashReporting: Boolean = false,
    val maxLogFileSize: Long = 10 * 1024 * 1024L, // 10MB
    val maxLogFiles: Int = 5
) {
    class Builder {
        private var isEnabled = true
        private var minLogLevel = LogLevel.VERBOSE
        private val destinations = mutableListOf<LogDestination>(ConsoleDestination())
        private var formatter: LogFormatter = DefaultLogFormatter()
        private val interceptors = mutableListOf<LogInterceptor>()
        private var enableMetadataCollection = true
        private var enableStackTrace = true
        private var bufferSize = 100
        private var flushInterval = 5000L
        private var enableAsync = true
        private var enableCrashReporting = false
        private var maxLogFileSize = 10 * 1024 * 1024L
        private var maxLogFiles = 5

        fun setEnabled(enabled: Boolean) = apply { this.isEnabled = enabled }
        fun setMinLogLevel(level: LogLevel) = apply { this.minLogLevel = level }
        fun addDestination(destination: LogDestination) =
            apply { this.destinations.add(destination) }

        fun setFormatter(formatter: LogFormatter) = apply { this.formatter = formatter }
        fun addInterceptor(interceptor: LogInterceptor) =
            apply { this.interceptors.add(interceptor) }

        fun setMetadataCollection(enabled: Boolean) =
            apply { this.enableMetadataCollection = enabled }

        fun setStackTrace(enabled: Boolean) = apply { this.enableStackTrace = enabled }
        fun setBufferSize(size: Int) = apply { this.bufferSize = size }
        fun setFlushInterval(interval: Long) = apply { this.flushInterval = interval }
        fun setAsync(enabled: Boolean) = apply { this.enableAsync = enabled }
        fun setCrashReporting(enabled: Boolean) = apply { this.enableCrashReporting = enabled }
        fun setMaxLogFileSize(size: Long) = apply { this.maxLogFileSize = size }
        fun setMaxLogFiles(count: Int) = apply { this.maxLogFiles = count }

        fun build() = SDKLoggerConfig(
            isEnabled, minLogLevel, destinations, formatter, interceptors,
            enableMetadataCollection, enableStackTrace, bufferSize, flushInterval,
            enableAsync, enableCrashReporting, maxLogFileSize, maxLogFiles
        )
    }
}
