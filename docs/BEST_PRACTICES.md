### CPU-Optimized Logging

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
}

class SecurityInterceptor : LogInterceptor {
    private val sensitiveKeys = setOf(
        "password", "token", "secret", "key", "auth", "credential",
        "ssn", "social", "credit", "card", "pin", "cvv"
    )
    
    private val creditCardRegex = Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b")
    private val ssnRegex = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")
    private val emailRegex = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")
    
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
```

### Log Encryption

```kotlin
class EncryptedFileDestination(
    private val logDirectory: File,
    private val encryptionKey: String
) : LogDestination {
    
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val secretKey = generateSecretKey(encryptionKey)
    
    override suspend fun writeLog(logEntry: LogEntry) {
        val plaintext = JsonLogFormatter().format(logEntry)
        val encrypted = encrypt(plaintext)
        
        val logFile = File(logDirectory, "encrypted_logs.dat")
        logFile.appendBytes(encrypted)
    }
    
    private fun encrypt(plaintext: String): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray())
        
        // Prepend IV to ciphertext
        return iv + ciphertext
    }
    
    private fun generateSecretKey(password: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), "salt".toByteArray(), 100000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
    
    override suspend fun flush() { /* Implementation */ }
    override suspend fun close() { /* Implementation */ }
    override fun getName(): String = "EncryptedFile"
}
```

---

## 🚨 Error Handling Patterns

### Resilient Error Logging

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
                "Error rate: ${(recentErrorRate * 100).format(1)}% in the last minute",
                mapOf("errorRate" to recentErrorRate, "timeWindow" to "1 minute")
            )
        }
        
        // Check memory usage
        val memoryUsage = getMemoryUsage()
        if (memoryUsage > 0.9) { // More than 90% memory usage
            alertManager.sendWarningAlert(
                "High Memory Usage",
                "Memory usage: ${(memoryUsage * 100).format(1)}%",
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
}
```

### Alert Management

```kotlin
class AlertManager {
    private val logger = SDKLogger.getInstance("AlertManager", "1.0.0")
    private val rateLimiter = RateLimiter()
    
    fun sendCriticalAlert(title: String, message: String, metadata: Map<String, Any>) {
        if (rateLimiter.shouldSendAlert(AlertLevel.CRITICAL, title)) {
            logger.assert("Alert", "CRITICAL: $title - $message", metadata = metadata)
            
            // Send to multiple channels
            sendToSlack(AlertLevel.CRITICAL, title, message, metadata)
            sendToPagerDuty(AlertLevel.CRITICAL, title, message, metadata)
            sendToEmail(AlertLevel.CRITICAL, title, message, metadata)
        } else {
            logger.warning("Alert", "Critical alert rate limited: $title")
        }
    }
    
    fun sendWarningAlert(title: String, message: String, metadata: Map<String, Any>) {
        if (rateLimiter.shouldSendAlert(AlertLevel.WARNING, title)) {
            logger.warning("Alert", "WARNING: $title - $message", metadata = metadata)
            
            sendToSlack(AlertLevel.WARNING, title, message, metadata)
        } else {
            logger.info("Alert", "Warning alert rate limited: $title")
        }
    }
    
    private fun sendToSlack(level: AlertLevel, title: String, message: String, metadata: Map<String, Any>) {
        // Implementation for Slack integration
        logger.debug("Alert", "Sent to Slack: $level - $title")
    }
    
    private fun sendToPagerDuty(level: AlertLevel, title: String, message: String, metadata: Map<String, Any>) {
        // Implementation for PagerDuty integration
        logger.debug("Alert", "Sent to PagerDuty: $level - $title")
    }
}

class RateLimiter {
    private val alertHistory = mutableMapOf<String, MutableList<Long>>()
    private val cleanupScheduler = Executors.newScheduledThreadPool(1)
    
    init {
        // Clean up old alerts every hour
        cleanupScheduler.scheduleAtFixedRate({
            cleanupOldAlerts()
        }, 1, 1, TimeUnit.HOURS)
    }
    
    fun shouldSendAlert(level: AlertLevel, alertKey: String): Boolean {
        val now = System.currentTimeMillis()
        val key = "${level.name}:$alertKey"
        val history = alertHistory.getOrPut(key) { mutableListOf() }
        
        val timeWindow = when (level) {
            AlertLevel.CRITICAL -> 5 * 60 * 1000L // 5 minutes
            AlertLevel.WARNING -> 15 * 60 * 1000L // 15 minutes
            AlertLevel.INFO -> 60 * 60 * 1000L // 1 hour
        }
        
        val maxAlerts = when (level) {
            AlertLevel.CRITICAL -> 3 // Max 3 critical alerts per 5 minutes
            AlertLevel.WARNING -> 2  // Max 2 warning alerts per 15 minutes
            AlertLevel.INFO -> 1     // Max 1 info alert per hour
        }
        
        // Remove old alerts outside time window
        history.removeAll { it < now - timeWindow }
        
        return if (history.size < maxAlerts) {
            history.add(now)
            true
        } else {
            false
        }
    }
    
    private fun cleanupOldAlerts() {
        val cutoffTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000L // 24 hours
        alertHistory.forEach { (key, history) ->
            history.removeAll { it < cutoffTime }
        }
        alertHistory.entries.removeAll { it.value.isEmpty() }
    }
}

enum class AlertLevel { INFO, WARNING, CRITICAL }
```

---

## 🧪 Testing Strategies

### Unit Testing Logger Configuration

