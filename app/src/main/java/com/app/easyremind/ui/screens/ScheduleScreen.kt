package com.app.easyremind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.ui.components.EmptyState
import com.app.easyremind.ui.components.NeoButton
import com.app.easyremind.ui.components.NeoCard
import com.app.easyremind.ui.components.NeoSegmentedToggle
import com.app.easyremind.ui.components.neoShadowColors
import com.app.easyremind.ui.viewmodel.ScheduleUiState
import com.app.easyremind.util.ScheduleMath
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onAddClass: () -> Unit,
    onOpenClass: (Long) -> Unit,
    onSelectDay: (DayOfWeek?) -> Unit,
    onToggleView: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            NeoSegmentedToggle(
                options = listOf("List", "Grid"),
                selectedIndex = if (state.gridMode) 1 else 0,
                onSelect = { index -> onToggleView(index == 1) },
            )
            Spacer(Modifier.height(14.dp))
        }

        if (state.classes.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(44.dp).height(44.dp),
                    )
                },
                title = "No classes yet",
                subtitle = "Add your first class to build your weekly schedule.",
                action = {
                    NeoButton(text = "+ Add Class", onClick = onAddClass)
                },
            )
            return@Column
        }

        if (state.gridMode) {
            WeeklyGrid(
                classes = state.classes,
                onOpenClass = onOpenClass,
            )
        } else {
            DayList(
                state = state,
                onOpenClass = onOpenClass,
                onSelectDay = onSelectDay,
            )
        }
    }
}

// ------------------------------------------------------------------ List view

@Composable
private fun DayList(
    state: ScheduleUiState,
    onOpenClass: (Long) -> Unit,
    onSelectDay: (DayOfWeek?) -> Unit,
) {
    val allDays = DayOfWeek.entries
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    DayFilterChip(
                        label = "All",
                        selected = state.selectedDay == null,
                        onClick = { onSelectDay(null) },
                    )
                }
            items(
                items = allDays,
                key = { it.value }
            ) { day ->
                DayFilterChip(
                    label = ScheduleMath.dayNames[day.value - 1],
                    selected = state.selectedDay == day,
                    onClick = { onSelectDay(day) },
                )
            }
            }
            Spacer(Modifier.height(12.dp))
        }

        fun classesFor(day: DayOfWeek): List<ClassEntity> =
            state.classes
                .filter { ScheduleMath.hasDay(it.daysBitmask, day) }
                .sortedBy { it.startMin }

        val selected = state.selectedDay
        val daysToShow = if (selected == null) allDays else listOf(selected)
        for (day in daysToShow) {
            val dayClasses = classesFor(day)
            item {
                Column {
                    Text(
                        text = ScheduleMath.dayNames[day.value - 1],
                        style = MaterialTheme.typography.titleSmall,
                        color = if (day == LocalDate.now().dayOfWeek) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (dayClasses.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "No classes this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            items(
                items = dayClasses,
                key = { it.id }
            ) { clazz ->
                ScheduleRowCard(
                    clazz = clazz,
                    onClick = { onOpenClass(clazz.id) },
                )
            }
        }
    }
}

@Composable
private fun DayFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val (dark, light) = neoShadowColors()
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                shape,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ScheduleRowCard(
    clazz: ClassEntity,
    onClick: () -> Unit,
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(70.dp)) {
                Text(
                    text = ScheduleMath.formatTime(clazz.startMin),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
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
                Spacer(Modifier.height(2.dp))
                Text(
                    text = ScheduleMath.formatTimeRange(clazz.startMin, clazz.endMin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Grid view

private val subjectPalette = listOf(
    Color(0xFF7C83F5),
    Color(0xFF70C9A5),
    Color(0xFFF2B66D),
    Color(0xFFA6E3E9),
    Color(0xFFE8A2B8),
    Color(0xFFB9BDFB),
    Color(0xFF8FC3F2),
    Color(0xFFC99AEB),
)

private fun subjectColor(subject: String): Color =
    subjectPalette[subject.hashCode().mod(subjectPalette.size)]

@Composable
private fun WeeklyGrid(
    classes: List<ClassEntity>,
    onOpenClass: (Long) -> Unit,
) {
    val now = LocalDate.now()
    val today = now.dayOfWeek
    val nowHour = java.time.LocalTime.now().hour

    val minClassHour = (classes.minOfOrNull { it.startMin } ?: 7 * 60) / 60
    val maxClassHour = ((classes.maxOfOrNull { it.endMin } ?: 20 * 60) + 59) / 60
    val startHour = (minClassHour - 1).coerceAtLeast(6)
    val endHour = (maxClassHour + 1).coerceAtMost(23)
    val hours = (startHour until endHour).toList()

    val days = DayOfWeek.entries

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(46.dp)) {
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            days.forEach { day ->
                val isToday = day == today
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ScheduleMath.dayShort[day.value - 1],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            itemsIndexed(hours) { _, hour ->
                // Current time indicator row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            if (hour == nowHour && today == now.dayOfWeek)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            else Color.Transparent,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            text = hourLabel(hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    days.forEach { day ->
                        val active = classes
                            .filter {
                                it.isEnabled &&
                                    ScheduleMath.hasDay(it.daysBitmask, day) &&
                                    it.startMin < (hour + 1) * 60 &&
                                    it.endMin > hour * 60
                            }
                        GridDayCell(
                            classes = active,
                            hour = hour,
                            onOpenClass = onOpenClass,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridDayCell(
    classes: List<ClassEntity>,
    hour: Int,
    onOpenClass: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(horizontal = 1.dp, vertical = 1.dp)) {
        if (classes.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                classes.forEach { clazz ->
                    val color = subjectColor(clazz.subjectName)
                    val textColor = if (color.luminance() < 0.45f) Color.White else Color(0xFF202B50)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.92f))
                            .clickable { onOpenClass(clazz.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = clazz.subjectName,
                            color = textColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private fun hourLabel(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "${h}$suffix"
}