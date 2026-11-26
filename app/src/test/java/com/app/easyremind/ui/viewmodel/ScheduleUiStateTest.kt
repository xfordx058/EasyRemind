package com.app.easyremind.ui.viewmodel

import com.app.easyremind.data.ClassEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleUiStateTest {

    private val math = ClassEntity(
        id = 1L,
        subjectName = "Mathematics",
        instructor = "Sir Jose",
        room = "Room 204",
    )
    private val physics = ClassEntity(
        id = 2L,
        subjectName = "Physics",
        instructor = "Ma'am Ana",
        room = "Lab 1",
    )
    private val state = ScheduleUiState(classes = listOf(math, physics))

    @Test
    fun blankSearch_showsEveryClass() {
        assertEquals(listOf(math, physics), state.filteredClasses)
        assertFalse(state.hasActiveSearch)
    }

    @Test
    fun search_isCaseInsensitiveAndTrimmed() {
        val result = state.copy(searchQuery = "  mAtHeMa  ")
        assertEquals(listOf(math), result.filteredClasses)
        assertTrue(result.hasActiveSearch)
    }

    @Test
    fun search_matchesInstructorAndRoom() {
        assertEquals(listOf(math), state.copy(searchQuery = "jose").filteredClasses)
        assertEquals(listOf(math), state.copy(searchQuery = "204").filteredClasses)
        assertEquals(listOf(physics), state.copy(searchQuery = "lab").filteredClasses)
    }

    @Test
    fun search_withNoMatches_returnsEmptyList() {
        assertTrue(state.copy(searchQuery = "chemistry").filteredClasses.isEmpty())
    }
}
