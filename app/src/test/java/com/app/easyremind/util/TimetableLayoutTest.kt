package com.app.easyremind.util

import com.app.easyremind.data.ClassEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TimetableLayoutTest {

    private fun cls(id: Long, start: Int, end: Int) = ClassEntity(
        id = id,
        subjectName = "S$id",
        startMin = start,
        endMin = end,
    )

    @Test
    fun nonOverlappingClasses_allUseColumnZero() {
        val placements = TimetableLayout.placeDayClasses(
            listOf(cls(1, 8 * 60, 9 * 60), cls(2, 9 * 60, 10 * 60)),
        )
        assertEquals(listOf(0, 0), placements.map { it.column })
        assertEquals(listOf(1, 1), placements.map { it.columns })
    }

    @Test
    fun overlappingClasses_shareColumnsSideBySide() {
        val placements = TimetableLayout.placeDayClasses(
            listOf(cls(1, 8 * 60, 10 * 60), cls(2, 9 * 60, 11 * 60)),
        )
        assertEquals(listOf(0, 1), placements.map { it.column })
        assertEquals(listOf(2, 2), placements.map { it.columns })
    }

    @Test
    fun threeOverlappingClasses_getThreeDistinctColumns() {
        val placements = TimetableLayout.placeDayClasses(
            listOf(
                cls(1, 8 * 60, 11 * 60),
                cls(2, 8 * 60 + 30, 10 * 60),
                cls(3, 9 * 60, 12 * 60),
            ),
        )
        assertEquals(listOf(0, 1, 2), placements.map { it.column })
        assertEquals(listOf(3, 3, 3), placements.map { it.columns })
    }

    @Test
    fun placements_keepInputOrderAndHandleDuplicateIds() {
        val placements = TimetableLayout.placeDayClasses(
            listOf(cls(0, 9 * 60, 10 * 60), cls(0, 8 * 60, 9 * 60)),
        )
        // Input order preserved: first class starts at 9, second at 8.
        assertEquals(listOf(0, 0), placements.map { it.column })
    }
}
