package com.app.easyremind.util

import com.app.easyremind.data.ClassEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleMathTest {

    @Test
    fun dayShort_isIndexedFromMonday() {
        DayOfWeek.entries.forEach { day ->
            assertEquals(day.name.first().toString(), ScheduleMath.dayShort[day.value - 1])
        }
    }

    @Test
    fun bitmaskRoundTrip_preservesEveryDay() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)
        assertEquals(days, ScheduleMath.daySetOf(ScheduleMath.bitmaskOf(days)))
    }

    @Test
    fun nextOccurrence_skipsTodayWhenReminderTimeAlreadyPassed() {
        val now = LocalDateTime.of(2026, 11, 26, 10, 0) // Thursday

        val nextWeek = ScheduleMath.nextOccurrence(now, DayOfWeek.THURSDAY, 615, leadMinutes = 15)
        assertEquals(LocalDate.of(2026, 12, 3), nextWeek)

        val today = ScheduleMath.nextOccurrence(now, DayOfWeek.THURSDAY, 615, leadMinutes = 10)
        assertEquals(now.toLocalDate(), today)
    }

    @Test
    fun conflictsFor_detectsOverlapAndIgnoresTheEditedClass() {
        val existing = ClassEntity(
            id = 7L,
            subjectName = "Math",
            daysBitmask = ScheduleMath.MON,
            startMin = 9 * 60,
            endMin = 11 * 60,
        )
        val overlap = existing.copy(id = 0L, subjectName = "Physics", startMin = 10 * 60, endMin = 12 * 60)

        val conflicts = ScheduleMath.conflictsFor(overlap, listOf(existing))
        assertTrue(conflicts.conflicting.contains(existing))
        assertFalse(conflicts.isDuplicate)

        val editingSelf = ScheduleMath.conflictsFor(existing.copy(startMin = 10 * 60), listOf(existing), ignoreId = 7L)
        assertTrue(editingSelf.conflicting.isEmpty())
    }

    @Test
    fun endOfDay_doesNotCrashAndRollsToNextMidnight() {
        val date = LocalDate.of(2026, 11, 26)

        assertEquals("12:00 AM", ScheduleMath.formatTime(1440))
        assertEquals(date.plusDays(1).atStartOfDay(), ScheduleMath.atTimeOn(date, 1440))
        assertEquals(date.atTime(23, 30), ScheduleMath.atTimeOn(date, 23 * 60 + 30))
        assertTrue(ScheduleMath.isEndOfDay(1440))
    }

    @Test
    fun nextClass_findsCurrentAndUpcoming() {
        val clazz = ClassEntity(
            id = 1L,
            subjectName = "Math",
            daysBitmask = ScheduleMath.bitmaskOf(DayOfWeek.MONDAY),
            startMin = 9 * 60,
            endMin = 10 * 60,
        )

        val current = ScheduleMath.nextClass(listOf(clazz), LocalDateTime.of(2026, 11, 23, 9, 30))
        assertTrue(current is ScheduleMath.NextClass.Current)
        assertEquals(30L, (current as ScheduleMath.NextClass.Current).remainingMin)

        val upcoming = ScheduleMath.nextClass(listOf(clazz), LocalDateTime.of(2026, 11, 23, 8, 0))
        assertTrue(upcoming is ScheduleMath.NextClass.Upcoming)
        assertEquals(60L, (upcoming as ScheduleMath.NextClass.Upcoming).startsInMin)
    }
}
