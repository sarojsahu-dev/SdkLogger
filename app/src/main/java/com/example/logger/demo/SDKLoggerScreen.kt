package com.example.logger.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logger.sdklogger.core.SDKLogger

@Composable
fun SDKLoggerScreen(
    modifier: Modifier = Modifier,
    logger: SDKLogger
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "🚀 SDKLogger Demo",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Basic Logging Section
        BasicLoggingSection(logger = logger)

        // Advanced Logging Section
        AdvancedLoggingSection(logger = logger)

        // Performance Section
        PerformanceSection(logger = logger)

        // Info Section
        InfoSection()

        // MultiSDK Section
        MultiSDKSection()
    }
}