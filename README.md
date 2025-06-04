# SDKLogger 🚀

[![](https://jitpack.io/v/sarojsahu-dev/SdkLogger.svg)](https://jitpack.io/#sarojsahu-dev/SdkLogger)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A comprehensive, high-performance logging system designed for Android SDKs and applications. Enterprise-grade logging with analytics, multiple destinations, and production-ready features.

## ✨ Key Features

- 🎯 **Multi-SDK Support** - Track logs from different SDKs separately
- 📊 **Rich Analytics** - Real-time insights and health monitoring  
- 🚀 **High Performance** - Async logging with coroutines
- 📁 **Multiple Destinations** - Console, File, Custom (Network, Database)
- 🔧 **Production Ready** - File rotation, error handling, resource management

## 📦 Installation

### Step 1: Add JitPack Repository

Add to your root `settings.gradle`:

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

Add to your module's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.sarojsahu-dev:SdkLogger:0.0.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

### Step 3: Permissions (For File Logging)

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 🚀 Quick Start

### Basic Usage (2 minutes setup)

```kotlin
class MySDK {
    // Simple logger setup
    private val logger = SDKLogger.getInstance("MySDK", "1.0.0")
    
    fun performOperation() {
        logger.info("Operation", "Started")
        
        try {
            // Your code here
            logger.info("Operation", "Success")
        } catch (e: Exception) {
            logger.error("Operation", "Failed", e)
        }
    }
}
```

### With Metadata (Rich logging)

```kotlin
logger.info("Payment", "Processing payment",
    metadata = mapOf(
        "transactionId" to "txn_12345",
        "amount" to 99.99,
        "currency" to "USD"
    )
)
```

## ⚙️ Configuration Options

### Console Only (Default)
```kotlin
val logger = SDKLogger.getInstance("MySDK", "1.0.0")
// Ready to use - logs to Android Logcat
```

### Console + File Logging
```kotlin
val config = SDKLoggerConfig.Builder()
    .addDestination(ConsoleDestination())
    .addDestination(FileDestination(
        logDirectory = File(context.getExternalFilesDir(null), "logs"),
        maxFileSize = 10 * 1024 * 1024L, // 10MB
        maxFiles = 5
    ))
    .build()

val logger = SDKLogger.getInstance("MySDK", "1.0.0", config)
```

### Production Ready
```kotlin
val config = SDKLoggerConfig.Builder()
    .setEnabled(BuildConfig.DEBUG) // Only in debug builds
    .setMinLogLevel(LogLevel.INFO)
    .setAsync(true) // High performance
    .build()
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

```kotlin
FileDestination(
    logDirectory = File(context.filesDir, "my_logs"),
    baseFileName = "app_logs",
    maxFileSize = 5 * 1024 * 1024L, // 5MB
    maxFiles = 3,
    formatter = JsonLogFormatter() // or DefaultLogFormatter()
)
```

## 🔍 How to View Logs

### Android Studio Logcat
1. Open **Logcat** tab
2. Filter by your SDK name (e.g., "MySDK")
3. See logs in real-time

### Log Files
- **Location**: `/Android/data/com.yourapp/files/logs/`
- **Format**: `app_logs.log`, `app_logs.1.log`, etc.
- **Rotation**: Automatic when size limit reached

## 📊 Log Analytics (Health Monitoring)

```kotlin
val analytics = LogAnalytics()

// Get insights
val stats = analytics.getStatistics()
val totalLogs = stats["totalLogs"] // Total log count
val errorRate = calculateErrorRate(stats) // Error percentage
val topSDKs = stats["logCountsBySDK"] // Most active SDKs

// Health check
when {
    errorRate > 0.1 -> "🔴 CRITICAL" 
    errorRate > 0.05 -> "🟡 WARNING"
    else -> "🟢 HEALTHY"
}
```

## 📚 Documentation

For detailed implementation guides and advanced features:

- **[📖 Complete Documentation](docs/README.md)** - Full API reference and examples
- **[🎯 Best Practices](docs/BEST_PRACTICES.md)** - Production deployment guide

## 💡 Examples

- **[📱 Sample App](app/)** - Complete demo with all features

## 🤝 Contributing

We welcome contributions! Here's how you can help make SDKLogger better:

### 🚀 Quick Start for Contributors

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/sarojsahu-dev/SdkLogger.git`
3. **Create** a feature branch: `git checkout -b feature/amazing-feature`
4. **Make** your changes
5. **Test** thoroughly
6. **Commit** with clear messages: `git commit -m 'Add amazing feature'`
7. **Push** to your branch: `git push origin feature/amazing-feature`
8. **Create** a Pull Request

### 💡 How to Contribute

- **🐛 Bug Reports** - Found a bug? [Open an issue](https://github.com/sarojsahu-dev/SdkLogger/issues)
- **✨ Feature Requests** - Have an idea? [Start a discussion](https://github.com/sarojsahu-dev/SdkLogger/discussions)
- **📖 Documentation** - Improve docs, add examples, fix typos
- **🧪 Testing** - Add tests, improve coverage
- **💻 Code** - Fix bugs, add features, optimize performance

### 📋 Development Guidelines

- Follow **Kotlin coding standards**
- Add **tests** for new features
- Update **documentation** for changes
- Ensure **backward compatibility**
- Keep **performance** in mind

### 🏗️ Areas We Need Help

- **Custom Destinations** (Database, Network, Cloud)
- **Advanced Analytics** features
- **Performance Optimizations**
- **Documentation** improvements
- **Example Projects** and tutorials

---

## 🗺️ Roadmap & Future Plans

### 🔮 Upcoming Features

#### **v0.1.0 - Enhanced Analytics** *(Next Release)*
- 📊 **Real-time Dashboard** - Web-based logging dashboard
- 📈 **Advanced Metrics** - Performance insights and trends
- 🔔 **Smart Alerts** - Intelligent error detection
- 📱 **Mobile Dashboard** - In-app log viewer

#### **v0.2.0 - Cloud Integration**
- ☁️ **Cloud Destinations** - Firebase, AWS CloudWatch, Azure
- 🌐 **Remote Configuration** - Dynamic logging config
- 🔄 **Log Streaming** - Real-time log streaming
- 📤 **Bulk Upload** - Efficient cloud synchronization

#### **v0.3.0 - Advanced Features**
- 🤖 **AI-Powered Insights** - Automatic issue detection
- 🔍 **Log Search & Query** - Powerful search capabilities
- 📊 **Custom Visualizations** - Charts and graphs
- 🎯 **Predictive Analytics** - Trend prediction

### 💡 Future Innovations

- **🌟 LogQL Support** - Query language for logs
- **🔐 End-to-End Encryption** - Advanced security
- **📱 Cross-Platform** - iOS and Flutter support
- **🧩 Plugin Marketplace** - Community plugins
- **📋 Compliance Tools** - GDPR, HIPAA automation

### 🎯 Vision

Create the **most comprehensive and developer-friendly logging solution** for mobile applications, with enterprise-grade features accessible to developers of all levels.

---

## 🌟 Community & Feedback

Join our growing community of developers:

- 💬 **[Discussions](https://github.com/sarojsahu-dev/SdkLogger/discussions)** - Ask questions, share ideas
- 🐛 **[Issues](https://github.com/sarojsahu-dev/SdkLogger/issues)** - Report bugs, request features
- 📧 **[Email](sarojsahu014@gmail.com)** - Direct feedback and suggestions
- ⭐ **Star this repo** if SDKLogger helps your project!
