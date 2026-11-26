package com.app.easyremind.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.app.easyremind.ui.components.NeoMessageDialog
import com.app.easyremind.ui.components.animateEnter
import com.app.easyremind.ui.components.NeoCard
import com.app.easyremind.ui.components.NeoSettingsCard
import com.app.easyremind.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenNotifications: () -> Unit,
    onOpenScheduleSettings: () -> Unit,
    onOpenFocusSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    fun show(msg: String) {
        message = msg
    }

    suspend fun runImport(text: String) {
        busyMessage = "Restoring backup..."
        val result = runCatching { viewModel.importJson(text) }
        busyMessage = null
        result.onSuccess { imported ->
            when (imported) {
                is com.app.easyremind.data.ScheduleRepository.ImportResult.Success ->
                    show("Imported ${imported.classesImported} class${if (imported.classesImported == 1) "" else "es"}.")
                is com.app.easyremind.data.ScheduleRepository.ImportResult.Error ->
                    show(imported.message)
            }
        }.onFailure {
            show("Could not restore the backup.")
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busyMessage = "Exporting schedule..."
                val json = runCatching { viewModel.exportJson() }.getOrNull()
                if (json == null) {
                    busyMessage = null
                    show("Could not export the schedule.")
                    return@launch
                }
                runCatching {
                    val written = context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                    if (!written) error("Could not open the file for writing.")
                }.onSuccess {
                    show("Schedule exported successfully.")
                }.onFailure {
                    show("Could not export the schedule.")
                }
                busyMessage = null
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    }
                }.getOrNull()

                if (text == null) {
                    show("Could not read the backup file.")
                } else {
                    // Import replaces everything; ask first so a wrong file
                    // cannot silently wipe the user's schedule.
                    pendingImport = text
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.animateEnter(delayMs = 0),
        )

        val notifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifEnabled) {
            NeoCard(modifier = Modifier.fillMaxWidth().animateEnter(delayMs = 80)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Notifications are disabled",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Class reminders may not appear until notifications are enabled. Your saved schedule still works offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
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

        NeoSettingsCard(modifier = Modifier.animateEnter(delayMs = 120)) {
            SettingsItem("Notifications", "Class reminders & sound", Icons.Default.Notifications, onOpenNotifications)
        }
        NeoSettingsCard(modifier = Modifier.animateEnter(delayMs = 120)) {
            SettingsItem("Schedule", "Default view & display", Icons.Default.Schedule, onOpenScheduleSettings)
        }
        NeoSettingsCard(modifier = Modifier.animateEnter(delayMs = 120)) {
            SettingsItem("Focus Timer", "Durations & sessions", Icons.Default.Timer, onOpenFocusSettings)
        }
        NeoSettingsCard(modifier = Modifier.animateEnter(delayMs = 120)) {
            SettingsItem("Appearance", "Light or dark theme", Icons.Default.Palette, onOpenAppearance)
        }
        NeoSettingsCard(modifier = Modifier.animateEnter(delayMs = 120)) {
            SettingsItem("About", "Version & credits", Icons.Default.Info, onOpenAbout)
        }

        NeoSettingsCard(modifier = Modifier.animateEnter(delayMs = 120)) {
            SettingsItem("Export Data", "Save schedule as a JSON file", Icons.Default.CloudUpload) {
                exportLauncher.launch("easy_remind_backup.json")
            }
        }
        NeoSettingsCard(modifier = Modifier.animateEnter(delayMs = 120)) {
            SettingsItem("Import Data", "Restore from a backup file", Icons.Default.CloudDownload) {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        }
    }

    pendingImport?.let { text ->
        NeoMessageDialog(
            title = "Replace schedule?",
            message = "Importing will delete all your current classes and settings and replace them with the backup. This cannot be undone.",
            confirmText = "Import",
            onConfirm = {
                pendingImport = null
                scope.launch { runImport(text) }
            },
            onDismiss = { pendingImport = null },
            destructive = true,
        )
    }

    busyMessage?.let { label ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(label, fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Please wait...", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
    }

    message?.let { msg ->
        NeoMessageDialog(
            title = "Easy Remind",
            message = msg,
            confirmText = "OK",
            onConfirm = { message = null },
            onDismiss = { message = null },
            showDismissButton = false,
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun SimpleSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .animateEnter(delayMs = 0),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .animateEnter(delayMs = 80),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
            Spacer(Modifier.height(96.dp))
        }
    }
}