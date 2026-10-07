package com.app.easyremind.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.ui.components.NeoButton
import com.app.easyremind.ui.components.NeoCard
import com.app.easyremind.ui.components.NeoIconButton
import com.app.easyremind.ui.components.EmptyState
import com.app.easyremind.ui.viewmodel.HomeUiState
import com.app.easyremind.util.ScheduleMath
import java.time.LocalDate

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenSchedule: () -> Unit,
    onAddClass: () -> Unit,
    onOpenClass: (Long) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${state.greeting} 👋",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NeoIconButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = onOpenNotificationSettings,
                    size = 48.dp,
                )
            }
        }

        if (state.classes.isEmpty()) {
            item(key = "empty_state") {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                    },
                    title = "No classes yet",
                    subtitle = "Add your first class\nto start your schedule.",
                    action = {
                        NeoButton(text = "+ Add Class", onClick = onAddClass)
                    },
                )
            }
            return@LazyColumn
        }

        item(key = "next_class") {
            NextClassHero(
                state = state,
                onOpenClass = onOpenClass,
                onOpenSchedule = onOpenSchedule,
            )
        }

        if (state.todaysClasses.isNotEmpty()) {
            item(key = "today_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Today's Classes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(onClick = onOpenSchedule) {
                        Text("See all", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(
                items = state.todaysClasses,
                key = { it.id }
            ) { clazz ->
                CompactClassCard(
                    clazz = clazz,
                    nowMin = state.nowMin,
                    onClick = { onOpenClass(clazz.id) },
                )
            }
        }
    }
}

@Composable
private fun NextClassHero(
    state: HomeUiState,
    onOpenClass: (Long) -> Unit,
    onOpenSchedule: () -> Unit,
) {
    when (val next = state.next) {
        is ScheduleMath.NextClass.Current -> HeroCard(
            label = "CURRENT CLASS",
            clazz = next.clazz,
            caption = "Ends in ${ScheduleMath.formatDuration(next.remainingMin)}",
            accent = MaterialTheme.colorScheme.primary,
            onClick = { onOpenClass(next.clazz.id) },
        )
        is ScheduleMath.NextClass.Upcoming -> HeroCard(
            label = if (next.date == state.today.toLocalDate()) "NEXT CLASS" else "UP NEXT",
            clazz = next.clazz,
            caption = buildString {
                if (next.date != state.today.toLocalDate()) {
                    append(next.date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale.US)))
                    append(" · ")
                }
                append("Starts in ")
                append(ScheduleMath.formatDuration(next.startsInMin))
            },
            accent = MaterialTheme.colorScheme.primary,
            onClick = { onOpenClass(next.clazz.id) },
        )
        ScheduleMath.NextClass.NoClasses -> {
            if (state.todaysClasses.isEmpty()) {
                // no more classes today; find first future class
                val future = state.classes
                    .filter { it.isEnabled }
                    .flatMap { c ->
                        ScheduleMath.daySetOf(c.daysBitmask).map { d -> c to d }.take(1)
                    }
                EmptyHero(
                    title = "You're all done! ✓",
                    subtitle = "No more classes scheduled for today.\nEnjoy your free time!",
                    onClick = onOpenSchedule,
                )
            } else {
                Box(Modifier.fillMaxWidth()) {}
            }
        }
    }
}

@Composable
private fun HeroCard(
    label: String,
    clazz: ClassEntity,
    caption: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    NeoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = clazz.subjectName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = ScheduleMath.formatTimeRange(clazz.startMin, clazz.endMin),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (clazz.room.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = clazz.room,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyHero(title: String, subtitle: String, onClick: () -> Unit) {
    NeoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.successColor(),
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CompactClassCard(
    clazz: ClassEntity,
    nowMin: Int,
    onClick: () -> Unit,
) {
    val isActive = clazz.startMin <= nowMin && nowMin < clazz.endMin
    val accent = if (isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    NeoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(64.dp)) {
                Text(
                    text = ScheduleMath.formatTime(clazz.startMin),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clazz.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        clazz.room.takeIf { it.isNotBlank() },
                        clazz.instructor.takeIf { it.isNotBlank() },
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun androidx.compose.material3.ColorScheme.successColor(): androidx.compose.ui.graphics.Color =
    if (background.luminance() > 0.5f)
        com.app.easyremind.ui.theme.SuccessLight
    else com.app.easyremind.ui.theme.SuccessDark