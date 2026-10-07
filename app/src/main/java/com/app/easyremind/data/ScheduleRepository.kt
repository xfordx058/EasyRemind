package com.app.easyremind.data

import com.app.easyremind.reminder.ReminderScheduler
import com.app.easyremind.util.ScheduleMath
import com.app.easyremind.util.Validation
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduleRepository(
    private val db: AppDatabase,
    private val reminderScheduler: ReminderScheduler,
) {

    val classesFlow: Flow<List<ClassEntity>> = db.classDao().observeAll()
    val settingsFlow: Flow<SettingsEntity?> = db.settingsDao().observe()

    suspend fun getAllClasses(): List<ClassEntity> = db.classDao().getAll()

    suspend fun getClass(id: Long): ClassEntity? = db.classDao().getById(id)

    suspend fun getSettings(): SettingsEntity {
        db.settingsDao().get()?.let { return it }
        val defaults = SettingsEntity()
        db.settingsDao().upsert(defaults)
        return defaults
    }

    suspend fun updateSettings(settings: SettingsEntity) {
        db.settingsDao().upsert(settings)
        if (!settings.notificationsEnabled) {
            db.classDao().getAll().forEach { reminderScheduler.cancelClass(it.id) }
        } else {
            rescheduleAll()
        }
    }

    sealed interface SaveResult {
        data object Ok : SaveResult
        data class Failed(val message: String) : SaveResult
        data class Conflict(val conflicts: List<ClassEntity>) : SaveResult
    }

    suspend fun saveClass(clazz: ClassEntity, isNew: Boolean): SaveResult {
        val subject = clazz.subjectName.trim()
        if (subject.isEmpty()) return SaveResult.Failed("Subject is required.")
        if (subject.length > Validation.MAX_SUBJECT) {
            return SaveResult.Failed("Subject must be under ${Validation.MAX_SUBJECT} characters.")
        }
        if (clazz.instructor.length > Validation.MAX_INSTRUCTOR) {
            return SaveResult.Failed("Instructor must be under ${Validation.MAX_INSTRUCTOR} characters.")
        }
        if (clazz.room.length > Validation.MAX_ROOM) {
            return SaveResult.Failed("Room must be under ${Validation.MAX_ROOM} characters.")
        }
        if (clazz.daysBitmask == 0) return SaveResult.Failed("Select at least one day.")
        if (clazz.endMin <= clazz.startMin) {
            return SaveResult.Failed("End time must be later than start time.")
        }
        if (!Validation.validReminder(clazz.reminderMinutes)) {
            return SaveResult.Failed("Select a valid reminder time.")
        }

        val all = db.classDao().getAll()
        val info = ScheduleMath.conflictsFor(clazz, all, ignoreId = if (isNew) -1L else clazz.id)
        if (info.isDuplicate) {
            return SaveResult.Failed("This class already exists in your schedule.")
        }
        if (info.conflicting.isNotEmpty()) {
            return SaveResult.Conflict(info.conflicting)
        }

        val updated = clazz.copy(
            subjectName = subject,
            updatedAt = System.currentTimeMillis(),
        )
        if (isNew) {
            val savedId = db.classDao().insert(updated)
            reminderScheduler.rescheduleClass(updated.copy(id = savedId))
        } else {
            db.classDao().update(updated)
            reminderScheduler.rescheduleClass(updated)
        }
        return SaveResult.Ok
    }

    suspend fun deleteClass(id: Long) {
        reminderScheduler.cancelClass(id)
        db.classDao().deleteById(id)
    }

    suspend fun setEnabled(clazz: ClassEntity, enabled: Boolean) {
        val updated = clazz.copy(isEnabled = enabled, updatedAt = System.currentTimeMillis())
        db.classDao().update(updated)
        reminderScheduler.rescheduleClass(updated)
    }

    suspend fun rescheduleAll() {
        for (clazz in db.classDao().getAll()) {
            reminderScheduler.rescheduleClass(clazz)
        }
    }

    // ----------------------------------------------------------------- backup

    suspend fun exportJson(): String {
        val settings = db.settingsDao().get() ?: SettingsEntity()
        val classes = db.classDao().getAll()
        val root = JSONObject().apply {
            put("app", "EasyRemind")
            put("formatVersion", FORMAT_VERSION)
            put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put("settings", settingsToJson(settings))
            put("classes", JSONArray().apply {
                classes.forEach {
                    put(
                        JSONObject()
                            .put("subjectName", it.subjectName)
                            .put("instructor", it.instructor)
                            .put("room", it.room)
                            .put("days", JSONArray().apply {
                                ScheduleMath.daySetOf(it.daysBitmask)
                                    .sortedBy { day -> day.value }
                                    .forEach { d -> put(d.value) }
                            })
                            .put("startMin", it.startMin)
                            .put("endMin", it.endMin)
                            .put("reminderMinutes", it.reminderMinutes)
                            .put("isEnabled", it.isEnabled)
                    )
                }
            })
        }
        return root.toString(2)
    }

    sealed interface ImportResult {
        data class Success(val classesImported: Int) : ImportResult
        data class Error(val message: String) : ImportResult
    }

    suspend fun importJson(text: String): ImportResult {
        return try {
            val root = JSONObject(text)
            if (root.optString("app") != "EasyRemind" || !root.has("classes")) {
                return ImportResult.Error("This doesn't look like an Easy Remind backup file.")
            }
            val classesJson = root.getJSONArray("classes")
            val imported = mutableListOf<ClassEntity>()
            for (i in 0 until classesJson.length()) {
                val obj = classesJson.getJSONObject(i)
                val subject = obj.optString("subjectName", "").trim()
                val daysArray = obj.optJSONArray("days")
                val days = if (daysArray == null) emptySet() else
                    (0 until daysArray.length()).mapNotNull { idx ->
                        DayOfWeek.entries.firstOrNull { it.value == daysArray.getInt(idx) }
                    }.toSet()
                val startMin = obj.optInt("startMin", -1)
                val endMin = obj.optInt("endMin", -1)
                if (subject.isEmpty()) {
                    return ImportResult.Error("Class ${i + 1} is missing a subject.")
                }
                if (days.isEmpty()) {
                    return ImportResult.Error("Class \"$subject\" has no days selected.")
                }
                if (startMin < 0 || endMin <= startMin) {
                    return ImportResult.Error("Class \"$subject\" has an invalid time range.")
                }
                imported += ClassEntity(
                    id = 0L,
                    subjectName = subject,
                    instructor = obj.optString("instructor", ""),
                    room = obj.optString("room", ""),
                    daysBitmask = ScheduleMath.bitmaskOf(days),
                    startMin = startMin,
                    endMin = endMin,
                    reminderMinutes = Validation.clampReminder(obj.optInt("reminderMinutes", 15)),
                    isEnabled = obj.optBoolean("isEnabled", true),
                )
            }

            db.classDao().clearAll()
            for (c in imported) {
                db.classDao().insert(c)
            }
            root.optJSONObject("settings")?.let { json ->
                settingsToEntity(json)?.let { entity -> settingsDao.upsert(entity) }
            }
            rescheduleAll()
            ImportResult.Success(imported.size)
        } catch (e: JSONException) {
            ImportResult.Error("The backup file could not be read: ${e.message ?: "invalid JSON"}")
        }
    }

    private val settingsDao = db.settingsDao()

    private fun settingsToJson(settings: SettingsEntity): JSONObject =
        JSONObject()
            .put("notificationsEnabled", settings.notificationsEnabled)
            .put("defaultReminderMinutes", settings.defaultReminderMinutes)
            .put("scheduleView", settings.scheduleView)
            .put("focusDuration", settings.focusDuration)
            .put("shortBreakDuration", settings.shortBreakDuration)
            .put("longBreakDuration", settings.longBreakDuration)
            .put("sessionCount", settings.sessionCount)
            .put("soundEnabled", settings.soundEnabled)
            .put("vibrationEnabled", settings.vibrationEnabled)
            .put("theme", settings.theme)
            .put("showInstructor", settings.showInstructor)
            .put("showRoom", settings.showRoom)
            .put("weekStartsOn", settings.weekStartsOn)

    private fun settingsToEntity(obj: JSONObject): SettingsEntity? = try {
        val theme = obj.optString("theme", "system")
        val weekDay = obj.optString("weekStartsOn", "MONDAY")
        SettingsEntity(
            notificationsEnabled = obj.optBoolean("notificationsEnabled", true),
            defaultReminderMinutes = Validation.clampReminder(obj.optInt("defaultReminderMinutes", 15)),
            scheduleView = if (obj.optString("scheduleView", "list") == "grid") "grid" else "list",
            focusDuration = obj.optInt("focusDuration", 25).coerceIn(1, 120),
            shortBreakDuration = obj.optInt("shortBreakDuration", 5).coerceIn(1, 60),
            longBreakDuration = obj.optInt("longBreakDuration", 15).coerceIn(1, 90),
            sessionCount = obj.optInt("sessionCount", 4).coerceIn(1, 12),
            soundEnabled = obj.optBoolean("soundEnabled", true),
            vibrationEnabled = obj.optBoolean("vibrationEnabled", true),
            theme = if (theme in setOf("light", "dark", "system")) theme else "system",
            showInstructor = obj.optBoolean("showInstructor", true),
            showRoom = obj.optBoolean("showRoom", true),
            weekStartsOn = if (weekDay in DayOfWeek.entries.map { it.name }) weekDay else "MONDAY",
        )
    } catch (_: Exception) {
        null
    }

    companion object {
        const val FORMAT_VERSION = 1
    }
}