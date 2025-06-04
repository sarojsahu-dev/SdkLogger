# SDKLogger 🚀

[![](https://jitpack.io/v/sarojsahu-dev/SdkLogger.svg)](https://jitpack.io/#sarojsahu-dev/SdkLogger)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**SDKLogger** is a comprehensive, high-performance logging system designed specifically for Android SDKs and applications. It provides enterprise-grade logging capabilities with advanced analytics, multiple destinations, and production-ready features.

## ✨ Features

### 🎯 **Core Features**
- **Multi-SDK Support** - Track logs from different SDKs separately
- **Rich Metadata** - Include context, timestamps, thread info, and custom data
- **Multiple Log Levels** - VERBOSE, DEBUG, INFO, WARNING, ERROR, ASSERT
- **Thread-Safe** - Concurrent logging with coroutine-based async processing
- **High Performance** - Buffered channels and optimized I/O operations

### 📊 **Advanced Analytics**
- **LogAnalytics** - Real-time insights into your app's health
- **Error Rate Monitoring** - Automatic calculation of error percentages
- **SDK Activity Tracking** - Monitor which SDKs are most/least active
- **Performance Metrics** - Track logging overhead and performance

### 🎛️ **Flexible Destinations**
- **Console Logging** - Android Logcat integration
- **File Logging** - Rotating log files with size management
- **Custom Destinations** - Extensible architecture for network, database, etc.
- **Multiple Formatters** - Default, JSON, and custom formatting options

### 🔧 **Production Ready**
- **Log File Rotation** - Automatic file size and count management
- **Resource Management** - Proper cleanup and memory handling
- **Configuration Management** - Runtime configuration updates
- **Error Handling** - Graceful failure recovery

## 📦 Installation

### Step 1: Add JitPack Repository

Add JitPack to your root `settings.gradle` file:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Dependency

Add the dependency to your module's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.github.sarojsahu-dev:SdkLogger:0.0.1'
    
    // Required dependencies
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

### Step 3: Permissions (Optional)

For file logging, add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 🚀 Quick Start

### Basic Usage

```kotlin
class MySDK {
    // Create logger instance for your SDK
    private val logger = SDKLogger.getInstance("MySDK", "1.0.0")
    
    fun performOperation() {
        logger.info("Operation", "Starting operation...")
        
        try {
            // Your SDK logic here
            logger.debug("Operation", "Processing data...")
            // Success
            logger.info("Operation", "Operation completed successfully")
        } catch (e: Exception) {
            logger.error("Operation", "Operation failed", e)
        }
    }
}
```

### Advanced Usage with Metadata

```kotlin
class PaymentSDK {
    private val logger = SDKLogger.getInstance("PaymentSDK", "2.1.0")
    
    fun processPayment(amount: Double, currency: String): PaymentResult {
        val transactionId = generateTransactionId()
        
        logger.info("Payment", "Processing payment",
            metadata = mapOf(
                "transactionId" to transactionId,
                "amount" to amount,
                "currency" to currency,
                "timestamp" to System.currentTimeMillis()
            )
        )
        
        return try {
            val result = performPayment(amount, currency)
            
            logger.info("Payment", "Payment successful",
                metadata = mapOf(
                    "transactionId" to transactionId,
                    "status" to "SUCCESS",
                    "processingTime" to result.processingTime
                )
            )
            
            result
        } catch (e: PaymentException) {
            logger.error("Payment", "Payment failed", e,
                metadata = mapOf(
                    "transactionId" to transactionId,
                    "errorCode" to e.errorCode,
                    "status" to "FAILED"
                )
            )
            throw e
        }
    }
}
```

## 📖 Detailed Usage Guide

### 1. Logger Configuration

#### Simple Configuration (Console Only)
```kotlin
val logger = SDKLogger.getInstance("MySDK", "1.0.0")
// Uses default configuration - console logging only
```

#### Advanced Configuration
```kotlin
val logDir = File(context.getExternalFilesDir(null), "sdk_logs")

val config = SDKLoggerConfig.Builder()
    .setEnabled(true)
    .setMinLogLevel(LogLevel.DEBUG)
    .addDestination(ConsoleDestination())
    .addDestination(FileDestination(
        logDirectory = logDir,
        baseFileName = "my_sdk_logs",
        maxFileSize = 10 * 1024 * 1024L, // 10MB
        maxFiles = 5,
        formatter = JsonLogFormatter()
    ))
    .setAsync(true)
    .setBufferSize(100)
    .setFlushInterval(5000L)
    .build()

val logger = SDKLogger.getInstance("MySDK", "1.0.0", config)
```

#### Production Configuration
```kotlin
val config = SDKLoggerConfig.Builder()
    .setEnabled(BuildConfig.DEBUG) // Only in debug builds
    .setMinLogLevel(LogLevel.INFO)
    .addDestination(ConsoleDestination())
    .apply {
        if (BuildConfig.DEBUG) {
            addDestination(FileDestination(File(context.filesDir, "logs")))
        }
    }
    .setMetadataCollection(true)
    .setStackTrace(true)
    .build()
```

### 2. Log Levels

```kotlin
// VERBOSE - Most detailed, for fine-grained debugging
logger.verbose("Network", "HTTP request headers prepared")

// DEBUG - Development debugging information
logger.debug("Parser", "Parsing JSON response: ${response.length} bytes")

// INFO - General information about app flow
logger.info("Authentication", "User login successful")

// WARNING - Potentially harmful situations
logger.warning("Cache", "Cache hit rate below 80%: ${hitRate}%")

// ERROR - Error events that might still allow app to continue
logger.error("Database", "Failed to save user preferences", exception)

// ASSERT - Critical errors that should never happen
logger.assert("Security", "Unauthorized access attempt detected")
```

### 3. Rich Metadata Logging

```kotlin
// User authentication with context
logger.info("Auth", "User login attempt",
    metadata = mapOf(
        "userId" to user.id,
        "loginMethod" to "oauth",
        "provider" to "google",
        "deviceType" to "android",
        "appVersion" to BuildConfig.VERSION_NAME,
        "sessionId" to sessionManager.getCurrentSessionId()
    )
)

// API call with performance metrics
logger.debug("API", "REST API call completed",
    metadata = mapOf(
        "endpoint" to "/api/v1/users",
        "method" to "GET",
        "statusCode" to response.code,
        "responseTime" to responseTime,
        "contentLength" to response.body?.contentLength(),
        "retryCount" to retryCount
    )
)

// Error with troubleshooting context
logger.error("Payment", "Transaction failed", exception,
    metadata = mapOf(
        "transactionId" to transactionId,
        "amount" to amount,
        "currency" to currency,
        "paymentMethod" to paymentMethod,
        "merchantId" to merchantId,
        "errorCode" to exception.errorCode,
        "userAgent" to userAgent
    )
)
```

### 4. Multiple SDK Support

```kotlin
class ECommerceApp {
    // Different loggers for different components
    private val paymentLogger = SDKLogger.getInstance("PaymentSDK", "2.1.0")
    private val authLogger = SDKLogger.getInstance("AuthSDK", "1.5.2")
    private val analyticsLogger = SDKLogger.getInstance("AnalyticsSDK", "3.0.1")
    
    fun checkout() {
        // Each SDK logs to its own namespace
        authLogger.info("Session", "Validating user session")
        paymentLogger.info("Checkout", "Initiating payment process")
        analyticsLogger.info("Event", "Checkout started")
    }
}
```

### 5. LogAnalytics - Health Monitoring

```kotlin
class AppHealthMonitor {
    private val logAnalytics = LogAnalytics()
    
    fun checkAppHealth(): HealthReport {
        val stats = logAnalytics.getStatistics()
        
        return HealthReport(
            totalLogs = stats["totalLogs"] as Long,
            logsByLevel = stats["logCountsByLevel"] as Map<LogLevel, Long>,
            logsBySDK = stats["logCountsBySDK"] as Map<String, Long>,
            errorTypes = stats["errorCountsByType"] as Map<String, Long>,
            healthStatus = calculateHealthStatus(stats)
        )
    }
    
    private fun calculateHealthStatus(stats: Map<String, Any>): HealthStatus {
        val totalLogs = stats["totalLogs"] as Long
        val errorCount = (stats["logCountsByLevel"] as Map<LogLevel, Long>)
            .getOrDefault(LogLevel.ERROR, 0L)
        
        val errorRate = if (totalLogs > 0) errorCount.toDouble() / totalLogs else 0.0
        
        return when {
            errorRate > 0.1 -> HealthStatus.CRITICAL
            errorRate > 0.05 -> HealthStatus.WARNING
            else -> HealthStatus.HEALTHY
        }
    }
}
```

### 6. Custom Destinations

```kotlin
class NetworkLogDestination(private val endpoint: String) : LogDestination {
    private val httpClient = OkHttpClient()
    
    override suspend fun writeLog(logEntry: LogEntry) {
        val json = JsonLogFormatter().format(logEntry)
        
        val request = Request.Builder()
            .url(endpoint)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
            
        httpClient.newCall(request).execute()
    }
    
    override suspend fun flush() {
        // Flush any pending network requests
    }
    
    override suspend fun close() {
        httpClient.dispatcher.executorService.shutdown()
    }
    
    override fun getName(): String = "Network($endpoint)"
}

// Usage
val config = SDKLoggerConfig.Builder()
    .addDestination(ConsoleDestination())
    .addDestination(NetworkLogDestination("https://logs.myapp.com/api/logs"))
    .build()
```

### 7. Performance Testing

```kotlin
class PerformanceTester {
    private val logger = SDKLogger.getInstance("PerformanceSDK", "1.0.0")
    
    suspend fun testAsyncPerformance() {
        val startTime = System.currentTimeMillis()
        
        // Log 1000 messages rapidly
        repeat(1000) { i ->
            logger.info("Performance", "Async test message $i",
                metadata = mapOf(
                    "messageIndex" to i,
                    "batchId" to "perf_test_${System.currentTimeMillis()}"
                )
            )
        }
        
        val duration = System.currentTimeMillis() - startTime
        logger.info("Performance", "Async test completed",
            metadata = mapOf(
                "totalMessages" to 1000,
                "durationMs" to duration,
                "averageMs" to duration.toDouble() / 1000.0,
                "messagesPerSecond" to 1000.0 / (duration / 1000.0)
            )
        )
    }
}
```

## 🔧 Configuration Options

### SDKLoggerConfig.Builder Options

| Method | Description | Default |
|--------|-------------|---------|
| `setEnabled(Boolean)` | Enable/disable logging | `true` |
| `setMinLogLevel(LogLevel)` | Minimum log level to process | `VERBOSE` |
| `addDestination(LogDestination)` | Add log destination | `ConsoleDestination()` |
| `setFormatter(LogFormatter)` | Set log formatter | `DefaultLogFormatter()` |
| `addInterceptor(LogInterceptor)` | Add log interceptor | None |
| `setMetadataCollection(Boolean)` | Enable metadata collection | `true` |
| `setStackTrace(Boolean)` | Enable stack trace capture | `true` |
| `setBufferSize(Int)` | Set async buffer size | `100` |
| `setFlushInterval(Long)` | Set flush interval (ms) | `5000` |
| `setAsync(Boolean)` | Enable async processing | `true` |
| `setMaxLogFileSize(Long)` | Max log file size (bytes) | `10MB` |
| `setMaxLogFiles(Int)` | Max number of log files | `5` |

### FileDestination Options

| Parameter | Description | Default |
|-----------|-------------|---------|
| `logDirectory` | Directory for log files | Required |
| `baseFileName` | Base name for log files | `"sdk_logs"` |
| `maxFileSize` | Maximum file size before rotation | `10MB` |
| `maxFiles` | Maximum number of files to keep | `5` |
| `formatter` | Log formatter to use | `DefaultLogFormatter()` |

## 📊 LogAnalytics API

### Available Statistics

```kotlin
val stats = logAnalytics.getStatistics()

// Returns Map<String, Any> with:
stats["totalLogs"]           // Long: Total number of logs
stats["logCountsByLevel"]    // Map<LogLevel, Long>: Logs per level
stats["logCountsBySDK"]      // Map<String, Long>: Logs per SDK
stats["logCountsByTag"]      // Map<String, Long>: Logs per tag/component
stats["errorCountsByType"]   // Map<String, Long>: Error count by exception type
```

### Health Monitoring

```kotlin
fun generateHealthReport(): String {
    val stats = logAnalytics.getStatistics()
    val totalLogs = stats["totalLogs"] as Long
    val errorCount = (stats["logCountsByLevel"] as Map<LogLevel, Long>)
        .getOrDefault(LogLevel.ERROR, 0L)
    
    val errorRate = if (totalLogs > 0) errorCount.toDouble() / totalLogs else 0.0
    
    return when {
        errorRate > 0.1 -> "🔴 CRITICAL: Error rate ${(errorRate * 100).format(1)}%"
        errorRate > 0.05 -> "🟡 WARNING: Error rate ${(errorRate * 100).format(1)}%"
        else -> "🟢 HEALTHY: Error rate ${(errorRate * 100).format(1)}%"
    }
}
```

## 🎯 Best Practices

### 1. SDK Initialization

```kotlin
class MySDK {
    companion object {
        private lateinit var logger: SDKLogger
        
        fun initialize(context: Context, debugMode: Boolean = false) {
            val config = SDKLoggerConfig.Builder()
                .setEnabled(debugMode)
                .setMinLogLevel(if (debugMode) LogLevel.DEBUG else LogLevel.INFO)
                .addDestination(ConsoleDestination())
                .apply {
                    if (debugMode) {
                        addDestination(FileDestination(
                            File(context.getExternalFilesDir(null), "my_sdk_logs")
                        ))
                    }
                }
                .build()
                
            logger = SDKLogger.getInstance("MySDK", BuildConfig.VERSION_NAME, config)
            logger.info("SDK", "MySDK initialized successfully")
        }
        
        internal fun getLogger(): SDKLogger = logger
    }
}
```

### 2. Structured Logging

```kotlin
// Good: Structured with consistent metadata
logger.info("UserAction", "Button clicked",
    metadata = mapOf(
        "buttonId" to "checkout_btn",
        "screen" to "product_detail",
        "userId" to currentUser.id,
        "timestamp" to System.currentTimeMillis()
    )
)

// Avoid: Unstructured string concatenation
logger.info("UserAction", "User ${currentUser.id} clicked checkout_btn on product_detail screen")
```

### 3. Error Handling

```kotlin
fun performNetworkCall() {
    try {
        val response = apiClient.getData()
        logger.info("Network", "API call successful",
            metadata = mapOf(
                "endpoint" to "/api/data",
                "statusCode" to response.code,
                "responseTime" to response.responseTime
            )
        )
    } catch (e: NetworkException) {
        logger.error("Network", "API call failed", e,
            metadata = mapOf(
                "endpoint" to "/api/data",
                "errorType" to e.javaClass.simpleName,
                "retryAttempt" to retryCount,
                "isRetryable" to e.isRetryable
            )
        )
        // Handle error appropriately
    }
}
```

### 4. Performance Considerations

```kotlin
// Good: Use async logging for high-frequency logs
val config = SDKLoggerConfig.Builder()
    .setAsync(true)
    .setBufferSize(200)
    .build()

// Good: Conditional verbose logging
if (logger.isLoggable(LogLevel.VERBOSE)) {
    logger.verbose("Parser", "Detailed parsing info: ${expensiveOperation()}")
}

// Avoid: Synchronous logging in performance-critical paths
// Avoid: Expensive operations in log messages without conditional checks
```

### 5. Production Deployment

```kotlin
val config = SDKLoggerConfig.Builder()
    .setEnabled(BuildConfig.DEBUG || isInternalBuild())
    .setMinLogLevel(if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARNING)
    .addDestination(ConsoleDestination())
    .apply {
        // Only add file logging for internal builds
        if (isInternalBuild()) {
            addDestination(FileDestination(getLogDirectory()))
        }
    }
    .setMetadataCollection(BuildConfig.DEBUG)
    .build()
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Logs Not Appearing in Logcat

**Problem**: No logs visible in Android Studio Logcat

**Solutions**:
- Check if logging is enabled: `config.isEnabled`
- Verify log level: Set `minLogLevel` to `VERBOSE`
- Check Logcat filters: Search for your SDK name (e.g., "MySDK")
- Ensure you're using `ConsoleDestination`

```kotlin
// Debug configuration
val config = SDKLoggerConfig.Builder()
    .setEnabled(true)
    .setMinLogLevel(LogLevel.VERBOSE)
    .addDestination(ConsoleDestination())
    .build()
```

#### 2. File Logging Not Working

**Problem**: Log files not created or empty

**Solutions**:
- Check permissions: `WRITE_EXTERNAL_STORAGE`
- Verify directory exists and is writable
- Call `flush()` to ensure logs are written
- Check file destination configuration

```kotlin
// Ensure directory exists
val logDir = File(context.getExternalFilesDir(null), "logs")
if (!logDir.exists()) {
    logDir.mkdirs()
}

val config = SDKLoggerConfig.Builder()
    .addDestination(FileDestination(logDir))
    .setFlushInterval(1000L) // Flush every second
    .build()
```

#### 3. Performance Issues

**Problem**: App slowing down due to logging

**Solutions**:
- Enable async logging: `setAsync(true)`
- Increase buffer size: `setBufferSize(500)`
- Reduce log verbosity in production
- Use conditional logging for expensive operations

```kotlin
val config = SDKLoggerConfig.Builder()
    .setAsync(true)
    .setBufferSize(500)
    .setMinLogLevel(LogLevel.INFO) // Skip DEBUG and VERBOSE
    .build()
```

#### 4. Memory Issues

**Problem**: High memory usage from logging

**Solutions**:
- Limit metadata size
- Reduce buffer size if needed
- Enable log file rotation
- Regular cleanup of old log files

```kotlin
val config = SDKLoggerConfig.Builder()
    .setBufferSize(100) // Smaller buffer
    .setMaxLogFileSize(5 * 1024 * 1024L) // 5MB max
    .setMaxLogFiles(3) // Keep only 3 files
    .build()
```

### Debug Mode Setup

```kotlin
// Enable comprehensive logging for debugging
val debugConfig = SDKLoggerConfig.Builder()
    .setEnabled(true)
    .setMinLogLevel(LogLevel.VERBOSE)
    .addDestination(ConsoleDestination())
    .addDestination(FileDestination(
        logDirectory = File(context.getExternalFilesDir(null), "debug_logs"),
        formatter = JsonLogFormatter() // Structured format for analysis
    ))
    .setMetadataCollection(true)
    .setStackTrace(true)
    .setAsync(false) // Synchronous for easier debugging
    .build()
```

## 📱 Sample App

Check out our [sample app](https://github.com/sarojsahu-dev/SdkLogger/tree/main/app) for a complete implementation example with:

- Multiple SDK configurations
- Interactive logging demonstrations  
- LogAnalytics dashboard
- Performance testing
- Real-world usage patterns

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository
2. Open in Android Studio
3. Run the sample app to test changes
4. Submit a pull request

### Reporting Issues

Please use [GitHub Issues](https://github.com/sarojsahu-dev/SdkLogger/issues) to report bugs or request features.

## 📄 License

```
MIT License

Copyright (c) 2024 Saroj Sahu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🙏 Acknowledgments

- Built with ❤️ for the Android developer community
- Inspired by modern logging frameworks and enterprise needs
---

**Happy Logging!** 🚀

For more examples and updates, follow [@sarojsahu-dev](https://github.com/sarojsahu-dev)
