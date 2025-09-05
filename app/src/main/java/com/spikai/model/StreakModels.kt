package com.spikai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

// MARK: - API Response Models
@Serializable
data class StreakResponse(
    @SerialName("has_streak")
    val hasStreak: Boolean,
    @SerialName("streak_days")
    val streakDays: Int,
    @SerialName("last_session_date")
    val lastSessionDate: String? = null,
    @SerialName("streak_start_date")
    val streakStartDate: String? = null,
    val message: String
)

// MARK: - Streak Models
@Serializable
data class StreakData(
    val currentStreak: Int,
    val weeklyProgress: List<DayProgress>,
    val hasStreak: Boolean,
    val lastSessionDate: @Serializable(with = DateSerializer::class) Date? = null,
    val streakStartDate: @Serializable(with = DateSerializer::class) Date? = null
) {
    companion object {
        val mockData = StreakData(
            currentStreak = 3,
            weeklyProgress = listOf(
                DayProgress(dayName = "L", dayIndex = 0, isCompleted = true),   // Lunes
                DayProgress(dayName = "M", dayIndex = 1, isCompleted = true),   // Martes
                DayProgress(dayName = "X", dayIndex = 2, isCompleted = true),   // Miércoles
                DayProgress(dayName = "J", dayIndex = 3, isCompleted = false),  // Jueves
                DayProgress(dayName = "V", dayIndex = 4, isCompleted = false),  // Viernes
                DayProgress(dayName = "S", dayIndex = 5, isCompleted = false),  // Sábado
                DayProgress(dayName = "D", dayIndex = 6, isCompleted = false)   // Domingo
            ),
            hasStreak = true,
            lastSessionDate = Date(),
            streakStartDate = Calendar.getInstance().apply {
                time = Date()
                add(Calendar.DAY_OF_MONTH, -2)
            }.time
        )

        /// Create StreakData from API response
        fun fromResponse(response: StreakResponse): StreakData {
            println("🔍 [StreakData] Creating from response:")
            println("   - streakDays: ${response.streakDays}")
            println("   - hasStreak: ${response.hasStreak}")
            println("   - lastSessionDate (raw): ${response.lastSessionDate ?: "null"}")
            println("   - streakStartDate (raw): ${response.streakStartDate ?: "null"}")

            val lastSessionDate = parseDate(response.lastSessionDate, "lastSessionDate")
            val streakStartDate = parseDate(response.streakStartDate, "streakStartDate")

            println("   - lastSessionDate (parsed): ${lastSessionDate?.toString() ?: "null"}")
            println("   - streakStartDate (parsed): ${streakStartDate?.toString() ?: "null"}")

            // Generate weekly progress based on current date and streak
            val weeklyProgress = generateWeeklyProgress(
                streakDays = response.streakDays,
                lastSessionDate = lastSessionDate
            )

            return StreakData(
                currentStreak = response.streakDays,
                hasStreak = response.hasStreak,
                lastSessionDate = lastSessionDate,
                streakStartDate = streakStartDate,
                weeklyProgress = weeklyProgress
            )
        }

        /// Parse date string with multiple possible formats
        private fun parseDate(dateString: String?, fieldName: String): Date? {
            if (dateString == null) {
                println("📭 [StreakData] $fieldName is null")
                return null
            }

            println("🔍 [StreakData] Parsing $fieldName: '$dateString'")

            // Try different date formatters
            val formatters = listOf(
                // ISO8601 with milliseconds: "2025-08-12T10:30:45.123Z"
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                } to "ISO8601 with milliseconds",

                // ISO8601 without milliseconds: "2025-08-12T10:30:45Z"
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                } to "ISO8601 without milliseconds",

                // Simple date: "2025-08-12"
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                } to "Simple date",

                // Firebase Timestamp: "2025-08-12 10:30:45"
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                } to "Firebase timestamp"
            )

            for ((formatter, description) in formatters) {
                try {
                    val date = formatter.parse(dateString)
                    if (date != null) {
                        println("✅ [StreakData] Successfully parsed $fieldName using $description: $date")
                        return date
                    }
                } catch (e: Exception) {
                    println("❌ [StreakData] Failed to parse $fieldName using $description: ${e.message}")
                }
            }

            println("❌ [StreakData] Failed to parse $fieldName with any formatter")
            return null
        }

        private fun generateWeeklyProgress(streakDays: Int, lastSessionDate: Date?): List<DayProgress> {
            val calendar = Calendar.getInstance()
            val today = Date()
            val dayNames = listOf("L", "M", "X", "J", "V", "S", "D") // Lunes = 0, Martes = 1, etc.

            // Get the start of the current week (Monday)
            calendar.time = today
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            
            // Set to start of day
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.time

            val weeklyProgress = mutableListOf<DayProgress>()

            for (dayIndex in 0..6) {
                calendar.time = startOfWeek
                calendar.add(Calendar.DAY_OF_MONTH, dayIndex)
                val dayDate = calendar.time
                val dayName = dayNames[dayIndex]

                // Check if this day should be marked as completed
                // This is a simplified logic - in reality, you might want to check against actual session dates
                val isCompleted: Boolean = if (lastSessionDate != null) {
                    val lastSessionCalendar = Calendar.getInstance().apply { time = lastSessionDate }
                    val dayCalendar = Calendar.getInstance().apply { time = dayDate }
                    val todayCalendar = Calendar.getInstance().apply { time = today }
                    
                    val daysSinceLastSession = ((lastSessionCalendar.timeInMillis - dayCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    daysSinceLastSession >= 0 && daysSinceLastSession < streakDays && dayDate.time <= today.time
                } else {
                    false
                }

                weeklyProgress.add(
                    DayProgress(
                        dayName = dayName,
                        dayIndex = dayIndex,
                        isCompleted = isCompleted
                    )
                )
            }

            return weeklyProgress
        }
    }
}

@Serializable
data class DayProgress(
    val dayName: String,
    val dayIndex: Int,
    val isCompleted: Boolean
)
