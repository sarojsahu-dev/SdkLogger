package com.example.logger.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logger.sdklogger.core.SDKLogger

@Composable
fun AdvancedLoggingSection(logger: SDKLogger) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎯 Advanced Logging",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LogButton("Log with Metadata") {
                logger.info("Purchase", "User made a purchase",
                    metadata = mapOf(
                        "productId" to "12345",
                        "amount" to 99.99,
                        "currency" to "USD",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            LogButton("Log with Exception") {
                val exception = RuntimeException("Sample network error")
                logger.error("Network", "Failed to connect to server",
                    throwable = exception,
                    metadata = mapOf(
                        "url" to "https://api.example.com",
                        "timeout" to 30000,
                        "retryAttempt" to 3
                    )
                )
            }

            LogButton("User Login Flow") {
                val sessionId = "session_${System.currentTimeMillis()}"
                logger.info("Auth", "User login started",
                    metadata = mapOf("sessionId" to sessionId))
                logger.info("Auth", "Credentials validated",
                    metadata = mapOf("sessionId" to sessionId))
                logger.info("Auth", "Login successful",
                    metadata = mapOf("sessionId" to sessionId, "userId" to "user123"))
            }
        }
    }
}