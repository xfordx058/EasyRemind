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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.data.ScheduleRepository
import com.app.easyremind.ui.components.NeoButton
import com.app.easyremind.ui.components.NeoConflictDialog
import com.app.easyremind.ui.components.NeoDaySelector
import com.app.easyremind.ui.components.NeoDropdown
import com.app.easyremind.ui.components.NeoLabeledField
import com.app.easyremind.ui.components.NeoMessageDialog
import com.app.easyremind.ui.components.NeoTimeField
import com.app.easyremind.ui.viewmodel.ClassEditorViewModel
import com.app.easyremind.util.ScheduleMath
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private val reminderLabels = listOf(
    "At class start" to 0,
    "5 minutes before" to 5,
    "10 minutes before" to 10,
    "15 minutes before" to 15,
    "30 minutes before" to 30,
    "1 hour before" to 60,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEditorScreen(
    viewModel: ClassEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var startPicker by remember { mutableStateOf(false) }
    var endPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var conflict by remember { mutableStateOf<List<ClassEntity>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = if (form.isNew) "Add Class" else "Edit Class",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NeoLabeledField(
                label = "Subject",
                value = form.subjectName,
                onValueChange = viewModel::onSubjectChange,
                placeholder = "e.g. Mobile Application Development",
            )

            NeoLabeledField(
                label = "Instructor",
                value = form.instructor,
                onValueChange = viewModel::onInstructorChange,
                placeholder = "Optional",
            )

            NeoLabeledField(
                label = "Room",
                value = form.room,
                onValueChange = viewModel::onRoomChange,
                placeholder = "e.g. Room 204",
            )

            NeoDaySelector(
                selected = form.selectedDays,
                onToggle = viewModel::toggleDay,
            )

            NeoTimeField(
                label = "Start Time",
                value = if (form.startSet) ScheduleMath.formatTime(form.startMin) else "",
                onClick = { startPicker = true },
            )

            NeoTimeField(
                label = "End Time",
                value = if (form.endSet) ScheduleMath.formatTime(form.endMin) else "",
                onClick = { endPicker = true },
            )

            val reminderIndex = reminderLabels.indexOfFirst { it.second == form.reminderMinutes }
                .coerceAtLeast(0)
            NeoDropdown(
                label = "Reminder",
                value = reminderLabels[reminderIndex].first,
                options = reminderLabels.map { it.first },
                onSelect = { index -> viewModel.setReminder(reminderLabels[index].second) },
            )

            Spacer(Modifier.height(4.dp))
            NeoButton(
                text = if (form.isNew) "Save Class" else "Save Changes",
                enabled = form.loaded,
                onClick = {
                    scope.launch {
                        when (val result = viewModel.save()) {
                            ScheduleRepository.SaveResult.Ok -> onSaved()
                            is ScheduleRepository.SaveResult.Failed -> errorMessage = result.message
                            is ScheduleRepository.SaveResult.Conflict -> conflict = result.conflicts
                        }
                    }
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (startPicker) {
        TimePickerDialog(
            initialMinutes = if (form.startSet) form.startMin else 9 * 60,
            onConfirm = { viewModel.setStart(it); startPicker = false },
            onDismiss = { startPicker = false },
        )
    }
    if (endPicker) {
        TimePickerDialog(
            initialMinutes = if (form.endSet) form.endMin else 10 * 60,
            onConfirm = { viewModel.setEnd(it); endPicker = false },
            onDismiss = { endPicker = false },
        )
    }

    errorMessage?.let { message ->
        NeoMessageDialog(
            title = "Check your class",
            message = message,
            confirmText = "OK",
            onConfirm = { errorMessage = null },
            onDismiss = { errorMessage = null },
        )
    }

    conflict?.let { conflicts ->
        NeoConflictDialog(
            newSubject = form.subjectName.ifBlank { "This class" },
            conflicts = conflicts,
            onChangeTime = { conflict = null },
            onDismiss = { conflict = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Select Time", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(state.hour * 60 + state.minute)
                },
            ) {
                Text("OK", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}