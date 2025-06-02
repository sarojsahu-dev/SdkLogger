package com.logger.sdklogger.core

import com.logger.sdklogger.core.config.SDKLoggerConfig
import com.logger.sdklogger.core.models.LogEntry
import com.logger.sdklogger.core.models.LogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Main SDKLogger class - Thread-safe, high-performance logging system
 */
class SDKLogger private constructor(
    private val sdkName: String,
    private val sdkVersion: String,
    private var config: SDKLoggerConfig
) {
    companion object {
        private val instances = ConcurrentHashMap<String, SDKLogger>()

        /**
         * Get or create logger instance for specific SDK
         */
        fun getInstance(
            sdkName: String,
            sdkVersion: String = "1.0.0",
            config: SDKLoggerConfig = SDKLoggerConfig()
        ): SDKLogger {
            return instances.getOrPut("$sdkName:$sdkVersion") {
                SDKLogger(sdkName, sdkVersion, config)
            }
        }

        /**
         * Get all active logger instances
         */
        fun getAllInstances(): Map<String, SDKLogger> {
            return instances.toMap()
        }

        /**
         * Shutdown all loggers
         */
        suspend fun shutdownAll() {
            instances.values.forEach { it.shutdown() }
            instances.clear()
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logChannel = Channel<LogEntry>(capacity = config.bufferSize)
    private val mutex = Mutex()
    private var isShutdown = false

    init {
        startLogProcessor()
        if (config.flushInterval > 0) {
            startPeriodicFlush()
        }
    }

    private fun startLogProcessor() {
        scope.launch {
            for (logEntry in logChannel) {
                processLogEntry(logEntry)
            }
        }
    }

    private fun startPeriodicFlush() {
        scope.launch {
            while (!isShutdown) {
                delay(config.flushInterval)
                flush()
            }
        }
    }

    private suspend fun processLogEntry(logEntry: LogEntry) {
        if (!config.isEnabled || logEntry.level.priority < config.minLogLevel.priority) {
            return
        }

        try {
            // Apply interceptors
            var processedEntry = logEntry
            for (interceptor in config.interceptors) {
                processedEntry = interceptor.intercept(processedEntry) ?: return
            }

            // Write to all destinations
            config.destinations.forEach { destination ->
                try {
                    destination.writeLog(processedEntry)
                } catch (e: Exception) {
                    // Log destination error (but avoid infinite loop)
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Log with specified level
     */
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        if (isShutdown || !config.isEnabled) return

        val stackTrace = if (config.enableStackTrace) {
            Thread.currentThread().stackTrace.getOrNull(3)
        } else null

        val logEntry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            metadata = if (config.enableMetadataCollection) metadata else emptyMap(),
            sdkName = sdkName,
            sdkVersion = sdkVersion,
            className = stackTrace?.className,
            methodName = stackTrace?.methodName,
            lineNumber = stackTrace?.lineNumber
        )

        if (config.enableAsync) {
            // Try to send to channel, drop if full
            logChannel.trySend(logEntry)
        } else {
            // Process synchronously
            scope.launch {
                processLogEntry(logEntry)
            }
        }
    }

    // Convenience methods
    fun verbose(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.VERBOSE, tag, message, throwable, metadata)
    }

    fun debug(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.DEBUG, tag, message, throwable, metadata)
    }

    fun info(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, tag, message, throwable, metadata)
    }

    fun warning(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.WARNING, tag, message, throwable, metadata)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.ERROR, tag, message, throwable, metadata)
    }

    fun assert(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.ASSERT, tag, message, throwable, metadata)
    }

    /**
     * Update logger configuration
     */
    suspend fun updateConfig(newConfig: SDKLoggerConfig) = mutex.withLock {
        val oldDestinations = config.destinations
        config = newConfig

        // Close old destinations that are not in new config
        oldDestinations.forEach { oldDest ->
            if (!newConfig.destinations.contains(oldDest)) {
                oldDest.close()
            }
        }
    }

    /**
     * Flush all destinations
     */
    suspend fun flush() {
        config.destinations.forEach { destination ->
            try {
                destination.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Shutdown logger and cleanup resources
     */
    suspend fun shutdown() = mutex.withLock {
        if (isShutdown) return@withLock

        isShutdown = true
        logChannel.close()

        // Wait for remaining logs to process
        scope.coroutineContext[Job]?.children?.forEach { it.join() }

        // Close all destinations
        config.destinations.forEach { destination ->
            try {
                destination.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        scope.cancel()
    }

    /**
     * Get logger information
     */
    fun getInfo(): Map<String, Any> {
        return mapOf(
            "sdkName" to sdkName,
            "sdkVersion" to sdkVersion,
            "isEnabled" to config.isEnabled,
            "minLogLevel" to config.minLogLevel.name,
            "destinations" to config.destinations.map { it.getName() },
            "enableAsync" to config.enableAsync,
            "bufferSize" to config.bufferSize,
            "isShutdown" to isShutdown
        )
    }
}