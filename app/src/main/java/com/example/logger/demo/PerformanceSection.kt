package com.example.logger.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logger.sdklogger.core.SDKLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PerformanceSection(logger: SDKLogger) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚡ Performance Testing",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LogButton("Bulk Logging Test") {
                scope.launch {
                    isLoading = true
                    val startTime = System.currentTimeMillis()

                    repeat(50) { i ->
                        when (i % 4) {
                            0 -> logger.info("BulkTest", "Info message $i")
                            1 -> logger.debug("BulkTest", "Debug message $i")
                            2 -> logger.warning("BulkTest", "Warning message $i")
                            3 -> logger.error("BulkTest", "Error message $i")
                        }
                    }

                    val duration = System.currentTimeMillis() - startTime
                    logger.info("Performance", "Bulk test completed",
                        metadata = mapOf(
                            "totalMessages" to 50,
                            "durationMs" to duration
                        ))

                    delay(500)
                    isLoading = false
                }
            }

            LogButton("Async Performance Test") {
                scope.launch {
                    isLoading = true
                    val startTime = System.currentTimeMillis()

                    repeat(100) { i ->
                        logger.info("AsyncTest", "Async message $i",
                            metadata = mapOf("messageIndex" to i))
                    }

                    val duration = System.currentTimeMillis() - startTime
                    logger.info("Performance", "Async test completed",
                        metadata = mapOf(
                            "totalMessages" to 100,
                            "durationMs" to duration,
                            "averageMs" to duration.toDouble() / 100.0
                        ))

                    delay(300)
                    isLoading = false
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}