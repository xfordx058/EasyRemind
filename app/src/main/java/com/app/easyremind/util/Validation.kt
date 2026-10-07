package com.app.easyremind.util

object Validation {

    const val MAX_SUBJECT = 100
    const val MAX_INSTRUCTOR = 100
    const val MAX_ROOM = 50

    sealed interface FieldError {
        data class Subject(val message: String) : FieldError
        data object InstructorTooLong : FieldError
        data object RoomTooLong : FieldError
        data object NoDays : FieldError
        data object NoStart : FieldError
        data object NoEnd : FieldError
        data object EndBeforeStart : FieldError
        data object SameTime : FieldError
    }

    private val reminderOptions = listOf(0, 5, 10, 15, 30, 60)

    fun validReminder(minutes: Int): Boolean = minutes in reminderOptions

    fun clampReminder(minutes: Int): Int =
        if (minutes in reminderOptions) minutes else 15

    fun validate(
        subject: String,
        instructor: String,
        room: String,
        hasDay: Boolean,
        startMin: Int,
        endMin: Int,
        isStartSet: Boolean,
        isEndSet: Boolean,
    ): List<FieldError> {
        val errors = mutableListOf<FieldError>()
        val s = subject.trim()
        if (s.isEmpty()) {
            errors += FieldError.Subject("Subject is required.")
        } else if (s.length > MAX_SUBJECT) {
            errors += FieldError.Subject("Subject must be under $MAX_SUBJECT characters.")
        }
        if (instructor.length > MAX_INSTRUCTOR) errors += FieldError.InstructorTooLong
        if (room.length > MAX_ROOM) errors += FieldError.RoomTooLong
        if (!hasDay) errors += FieldError.NoDays
        if (!isStartSet) errors += FieldError.NoStart
        if (!isEndSet) errors += FieldError.NoEnd
        if (isStartSet && isEndSet) {
            if (endMin < startMin) errors += FieldError.EndBeforeStart
            if (endMin == startMin) errors += FieldError.SameTime
        }
        return errors
    }

    fun humanMessage(error: FieldError): String = when (error) {
        is FieldError.Subject -> error.message
        FieldError.InstructorTooLong -> "Instructor must be under $MAX_INSTRUCTOR characters."
        FieldError.RoomTooLong -> "Room must be under $MAX_ROOM characters."
        FieldError.NoDays -> "Select at least one day."
        FieldError.NoStart -> "Select a start time."
        FieldError.NoEnd -> "Select an end time."
        FieldError.EndBeforeStart -> "End time must be later than start time."
        FieldError.SameTime -> "End time must be later than start time."
    }
}