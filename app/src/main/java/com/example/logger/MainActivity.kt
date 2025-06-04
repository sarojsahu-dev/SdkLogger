package com.example.logger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.logger.demo.SDKLoggerScreen
import com.example.logger.ui.theme.LoggerTheme
import com.logger.sdklogger.core.SDKLogger

class MainActivity : ComponentActivity() {

    // Simple logger initialization
    private val appLogger by lazy {
        SDKLogger.getInstance("MyApp", "1.0.0")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize logger
        appLogger.info("App", "Application started successfully")

        enableEdgeToEdge()
        setContent {
            LoggerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Use your SDKLogger demo screen
                    SDKLoggerScreen(
                        modifier = Modifier.padding(innerPadding),
                        logger = appLogger
                    )
                }
            }
        }
    }
}

