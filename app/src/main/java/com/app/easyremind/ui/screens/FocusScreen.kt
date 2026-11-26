package com.app.easyremind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.easyremind.focus.FocusUiState
import com.app.easyremind.focus.FocusViewModel
import com.app.easyremind.focus.TimerMode
import com.app.easyremind.focus.TimerStatus
import com.app.easyremind.ui.components.animateEnter
import com.app.easyremind.ui.components.NeoButton
import com.app.easyremind.ui.components.NeoIconButton
import com.app.easyremind.ui.components.NeoSegmentedToggle
import com.app.easyremind.ui.components.neoShadowColors

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val completionPending = state.completionSeq > 0 && state.status == TimerStatus.Idle

    if (completionPending) {
        SessionCompleteCard(
            state = state,
            onStartBreak = {
                viewModel.dismissCompletion()
                viewModel.setMode(state.suggestedBreak)
                viewModel.startCurrentMode()
            },
            onBackHome = {
                viewModel.dismissCompletion()
                onBackHome()
            },
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Focus",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().animateEnter(delayMs = 0),
        )
        Spacer(Modifier.height(18.dp))

        if (state.status == TimerStatus.Idle) {
            NeoSegmentedToggle(
                options = listOf("Pomodoro", "Short Break", "Long Break"),
                selectedIndex = when (state.mode) {
                    TimerMode.Focus -> 0
                    TimerMode.Short -> 1
                    TimerMode.Long -> 2
                },
                onSelect = { index ->
                    viewModel.setMode(
                        when (index) {
                            0 -> TimerMode.Focus
                            1 -> TimerMode.Short
                            else -> TimerMode.Long
                        },
                    )
                },
            )
        }

        Spacer(Modifier.height(32.dp))
        TimerRing(state = state, modifier = Modifier.animateEnter(delayMs = 120))
        Spacer(Modifier.height(16.dp))

        Text(
            text = when {
                state.status == TimerStatus.Running && state.mode == TimerMode.Long -> "LONG BREAK"
                state.status == TimerStatus.Running && state.isBreak -> "SHORT BREAK"
                state.status == TimerStatus.Running -> "FOCUS SESSION"
                state.status == TimerStatus.Paused -> "PAUSED"
                state.isBreak -> "READY FOR A BREAK"
                else -> "READY TO FOCUS"
            },
            style = MaterialTheme.typography.titleSmall,
            color = when (state.status) {
                TimerStatus.Running -> MaterialTheme.colorScheme.primary
                TimerStatus.Paused -> MaterialTheme.colorScheme.warningColor()
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Session ${state.sessionNumber} of ${state.sessionCount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        when (state.status) {
            TimerStatus.Idle -> {
                NeoButton(
                    text = "Start",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = viewModel::startCurrentMode,
                    modifier = Modifier.fillMaxWidth().animateEnter(delayMs = 200),
                )
                Spacer(Modifier.height(16.dp))
            }

            TimerStatus.Running -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .animateEnter(delayMs = 200),
                        contentAlignment = Alignment.Center,
                    ) {
                        NeoIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            onClick = viewModel::pause,
                            modifier = Modifier.fillMaxWidth(),
                            size = 60.dp,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .animateEnter(delayMs = 200),
                        contentAlignment = Alignment.Center,
                    ) {
                        NeoIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = viewModel::reset,
                            modifier = Modifier.fillMaxWidth(),
                            size = 60.dp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            TimerStatus.Paused -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    NeoButton(
                        text = "Resume",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = viewModel::resume,
                        modifier = Modifier.weight(1f).animateEnter(delayMs = 200),
                    )
                    NeoButton(
                        text = "Reset",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = viewModel::reset,
                        modifier = Modifier.weight(1f).animateEnter(delayMs = 240),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Text(
            text = when (state.status) {
                TimerStatus.Running -> if (state.isBreak) "Enjoy your break." else "Stay focused. You've got this!"
                TimerStatus.Paused -> "Take your time, then resume."
                else -> "When you're ready, press start."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TimerRing(state: FocusUiState, modifier: Modifier = Modifier) {
    val (dark, light) = neoShadowColors()
    val ringSize = 240.dp
    val stroke = 16.dp
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    val accent = when (state.mode) {
        TimerMode.Focus -> MaterialTheme.colorScheme.primary
        TimerMode.Short -> MaterialTheme.colorScheme.tertiary
        TimerMode.Long -> MaterialTheme.colorScheme.tertiary
    }
    Box(
        modifier = modifier
            .size(ringSize)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arcSize = Size(this.size.width - stroke.toPx(), this.size.height - stroke.toPx())
            val topLeft = Offset(
                (this.size.width - arcSize.width) / 2f,
                (this.size.height - arcSize.height) / 2f,
            )
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * state.progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatRemaining(state.remainingMs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (state.mode) {
                    TimerMode.Focus -> "Focus Time"
                    TimerMode.Short -> "Short Break"
                    TimerMode.Long -> "Long Break"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = ((ms + 999) / 1000).coerceAtLeast(0L)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun SessionCompleteCard(
    state: FocusUiState,
    onStartBreak: () -> Unit,
    onBackHome: () -> Unit,
) {
    val isBreakDone = state.lastCompletedMode == TimerMode.Short ||
        state.lastCompletedMode == TimerMode.Long
    val breakMinutes = if (state.suggestedBreak == TimerMode.Long) {
        state.longBreakDuration
    } else {
        state.shortBreakDuration
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Completed",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp).animateEnter(delayMs = 0),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isBreakDone) "Break Complete" else "Focus Session Complete",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isBreakDone) {
                "Great work! Ready for your next focus session."
            } else {
                "Great work!\n\nTime for a $breakMinutes-minute ${if (state.suggestedBreak == TimerMode.Long) "long" else "short"} break."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(0.8f))
        if (!isBreakDone) {
            NeoButton(
                text = if (state.suggestedBreak == TimerMode.Long) "Start Long Break" else "Start Break",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onStartBreak,
                modifier = Modifier.animateEnter(delayMs = 160),
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(modifier = Modifier.padding(8.dp).animateEnter(delayMs = 200)) {
            androidx.compose.material3.TextButton(onClick = onBackHome) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Back to Home", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun androidx.compose.material3.ColorScheme.warningColor(): androidx.compose.ui.graphics.Color =
    if (background.luminance() > 0.5f)
        com.app.easyremind.ui.theme.WarningLight
    else com.app.easyremind.ui.theme.WarningDark