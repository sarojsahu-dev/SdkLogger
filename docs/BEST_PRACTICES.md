# SDKLogger - Best Practices

This guide covers production-ready patterns, performance optimization, and enterprise deployment strategies for SDKLogger.

## 📚 Table of Contents

- [🏭 Production Deployment](#-production-deployment)
- [⚡ Performance Optimization](#-performance-optimization)
- [🔒 Security Guidelines](#-security-guidelines)
- [🚨 Error Handling Patterns](#-error-handling-patterns)
- [📊 Monitoring & Alerting](#-monitoring--alerting)
- [🧠 Memory Management](#-memory-management)
- [🏗️ SDK Integration Patterns](#️-sdk-integration-patterns)
- [🧪 Testing Strategies](#-testing-strategies)
- [📋 Compliance & Privacy](#-compliance--privacy)
- [🎯 Continuous Improvement](#-continuous-improvement)

---

## 🏭 Production Deployment

### Environment-Based Configuration

Create different logger configurations for different build environments to ensure optimal performance and security.

```kotlin
object LoggerFactory {
    fun createLogger(context: Context, sdkName: String, version: String): SDKLogger {
        return when (BuildConfig.BUILD_TYPE) {
            "debug" -> createDebugLogger(context, sdkName, version)
            "staging" -> createStagingLogger(context, sdkName, version)
            "release" -> createProductionLogger(context, sdkName, version)
            else -> createDefaultLogger(sdkName, version)
        }
    }
    
    private fun createDebugLogger(context: Context, sdkName: String, version: String): SDKLogger {
        val config = SDKLoggerConfig.Builder()
            .setEnabled(true)
            .setMinLogLevel(LogLevel.VERBOSE)
            .addDestination(ConsoleDestination())
            .addDestination(FileDestination(
                logDirectory = File(context.getExternalFilesDir(null), "debug_logs"),
                baseFileName = sdkName.lowercase(),
                formatter = JsonLogFormatter()
            ))
            .setAsync(false) // Synchronous for easier debugging
            .setMetadataCollection(true)
            .setStackTrace(true)
            .build()
            
        return SDKLogger.getInstance(sdkName, version, config)
    }
    
    private fun createStagingLogger(context: Context, sdkName: String, version: String): SDKLogger {
        val config = SDKLoggerConfig.Builder()
            .setEnabled(true)
            .setMinLogLevel(LogLevel.INFO)
            .addDestination(ConsoleDestination())
            .addDestination(FileDestination(
                logDirectory = File(context.getExternalFilesDir(null), "staging_logs"),
                baseFileName = sdkName.lowercase(),
                maxFileSize = 5 * 1024 * 1024L, // 5MB
                maxFiles = 3
            ))
            .setAsync(true)
            .setBufferSize(100)
            .setFlushInterval(10000L)
            .build()
            
        return SDKLogger.getInstance(sdkName, version, config)
    }
    
    private fun createProductionLogger(context: Context, sdkName: String, version: String): SDKLogger {
        val config = SDKLoggerConfig.Builder()
            .setEnabled(isLoggingEnabledForUser()) // Selective enablement
            .setMinLogLevel(LogLevel.WARNING) // Only warnings and errors
            .addDestination(ConsoleDestination())
            .apply {
                // File logging only for specific users or error scenarios
                if (shouldEnableFileLogging()) {
                    addDestination(createProductionFileDestination(context, sdkName))
                }
                
                // Network logging for critical errors
                if (shouldEnableNetworkLogging()) {
                    addDestination(createNetworkDestination())
                }
            }
            .addInterceptor(SensitiveDataInterceptor())
            .addInterceptor(ProductionFilterInterceptor())
            .setAsync(true)
            .setBufferSize(50) // Smaller buffer for production
            .setFlushInterval(30000L) // 30 seconds
            .setMaxLogFileSize(3 * 1024 * 1024L) // 3MB
            .setMaxLogFiles(2) // Keep only 2 files
            .build()
            
        return SDKLogger.getInstance(sdkName, version, config)
    }
    
    private fun isLoggingEnabledForUser(): Boolean {
        // Enable logging for beta users or internal testing
        return BuildConfig.DEBUG || isInternalUser() || isBetaUser()
    }
    
    private fun shouldEnableFileLogging(): Boolean {
        // Enable file logging for specific scenarios
        return BuildConfig.DEBUG || hasUserOptedIntoLogging()
    }
    
    private fun shouldEnableNetworkLogging(): Boolean {
        // Enable network logging for crash reporting
        return isCrashReportingEnabled()
    }
}
```

### Feature Flags Integration

Use feature flags to control logging behavior without app updates:

```kotlin
class FeatureFlaggedLogger {
    private val logger: SDKLogger
    private val featureFlags: FeatureFlags
    
    init {
        val config = SDKLoggerConfig.Builder()
            .setEnabled(featureFlags.isEnabled("sdk_logging"))
            .setMinLogLevel(
                if (featureFlags.isEnabled("verbose_logging")) LogLevel.DEBUG 
                else LogLevel.INFO
            )
            .addDestination(ConsoleDestination())
            .apply {
                if (featureFlags.isEnabled("file_logging")) {
                    addDestination(createFileDestination())
                }
                if (featureFlags.isEnabled("analytics_logging")) {
                    addDestination(createAnalyticsDestination())
                }
            }
            .build()
            
        logger = SDKLogger.getInstance("FeatureFlaggedSDK", "1.0.0", config)
    }
    
    fun updateFromFeatureFlags() {
        // Update configuration when feature flags change
        val newConfig = buildConfigFromFeatureFlags()
        GlobalScope.launch {
            logger.updateConfig(newConfig)
        }
    }
    
    private fun buildConfigFromFeatureFlags(): SDKLoggerConfig {
        return SDKLoggerConfig.Builder()
            .setEnabled(featureFlags.isEnabled("sdk_logging"))
            .setMinLogLevel(
                if (featureFlags.isEnabled("verbose_logging")) LogLevel.DEBUG 
                else LogLevel.INFO
            )
            .apply {
                if (featureFlags.isEnabled("file_logging")) {
                    addDestination(createFileDestination())
                }
            }
            .build()
    }
}
```

### Gradual Rollout Strategy

Implement gradual rollout to minimize risk when deploying new logging features:

```kotlin
class GradualRolloutLogger {
    companion object {
        fun createWithRollout(
            context: Context, 
            userId: String, 
            sdkName: String
        ): SDKLogger {
            val rolloutPercentage = getRolloutPercentage(sdkName)
            val userHash = userId.hashCode().absoluteValue % 100
            val isInRollout = userHash < rolloutPercentage
            
            val config = SDKLoggerConfig.Builder()
                .setEnabled(isInRollout || BuildConfig.DEBUG)
                .setMinLogLevel(if (isInRollout) LogLevel.INFO else LogLevel.ERROR)
                .addDestination(ConsoleDestination())
                .apply {
                    if (isInRollout) {
                        addDestination(createRolloutFileDestination(context, sdkName))
                    }
                }
                .build()
                
            return SDKLogger.getInstance(sdkName, "1.0.0", config)
        }
        
        private fun getRolloutPercentage(sdkName: String): Int {
            // Get rollout percentage from remote config or feature flags
            return when (sdkName) {
                "PaymentSDK" -> 10  // 10% rollout
                "AuthSDK" -> 25     // 25% rollout
                "AnalyticsSDK" -> 50 // 50% rollout
                else -> 5           // 5% default rollout
            }
        }
        
        private fun createRolloutFileDestination(context: Context, sdkName: String): FileDestination {
            return FileDestination(
                logDirectory = File(context.getExternalFilesDir(null), "rollout_logs"),
                baseFileName = "${sdkName.lowercase()}_rollout",
                maxFileSize = 2 * 1024 * 1024L, // 2MB for rollout
                maxFiles = 2
            )
        }
    }
}
```

---

## ⚡ Performance Optimization

### High-Volume Logging

For applications that generate large volumes of logs, implement batching and optimize buffer settings:

```kotlin
class HighVolumeLogger {
    private val logger: SDKLogger
    
    init {
        val config = SDKLoggerConfig.Builder()
            .setAsync(true)
            .setBufferSize(1000) // Large buffer
            .setFlushInterval(60000L) // Flush every minute
            .setMinLogLevel(LogLevel.INFO) // Skip debug/verbose
            .addDestination(ConsoleDestination())
            .addDestination(FileDestination(
                logDirectory = getHighVolumeLogDirectory(),
                maxFileSize = 50 * 1024 * 1024L, // 50MB files
                maxFiles = 10,
                formatter = JsonLogFormatter()
            ))
            .build()
            
        logger = SDKLogger.getInstance("HighVolumeSDK", "1.0.0", config)
    }
    
    // Batch similar events
    private val eventBatch = mutableListOf<Event>()
    private val batchLock = Mutex()
    
    suspend fun logEventBatch(event: Event) = batchLock.withLock {
        eventBatch.add(event)
        
        if (eventBatch.size >= 50) {
            flushEventBatch()
        }
    }
    
    private suspend fun flushEventBatch() {
        if (eventBatch.isEmpty()) return
        
        val batch = eventBatch.toList()
        eventBatch.clear()
        
        logger.info("Events", "Batch processed",
            metadata = mapOf(
                "eventCount" to batch.size,
                "eventTypes" to batch.groupingBy { it.type }.eachCount(),
                "timespan" to (batch.last().timestamp - batch.first().timestamp)
            ))
    }
    
    private fun getHighVolumeLogDirectory(): File {
        return File(context.getExternalFilesDir(null), "high_volume_logs")
    }
}

data class Event(
    val type: String,
    val timestamp: Long,
    val data: Map<String, Any>
)
```

### Memory-Efficient Logging

Implement safeguards to prevent memory issues from large metadata or log messages:

```kotlin
class MemoryEfficientLogger {
    private val logger = SDKLogger.getInstance("MemorySDK", "1.0.0")
    
    // Limit metadata size to prevent memory issues
    fun logWithSafeMetadata(tag: String, message: String, metadata: Map<String, Any>) {
        val safeMetadata = metadata.entries
            .take(MAX_METADATA_ENTRIES) // Limit number of entries
            .associate { (key, value) ->
                key to limitValueSize(value)
            }
        
        logger.info(tag, message, metadata = safeMetadata)
    }
    
    private fun limitValueSize(value: Any): Any {
        return when (value) {
            is String -> if (value.length > MAX_STRING_LENGTH) {
                value.take(MAX_STRING_LENGTH) + "..."
            } else value
            
            is Collection<*> -> if (value.size > MAX_COLLECTION_SIZE) {
                value.take(MAX_COLLECTION_SIZE).toList() + listOf("... ${value.size - MAX_COLLECTION_SIZE} more")
            } else value
            
            is ByteArray -> if (value.size > MAX_BYTE_ARRAY_SIZE) {
                mapOf(
                    "size" to value.size,
                    "hash" to value.contentHashCode(),
                    "preview" to value.take(10).toString()
                )
            } else value
            
            else -> value
        }
    }
    
    companion object {
        private const val MAX_METADATA_ENTRIES = 20
        private const val MAX_STRING_LENGTH = 500
        private const val MAX_COLLECTION_SIZE = 10
        private const val MAX_BYTE_ARRAY_SIZE = 1024
    }
}
```

### CPU-Optimized Logging

Optimize CPU usage with lazy evaluation and object pooling:

```kotlin
class CPUOptimizedLogger {
    private val logger = SDKLogger.getInstance("CPUOptimizedSDK", "1.0.0")
    
    // Use lazy evaluation for expensive operations
    fun logWithLazyEvaluation(tag: String, expensiveOperation: () -> String) {
        if (logger.isLoggable(LogLevel.DEBUG)) {
            logger.debug(tag, expensiveOperation())
        }
    }
    
    // Pre-compute common metadata
    private val staticMetadata = mapOf(
        "appVersion" to BuildConfig.VERSION_NAME,
        "buildType" to BuildConfig.BUILD_TYPE,
        "deviceModel" to Build.MODEL,
        "osVersion" to Build.VERSION.RELEASE
    )
    
    fun logWithStaticMetadata(tag: String, message: String, dynamicMetadata: Map<String, Any> = emptyMap()) {
        logger.info(tag, message, metadata = staticMetadata + dynamicMetadata)
    }
    
    // Use object pooling for frequently created objects
    private val metadataPool = object : LinkedHashMap<String, Any>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any>?): Boolean {
            return size > 10 // Keep pool size manageable
        }
    }
    
    fun logWithPooledMetadata(tag: String, message: String, key: String, value: Any) {
        val metadata = metadataPool.apply {
            clear()
            put(key, value)
            putAll(staticMetadata)
        }
        
        logger.info(tag, message, metadata = metadata.toMap())
    }
}
```

---

## 🔒 Security Guidelines

### Sensitive Data Protection

Implement comprehensive data sanitization to protect user privacy:

```kotlin
class SecureLogger {
    private val logger: SDKLogger
    
    init {
        val config = SDKLoggerConfig.Builder()
            .addDestination(ConsoleDestination())
            .addDestination(FileDestination(
                logDirectory = getSecureLogDirectory(),
                formatter = JsonLogFormatter()
            ))
            .addInterceptor(SecurityInterceptor())
            .addInterceptor(PIIRedactionInterceptor())
            .build()
            
        logger = SDKLogger.getInstance("SecureSDK", "1.0.0", config)
    }
    
    private fun getSecureLogDirectory(): File {
        // Use internal storage for better security
        return File(context.filesDir, "secure_logs")
    }
}

class SecurityInterceptor : LogInterceptor {
    private val sensitiveKeys = setOf(
        "password", "token", "secret", "key", "auth", "credential",
        "ssn", "social", "credit", "card", "pin", "cvv", "api_key"
    )
    
    private val creditCardRegex = Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b")
    private val ssnRegex = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")
    private val emailRegex = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")
    private val phoneRegex = Regex("\\b\\+?\\d{1,3}[\\s-]?\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4}\\b")
    
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        val sanitizedMessage = sanitizeString(logEntry.message)
        val sanitizedMetadata = sanitizeMetadata(logEntry.metadata)
        
        return logEntry.copy(
            message = sanitizedMessage,
            metadata = sanitizedMetadata
        )
    }
    
    private fun sanitizeString(input: String): String {
        return input
            .replace(creditCardRegex, "****-****-****-****")
            .replace(ssnRegex, "***-**-****")
            .replace(phoneRegex, "***-***-****")
            .replace(emailRegex) { matchResult ->
                val email = matchResult.value
                val atIndex = email.indexOf('@')
                "${email.substring(0, 2)}***${email.substring(atIndex)}"
            }
    }
    
    private fun sanitizeMetadata(metadata: Map<String, Any>): Map<String, Any> {
        return metadata.mapValues { (key, value) ->
            when {
                sensitiveKeys.any { key.contains(it, ignoreCase = true) } -> "***"
                value is String -> sanitizeString(value)
                value is Map<*, *> -> sanitizeNestedMap(value)
                else -> value
            }
        }
    }
    
    private fun sanitizeNestedMap(map: Map<*, *>): Map<*, *> {
        return map.mapValues { (key, value) ->
            when {
                key is String && sensitiveKeys.any { key.contains(it, ignoreCase = true) } -> "***"
                value is String -> sanitizeString(value)
                value is Map<*, *> -> sanitizeNestedMap(value)
                else -> value
            }
        }
    }
}
```

### Access Control

Implement role-based access control for logging:

```kotlin
class AccessControlledLogger {
    private val logger: SDKLogger
    private val accessChecker: AccessChecker
    
    init {
        logger = SDKLogger.getInstance("AccessControlledSDK", "1.0.0")
        accessChecker = AccessChecker()
    }
    
    fun secureLog(
        level: LogLevel,
        tag: String, 
        message: String,
        requiredPermission: Permission,
        metadata: Map<String, Any> = emptyMap()
    ) {
        if (!accessChecker.hasPermission(requiredPermission)) {
            logger.warning("Security", "Log access denied for $tag",
                metadata = mapOf(
                    "requiredPermission" to requiredPermission.name,
                    "attemptedAction" to "LOG_$level"
                ))
            return
        }
        
        when (level) {
            LogLevel.INFO -> logger.info(tag, message, metadata = metadata)
            LogLevel.WARNING -> logger.warning(tag, message, metadata = metadata)
            LogLevel.ERROR -> logger.error(tag, message, metadata = metadata)
            else -> logger.debug(tag, message, metadata = metadata)
        }
    }
}

enum class Permission {
    BASIC_LOGGING,
    SENSITIVE_DATA_LOGGING,
    SYSTEM_LOGGING,
    DEBUG_LOGGING
}

class AccessChecker {
    fun hasPermission(permission: Permission): Boolean {
        return when (permission) {
            Permission.BASIC_LOGGING -> true
            Permission.SENSITIVE_DATA_LOGGING -> isAuthorizedUser()
            Permission.SYSTEM_LOGGING -> isSystemUser()
            Permission.DEBUG_LOGGING -> BuildConfig.DEBUG
        }
    }
    
    private fun isAuthorizedUser(): Boolean {
        // Check if user has permission to log sensitive data
        return getCurrentUser().hasRole("DATA_ACCESS")
    }
    
    private fun isSystemUser(): Boolean {
        // Check if this is a system-level operation
        return getCurrentUser().hasRole("SYSTEM_ADMIN")
    }
}
```

---

## 🚨 Error Handling Patterns

### Resilient Error Logging

Implement comprehensive error handling that provides context and recovery suggestions:

```kotlin
class ResilientErrorLogger {
    private val logger = SDKLogger.getInstance("ResilientSDK", "1.0.0")
    
    fun logErrorWithContext(
        operation: String,
        error: Throwable,
        context: ErrorContext
    ) {
        try {
            val errorMetadata = buildErrorMetadata(error, context)
            val classification = classifyError(error)
            
            logger.error(
                tag = "Error",
                message = "Operation '$operation' failed: ${error.message}",
                throwable = error,
                metadata = errorMetadata + mapOf(
                    "errorClassification" to classification.name,
                    "severity" to classification.severity,
                    "isRetryable" to classification.isRetryable,
                    "suggestedAction" to classification.suggestedAction
                )
            )
            
            // Send to error tracking service if critical
            if (classification.severity == ErrorSeverity.CRITICAL) {
                sendToCrashlytics(error, errorMetadata)
            }
            
        } catch (loggingError: Exception) {
            // Fallback logging mechanism
            println("FALLBACK: Error in $operation: ${error.message}")
            println("FALLBACK: Logging error: ${loggingError.message}")
        }
    }
    
    private fun buildErrorMetadata(error: Throwable, context: ErrorContext): Map<String, Any> {
        return mapOf(
            "errorType" to error.javaClass.simpleName,
            "errorMessage" to (error.message ?: "No message"),
            "stackTrace" to error.stackTraceToString().take(1000), // Limit size
            "causedBy" to (error.cause?.javaClass?.simpleName ?: "None"),
            "timestamp" to System.currentTimeMillis(),
            "userId" to context.userId,
            "sessionId" to context.sessionId,
            "feature" to context.feature,
            "version" to context.appVersion,
            "deviceInfo" to context.deviceInfo,
            "networkState" to context.networkState,
            "memoryUsage" to Runtime.getRuntime().let { 
                "${it.totalMemory() - it.freeMemory()}/${it.totalMemory()}" 
            }
        )
    }
    
    private fun classifyError(error: Throwable): ErrorClassification {
        return when (error) {
            is OutOfMemoryError -> ErrorClassification.CRITICAL_MEMORY
            is SecurityException -> ErrorClassification.CRITICAL_SECURITY
            is NetworkException -> ErrorClassification.RECOVERABLE_NETWORK
            is IllegalArgumentException -> ErrorClassification.DEVELOPER_ERROR
            is RuntimeException -> ErrorClassification.APPLICATION_ERROR
            else -> ErrorClassification.UNKNOWN
        }
    }
    
    private fun sendToCrashlytics(error: Throwable, metadata: Map<String, Any>) {
        // Implementation for crash reporting service integration
        // Example: Firebase Crashlytics, Bugsnag, etc.
    }
}

data class ErrorContext(
    val userId: String,
    val sessionId: String,
    val feature: String,
    val appVersion: String,
    val deviceInfo: Map<String, String>,
    val networkState: String
)

enum class ErrorClassification(
    val severity: ErrorSeverity,
    val isRetryable: Boolean,
    val suggestedAction: String
) {
    CRITICAL_MEMORY(ErrorSeverity.CRITICAL, false, "RESTART_APP"),
    CRITICAL_SECURITY(ErrorSeverity.CRITICAL, false, "CONTACT_SUPPORT"),
    RECOVERABLE_NETWORK(ErrorSeverity.MEDIUM, true, "RETRY_WITH_BACKOFF"),
    DEVELOPER_ERROR(ErrorSeverity.HIGH, false, "FIX_CODE"),
    APPLICATION_ERROR(ErrorSeverity.MEDIUM, true, "RETRY_OR_FALLBACK"),
    UNKNOWN(ErrorSeverity.LOW, true, "MONITOR")
}

enum class ErrorSeverity { LOW, MEDIUM, HIGH, CRITICAL }
```

### Circuit Breaker Pattern

Implement circuit breaker pattern to prevent logging system failures from affecting the main application:

```kotlin
class CircuitBreakerLogger {
    private val logger = SDKLogger.getInstance("CircuitBreakerSDK", "1.0.0")
    private val circuitBreaker = CircuitBreaker()
    
    fun logWithCircuitBreaker(tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        circuitBreaker.execute(
            operation = {
                logger.info(tag, message, metadata = metadata)
            },
            fallback = {
                // Fallback to simple console logging
                println("[$tag] $message")
            },
            onFailure = { error ->
                println("CIRCUIT_BREAKER: Logging failed - ${error.message}")
            }
        )
    }
}

class CircuitBreaker {
    private var state = State.CLOSED
    private var failureCount = 0
    private var lastFailureTime = 0L
    private val failureThreshold = 5
    private val recoveryTimeoutMs = 60000L // 1 minute
    
    fun <T> execute(
        operation: () -> T,
        fallback: () -> T,
        onFailure: (Exception) -> Unit
    ): T {
        return when (state) {
            State.CLOSED -> {
                try {
                    val result = operation()
                    reset()
                    result
                } catch (e: Exception) {
                    recordFailure()
                    onFailure(e)
                    fallback()
                }
            }
            
            State.OPEN -> {
                if (shouldAttemptReset()) {
                    state = State.HALF_OPEN
                    execute(operation, fallback, onFailure)
                } else {
                    fallback()
                }
            }
            
            State.HALF_OPEN -> {
                try {
                    val result = operation()
                    reset()
                    result
                } catch (e: Exception) {
                    state = State.OPEN
                    lastFailureTime = System.currentTimeMillis()
                    onFailure(e)
                    fallback()
                }
            }
        }
    }
    
    private fun recordFailure() {
        failureCount++
        if (failureCount >= failureThreshold) {
            state = State.OPEN
            lastFailureTime = System.currentTimeMillis()
        }
    }
    
    private fun reset() {
        failureCount = 0
        state = State.CLOSED
    }
    
    private fun shouldAttemptReset(): Boolean {
        return System.currentTimeMillis() - lastFailureTime >= recoveryTimeoutMs
    }
    
    enum class State { CLOSED, OPEN, HALF_OPEN }
}
```

---

## 📊 Monitoring & Alerting

### Production Health Monitoring

Implement comprehensive health monitoring for production environments:

```kotlin
class ProductionHealthMonitor {
    private val analytics = LogAnalytics()
    private val logger = SDKLogger.getInstance("HealthMonitor", "1.0.0")
    private val alertManager = AlertManager()
    
    // Schedule health checks
    fun startMonitoring() {
        val scheduler = Executors.newScheduledThreadPool(2)
        
        // Real-time monitoring (every 30 seconds)
        scheduler.scheduleAtFixedRate({
            checkRealTimeHealth()
        }, 0, 30, TimeUnit.SECONDS)
        
        // Comprehensive analysis (every 5 minutes)
        scheduler.scheduleAtFixedRate({
            performComprehensiveAnalysis()
        }, 0, 5, TimeUnit.MINUTES)
        
        // Daily report (every 24 hours)
        scheduler.scheduleAtFixedRate({
            generateDailyReport()
        }, 0, 24, TimeUnit.HOURS)
    }
    
    private fun checkRealTimeHealth() {
        val stats = analytics.getStatistics()
        val currentTime = System.currentTimeMillis()
        
        // Check error rate spike
        val recentErrorRate = calculateRecentErrorRate(stats, currentTime - 60000) // Last minute
        if (recentErrorRate > 0.1) { // More than 10% errors
            alertManager.sendCriticalAlert(
                "High Error Rate",
                "Error rate: ${String.format("%.1f", recentErrorRate * 100)}% in the last minute",
                mapOf("errorRate" to recentErrorRate, "timeWindow" to "1 minute")
            )
        }
        
        // Check memory usage
        val memoryUsage = getMemoryUsage()
        if (memoryUsage > 0.9) { // More than 90% memory usage
            alertManager.sendWarningAlert(
                "High Memory Usage",
                "Memory usage: ${String.format("%.1f", memoryUsage * 100)}%",
                mapOf("memoryUsage" to memoryUsage)
            )
        }
        
        // Check logging volume
        val logVolume = calculateLogVolume(stats, currentTime - 60000)
        if (logVolume > 1000) { // More than 1000 logs per minute
            logger.warning("Health", "High log volume detected",
                metadata = mapOf("logsPerMinute" to logVolume))
        }
    }
    
    private fun performComprehensiveAnalysis() {
        val stats = analytics.getStatistics()
        val healthReport = generateHealthReport(stats)
        
        logger.info("Health", "Health check completed",
            metadata = mapOf(
                "overallHealth" to healthReport.status.name,
                "errorRate" to healthReport.errorRate,
                "warningRate" to healthReport.warningRate,
                "totalLogs" to healthReport.totalLogs,
                "activeSDKs" to healthReport.activeSDKs,
                "topErrors" to healthReport.topErrors.take(3)
            ))
        
        // Send alerts based on health status
        when (healthReport.status) {
            HealthStatus.CRITICAL -> {
                alertManager.sendCriticalAlert(
                    "System Health Critical",
                    healthReport.summary,
                    healthReport.toMap()
                )
            }
            
            HealthStatus.WARNING -> {
                alertManager.sendWarningAlert(
                    "System Health Warning",
                    healthReport.summary,
                    healthReport.toMap()
                )
            }
            
            HealthStatus.HEALTHY -> {
                // All good, just log
            }
        }
    }
    
    private fun generateDailyReport() {
        val stats = analytics.getStatistics()
        val report = generateDailyHealthReport(stats)
        
        // Send to management dashboard
        sendToManagementDashboard(report)
        
        // Reset daily metrics
        analytics.reset()
        
        logger.info("Health", "Daily report generated",
            metadata = mapOf(
                "reportDate" to SimpleDateFormat("yyyy-MM-dd").format(Date()),
                "summary" to report.executiveSummary
            ))
    }
    
    private fun calculateRecentErrorRate(stats: Map<String, Any>, since: Long): Double {
        // Implementation to calculate error rate for recent time window
        return 
```

---
## 🧠 Memory Management

### Memory-Conscious Configuration

Configure logging based on available memory and device capabilities:

```kotlin
class MemoryOptimizedConfig {
    companion object {
        fun createOptimalConfig(context: Context): SDKLoggerConfig {
            val memoryClass = getMemoryClass(context)
            
            return when {
                memoryClass >= 512 -> createHighMemoryConfig(context)
                memoryClass >= 256 -> createMediumMemoryConfig(context)
                else -> createLowMemoryConfig(context)
            }
        }
        
        private fun createLowMemoryConfig(context: Context): SDKLoggerConfig {
            return SDKLoggerConfig.Builder()
                .setBufferSize(25) // Small buffer
                .setFlushInterval(5000L) // Frequent flushes
                .setMaxLogFileSize(1 * 1024 * 1024L) // 1MB files
                .setMaxLogFiles(2) // Keep only 2 files
                .setMetadataCollection(false) // Disable metadata
                .setStackTrace(false) // Disable stack traces
                .addDestination(ConsoleDestination())
                .build()
        }
        
        private fun createMediumMemoryConfig(context: Context): SDKLoggerConfig {
            return SDKLoggerConfig.Builder()
                .setBufferSize(100) // Medium buffer
                .setFlushInterval(10000L) // Moderate flush interval
                .setMaxLogFileSize(5 * 1024 * 1024L) // 5MB files
                .setMaxLogFiles(3) // Keep 3 files
                .setMetadataCollection(true)
                .setStackTrace(false) // Limited stack traces
                .addDestination(ConsoleDestination())
                .addDestination(FileDestination(
                    logDirectory = File(context.getExternalFilesDir(null), "logs")
                ))
                .build()
        }
        
        private fun createHighMemoryConfig(context: Context): SDKLoggerConfig {
            return SDKLoggerConfig.Builder()
                .setBufferSize(500) // Large buffer
                .setFlushInterval(30000L) // Less frequent flushes
                .setMaxLogFileSize(50 * 1024 * 1024L) // 50MB files
                .setMaxLogFiles(10) // Keep 10 files
                .setMetadataCollection(true)
                .setStackTrace(true)
                .addDestination(ConsoleDestination())
                .addDestination(FileDestination(
                    logDirectory = File(context.getExternalFilesDir(null), "logs")
                ))
                .build()
        }
        
        private fun getMemoryClass(context: Context): Int {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return activityManager.memoryClass
        }
    }
}
```

---

## 🏗️ SDK Integration Patterns

### Microservices Architecture

Implement logging for microservices with correlation IDs and distributed tracing:

```kotlin
class MicroserviceLogger {
    companion object {
        fun create(serviceName: String, context: Context): SDKLogger {
            val config = SDKLoggerConfig.Builder()
                .addDestination(ConsoleDestination())
                .addDestination(FileDestination(
                    logDirectory = File(context.getExternalFilesDir(null), "microservices"),
                    baseFileName = serviceName.lowercase()
                ))
                .addInterceptor(CorrelationIdInterceptor())
                .addInterceptor(ServiceContextInterceptor(serviceName))
                .build()
                
            return SDKLogger.getInstance(serviceName, BuildConfig.VERSION_NAME, config)
        }
    }
}

class CorrelationIdInterceptor : LogInterceptor {
    private val correlationIdThreadLocal = ThreadLocal.withInitial { UUID.randomUUID().toString() }
    
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        val correlationId = correlationIdThreadLocal.get()
        
        return logEntry.copy(
            metadata = logEntry.metadata + mapOf(
                "correlationId" to correlationId,
                "traceId" to getDistributedTraceId(),
                "spanId" to getCurrentSpanId()
            )
        )
    }
    
    private fun getDistributedTraceId(): String {
        // Integration with distributed tracing system (Jaeger, Zipkin, etc.)
        return MDC.get("traceId") ?: UUID.randomUUID().toString()
    }
    
    private fun getCurrentSpanId(): String {
        return MDC.get("spanId") ?: UUID.randomUUID().toString()
    }
    
    fun setCorrelationId(correlationId: String) {
        correlationIdThreadLocal.set(correlationId)
    }
}

class ServiceContextInterceptor(
    private val serviceName: String
) : LogInterceptor {
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        val serviceContext = mapOf(
            "serviceName" to serviceName,
            "serviceVersion" to BuildConfig.VERSION_NAME,
            "environment" to BuildConfig.BUILD_TYPE,
            "instanceId" to getInstanceId(),
            "nodeId" to getNodeId(),
            "clusterId" to getClusterId(),
            "region" to getRegion()
        )
        
        return logEntry.copy(
            metadata = logEntry.metadata + serviceContext
        )
    }
    
    private fun getInstanceId(): String = System.getProperty("instance.id") ?: "unknown"
    private fun getNodeId(): String = System.getProperty("node.id") ?: "unknown"
    private fun getClusterId(): String = System.getProperty("cluster.id") ?: "unknown"
    private fun getRegion(): String = System.getProperty("region") ?: "unknown"
}
```

---

## 🧪 Testing Strategies

### Unit Testing Logger Configuration

Create comprehensive tests for different logger configurations:

```kotlin
class LoggerConfigurationTest {
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    @Test
    fun `test debug configuration creates verbose logging`() {
        val logger = LoggerFactory.createDebugLogger(context, "TestSDK", "1.0.0")
        val config = logger.getConfig()
        
        assertTrue("Debug logger should be enabled", config.isEnabled)
        assertEquals("Debug logger should use VERBOSE level", LogLevel.VERBOSE, config.minLogLevel)
        assertFalse("Debug logger should be synchronous", config.enableAsync)
        assertTrue("Debug logger should collect metadata", config.enableMetadataCollection)
        assertTrue("Debug logger should include stack traces", config.enableStackTrace)
    }
    
    @Test
    fun `test production configuration limits logging`() {
        val logger = LoggerFactory.createProductionLogger(context, "TestSDK", "1.0.0")
        val config = logger.getConfig()
        
        assertEquals("Production logger should use WARNING level", LogLevel.WARNING, config.minLogLevel)
        assertTrue("Production logger should be async", config.enableAsync)
        assertTrue("Production logger should have interceptors", config.interceptors.isNotEmpty())
        assertTrue("Production logger should have small buffer", config.bufferSize <= 50)
    }
    
    @Test
    fun `test memory optimized configuration for low memory devices`() {
        val config = MemoryOptimizedConfig.createLowMemoryConfig(context)
        
        assertEquals("Low memory config should have small buffer", 25, config.bufferSize)
        assertEquals("Low memory config should have small files", 1 * 1024 * 1024L, config.maxLogFileSize)
        assertEquals("Low memory config should keep few files", 2, config.maxLogFiles)
        assertFalse("Low memory config should disable metadata", config.enableMetadataCollection)
        assertFalse("Low memory config should disable stack traces", config.enableStackTrace)
    }
}
```

### Integration Testing

Test the complete logging pipeline:

```kotlin
class LoggerIntegrationTest {
    private lateinit var testLogger: SDKLogger
    private lateinit var testDestination: TestLogDestination
    
    @Before
    fun setup() {
        testDestination = TestLogDestination()
        val config = SDKLoggerConfig.Builder()
            .addDestination(testDestination)
            .setAsync(false) // Synchronous for testing
            .build()
            
        testLogger = SDKLogger.getInstance("TestSDK", "1.0.0", config)
    }
    
    @Test
    fun `test log entry creation with metadata`() {
        val metadata = mapOf("key1" to "value1", "key2" to 123, "key3" to true)
        testLogger.info("TestTag", "Test message", metadata = metadata)
        
        val logEntries = testDestination.getLogEntries()
        assertEquals("Should have one log entry", 1, logEntries.size)
        
        val entry = logEntries.first()
        assertEquals("Log level should be INFO", LogLevel.INFO, entry.level)
        assertEquals("Tag should match", "TestTag", entry.tag)
        assertEquals("Message should match", "Test message", entry.message)
        assertEquals("SDK name should match", "TestSDK", entry.sdkName)
        assertEquals("Metadata should match", metadata, entry.metadata)
        assertNotNull("Timestamp should be set", entry.timestamp)
        assertNotNull("Thread name should be set", entry.threadName)
    }
    
    @Test
    fun `test error logging with exception`() {
        val exception = RuntimeException("Test exception")
        testLogger.error("ErrorTag", "Error occurred", exception,
            metadata = mapOf("errorCode" to "TEST_001"))
        
        val logEntries = testDestination.getLogEntries()
        val entry = logEntries.first()
        
        assertEquals("Log level should be ERROR", LogLevel.ERROR, entry.level)
        assertEquals("Exception should be captured", exception, entry.throwable)
        assertTrue("Metadata should include error code", 
                  entry.metadata["errorCode"] == "TEST_001")
    }
    
    @Test
    fun `test interceptor chain execution`() {
        val testInterceptor = TestInterceptor()
        val config = SDKLoggerConfig.Builder()
            .addDestination(testDestination)
            .addInterceptor(testInterceptor)
            .setAsync(false)
            .build()
            
        val logger = SDKLogger.getInstance("InterceptorTestSDK", "1.0.0", config)
        logger.info("Test", "Test message")
        
        assertTrue("Interceptor should have been called", testInterceptor.wasCalled)
        
        val logEntries = testDestination.getLogEntries()
        val entry = logEntries.first()
        assertTrue("Interceptor should have modified metadata", 
                  entry.metadata.containsKey("intercepted"))
    }
}

class TestLogDestination : LogDestination {
    private val logEntries = mutableListOf<LogEntry>()
    
    override suspend fun writeLog(logEntry: LogEntry) {
        logEntries.add(logEntry)
    }
    
    override suspend fun flush() {}
    override suspend fun close() {}
    override fun getName(): String = "Test"
    
    fun getLogEntries(): List<LogEntry> = logEntries.toList()
    fun clear() = logEntries.clear()
}

class TestInterceptor : LogInterceptor {
    var wasCalled = false
    
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        wasCalled = true
        return logEntry.copy(
            metadata = logEntry.metadata + mapOf("intercepted" to true)
        )
    }
}
```

### Performance Testing

Measure and validate logging performance:

```kotlin
class LoggerPerformanceTest {
    
    @Test
    fun `test async logging performance under load`() {
        val config = SDKLoggerConfig.Builder()
            .setAsync(true)
            .setBufferSize(1000)
            .addDestination(TestLogDestination())
            .build()
            
        val logger = SDKLogger.getInstance("PerformanceSDK", "1.0.0", config)
        
        val startTime = System.currentTimeMillis()
        val logCount = 10000
        
        repeat(logCount) { i ->
            logger.info("Performance", "Log message $i",
                metadata = mapOf(
                    "index" to i, 
                    "timestamp" to System.currentTimeMillis(),
                    "data" to "test_data_$i"
                ))
        }
        
        // Flush to ensure all logs are processed
        runBlocking { logger.flush() }
        
        val duration = System.currentTimeMillis() - startTime
        val logsPerSecond = (logCount.toDouble() / duration) * 1000
        
        println("Logged $logCount messages in ${duration}ms")
        println("Performance: ${String.format("%.2f", logsPerSecond)} logs/second")
        
        // Assert performance is acceptable (adjust threshold as needed)
        assertTrue("Performance too slow: $logsPerSecond logs/second", 
                  logsPerSecond > 1000)
    }
    
    @Test
    fun `test memory usage during high volume logging`() {
        val initialMemory = getUsedMemory()
        
        val logger = SDKLogger.getInstance("MemoryTestSDK", "1.0.0")
        
        // Generate large volume of logs
        repeat(50000) { i ->
            logger.info("Memory", "Memory test log $i",
                metadata = mapOf(
                    "data" to "x".repeat(100), // 100 char string
                    "index" to i,
                    "timestamp" to System.currentTimeMillis()
                ))
        }
        
        // Force garbage collection and measure
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = getUsedMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseMB = memoryIncrease / 1024 / 1024
        
        println("Memory increase: ${memoryIncreaseMB}MB")
        
        // Assert memory increase is reasonable (adjust threshold as needed)
        assertTrue("Memory increase too high: ${memoryIncreaseMB}MB", 
                  memoryIncreaseMB < 50)
    }
    
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}
```

---

## 📋 Compliance & Privacy

### GDPR Compliance

Implement GDPR-compliant logging with proper consent management:

```kotlin
class GDPRCompliantLogger {
    private val logger: SDKLogger
    private val consentManager: ConsentManager
    
    init {
        val config = SDKLoggerConfig.Builder()
            .addDestination(ConsoleDestination())
            .addDestination(FileDestination(
                logDirectory = getGDPRCompliantLogDirectory(),
                maxFileSize = 5 * 1024 * 1024L,
                maxFiles = 3
            ))
            .addInterceptor(GDPRInterceptor(consentManager))
            .addInterceptor(DataRetentionInterceptor())
            .build()
            
        logger = SDKLogger.getInstance("GDPRCompliantSDK", "1.0.0", config)
    }
    
    fun logUserAction(action: String, userId: String?, metadata: Map<String, Any>) {
        if (consentManager.hasLoggingConsent(userId)) {
            logger.info("UserAction", action,
                metadata = metadata + mapOf(
                    "userId" to (userId ?: "anonymous"),
                    "consentGiven" to true,
                    "dataProcessingLegal" to true
                ))
        } else {
            // Log without personal data
            logger.info("UserAction", action,
                metadata = metadata.filterKeys { !isPersonalData(it) } + mapOf(
                    "userId" to "anonymous",
                    "consentGiven" to false
                ))
        }
    }
    
    private fun isPersonalData(key: String): Boolean {
        val personalDataKeys = setOf(
            "userId", "email", "phone", "name", "address", 
            "ip", "deviceId", "sessionId", "location"
        )
        return personalDataKeys.any { key.contains(it, ignoreCase = true) }
    }
    
    private fun getGDPRCompliantLogDirectory(): File {
        // Use secure internal storage for GDPR compliance
        return File(context.filesDir, "gdpr_logs")
    }
}

class GDPRInterceptor(
    private val consentManager: ConsentManager
) : LogInterceptor {
    
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        val userId = logEntry.metadata["userId"] as? String
        
        return if (consentManager.hasLoggingConsent(userId)) {
            // User has given consent, log normally
            logEntry.copy(
                metadata = logEntry.metadata + mapOf("gdprCompliant" to true)
            )
        } else {
            // Remove personal data
            val sanitizedMetadata = logEntry.metadata.filterKeys { key ->
                !isPersonalData(key)
            } + mapOf(
                "dataProcessingConsent" to false,
                "gdprCompliant" to true
            )
            
            logEntry.copy(
                metadata = sanitizedMetadata,
                message = sanitizeMessage(logEntry.message)
            )
        }
    }
    
    private fun isPersonalData(key: String): Boolean {
        val personalDataKeys = setOf(
            "userId", "email", "phone", "name", "address", 
            "ip", "deviceId", "sessionId", "location"
        )
        return personalDataKeys.any { key.contains(it, ignoreCase = true) }
    }
    
    private fun sanitizeMessage(message: String): String {
        // Remove potential personal identifiers from message
        return message
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "[EMAIL]")
            .replace(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), "[PHONE]")
            .replace(Regex("\\buser_\\d+\\b"), "[USER_ID]")
            .replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"), "[IP_ADDRESS]")
    }
}

class DataRetentionInterceptor : LogInterceptor {
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        // Add data retention metadata
        val retentionMetadata = mapOf(
            "dataRetentionDays" to getRetentionPeriod(logEntry.level),
            "dataClassification" to classifyData(logEntry),
            "autoDeleteDate" to calculateDeleteDate(logEntry.level)
        )
        
        return logEntry.copy(
            metadata = logEntry.metadata + retentionMetadata
        )
    }
    
    private fun getRetentionPeriod(level: LogLevel): Int {
        return when (level) {
            LogLevel.ERROR, LogLevel.ASSERT -> 90 // Keep errors for 90 days
            LogLevel.WARNING -> 30 // Keep warnings for 30 days
            LogLevel.INFO -> 14 // Keep info for 14 days
            LogLevel.DEBUG, LogLevel.VERBOSE -> 7 // Keep debug for 7 days
        }
    }
    
    private fun classifyData(logEntry: LogEntry): String {
        val hasPersonalData = logEntry.metadata.keys.any { isPersonalData(it) }
        return when {
            hasPersonalData -> "PERSONAL_DATA"
            logEntry.level in listOf(LogLevel.ERROR, LogLevel.ASSERT) -> "OPERATIONAL_DATA"
            else -> "TECHNICAL_DATA"
        }
    }
    
    private fun calculateDeleteDate(level: LogLevel): Long {
        val retentionDays = getRetentionPeriod(level)
        return System.currentTimeMillis() + (retentionDays * 24 * 60 * 60 * 1000L)
    }
    
    private fun isPersonalData(key: String): Boolean {
        val personalDataKeys = setOf(
            "userId", "email", "phone", "name", "address", 
            "ip", "deviceId", "sessionId", "location"
        )
        return personalDataKeys.any { key.contains(it, ignoreCase = true) }
    }
}

interface ConsentManager {
    fun hasLoggingConsent(userId: String?): Boolean
    fun recordConsentChange(userId: String, hasConsent: Boolean)
}
```

---

## 🎯 Continuous Improvement

### A/B Testing for Logging

Test different logging configurations to optimize performance and utility:

```kotlin
class ABTestingLogger {
    private val loggerA: SDKLogger
    private val loggerB: SDKLogger
    private val experimentManager: ExperimentManager
    
    init {
        // Configuration A: High verbosity, more metadata
        val configA = SDKLoggerConfig.Builder()
            .setMinLogLevel(LogLevel.DEBUG)
            .setBufferSize(200)
            .setFlushInterval(5000L)
            .setMetadataCollection(true)
            .setStackTrace(true)
            .build()
            
        // Configuration B: Low verbosity, high performance
        val configB = SDKLoggerConfig.Builder()
            .setMinLogLevel(LogLevel.INFO)
            .setBufferSize(500)
            .setFlushInterval(30000L)
            .setMetadataCollection(false)
            .setStackTrace(false)
            .build()
            
        loggerA = SDKLogger.getInstance("ABTestA", "1.0.0", configA)
        loggerB = SDKLogger.getInstance("ABTestB", "1.0.0", configB)
        
        experimentManager = ExperimentManager()
    }
    
    fun log(level: LogLevel, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        val userId = metadata["userId"] as? String ?: "anonymous"
        val variant = experimentManager.getVariant("logging_config", userId)
        
        val logger = when (variant) {
            "A" -> loggerA
            "B" -> loggerB
            else -> loggerA // Default
        }
        
        val enhancedMetadata = metadata + mapOf(
            "abTestVariant" to variant,
            "experimentId" to "logging_config"
        )
        
        when (level) {
            LogLevel.INFO -> logger.info(tag, message, metadata = enhancedMetadata)
            LogLevel.WARNING -> logger.warning(tag, message, metadata = enhancedMetadata)
            LogLevel.ERROR -> logger.error(tag, message, metadata = enhancedMetadata)
            LogLevel.DEBUG -> logger.debug(tag, message, metadata = enhancedMetadata)
            else -> logger.verbose(tag, message, metadata = enhancedMetadata)
        }
    }
}

interface ExperimentManager {
    fun getVariant(experimentId: String, userId: String): String
}
```




