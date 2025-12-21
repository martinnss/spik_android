package com.spikai.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesService private constructor(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesService? = null
        
        fun getInstance(context: Context): UserPreferencesService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesService(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Keys for preferences
        private const val KEY_NOTIFICATIONS_ENABLED = "notificationsEnabled"
        private const val KEY_DARK_MODE_ENABLED = "darkModeEnabled" 
        private const val KEY_HAPTIC_FEEDBACK_ENABLED = "hapticFeedbackEnabled"
        private const val KEY_SOUND_EFFECTS_ENABLED = "soundEffectsEnabled"
        private const val KEY_AUTO_PLAY_ENABLED = "autoPlayEnabled"
        private const val KEY_PREFERRED_LANGUAGE = "preferredLanguage"
        private const val KEY_REMINDER_TIME = "reminderTime"
        private const val KEY_DIFFICULTY_LEVEL = "difficultyLevel"
        private const val KEY_SESSION_DURATION = "sessionDuration"
        private const val KEY_AI_SPEAKING_SPEED = "user_ai_speaking_speed"
        private const val KEY_HAS_SHOWN_DAILY_REMINDER_POPUP = "hasShownDailyReminderPopup"
        private const val KEY_IS_REMINDER_ENABLED = "isReminderEnabled"
    }
    
    init {
        println("⚙️ [UserPreferencesService] Service initialized")
        println("📱 [UserPreferencesService] Using SharedPreferences: user_preferences")
    }
    
    // MARK: - Notification Settings
    
    var notificationsEnabled: Boolean
        get() {
            val value = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
            println("🔔 [UserPreferencesService] Get notifications enabled: $value")
            return value
        }
        set(value) {
            println("🔔 [UserPreferencesService] Set notifications enabled: $value")
            sharedPreferences.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, value)
                .apply()
        }
    
    // MARK: - Appearance Settings
    
    var darkModeEnabled: Boolean
        get() {
            val value = sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
            println("🌙 [UserPreferencesService] Get dark mode enabled: $value")
            return value
        }
        set(value) {
            println("🌙 [UserPreferencesService] Set dark mode enabled: $value")
            sharedPreferences.edit()
                .putBoolean(KEY_DARK_MODE_ENABLED, value)
                .apply()
        }
    
    // MARK: - Audio/Haptic Settings
    
    var hapticFeedbackEnabled: Boolean
        get() {
            val value = sharedPreferences.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, true)
            println("📳 [UserPreferencesService] Get haptic feedback enabled: $value")
            return value
        }
        set(value) {
            println("📳 [UserPreferencesService] Set haptic feedback enabled: $value")
            sharedPreferences.edit()
                .putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, value)
                .apply()
        }
    
    var soundEffectsEnabled: Boolean
        get() {
            val value = sharedPreferences.getBoolean(KEY_SOUND_EFFECTS_ENABLED, true)
            println("🔊 [UserPreferencesService] Get sound effects enabled: $value")
            return value
        }
        set(value) {
            println("🔊 [UserPreferencesService] Set sound effects enabled: $value")
            sharedPreferences.edit()
                .putBoolean(KEY_SOUND_EFFECTS_ENABLED, value)
                .apply()
        }
    
    // MARK: - Learning Settings
    
    var autoPlayEnabled: Boolean
        get() {
            val value = sharedPreferences.getBoolean(KEY_AUTO_PLAY_ENABLED, false)
            println("▶️ [UserPreferencesService] Get auto play enabled: $value")
            return value
        }
        set(value) {
            println("▶️ [UserPreferencesService] Set auto play enabled: $value")
            sharedPreferences.edit()
                .putBoolean(KEY_AUTO_PLAY_ENABLED, value)
                .apply()
        }
    
    var preferredLanguage: String
        get() {
            val value = sharedPreferences.getString(KEY_PREFERRED_LANGUAGE, "es") ?: "es"
            println("🌐 [UserPreferencesService] Get preferred language: $value")
            return value
        }
        set(value) {
            println("🌐 [UserPreferencesService] Set preferred language: $value")
            sharedPreferences.edit()
                .putString(KEY_PREFERRED_LANGUAGE, value)
                .apply()
        }
    
    private val _reminderTimeFlow = MutableStateFlow(sharedPreferences.getString(KEY_REMINDER_TIME, "20:00") ?: "20:00")
    val reminderTimeFlow: StateFlow<String> = _reminderTimeFlow.asStateFlow()

    var reminderTime: String
        get() {
            val value = sharedPreferences.getString(KEY_REMINDER_TIME, "20:00") ?: "20:00"
            println("⏰ [UserPreferencesService] Get reminder time: $value")
            return value
        }
        set(value) {
            println("⏰ [UserPreferencesService] Set reminder time: $value")
            sharedPreferences.edit()
                .putString(KEY_REMINDER_TIME, value)
                .apply()
            _reminderTimeFlow.value = value
        }
    
    var difficultyLevel: String
        get() {
            val value = sharedPreferences.getString(KEY_DIFFICULTY_LEVEL, "intermedio") ?: "intermedio"
            println("🎯 [UserPreferencesService] Get difficulty level: $value")
            return value
        }
        set(value) {
            println("🎯 [UserPreferencesService] Set difficulty level: $value")
            sharedPreferences.edit()
                .putString(KEY_DIFFICULTY_LEVEL, value)
                .apply()
        }
    
    var sessionDuration: Int
        get() {
            val value = sharedPreferences.getInt(KEY_SESSION_DURATION, 15)
            println("⏱️ [UserPreferencesService] Get session duration: $value minutes")
            return value
        }
        set(value) {
            println("⏱️ [UserPreferencesService] Set session duration: $value minutes")
            sharedPreferences.edit()
                .putInt(KEY_SESSION_DURATION, value)
                .apply()
        }
    
    // MARK: - AI Speaking Speed
    var aiSpeakingSpeed: Double
        get() {
            // Use Float for SharedPreferences since it doesn't support Double directly
            val speed = sharedPreferences.getFloat(KEY_AI_SPEAKING_SPEED, 1.0f).toDouble()
            println("🎤 [UserPreferencesService] Get AI speaking speed: $speed")
            return speed
        }
        set(value) {
            println("🎤 [UserPreferencesService] Set AI speaking speed: $value")
            sharedPreferences.edit()
                .putFloat(KEY_AI_SPEAKING_SPEED, value.toFloat())
                .apply()
        }
        
    // MARK: - Daily Reminder
    
    var hasShownDailyReminderPopup: Boolean
        get() {
            val value = sharedPreferences.getBoolean(KEY_HAS_SHOWN_DAILY_REMINDER_POPUP, false)
            println("🔔 [UserPreferencesService] Get has shown daily reminder popup: $value")
            return value
        }
        set(value) {
            println("🔔 [UserPreferencesService] Set has shown daily reminder popup: $value")
            sharedPreferences.edit()
                .putBoolean(KEY_HAS_SHOWN_DAILY_REMINDER_POPUP, value)
                .apply()
        }

    private val _isReminderEnabledFlow = MutableStateFlow(sharedPreferences.getBoolean(KEY_IS_REMINDER_ENABLED, false))
    val isReminderEnabledFlow: StateFlow<Boolean> = _isReminderEnabledFlow.asStateFlow()

    var isReminderEnabled: Boolean
        get() {
            val value = sharedPreferences.getBoolean(KEY_IS_REMINDER_ENABLED, false)
            println("🔔 [UserPreferencesService] Get is reminder enabled: $value")
            return value
        }
        set(value) {
            println("🔔 [UserPreferencesService] Set is reminder enabled: $value")
            sharedPreferences.edit()
                .putBoolean(KEY_IS_REMINDER_ENABLED, value)
                .apply()
            _isReminderEnabledFlow.value = value
        }

    fun saveReminderTime(hour: Int, minute: Int) {
        val timeString = String.format("%02d:%02d", hour, minute)
        reminderTime = timeString
        isReminderEnabled = true
        println("💾 [UserPreferencesService] Saved daily reminder time: $timeString")
        
        // Analytics
        AnalyticsService.logReminderSettings(true, timeString)
    }
    
    fun getReminderTimeComponents(): Pair<Int, Int> {
        val parts = reminderTime.split(":")
        if (parts.size == 2) {
            return Pair(parts[0].toIntOrNull() ?: 9, parts[1].toIntOrNull() ?: 0)
        }
        return Pair(9, 0)
    }
    
    val hasCustomPreferences: Boolean
        get() = sharedPreferences.all.isNotEmpty()
    
    // MARK: - Utility Methods
    
    fun resetToDefaults() {
        println("🔄 [UserPreferencesService] Resetting all preferences to defaults")
        sharedPreferences.edit().clear().apply()
        println("✅ [UserPreferencesService] All preferences reset successfully")
    }
    
    fun exportPreferences(): Map<String, Any> {
        println("📤 [UserPreferencesService] Exporting all preferences")
        val preferences = mutableMapOf<String, Any>()
        
        preferences[KEY_NOTIFICATIONS_ENABLED] = notificationsEnabled
        preferences[KEY_DARK_MODE_ENABLED] = darkModeEnabled
        preferences[KEY_HAPTIC_FEEDBACK_ENABLED] = hapticFeedbackEnabled
        preferences[KEY_SOUND_EFFECTS_ENABLED] = soundEffectsEnabled
        preferences[KEY_AUTO_PLAY_ENABLED] = autoPlayEnabled
        preferences[KEY_PREFERRED_LANGUAGE] = preferredLanguage
        preferences[KEY_REMINDER_TIME] = reminderTime
        preferences[KEY_DIFFICULTY_LEVEL] = difficultyLevel
        preferences[KEY_SESSION_DURATION] = sessionDuration
        
        println("✅ [UserPreferencesService] Exported ${preferences.size} preferences")
        return preferences
    }
    
    fun importPreferences(preferences: Map<String, Any>) {
        println("📥 [UserPreferencesService] Importing ${preferences.size} preferences")
        
        val editor = sharedPreferences.edit()
        
        preferences[KEY_NOTIFICATIONS_ENABLED]?.let { 
            if (it is Boolean) editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, it)
        }
        preferences[KEY_DARK_MODE_ENABLED]?.let { 
            if (it is Boolean) editor.putBoolean(KEY_DARK_MODE_ENABLED, it)
        }
        preferences[KEY_HAPTIC_FEEDBACK_ENABLED]?.let { 
            if (it is Boolean) editor.putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, it)
        }
        preferences[KEY_SOUND_EFFECTS_ENABLED]?.let { 
            if (it is Boolean) editor.putBoolean(KEY_SOUND_EFFECTS_ENABLED, it)
        }
        preferences[KEY_AUTO_PLAY_ENABLED]?.let { 
            if (it is Boolean) editor.putBoolean(KEY_AUTO_PLAY_ENABLED, it)
        }
        preferences[KEY_PREFERRED_LANGUAGE]?.let { 
            if (it is String) editor.putString(KEY_PREFERRED_LANGUAGE, it)
        }
        preferences[KEY_REMINDER_TIME]?.let { 
            if (it is String) editor.putString(KEY_REMINDER_TIME, it)
        }
        preferences[KEY_DIFFICULTY_LEVEL]?.let { 
            if (it is String) editor.putString(KEY_DIFFICULTY_LEVEL, it)
        }
        preferences[KEY_SESSION_DURATION]?.let { 
            if (it is Int) editor.putInt(KEY_SESSION_DURATION, it)
        }
        
        editor.apply()
        println("✅ [UserPreferencesService] Preferences imported successfully")
    }
    
    // MARK: - Debug Methods
    
    fun debugPreferencesStatus() {
        println("🔍 [UserPreferencesService] === DEBUG PREFERENCES STATUS ===")
        println("🔔 Notifications enabled: $notificationsEnabled")
        println("🌙 Dark mode enabled: $darkModeEnabled")
        println("📳 Haptic feedback enabled: $hapticFeedbackEnabled")
        println("🔊 Sound effects enabled: $soundEffectsEnabled")
        println("▶️ Auto play enabled: $autoPlayEnabled")
        println("🌐 Preferred language: $preferredLanguage")
        println("⏰ Reminder time: $reminderTime")
        println("🎯 Difficulty level: $difficultyLevel")
        println("⏱️ Session duration: $sessionDuration minutes")
        println("===============================================")
    }

    /// Log current reminder settings to Analytics (call on app launch)
    fun logCurrentReminderSettings() {
        val enabled = isReminderEnabled
        val timeString = if (enabled) reminderTime else null
        
        AnalyticsService.logReminderSettings(enabled, timeString)
    }
}
