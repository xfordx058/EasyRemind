package com.app.easyremind.util

import com.app.easyremind.data.ClassEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object ScheduleMath {

    const val MON = 1 shl 0
    const val TUE = 1 shl 1
    const val WED = 1 shl 2
    const val THU = 1 shl 3
    const val FRI = 1 shl 4
    const val SAT = 1 shl 5
    const val SUN = 1 shl 6

    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val dayShort = listOf("S", "M", "T", "W", "T", "F", "S")

    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    fun bitFor(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun daySetOf(bitmask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { bitmask and bitFor(it) != 0 }.toSet()

    fun hasDay(bitmask: Int, day: DayOfWeek): Boolean = bitmask and bitFor(day) != 0

    fun bitmaskOf(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or bitFor(d) }

    fun bitmaskOf(vararg days: DayOfWeek): Int = bitmaskOf(days.toSet())

    fun formatTime(minutes: Int): String =
        LocalTime.of(minutes / 60, minutes % 60).format(timeFormat)

    fun formatTimeRange(startMin: Int, endMin: Int): String =
        "${formatTime(startMin)} – ${formatTime(endMin)}"

    fun formatDuration(minutes: Long): String {
        return when {
            minutes < 1 -> "less than a minute"
            minutes == 1L -> "1 minute"
            minutes < 60 -> "$minutes minutes"
            minutes % 60 == 0L -> "${minutes / 60} hour${if (minutes > 60) "s" else ""}"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    fun dayNamesOf(bitmask: Int): String {
        val set = daySetOf(bitmask)
        return if (set.isEmpty()) "" else {
            val sorted = set.sortedBy { it.value }
            if (sorted.size == 7) "Every day"
            else sorted.joinToString(", ") { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        }
    }

    /** Minutes between now and the given start (of the same/any day). */
    fun minutesUntil(now: LocalDateTime, occurrence: LocalDate, startMin: Int): Long =
        ChronoUnit.MINUTES.between(now, occurrence.atTime(startMin / 60, startMin % 60))

    fun minutesEnding(now: LocalDateTime, occurrence: LocalDate, endMin: Int): Long =
        ChronoUnit.MINUTES.between(now, occurrence.atTime(endMin / 60, endMin % 60))

    /** Next weekday equal to [day] on/after [start] such that start time is in the future at [now]. */
    fun nextOccurrence(now: LocalDateTime, day: DayOfWeek, startMin: Int): LocalDate {
        val today = now.toLocalDate()
        val todayMinutes = now.hour * 60 + now.minute
        var candidate = today
        var guard = 0
        while (guard < 8) {
            if (candidate.dayOfWeek == day) {
                if (candidate != today || startMin > todayMinutes) return candidate
            }
            candidate = candidate.plusDays(1)
            guard++
        }
        return today.plusDays(7)
    }

    // ----------------------------------------------------------------- next class

    sealed interface NextClass {
        data object NoClasses : NextClass
        data class Current(val clazz: ClassEntity, val remainingMin: Long) : NextClass
        data class Upcoming(val clazz: ClassEntity, val date: LocalDate, val startsInMin: Long) : NextClass
    }

    fun nextClass(classes: List<ClassEntity>, now: LocalDateTime = LocalDateTime.now()): NextClass {
        val active = classes.filter { it.isEnabled && hasDay(it.daysBitmask, now.dayOfWeek) }
        val todayMinutes = now.hour * 60 + now.minute

        // Current class (end will always be > start due to validation)
        active.sortedBy { it.startMin }.firstOrNull {
            it.startMin <= todayMinutes && todayMinutes < it.endMin
        }?.let {
            return NextClass.Current(it, minutesEnding(now, now.toLocalDate(), it.endMin))
        }

        // Next class today
        val nextToday = active.sortedBy { it.startMin }.firstOrNull { it.startMin > todayMinutes }
        if (nextToday != null) {
            return NextClass.Upcoming(nextToday, now.toLocalDate(), nextToday.startMin - todayMinutes.toLong())
        }

        // Next class on a future weekday (today -> today+7)
        val enabled = classes.filter { it.isEnabled }
        if (enabled.isEmpty()) return NextClass.NoClasses

        var best: Pair<ClassEntity, LocalDate>? = null
        for (offset in 1..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val dayClasses = enabled
                .filter { hasDay(it.daysBitmask, date.dayOfWeek) }
                .sortedBy { it.startMin }
            if (dayClasses.isNotEmpty()) {
                best = dayClasses.first() to date
                break
            }
        }
        return best?.let { (c, d) ->
            NextClass.Upcoming(c, d, minutesUntil(now, d, c.startMin))
        } ?: NextClass.NoClasses
    }

    // ------------------------------------------------------------ conflicts

    data class ConflictInfo(
        val conflicting: List<ClassEntity>,
        val isDuplicate: Boolean,
    )

    fun conflictsFor(
        newClass: ClassEntity,
        existing: List<ClassEntity>,
        ignoreId: Long = -1L,
    ): ConflictInfo {
        val sameTiming = existing.any {
            it.id != ignoreId &&
                it.daysBitmask == newClass.daysBitmask &&
                it.startMin == newClass.startMin &&
                it.endMin == newClass.endMin &&
                it.subjectName.trim().equals(newClass.subjectName.trim(), ignoreCase = true)
        }

        val overlaps = existing.filter { c ->
            c.id != ignoreId &&
                c.daysBitmask != 0 &&
                (c.daysBitmask and newClass.daysBitmask) != 0 &&
                newClass.startMin < c.endMin &&
                newClass.endMin > c.startMin
        }
        return ConflictInfo(conflicting = overlaps, isDuplicate = sameTiming)
    }
}