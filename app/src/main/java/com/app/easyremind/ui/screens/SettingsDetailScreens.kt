package com.app.easyremind.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.easyremind.ui.components.AppLogo
import com.app.easyremind.ui.components.NeoCard
import com.app.easyremind.ui.components.NeoDropdown
import com.app.easyremind.ui.components.NeoSegmentedToggle
import com.app.easyremind.ui.components.NeoSwitchRow
import com.app.easyremind.ui.viewmodel.SettingsViewModel
import java.time.DayOfWeek

@Composable
fun NotificationSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: com.app.easyremind.data.SettingsEntity()
    val context = LocalContext.current

    SimpleSettingsScaffold(title = "Notifications", onBack = onBack) {
        NeoCard(Modifier.fillMaxWidth()) {
            NeoSwitchRow(
                title = "Class Reminders",
                subtitle = "Remind me before every class",
                checked = s.notificationsEnabled,
                onCheckedChange = { v -> viewModel.update { it.copy(notificationsEnabled = v) } },
            )
        }

        NeoCard(Modifier.fillMaxWidth()) {
            val options = listOf(
                "At class start" to 0,
                "5 minutes before" to 5,
                "10 minutes before" to 10,
                "15 minutes before" to 15,
                "30 minutes before" to 30,
                "1 hour before" to 60,
            )
            val index = options.indexOfFirst { it.second == s.defaultReminderMinutes }.coerceAtLeast(0)
            NeoDropdown(
                label = "Default Reminder",
                value = options[index].first,
                options = options.map { it.first },
                onSelect = { i ->
                    viewModel.update { it.copy(defaultReminderMinutes = options[i].second) }
                },
            )
        }

        NeoCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NeoSwitchRow(
                    title = "Sound",
                    checked = s.soundEnabled,
                    onCheckedChange = { v ->
                        viewModel.update { it.copy(soundEnabled = v) }
                    },
                )
                NeoSwitchRow(
                    title = "Vibration",
                    checked = s.vibrationEnabled,
                    onCheckedChange = { v ->
                        viewModel.update { it.copy(vibrationEnabled = v) }
                    },
                )
            }
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NeoCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "⚠️ Notifications disabled",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Class reminders won't appear until you enable notifications in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = {
                    val intent = Intent(
                        "android.settings.APP_NOTIFICATION_SETTINGS",
                    ).putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                    runCatching { context.startActivity(intent) }
                }) {
                    Text("Open System Settings", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Schedule

@Composable
fun ScheduleSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: com.app.easyremind.data.SettingsEntity()

    SimpleSettingsScaffold(title = "Schedule Settings", onBack = onBack) {
        NeoCard(Modifier.fillMaxWidth()) {
            Text("Default View", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            NeoSegmentedToggle(
                options = listOf("List", "Grid"),
                selectedIndex = if (s.scheduleView == "grid") 1 else 0,
                onSelect = { i ->
                    viewModel.update {
                        it.copy(scheduleView = if (i == 1) "grid" else "list")
                    }
                },
            )
        }

        NeoCard(Modifier.fillMaxWidth()) {
            NeoDropdown(
                label = "Week Starts On",
                value = DayOfWeek.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
                    .firstOrNull { t ->
                        t.equals(s.weekStartsOn.lowercase().replaceFirstChar(Char::uppercase), ignoreCase = true)
                    } ?: "Monday",
                options = DayOfWeek.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                onSelect = { i ->
                    viewModel.update {
                        it.copy(weekStartsOn = DayOfWeek.entries[i].name)
                    }
                },
            )
        }

        NeoCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NeoSwitchRow(
                    title = "Show Instructor",
                    checked = s.showInstructor,
                    onCheckedChange = { v -> viewModel.update { it.copy(showInstructor = v) } },
                )
                NeoSwitchRow(
                    title = "Show Room",
                    checked = s.showRoom,
                    onCheckedChange = { v -> viewModel.update { it.copy(showRoom = v) } },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Focus Timer

@Composable
fun FocusTimerSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: com.app.easyremind.data.SettingsEntity()
    val focusOptions = listOf(10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60)
    val shortOptions = listOf(3, 5, 7, 10, 15)
    val longOptions = listOf(10, 12, 15, 20, 25, 30)
    val sessionOptions = listOf(2, 3, 4, 5, 6, 8)

    SimpleSettingsScaffold(title = "Focus Timer Settings", onBack = onBack) {
        NeoCard(Modifier.fillMaxWidth()) {
            NeoDropdown(
                label = "Focus Duration",
                value = "${s.focusDuration} minutes",
                options = focusOptions.map { "$it minutes" },
                onSelect = { i -> viewModel.update { it.copy(focusDuration = focusOptions[i]) } },
            )
        }
        NeoCard(Modifier.fillMaxWidth()) {
            NeoDropdown(
                label = "Short Break",
                value = "${s.shortBreakDuration} minutes",
                options = shortOptions.map { "$it minutes" },
                onSelect = { i -> viewModel.update { it.copy(shortBreakDuration = shortOptions[i]) } },
            )
        }
        NeoCard(Modifier.fillMaxWidth()) {
            NeoDropdown(
                label = "Long Break",
                value = "${s.longBreakDuration} minutes",
                options = longOptions.map { "$it minutes" },
                onSelect = { i -> viewModel.update { it.copy(longBreakDuration = longOptions[i]) } },
            )
        }
        NeoCard(Modifier.fillMaxWidth()) {
            NeoDropdown(
                label = "Sessions Before Long Break",
                value = "${s.sessionCount}",
                options = sessionOptions.map { "$it" },
                onSelect = { i -> viewModel.update { it.copy(sessionCount = sessionOptions[i]) } },
            )
        }
        NeoCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NeoSwitchRow(
                    title = "Sound",
                    checked = s.soundEnabled,
                    onCheckedChange = { v -> viewModel.update { it.copy(soundEnabled = v) } },
                )
                NeoSwitchRow(
                    title = "Vibration",
                    checked = s.vibrationEnabled,
                    onCheckedChange = { v -> viewModel.update { it.copy(vibrationEnabled = v) } },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Appearance

@Composable
fun AppearanceScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: com.app.easyremind.data.SettingsEntity()

    SimpleSettingsScaffold(title = "Appearance", onBack = onBack) {
        NeoCard(Modifier.fillMaxWidth()) {
            Text("Choose Theme", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            ThemeOption("Light", selected = s.theme == "light", onClick = {
                viewModel.update { it.copy(theme = "light") }
            })
            ThemeOption("Dark", selected = s.theme == "dark", onClick = {
                viewModel.update { it.copy(theme = "dark") }
            })
            ThemeOption("System Default", selected = s.theme == "system", onClick = {
                viewModel.update { it.copy(theme = "system") }
            })
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) "●" else "○",
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 20.sp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ------------------------------------------------------------------ About

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            AppLogo(size = 96.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Easy Remind",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "A simple class schedule and reminder app for students.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Set your schedule once.\nEasy Remind handles the rest.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Made for students.\nBuilt for focus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}