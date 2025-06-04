# SDKLogger - Complete Documentation

Welcome to the comprehensive SDKLogger documentation. This guide covers everything from basic setup to advanced enterprise features.

## 📚 Table of Contents

- [Installation & Setup](#installation--setup)
- [Core Concepts](#core-concepts)
- [Configuration Reference](#configuration-reference)
- [Log Levels Guide](#log-levels-guide)
- [Metadata Logging](#metadata-logging)
- [Multiple SDK Management](#multiple-sdk-management)
- [File Logging](#file-logging)
- [LogAnalytics API](#loganalytics-api)
- [Custom Destinations](#custom-destinations)
- [Log Interceptors](#log-interceptors)
- [Performance Optimization](#performance-optimization)
- [Error Handling](#error-handling)
- [Production Deployment](#production-deployment)

---

## 🚀 Installation & Setup

### Prerequisites

- **Minimum SDK**: Android API 21+
- **Kotlin**: 1.8.0+
- **Coroutines**: 1.7.0+

### Detailed Installation

#### 1. Repository Configuration

Add JitPack to your **root** `settings.gradle` or `settings.gradle.kts`:

**Groovy (`settings.gradle`):**
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Kotlin DSL (`settings.gradle.kts`):**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

#### 2. Module Dependencies

Add to your **module's** `build.gradle` or `build.gradle.kts`:

**Groovy (`build.gradle`):**
```gradle
dependencies {
    implementation 'com.github.sarojsahu-dev:SdkLogger:0.0.1'
    
    // Required dependencies
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // Optional: For JSON logging
    implementation 'org.json:json:20230227'
}
```

**Kotlin DSL (`build.gradle.kts`):**
```kotlin
dependencies {
    implementation("com.github.sarojsahu-dev:SdkLogger:0.0.1")
    
    // Required dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Optional: For JSON logging
    implementation("org.json:json:20230227")
}
```

#### 3. Permissions

For file logging, add to `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Required for file logging -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <!-- For network logging (if using custom network destination) -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application ...>
        ...
    </application>
</manifest>
```

---

## 🧠 Core Concepts

### Logger Instances

Each SDK gets its own logger instance identified by `sdkName` and `sdkVersion`:

```kotlin
// Each creates a separate logger instance
val paymentLogger = SDKLogger.getInstance("PaymentSDK", "2.1.0")
val authLogger = SDKLogger.getInstance("AuthSDK", "1.5.2")
val analyticsLogger = SDKLogger.getInstance("AnalyticsSDK", "3.0.1")

// Same SDK name + version = same instance (singleton pattern)
val sameLogger = SDKLogger.getInstance("PaymentSDK", "2.1.0") // Returns existing instance
```

### Log Entry Structure

Every log entry contains:

```kotlin
data class LogEntry(
    val id: String,                    // Unique log ID
    val timestamp: Long,               // When log was created
    val level: LogLevel,               // VERBOSE, DEBUG, INFO, etc.
    val tag: String,                   // Component/feature identifier
    val message: String,               // Log message
    val throwable: Throwable?,         // Exception (if any)
    val metadata: Map<String, Any>,    // Custom data
    val sdkName: String,               // Which SDK logged this
    val sdkVersion: String,            // SDK version
    val threadName: String,            // Thread that logged
    val className: String?,            // Class that logged (if enabled)
    val methodName: String?,           // Method that logged (if enabled)
    val lineNumber: Int?               // Line number (if enabled)
)
```

### Destinations

Logs can be sent to multiple destinations simultaneously:

- **ConsoleDestination**: Android Logcat
- **FileDestination**: Local files with rotation
- **Custom Destinations**: Network, Database, etc.

---

## ⚙️ Configuration Reference

### Complete Configuration Example

```kotlin
val config = SDKLoggerConfig.Builder()
    // Basic settings
    .setEnabled(true)
    .setMinLogLevel(LogLevel.DEBUG)
    
    // Destinations
    .addDestination(ConsoleDestination())
    .addDestination(FileDestination(
        logDirectory = File(context.getExternalFilesDir(null), "sdk_logs"),
        baseFileName = "my_app_logs",
        maxFileSize = 10 * 1024 * 1024L, // 10MB
        maxFiles = 5,
        formatter = JsonLogFormatter()
    ))
    
    // Performance settings
    .setAsync(true)
    .setBufferSize(200)
    .setFlushInterval(3000L)
    
    // Feature settings
    .setMetadataCollection(true)
    .setStackTrace(true)
    
    // File rotation settings
    .setMaxLogFileSize(5 * 1024 * 1024L) // 5MB
    .setMaxLogFiles(3)
    
    .build()

val logger = SDKLogger.getInstance("MySDK", "1.0.0", config)
```

### Configuration Options Detailed

#### setEnabled(Boolean)
```kotlin
.setEnabled(BuildConfig.DEBUG) // Only in debug builds
.setEnabled(true)               // Always enabled
.setEnabled(false)              // Completely disabled
```

#### setMinLogLevel(LogLevel)
```kotlin
.setMinLogLevel(LogLevel.VERBOSE) // Log everything
.setMinLogLevel(LogLevel.INFO)    // Skip VERBOSE and DEBUG
.setMinLogLevel(LogLevel.ERROR)   // Only errors and asserts
```

#### setAsync(Boolean)
```kotlin
.setAsync(true)   // High performance, non-blocking (recommended)
.setAsync(false)  // Synchronous, easier for debugging
```

#### setBufferSize(Int)
```kotlin
.setBufferSize(50)   // Small buffer, frequent flushes
.setBufferSize(200)  // Large buffer, better performance
.setBufferSize(1000) // Very large, maximum throughput
```

#### setFlushInterval(Long)
```kotlin
.setFlushInterval(1000L)  // Flush every 1 second
.setFlushInterval(5000L)  // Flush every 5 seconds (default)
.setFlushInterval(0L)     // Disable automatic flushing
```

---

## 📊 Log Levels Guide

### Level Hierarchy

```
VERBOSE (2) - Most detailed
DEBUG   (3) - Development info
INFO    (4) - General information
WARNING (5) - Potential issues
ERROR   (6) - Actual problems
ASSERT  (7) - Critical failures
```

### When to Use Each Level

#### VERBOSE
Most detailed information, typically only of interest when diagnosing problems:

```kotlin
logger.verbose("NetworkClient", "HTTP headers: ${request.headers}")
logger.verbose("Parser", "Parsing token: ${token.substring(0, 10)}...")
logger.verbose("Cache", "Cache lookup for key: $cacheKey")
```

#### DEBUG
Fine-grained informational events that are most useful to debug an application:

```kotlin
logger.debug("PaymentProcessor", "Validating credit card format")
logger.debug("AuthManager", "Token refresh initiated")
logger.debug("DatabaseHelper", "Executing query: ${query.take(50)}")
```

#### INFO
Informational messages that highlight the progress of the application:

```kotlin
logger.info("UserSession", "User logged in successfully")
logger.info("PaymentFlow", "Payment processing started")
logger.info("AppLifecycle", "Application started")
```

#### WARNING
Potentially harmful situations that don't prevent operation:

```kotlin
logger.warning("CacheManager", "Cache hit rate below 70%: ${hitRate}%")
logger.warning("NetworkClient", "Request took longer than expected: ${duration}ms")
logger.warning("AuthManager", "Token expires soon: ${expiryTime}")
```

#### ERROR
Error events that might still allow the application to continue:

```kotlin
logger.error("PaymentGateway", "Payment failed", paymentException)
logger.error("DatabaseHelper", "Failed to save user data", sqlException)
logger.error("NetworkClient", "API call failed", networkException)
```

#### ASSERT
Critical errors that should never happen:

```kotlin
logger.assert("SecurityManager", "Unauthorized access attempt detected")
logger.assert("DataIntegrity", "Critical data corruption found")
logger.assert("SystemState", "Invalid state reached: ${currentState}")
```

---

## 🏷️ Metadata Logging

### Basic Metadata

```kotlin
logger.info("UserAction", "Button clicked",
    metadata = mapOf(
        "buttonId" to "checkout_button",
        "screen" to "product_details",
        "timestamp" to System.currentTimeMillis()
    )
)
```

### Rich Context Metadata

```kotlin
logger.info("Purchase", "Order completed",
    metadata = mapOf(
        // Transaction details
        "orderId" to order.id,
        "totalAmount" to order.total,
        "currency" to order.currency,
        "itemCount" to order.items.size,
        
        // User context
        "userId" to user.id,
        "userTier" to user.membershipTier,
        "isFirstPurchase" to user.isFirstTimeCustomer,
        
        // Technical context
        "appVersion" to BuildConfig.VERSION_NAME,
        "deviceModel" to Build.MODEL,
        "osVersion" to Build.VERSION.RELEASE,
        "connectionType" to networkManager.getConnectionType(),
        
        // Performance metrics
        "processingTime" to processingDuration,
        "retryCount" to retryAttempts,
        "cacheHit" to wasCacheHit
    )
)
```

### Structured Error Metadata

```kotlin
try {
    paymentGateway.processPayment(paymentRequest)
} catch (e: PaymentException) {
    logger.error("Payment", "Payment processing failed", e,
        metadata = mapOf(
            // Error classification
            "errorType" to e.javaClass.simpleName,
            "errorCode" to e.errorCode,
            "isRetryable" to e.isRetryable,
            "httpStatus" to e.httpStatusCode,
            
            // Request context
            "transactionId" to paymentRequest.transactionId,
            "amount" to paymentRequest.amount,
            "currency" to paymentRequest.currency,
            "paymentMethod" to paymentRequest.method,
            
            // Troubleshooting info
            "gatewayEndpoint" to e.gatewayUrl,
            "requestId" to e.requestId,
            "timestamp" to e.timestamp,
            "userAgent" to getUserAgent(),
            
            // Recovery info
            "canRetry" to e.isRetryable,
            "suggestedRetryDelay" to e.retryAfterSeconds,
            "alternativeMethod" to e.suggestedAlternative
        )
    )
}
```

### Performance Tracking Metadata

```kotlin
val startTime = System.currentTimeMillis()

// ... perform operation ...

val endTime = System.currentTimeMillis()

logger.info("Performance", "Operation completed",
    metadata = mapOf(
        "operationType" to "user_authentication",
        "duration" to (endTime - startTime),
        "success" to true,
        "cacheHit" to cacheUsed,
        "networkCalls" to networkCallCount,
        "databaseQueries" to dbQueryCount,
        "memoryUsage" to Runtime.getRuntime().totalMemory(),
        "cpuUsage" to getCpuUsage(),
        "userCount" to activeUserCount
    )
)
```

---

## 🏢 Multiple SDK Management

### Enterprise Multi-SDK Setup

```kotlin
class EnterpriseApp {
    companion object {
        // Different configurations for different SDKs
        private val paymentLogger = createPaymentLogger()
        private val authLogger = createAuthLogger()
        private val analyticsLogger = createAnalyticsLogger()
        private val crashLogger = createCrashLogger()
        
        private fun createPaymentLogger(): SDKLogger {
            val config = SDKLoggerConfig.Builder()
                .setEnabled(true)
                .setMinLogLevel(LogLevel.INFO)
                .addDestination(ConsoleDestination())
                .addDestination(FileDestination(
                    logDirectory = File(context.getExternalFilesDir(null), "payment_logs"),
                    baseFileName = "payment",
                    formatter = JsonLogFormatter()
                ))
                .setAsync(true)
                .build()
                
            return SDKLogger.getInstance("PaymentSDK", "2.1.0", config)
        }
        
        private fun createAuthLogger(): SDKLogger {
            val config = SDKLoggerConfig.Builder()
                .setEnabled(true)
                .setMinLogLevel(LogLevel.DEBUG)
                .addDestination(ConsoleDestination())
                .addDestination(FileDestination(
                    logDirectory = File(context.getExternalFilesDir(null), "auth_logs"),
                    baseFileName = "auth",
                    maxFileSize = 5 * 1024 * 1024L,
                    maxFiles = 3
                ))
                .setMetadataCollection(true)
                .setStackTrace(true)
                .build()
                
            return SDKLogger.getInstance("AuthSDK", "1.5.2", config)
        }
        
        fun getPaymentLogger() = paymentLogger
        fun getAuthLogger() = authLogger
        fun getAnalyticsLogger() = analyticsLogger
    }
}
```

### Cross-SDK Correlation

```kotlin
class OrderProcessor {
    private val paymentLogger = EnterpriseApp.getPaymentLogger()
    private val authLogger = EnterpriseApp.getAuthLogger()
    
    fun processOrder(order: Order) {
        val correlationId = UUID.randomUUID().toString()
        val baseMetadata = mapOf(
            "correlationId" to correlationId,
            "orderId" to order.id,
            "userId" to order.userId
        )
        
        // Auth step
        authLogger.info("OrderFlow", "Validating user session", 
            metadata = baseMetadata + mapOf("step" to "authentication"))
            
        // Payment step  
        paymentLogger.info("OrderFlow", "Processing payment",
            metadata = baseMetadata + mapOf("step" to "payment", "amount" to order.total))
            
        // This allows you to trace the complete flow across SDKs
    }
}
```

### SDK Health Monitoring

```kotlin
class SDKHealthMonitor {
    fun generateHealthReport(): SDKHealthReport {
        val allLoggers = SDKLogger.getAllInstances()
        val healthData = mutableMapOf<String, SDKHealth>()
        
        allLoggers.forEach { (name, logger) ->
            val info = logger.getInfo()
            healthData[name] = SDKHealth(
                name = name,
                isEnabled = info["isEnabled"] as Boolean,
                logLevel = info["minLogLevel"] as String,
                destinations = info["destinations"] as List<String>,
                isAsync = info["enableAsync"] as Boolean,
                bufferSize = info["bufferSize"] as Int
            )
        }
        
        return SDKHealthReport(
            timestamp = System.currentTimeMillis(),
            totalSDKs = allLoggers.size,
            healthData = healthData
        )
    }
}
```

---

## 📁 File Logging

### File Structure

```
/Android/data/com.yourapp/files/
├── payment_logs/
│   ├── payment.log          ← Current log file
│   ├── payment.1.log        ← Previous log file
│   └── payment.2.log        ← Older log file
├── auth_logs/
│   ├── auth.log
│   └── auth.1.log
└── analytics_logs/
    ├── analytics.log
    └── analytics.1.log
```

### File Rotation

```kotlin
FileDestination(
    logDirectory = File(context.getExternalFilesDir(null), "app_logs"),
    baseFileName = "app",
    maxFileSize = 10 * 1024 * 1024L, // 10MB per file
    maxFiles = 5,                    // Keep 5 files maximum
    formatter = DefaultLogFormatter()
)

// When app.log reaches 10MB:
// app.log → app.1.log
// app.1.log → app.2.log  
// app.2.log → app.3.log
// app.3.log → app.4.log
// app.4.log → deleted
// New app.log created
```

### Log Formatters

#### Default Formatter Output
```
2024-06-04 10:15:30.123 [I][PaymentSDK][main] Transaction: Payment processing started
  Metadata: transactionId=txn_123, amount=99.99, currency=USD
  Location: PaymentProcessor.processPayment:145
```

#### JSON Formatter Output
```json
{
  "id": "log_123456789",
  "timestamp": 1717491330123,
  "level": "INFO",
  "tag": "Transaction",
  "message": "Payment processing started",
  "sdkName": "PaymentSDK",
  "sdkVersion": "2.1.0",
  "threadName": "main",
  "className": "PaymentProcessor",
  "methodName": "processPayment", 
  "lineNumber": 145,
  "metadata": {
    "transactionId": "txn_123",
    "amount": 99.99,
    "currency": "USD"
  }
}
```

#### Custom Formatter

```kotlin
class CustomLogFormatter : LogFormatter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    override fun format(logEntry: LogEntry): String {
        val timestamp = dateFormat.format(Date(logEntry.timestamp))
        val level = logEntry.level.tag
        val sdk = logEntry.sdkName
        val message = logEntry.message
        
        return "[$timestamp] $sdk-$level: $message"
    }
}
```

---

## 📊 LogAnalytics API

### Basic Analytics

```kotlin
val analytics = LogAnalytics()

// Generate some logs first
repeat(100) { i ->
    when (i % 4) {
        0 -> logger.info("Test", "Info message $i")
        1 -> logger.debug("Test", "Debug message $i")
        2 -> logger.warning("Test", "Warning message $i")
        3 -> logger.error("Test", "Error message $i", RuntimeException("Test error"))
    }
}

// Get statistics
val stats = analytics.getStatistics()
```

### Statistics Breakdown

```kotlin
val stats = analytics.getStatistics()

// Total counts
val totalLogs = stats["totalLogs"] as Long
println("Total logs: $totalLogs")

// Logs by level
val logsByLevel = stats["logCountsByLevel"] as Map<LogLevel, Long>
logsByLevel.forEach { (level, count) ->
    println("$level: $count logs")
}

// Logs by SDK
val logsBySDK = stats["logCountsBySDK"] as Map<String, Long>
logsBySDK.forEach { (sdk, count) ->
    println("$sdk: $count logs")
}

// Logs by tag/component
val logsByTag = stats["logCountsByTag"] as Map<String, Long>
logsByTag.forEach { (tag, count) ->
    println("$tag: $count logs")
}

// Error types
val errorsByType = stats["errorCountsByType"] as Map<String, Long>
errorsByType.forEach { (errorType, count) ->
    println("$errorType: $count occurrences")
}
```

### Health Assessment

```kotlin
class HealthAnalyzer {
    fun analyzeHealth(stats: Map<String, Any>): HealthReport {
        val totalLogs = stats["totalLogs"] as Long
        val logsByLevel = stats["logCountsByLevel"] as Map<LogLevel, Long>
        
        val errorCount = logsByLevel.getOrDefault(LogLevel.ERROR, 0L)
        val warningCount = logsByLevel.getOrDefault(LogLevel.WARNING, 0L)
        
        val errorRate = if (totalLogs > 0) errorCount.toDouble() / totalLogs else 0.0
        val warningRate = if (totalLogs > 0) warningCount.toDouble() / totalLogs else 0.0
        
        return HealthReport(
            status = when {
                errorRate > 0.1 -> HealthStatus.CRITICAL
                errorRate > 0.05 -> HealthStatus.WARNING  
                warningRate > 0.2 -> HealthStatus.CAUTION
                else -> HealthStatus.HEALTHY
            },
            errorRate = errorRate,
            warningRate = warningRate,
            totalLogs = totalLogs,
            recommendations = generateRecommendations(errorRate, warningRate, stats)
        )
    }
    
    private fun generateRecommendations(
        errorRate: Double, 
        warningRate: Double, 
        stats: Map<String, Any>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (errorRate > 0.1) {
            recommendations.add("🚨 Critical: Error rate above 10% - immediate investigation required")
        }
        
        if (warningRate > 0.3) {
            recommendations.add("⚠️ High warning rate - review warning conditions")
        }
        
        val logsBySDK = stats["logCountsBySDK"] as Map<String, Long>
        val topSDK = logsBySDK.maxByOrNull { it.value }
        if (topSDK != null && topSDK.value > totalLogs * 0.5) {
            recommendations.add("📊 ${topSDK.key} generates >50% of logs - consider reducing verbosity")
        }
        
        return recommendations
    }
}
```

### Performance Metrics

```kotlin
class PerformanceAnalyzer {
    fun analyzePerformance(stats: Map<String, Any>): PerformanceReport {
        val logsByTag = stats["logCountsByTag"] as Map<String, Long>
        val totalLogs = stats["totalLogs"] as Long
        
        // Identify chatty components
        val chattiest = logsByTag.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { 
                ComponentActivity(
                    name = it.key,
                    logCount = it.value,
                    percentage = (it.value.toDouble() / totalLogs) * 100
                )
            }
        
        // Calculate logging overhead
        val avgLogsPerSecond = calculateLogsPerSecond()
        val estimatedOverhead = estimateLoggingOverhead(avgLogsPerSecond)
        
        return PerformanceReport(
            chattiestComponents = chattiest,
            logsPerSecond = avgLogsPerSecond,
            estimatedOverhead = estimatedOverhead,
            recommendations = generatePerformanceRecommendations(chattiest)
        )
    }
}
```

### Business Intelligence

```kotlin
class BusinessAnalyzer {
    fun generateBusinessInsights(stats: Map<String, Any>): BusinessInsights {
        val logsBySDK = stats["logCountsBySDK"] as Map<String, Long>
        val logsByTag = stats["logCountsByTag"] as Map<String, Long>
        
        // Feature usage analysis
        val featureUsage = mapOf(
            "Payment Processing" to logsByTag.getOrDefault("Payment", 0L),
            "User Authentication" to logsByTag.getOrDefault("Auth", 0L),
            "Content Viewing" to logsByTag.getOrDefault("Content", 0L),
            "Social Features" to logsByTag.getOrDefault("Social", 0L),
            "Settings" to logsByTag.getOrDefault("Settings", 0L)
        )
        
        // Investment priorities
        val priorities = generateInvestmentPriorities(featureUsage, stats)
        
        return BusinessInsights(
            featureUsage = featureUsage,
            investmentPriorities = priorities,
            userBehaviorInsights = analyzeUserBehavior(logsByTag),
            riskAssessment = assessRisks(stats)
        )
    }
}
```

---

## 🎛️ Custom Destinations

### Network Destination Example

```kotlin
class NetworkLogDestination(
    private val endpoint: String,
    private val apiKey: String
) : LogDestination {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val jsonFormatter = JsonLogFormatter()
    private val pendingLogs = mutableListOf<LogEntry>()
    private val mutex = Mutex()
    
    override suspend fun writeLog(logEntry: LogEntry) = mutex.withLock {
        pendingLogs.add(logEntry)
        
        // Batch send when we have enough logs
        if (pendingLogs.size >= 10) {
            sendBatch()
        }
    }
    
    private suspend fun sendBatch() {
        if (pendingLogs.isEmpty()) return
        
        val batch = pendingLogs.toList()
        pendingLogs.clear()
        
        try {
            val jsonArray = JSONArray()
            batch.forEach { logEntry ->
                val json = JSONObject(jsonFormatter.format(logEntry))
                jsonArray.put(json)
            }
            
            val requestBody = jsonArray.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Re-add logs to pending for retry
                    mutex.withLock {
                        pendingLogs.addAll(0, batch)
                    }
                }
            }
        } catch (e: Exception) {
            // Re-add logs to pending for retry
            mutex.withLock {
                pendingLogs.addAll(0, batch)
            }
        }
    }
    
    override suspend fun flush() {
        mutex.withLock {
            if (pendingLogs.isNotEmpty()) {
                sendBatch()
            }
        }
    }
    
    override suspend fun close() {
        flush()
        httpClient.dispatcher.executorService.shutdown()
    }
    
    override fun getName(): String = "Network($endpoint)"
}
```

### Database Destination Example

```kotlin
class DatabaseLogDestination(
    private val database: SQLiteDatabase
) : LogDestination {
    
    init {
        createLogTable()
    }
    
    private fun createLogTable() {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS logs (
                id TEXT PRIMARY KEY,
                timestamp INTEGER,
                level TEXT,
                tag TEXT,
                message TEXT,
                sdk_name TEXT,
                sdk_version TEXT,
                thread_name TEXT,
                metadata TEXT,
                exception TEXT
            )
        """.trimIndent()
        
        database.execSQL(createTableSQL)
    }
    
    override suspend fun writeLog(logEntry: LogEntry) {
        val contentValues = ContentValues().apply {
            put("id", logEntry.id)
            put("timestamp", logEntry.timestamp)
            put("level", logEntry.level.name)
            put("tag", logEntry.tag)
            put("message", logEntry.message)
            put("sdk_name", logEntry.sdkName)
            put("sdk_version", logEntry.sdkVersion)
            put("thread_name", logEntry.threadName)
            put("metadata", JSONObject(logEntry.metadata).toString())
            put("exception", logEntry.throwable?.stackTraceToString())
        }
        
        database.insert("logs", null, contentValues)
    }
    
    override suspend fun flush() {
        // Database writes are immediate, no buffering needed
    }
    
    override suspend fun close() {
        database.close()
    }
    
    override fun getName(): String = "Database"
}
```

---

## 🎯 Log Interceptors

Log interceptors allow you to modify or filter logs before they're processed:

### Sensitive Data Masking

```kotlin
class SensitiveDataInterceptor : LogInterceptor {
    private val creditCardRegex = Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b")
    private val emailRegex = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")
    private val phoneRegex = Regex("\\b\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4}\\b")
    
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        val sanitizedMessage = logEntry.message
            .replace(creditCardRegex, "****-****-****-****")
            .replace(emailRegex) { matchResult ->
                val email = matchResult.value
                val atIndex = email.indexOf('@')
                "${email.substring(0, 2)}***${email.substring(atIndex)}"
            }
            .replace(phoneRegex, "***-***-****")
        
        val sanitizedMetadata = logEntry.metadata.mapValues { (key, value) ->
            when {
                key.contains("password", ignoreCase = true) -> "***"
                key.contains("token", ignoreCase = true) -> "***"
                key.contains("secret", ignoreCase = true) -> "***"
                else -> value
            }
        }
        
        return logEntry.copy(
            message = sanitizedMessage,
            metadata = sanitizedMetadata
        )
    }
}
```

### Environment-Based Filtering

```kotlin
class EnvironmentFilterInterceptor : LogInterceptor {
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        return when {
            // In production, skip verbose and debug logs
            !BuildConfig.DEBUG && logEntry.level.priority <= LogLevel.DEBUG.priority -> null
            
            // Skip test logs in production
            !BuildConfig.DEBUG && logEntry.tag.contains("Test", ignoreCase = true) -> null
            
            // Allow all logs in debug builds
            else -> logEntry
        }
    }
}
```

### Metadata Enhancement

```kotlin
class MetadataEnhancementInterceptor(
    private val context: Context
) : LogInterceptor {
    
    override suspend fun intercept(logEntry: LogEntry): LogEntry? {
        val enhancedMetadata = logEntry.metadata.toMutableMap()
        
        // Add system information
        enhancedMetadata["appVersion"] = BuildConfig.VERSION_NAME
        enhancedMetadata["buildType"] = BuildConfig.BUILD_TYPE
        enhancedMetadata["deviceModel"] = Build.MODEL
        enhancedMetadata["osVersion"] = Build.VERSION.RELEASE
        enhancedMetadata["memoryUsage"] = Runtime.getRuntime().totalMemory()
        
        // Add network information
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        enhancedMetadata["networkType"] = networkInfo?.typeName ?: "NONE"
        enhancedMetadata["isConnected"] = networkInfo?.isConnected ?: false
        
        return logEntry.copy(metadata = enhancedMetadata)
    }
}
```

---

## ⚡ Performance Optimization

### Async Logging Configuration

```kotlin
// High-performance configuration
val config = SDKLoggerConfig.Builder()
    .setAsync(true)                    // Enable async processing
    .setBufferSize(500)               // Large buffer for high throughput
    .setFlushInterval(10000L)         // Flush every 10 seconds
    .setMinLogLevel(LogLevel.INFO)    // Skip verbose debug logs
    .build()
```

### Conditional Logging

```kotlin
class PerformanceOptimizedLogger {
    private val logger = SDKLogger.getInstance("OptimizedSDK", "1.0.0")
    
    fun expensiveOperation() {
        // Only calculate expensive debug info if debug logging is enabled
        if (logger.isLoggable(LogLevel.DEBUG)) {
            val expensiveDebugInfo = calculateExpensiveDebugInfo()
            logger.debug("Performance", "Debug info: $expensiveDebugInfo")
        }
        
        // Always log important info
        logger.info("Performance", "Operation completed")
    }
    
    private fun calculateExpensiveDebugInfo(): String {
        // Expensive computation here
        return "expensive_debug_data"
    }
}
```

### Batch Processing

```kotlin
class BatchLogger {
    private val logger = SDKLogger.getInstance("BatchSDK", "1.0.0")
    private val batchBuffer = mutableListOf<LogData>()
    private val maxBatchSize = 50
    
    fun logEvent(event: String, data: Map<String, Any>) {
        batchBuffer.add(LogData(event, data, System.currentTimeMillis()))
        
        if (batchBuffer.size >= maxBatchSize) {
            flushBatch()
        }
    }
    
    private fun flushBatch() {
        logger.info("BatchEvents", "Processing ${batchBuffer.size} events",
            metadata = mapOf(
                "events" to batchBuffer.map { it.event },
                "timespan" to (batchBuffer.last().timestamp - batchBuffer.first().timestamp)
            ))
        
        batchBuffer.clear()
    }
}
```

### Memory Management

```kotlin
class MemoryOptimizedLogger {
    private val logger = SDKLogger.getInstance("MemorySDK", "1.0.0")
    
    fun logLargeData(data: ByteArray) {
        // Don't include large data in metadata
        logger.info("DataProcessing", "Processing large data",
            metadata = mapOf(
                "dataSize" to data.size,
                "dataHash" to data.contentHashCode(),
                "timestamp" to System.currentTimeMillis()
            ))
    }
    
    fun logWithLimitedMetadata(unlimitedData: Map<String, Any>) {
        // Limit metadata size to prevent memory issues
        val limitedMetadata = unlimitedData.entries
            .take(10) // Only take first 10 entries
            .associate { (key, value) ->
                key to when {
                    value is String && value.length > 100 -> value.take(100) + "..."
                    value is Collection<*> && value.size > 10 -> "${value.take(10)}... (${value.size} total)"
                    else -> value
                }
            }
        
        logger.info("DataProcessing", "Processing data", metadata = limitedMetadata)
    }
}
```

---

## 🚨 Error Handling

### Comprehensive Error Logging

```kotlin
class ErrorHandler {
    private val logger = SDKLogger.getInstance("ErrorHandler", "1.0.0")
    
    fun handleNetworkError(operation: String, exception: Exception, context: Map<String, Any>) {
        when (exception) {
            is SocketTimeoutException -> {
                logger.error("Network", "Request timeout during $operation", exception,
                    metadata = context + mapOf(
                        "errorType" to "TIMEOUT",
                        "isRetryable" to true,
                        "suggestedAction" to "RETRY_WITH_BACKOFF"
                    ))
            }
            
            is UnknownHostException -> {
                logger.error("Network", "DNS resolution failed during $operation", exception,
                    metadata = context + mapOf(
                        "errorType" to "DNS_FAILURE",
                        "isRetryable" to false,
                        "suggestedAction" to "CHECK_CONNECTIVITY"
                    ))
            }
            
            is SSLException -> {
                logger.error("Network", "SSL/TLS error during $operation", exception,
                    metadata = context + mapOf(
                        "errorType" to "SSL_ERROR",
                        "isRetryable" to false,
                        "suggestedAction" to "CHECK_CERTIFICATES"
                    ))
            }
            
            else -> {
                logger.error("Network", "Unknown network error during $operation", exception,
                    metadata = context + mapOf(
                        "errorType" to "UNKNOWN",
                        "isRetryable" to true,
                        "suggestedAction" to "GENERIC_RETRY"
                    ))
            }
        }
    }
}
```

### Error Recovery Patterns

```kotlin
class ResilientOperation {
    private val logger = SDKLogger.getInstance("ResilientSDK", "1.0.0")
    
    suspend fun performWithRetry(operation: suspend () -> Result<String>): Result<String> {
        val maxRetries = 3
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                logger.info("Retry", "Attempting operation (attempt ${attempt + 1}/$maxRetries)")
                
                val result = operation()
                if (result.isSuccess) {
                    logger.info("Retry", "Operation succeeded on attempt ${attempt + 1}",
                        metadata = mapOf("totalAttempts" to attempt + 1))
                    return result
                }
                
            } catch (e: Exception) {
                lastException = e
                logger.warning("Retry", "Operation failed on attempt ${attempt + 1}", e,
                    metadata = mapOf(
                        "attempt" to attempt + 1,
                        "maxRetries" to maxRetries,
                        "willRetry" to (attempt < maxRetries - 1)
                    ))
                
                if (attempt < maxRetries - 1) {
                    val delay = calculateBackoffDelay(attempt)
                    logger.debug("Retry", "Waiting ${delay}ms before retry")
                    delay(delay)
                }
            }
        }
        
        logger.error("Retry", "All retry attempts failed", lastException,
            metadata = mapOf("totalAttempts" to maxRetries))
        
        return Result.failure(lastException ?: RuntimeException("All retries failed"))
    }
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        return (1000L * (1L shl attempt)) // Exponential backoff: 1s, 2s, 4s, etc.
    }
}
```

---

## 🏭 Production Deployment

### Production Configuration

```kotlin
class ProductionLoggerSetup {
    companion object {
        fun createProductionLogger(context: Context, sdkName: String, version: String): SDKLogger {
            val config = SDKLoggerConfig.Builder()
                // Only enable logging in debug builds or for internal users
                .setEnabled(BuildConfig.DEBUG || isInternalUser())
                
                // Reduce log verbosity in production
                .setMinLogLevel(if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARNING)
                
                // Always log to console for debugging
                .addDestination(ConsoleDestination())
                
                // File logging only for internal builds
                .apply {
                    if (BuildConfig.DEBUG || isInternalUser()) {
                        addDestination(createFileDestination(context, sdkName))
                    }
                }
                
                // Add network logging for crash reports (if enabled)
                .apply {
                    if (isCrashReportingEnabled()) {
                        addDestination(createCrashReportingDestination())
                    }
                }
                
                // Performance settings
                .setAsync(true)
                .setBufferSize(100)
                .setFlushInterval(30000L) // 30 seconds
                
                // Privacy settings
                .addInterceptor(SensitiveDataInterceptor())
                .addInterceptor(EnvironmentFilterInterceptor())
                
                // Resource management
                .setMaxLogFileSize(5 * 1024 * 1024L) // 5MB
                .setMaxLogFiles(3)
                
                .build()
            
            return SDKLogger.getInstance(sdkName, version, config)
        }
        
        private fun createFileDestination(context: Context, sdkName: String): FileDestination {
            val logDir = File(context.getExternalFilesDir(null), "production_logs")
            return FileDestination(
                logDirectory = logDir,
                baseFileName = sdkName.lowercase(),
                maxFileSize = 5 * 1024 * 1024L,
                maxFiles = 3,
                formatter = JsonLogFormatter() // Structured for analysis
            )
        }
        
        private fun isInternalUser(): Boolean {
            // Check if user is internal tester
            return BuildConfig.DEBUG || isInternalBuild()
        }
        
        private fun isInternalBuild(): Boolean {
            return BuildConfig.BUILD_TYPE == "internal" || BuildConfig.FLAVOR == "internal"
        }
        
        private fun isCrashReportingEnabled(): Boolean {
            // Check if crash reporting is enabled
            return true // Or check user preferences
        }
    }
}
```

### Monitoring and Alerting

```kotlin
class ProductionMonitoring {
    private val logger = SDKLogger.getInstance("MonitoringSDK", "1.0.0")
    private val analytics = LogAnalytics()
    
    // Check system health every 5 minutes
    fun setupHealthMonitoring() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkSystemHealth()
            }
        }, 0, 5 * 60 * 1000) // 5 minutes
    }
    
    private fun checkSystemHealth() {
        val stats = analytics.getStatistics()
        val healthReport = analyzeHealth(stats)
        
        when (healthReport.status) {
            HealthStatus.CRITICAL -> {
                sendCriticalAlert(healthReport)
                logger.assert("Health", "Critical system health detected", 
                    metadata = healthReport.toMap())
            }
            
            HealthStatus.WARNING -> {
                sendWarningAlert(healthReport)
                logger.warning("Health", "System health warning",
                    metadata = healthReport.toMap())
            }
            
            HealthStatus.HEALTHY -> {
                logger.info("Health", "System health check passed",
                    metadata = mapOf("errorRate" to healthReport.errorRate))
            }
        }
    }
    
    private fun sendCriticalAlert(report: HealthReport) {
        // Send to monitoring service (PagerDuty, Slack, etc.)
        logger.error("Alert", "Critical alert sent to monitoring service",
            metadata = mapOf(
                "alertType" to "CRITICAL_HEALTH",
                "errorRate" to report.errorRate,
                "totalErrors" to report.totalErrors
            ))
    }
}
```

---

## 🔍 Debugging and Troubleshooting

### Debug Mode Setup

```kotlin
val debugConfig = SDKLoggerConfig.Builder()
    .setEnabled(true)
    .setMinLogLevel(LogLevel.VERBOSE)
    .addDestination(ConsoleDestination())
    .addDestination(FileDestination(
        logDirectory = File(context.getExternalFilesDir(null), "debug_logs"),
        formatter = JsonLogFormatter()
    ))
    .setAsync(false) // Synchronous for easier debugging
    .setMetadataCollection(true)
    .setStackTrace(true)
    .build()
```

### Common Issues and Solutions

#### Issue: Logs not appearing in Logcat
```kotlin
// Solution: Check configuration
val logger = SDKLogger.getInstance("TestSDK", "1.0.0")
val info = logger.getInfo()
println("Logger enabled: ${info["isEnabled"]}")
println("Min log level: ${info["minLogLevel"]}")
println("Destinations: ${info["destinations"]}")
```

#### Issue: File logs not created
```kotlin
// Solution: Verify permissions and directory
val logDir = File(context.getExternalFilesDir(null), "logs")
if (!logDir.exists()) {
    val created = logDir.mkdirs()
    logger.info("Setup", "Log directory created: $created")
}

// Check if directory is writable
if (logDir.canWrite()) {
    logger.info("Setup", "Log directory is writable")
} else {
    logger.error("Setup", "Log directory is not writable")
}
```

#### Issue: Performance problems
```kotlin
// Solution: Optimize configuration
val optimizedConfig = SDKLoggerConfig.Builder()
    .setAsync(true)                    // Enable async
    .setBufferSize(200)               // Increase buffer
    .setMinLogLevel(LogLevel.INFO)    // Reduce verbosity
    .setFlushInterval(10000L)         // Less frequent flushing
    .build()
```

---

## 📋 API Reference

### SDKLogger Methods

```kotlin
class SDKLogger {
    // Log methods
    fun verbose(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap())
    fun debug(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap())
    fun info(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap())
    fun warning(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap())
    fun error(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap())
    fun assert(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap())
    
    // Utility methods
    suspend fun flush()
    suspend fun shutdown()
    suspend fun updateConfig(newConfig: SDKLoggerConfig)
    fun getInfo(): Map<String, Any>
    fun isLoggable(level: LogLevel): Boolean
    
    // Static methods
    companion object {
        fun getInstance(sdkName: String, sdkVersion: String = "1.0.0", config: SDKLoggerConfig = SDKLoggerConfig()): SDKLogger
        fun getAllInstances(): Map<String, SDKLogger>
        suspend fun shutdownAll()
    }
}
```

### LogAnalytics Methods

```kotlin
class LogAnalytics {
    fun recordLog(logEntry: LogEntry)
    fun getStatistics(): Map<String, Any>
    fun reset()
}
```

---

**Next:** Check out [Best Practices](BEST_PRACTICES.md) for production deployment guidelines.
