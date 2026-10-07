package com.app.easyremind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.ui.components.NeoButton
import com.app.easyremind.ui.components.NeoCard
import com.app.easyremind.ui.components.NeoConfirmDeleteDialog
import com.app.easyremind.ui.components.NeoMessageDialog
import com.app.easyremind.ui.viewmodel.ClassDetailViewModel
import com.app.easyremind.util.ScheduleMath
import java.time.DayOfWeek

@Composable
fun ClassDetailsScreen(
    viewModel: ClassDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clazz by viewModel.classState.collectAsStateWithLifecycle()
    var showDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var toggleError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                text = "Class Details",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }

        if (clazz == null) {
            Text(
                text = "Class not found.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val c = clazz!!
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = c.subjectName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))

            NeoCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow(label = "Time", value = ScheduleMath.formatTimeRange(c.startMin, c.endMin))
                HorizontalDivider()
                DetailRow(label = "Days", value = ScheduleMath.dayNamesOf(c.daysBitmask))
                HorizontalDivider()
                DetailRow(label = "Room", value = c.room.ifBlank { "—" })
                HorizontalDivider()
                DetailRow(label = "Instructor", value = c.instructor.ifBlank { "—" })
                HorizontalDivider()
                DetailRow(
                    label = "Reminder",
                    value = if (c.reminderMinutes == 0) "At class start"
                    else "${c.reminderMinutes} minutes before",
                )
            }

            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Class enabled",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (c.isEnabled) "Reminders are active." else "Disabled — no reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = c.isEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleEnabled(c, enabled, onError = { toggleError = it })
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            NeoButton(text = "Edit Class", onClick = { onEdit(c.id) })
            Spacer(Modifier.height(4.dp))
            NeoButton(
                text = "Delete Class",
                onClick = { showDelete = true },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDelete) {
        val target = clazz
        NeoConfirmDeleteDialog(
            subject = target?.subjectName.orEmpty(),
            onConfirm = {
                showDelete = false
                target?.let { viewModel.delete(it.id) }
                onDeleted()
            },
            onDismiss = { showDelete = false },
        )
    }

    toggleError?.let { message ->
        NeoMessageDialog(
            title = "Could not update",
            message = message,
            confirmText = "OK",
            onConfirm = { toggleError = null },
            onDismiss = { toggleError = null },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HorizontalDivider() {
    Spacer(Modifier.height(10.dp))
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
    )
    Spacer(Modifier.height(10.dp))
}