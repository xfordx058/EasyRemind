package com.app.easyremind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.appcompat.app.AppCompatDelegate
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
    val mode = theme?.theme ?: "system"
    val darkTheme = when (mode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    LaunchedEffect(mode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
    EasyRemindTheme(darkTheme = darkTheme) {
        content()
    }
}