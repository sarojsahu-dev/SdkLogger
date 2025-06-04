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
fun MultiSDKSection() {
    // Create separate loggers for different SDKs
    val paymentLogger = SDKLogger.getInstance("PaymentSDK", "2.1.0")
    val authLogger = SDKLogger.getInstance("AuthSDK", "1.5.0")
    val analyticsLogger = SDKLogger.getInstance("AnalyticsSDK", "3.0.0")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🏢 Multiple SDK Loggers",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogButton(
                    text = "Payment SDK",
                    modifier = Modifier.weight(1f)
                ) {
                    paymentLogger.info("Payment", "Processing payment",
                        metadata = mapOf("amount" to 50.0, "currency" to "USD"))
                }

                LogButton(
                    text = "Auth SDK",
                    modifier = Modifier.weight(1f)
                ) {
                    authLogger.info("Auth", "User authentication",
                        metadata = mapOf("method" to "oauth", "provider" to "google"))
                }
            }

            LogButton("Analytics SDK") {
                analyticsLogger.info("Analytics", "User event tracked",
                    metadata = mapOf("event" to "button_click", "screen" to "home"))
            }
        }
    }
}