```kotlin
class LoggerConfigurationTest {
    
    @Test
    fun `test debug configuration`() {
        val config = LoggerFactory.createDebugConfig(context)
        
        assertTrue(config.isEnabled)
        assertEquals(LogLevel.VERBOSE, config.minLogLevel)
        assertTrue(config.enableAsync == false) // Synchronous for debugging
        assertTrue(config.destinations.any { it is ConsoleDestination })
        assertTrue(config.destinations.any { it is FileDestination })
    }
    
    @Test
    fun `test production configuration`() {
        val config = LoggerFactory.createProductionConfig(context)
        
        assertEquals(LogLevel.WARNING, config.minLogLevel)
        assertTrue(config.enableAsync)
        assertTrue(config.interceptors.any { it is SensitiveDataInterceptor })
        assertTrue(config.maxLogFileSize <= 5 * 1024 * 1024L) // Max 5MB
    }
}
```

### Integration Testing

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
    fun `test log entry creation`() {
        val metadata = mapOf("key1" to "value1", "key2" to 123)
        testLogger.info("TestTag", "Test message", metadata = metadata)
        
        val logEntries = testDestination.getLogEntries()
        assertEquals(1, logEntries.size)
        
        val entry = logEntries.first()
        assertEquals(LogLevel.INFO, entry.level)
        assertEquals("TestTag", entry.tag)
        assertEquals("Test message", entry.message)
        assertEquals("TestSDK", entry.sdkName)
        assertEquals(metadata, entry.metadata)
    }
    
    @Test
    fun `test error logging with exception`() {
        val exception = RuntimeException("Test exception")
        testLogger.error("ErrorTag", "Error occurred", exception)
        
        val logEntries = testDestination.getLogEntries()
        val entry = logEntries.first()
        
        assertEquals(LogLevel.ERROR, entry.level)
        assertEquals(exception, entry.throwable)
        assertNotNull(entry.throwable?.stackTrace)
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
```

### Performance Testing

```kotlin
class LoggerPerformanceTest {
    
    @Test
    fun `test async logging performance`() {
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
                metadata = mapOf("index" to i, "timestamp" to System.currentTimeMillis()))
        }
        
        val duration = System.currentTimeMillis() - startTime
        val logsPerSecond = (logCount.toDouble() / duration) * 1000
        
        println("Logged $logCount messages in ${duration}ms")
        println("Performance: ${logsPerSecond.format(2)} logs/second")
        
        // Assert performance is acceptable
        assertTrue("Performance too slow: $logsPerSecond logs/second", logsPerSecond > 1000)
    }
    
    @Test
    fun `test memory usage during high volume logging`() {
        val initialMemory = getUsedMemory()
        
        val logger = SDKLogger.getInstance("MemoryTestSDK", "1.0.0")
        
        repeat(50000) { i ->
            logger.info("Memory", "Memory test log $i",
                metadata = mapOf("data" to "x".repeat(100))) // 100 char string
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = getUsedMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        println("Memory increase: ${memoryIncrease / 1024 / 1024}MB")
        
        // Assert memory increase is reasonable (less than 50MB)
        assertTrue("Memory increase too high: ${memoryIncrease / 1024 / 1024}MB", 
                  memoryIncrease < 50 * 1024 * 1024)
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
            "ip", "deviceId", "sessionId"
        )
        return personalDataKeys.any { key.contains(it, ignoreCase = true) }
    }
}

class GDPRInterceptor(
    private val consentManager: ConsentManager
) : LogInterceptor {
    
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        val userId = logEntry.metadata["userId"] as? String
        
        return if (consentManager.hasLoggingConsent(userId)) {
            // User has given consent, log normally
            logEntry
        } else {
            // Remove personal data
            val sanitizedMetadata = logEntry.metadata.filterKeys { key ->
                !isPersonalData(key)
            } + mapOf("dataProcessingConsent" to false)
            
            logEntry.copy(
                metadata = sanitizedMetadata,
                message = sanitizeMessage(logEntry.message)
            )
        }
    }
    
    private fun sanitizeMessage(message: String): String {
        // Remove potential personal identifiers from message
        return message
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "[EMAIL]")
            .replace(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), "[PHONE]")
            .replace(Regex("\\buser_\\d+\\b"), "[USER_ID]")
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
            metadata = logEntry.metadata +# SDKLogger - Best Practices

This guide covers production-ready patterns, performance optimization, and enterprise deployment strategies for SDKLogger.

## 📚 Table of Contents

- [Production Deployment](#production-deployment)
- [Performance Optimization](#performance-optimization)
- [Security Guidelines](#security-guidelines)
- [Error Handling Patterns](#error-handling-patterns)
- [Monitoring & Alerting](#monitoring--alerting)
- [Memory Management](#memory-management)
- [SDK Integration Patterns](#sdk-integration-patterns)
- [Testing Strategies](#testing-strategies)
- [Compliance & Privacy](#compliance--privacy)

---

## 🏭 Production Deployment

### Environment-Based Configuration

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
}
```

### Feature Flags Integration

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
}
```

### Gradual Rollout Strategy

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
                else -> 5           // 5% default rollout
            }
        }
    }
}
```

---

## ⚡ Performance Optimization

### High-Volume Logging

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
}
```

### Memory-Efficient Logging

```kotlin
class MemoryEfficientLogger {
    private val logger = SDKLogger.getInstance("MemorySDK", "1.0.0")
    
    // Limit metadata size
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

```kotlin
class CPUOptimizedLogger {
    private val logger = SDKLogger.getInstance("CPUOptimizedSDK", "1.0.0")
    
    //
