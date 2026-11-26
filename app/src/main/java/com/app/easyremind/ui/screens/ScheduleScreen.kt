package com.app.easyremind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.ui.components.EmptyState
import com.app.easyremind.ui.components.animateEnter
import com.app.easyremind.ui.components.NeoButton
import com.app.easyremind.ui.components.NeoCard
import com.app.easyremind.ui.components.NeoIconButton
import com.app.easyremind.ui.components.NeoLabeledField
import com.app.easyremind.ui.components.NeoSegmentedToggle
import com.app.easyremind.ui.components.neoShadowColors
import com.app.easyremind.ui.viewmodel.ScheduleUiState
import com.app.easyremind.util.ScheduleMath
import com.app.easyremind.util.TimetableLayout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onAddClass: () -> Unit,
    onOpenClass: (Long) -> Unit,
    onSelectDay: (DayOfWeek?) -> Unit,
    onToggleView: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).animateEnter(delayMs = 0)) {
            // Minimal header: title + the single Add action. No settings or
            // overflow icons here by design.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                // Primary container + onPrimary icon: a white icon on the old
                // surface-colored button was invisible in light theme.
                NeoIconButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add class",
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = onAddClass,
                    size = 48.dp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.height(12.dp))
            NeoSegmentedToggle(
                options = listOf("List", "Grid"),
                selectedIndex = if (state.gridMode) 1 else 0,
                onSelect = { index -> onToggleView(index == 1) },
            )
            if (state.classes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                NeoLabeledField(
                    label = "Search classes",
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Subject, instructor, or room",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
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
                modifier = Modifier.animateEnter(delayMs = 80),
                action = {
                    NeoButton(
                        text = "Add Class",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = onAddClass,
                    )
                },
            )
            return@Column
        }

        if (state.filteredClasses.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(44.dp).height(44.dp),
                    )
                },
                title = "No classes found",
                subtitle = "Nothing matches \"${state.searchQuery.trim()}\".\nTry a different subject, instructor, or room.",
                modifier = Modifier.animateEnter(delayMs = 80),
                action = {
                    NeoButton(
                        text = "Clear Search",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = { onSearchQueryChange("") },
                    )
                },
            )
            return@Column
        }

        if (state.gridMode) {
            WeeklyGrid(
                classes = state.filteredClasses,
                weekStartsOn = state.settings.weekStartsOn,
                showRoom = state.settings.showRoom,
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

private fun orderedDays(weekStartsOn: String): List<DayOfWeek> {
    val first = runCatching { DayOfWeek.valueOf(weekStartsOn) }
        .getOrDefault(DayOfWeek.MONDAY)
    return DayOfWeek.entries.sortedBy { (it.value - first.value + 7) % 7 }
}

@Composable
private fun DayList(
    state: ScheduleUiState,
    onOpenClass: (Long) -> Unit,
    onSelectDay: (DayOfWeek?) -> Unit,
) {
    val allDays = orderedDays(state.settings.weekStartsOn)
    // Animate each card only on first appearance so scrolling back doesn't replay it.
    val animatedCards = remember { mutableSetOf<Long>() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().animateEnter(delayMs = 120),
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
            state.filteredClasses
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
            itemsIndexed(
                items = dayClasses,
                key = { _, clazz -> clazz.id },
            ) { index, clazz ->
                val firstAppearance = remember(clazz.id) { animatedCards.add(clazz.id) }
                ScheduleRowCard(
                    clazz = clazz,
                    showInstructor = state.settings.showInstructor,
                    showRoom = state.settings.showRoom,
                    modifier = if (firstAppearance) {
                        Modifier.animateEnter(delayMs = 160 + minOf(index * 40, 200))
                    } else Modifier,
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
    showInstructor: Boolean = true,
    showRoom: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 14.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(if (clazz.isEnabled) 1f else 0.6f),
        ) {
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
                        clazz.room.takeIf { showRoom && it.isNotBlank() },
                        clazz.instructor.takeIf { showInstructor && it.isNotBlank() },
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
                if (!clazz.isEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Reminders off",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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

private val TimeColumnWidth = 52.dp
private val HourHeight = 64.dp
private val MinDayWidth = 96.dp
private val DayHeaderHeight = 56.dp

/**
 * Weekly timetable. Class blocks are absolutely positioned from their real
 * start/end minutes, so a two-hour class is exactly twice as tall as a
 * one-hour class. Overlapping classes share the day column side by side.
 */
@Composable
private fun WeeklyGrid(
    classes: List<ClassEntity>,
    weekStartsOn: String,
    showRoom: Boolean,
    onOpenClass: (Long) -> Unit,
) {
    val today = LocalDate.now()
    val days = orderedDays(weekStartsOn)
    val weekStart = today.minusDays(
        ((today.dayOfWeek.value - days.first().value + 7) % 7).toLong(),
    )

    val minClassHour = (classes.minOfOrNull { it.startMin } ?: 7 * 60) / 60
    val maxClassHour = ((classes.maxOfOrNull { it.endMin } ?: 20 * 60) + 59) / 60
    val startHour = (minClassHour - 1).coerceAtLeast(6)
    val endHour = (maxClassHour + 1).coerceAtMost(24)
    val hours = (startHour until endHour).toList()
    val totalHeight = HourHeight * hours.size

    // Shared between the day header and the body so both scroll together.
    val dayScroll = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().animateEnter(delayMs = 160)) {
        val usableWidth = maxWidth - TimeColumnWidth
        val dayWidth = if (usableWidth / 7 >= MinDayWidth) usableWidth / 7 else MinDayWidth
        val gridWidth = dayWidth * days.size

        Column(modifier = Modifier.fillMaxSize()) {
            // ----- Sticky day header (day name + date number) -----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DayHeaderHeight)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Box(
                    modifier = Modifier
                        .width(TimeColumnWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(dayScroll),
                ) {
                    days.forEachIndexed { index, day ->
                        val date = weekStart.plusDays(index.toLong())
                        val isToday = date == today
                        Column(
                            modifier = Modifier
                                .width(dayWidth)
                                .fillMaxHeight()
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = day.name.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }

            // ----- Scrollable timetable body -----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp),
            ) {
                Row {
                    // Time ruler stays fixed while the days scroll sideways.
                    Column(modifier = Modifier.width(TimeColumnWidth)) {
                        hours.forEach { hour ->
                            Box(
                                modifier = Modifier
                                    .width(TimeColumnWidth)
                                    .height(HourHeight),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                Text(
                                    text = hourLabel(hour),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp, top = 6.dp),
                                )
                            }
                        }
                    }

                    // Resolve the theme color here: drawBehind's lambda is not
                    // composable, so MaterialTheme cannot be read inside it.
                    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.13f)
                    Box(
                        modifier = Modifier
                            .horizontalScroll(dayScroll)
                            .width(gridWidth)
                            .height(totalHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .drawBehind {
                                val dayPx = dayWidth.toPx()
                                val hourPx = HourHeight.toPx()
                                for (i in 0..days.size) {
                                    val x = i * dayPx
                                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                                }
                                for (i in 0..hours.size) {
                                    val y = i * hourPx
                                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                                }
                            },
                    ) {
                        days.forEachIndexed { dayIndex, day ->
                            val dayClasses = classes
                                .filter { ScheduleMath.hasDay(it.daysBitmask, day) }
                                .sortedBy { it.startMin }
                            val placements = TimetableLayout.placeDayClasses(dayClasses)

                            dayClasses.forEachIndexed { index, clazz ->
                                val placement = placements[index]
                                val colWidth = dayWidth / placement.columns
                                val x = dayWidth * dayIndex + colWidth * placement.column
                                val y = (((clazz.startMin - startHour * 60).toFloat() / 60f) *
                                    HourHeight.value).dp
                                val height = (((clazz.endMin - clazz.startMin).toFloat() / 60f) *
                                    HourHeight.value).dp.coerceAtLeast(18.dp)
                                val width = (colWidth.value - 6f).dp.coerceAtLeast(14.dp)

                                TimetableBlock(
                                    clazz = clazz,
                                    width = width,
                                    height = height,
                                    showRoom = showRoom,
                                    onClick = { onOpenClass(clazz.id) },
                                    modifier = Modifier.offset(x = x, y = y),
                                )
                            }
                        }

                        // "Now" marker on today's column.
                        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
                        val todayIndex = days.indexOf(today.dayOfWeek)
                        if (todayIndex >= 0 && nowMinutes in startHour * 60..endHour * 60) {
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = dayWidth * todayIndex,
                                        y = (((nowMinutes - startHour * 60).toFloat() / 60f) *
                                            HourHeight.value).dp,
                                    )
                                    .width(dayWidth)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableBlock(
    clazz: ClassEntity,
    width: Dp,
    height: Dp,
    showRoom: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = subjectColor(clazz.subjectName)
    val color = base.copy(alpha = if (clazz.isEnabled) 0.92f else 0.38f)
    val textColor = if (clazz.isEnabled && base.luminance() < 0.45f) Color.White else Color(0xFF202B50)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Column {
            Text(
                text = clazz.subjectName,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (height >= 58.dp) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showRoom && clazz.room.isNotBlank() && height >= 54.dp) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = clazz.room,
                    color = textColor.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun hourLabel(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "${h}$suffix"
}