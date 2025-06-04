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
fun BasicLoggingSection(logger: SDKLogger) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📝 Basic Logging",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LogButton("Info Message") {
                logger.info("UserAction", "User clicked info button")
            }

            LogButton("Warning Message") {
                logger.warning("UserAction", "User clicked warning button")
            }

            LogButton("Error Message") {
                logger.error("UserAction", "User clicked error button")
            }

            LogButton("Debug Message") {
                logger.debug("UserAction", "User clicked debug button")
            }
        }
    }
}