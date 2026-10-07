package com.app.easyremind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.app.easyremind.ui.EasyRemindRoot
import com.app.easyremind.ui.theme.EasyRemindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                EasyRemindRoot()
            }
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val app = LocalContext.current.applicationContext as EasyRemindApp
    val theme by app.container.repository.settingsFlow.collectAsState(initial = null)
    val darkTheme = when (theme?.theme ?: "system") {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    EasyRemindTheme(darkTheme = darkTheme) {
        content()
    }
}