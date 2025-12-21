package com.spikai.service

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.spikai.BuildConfig

object AnalyticsService {
    private val analytics: FirebaseAnalytics = Firebase.analytics

    // MARK: - Generic Logging
    fun logEvent(name: String, parameters: Map<String, Any>? = null) {
        val bundle = Bundle()
        parameters?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putBoolean(key, value)
            }
        }

        // Add default parameters
        bundle.putString("platform", "Android")
        bundle.putString("app_version", BuildConfig.VERSION_NAME)
        bundle.putString("os_version", android.os.Build.VERSION.RELEASE)

        analytics.logEvent(name, bundle)
        println("📊 [Analytics] Logged event: $name, params: $parameters")
    }

    // MARK: - Auth Events
    fun logLogin(method: String) {
        logEvent(FirebaseAnalytics.Event.LOGIN, mapOf(
            FirebaseAnalytics.Param.METHOD to method
        ))
    }

    fun logSignUp(method: String) {
        logEvent(FirebaseAnalytics.Event.SIGN_UP, mapOf(
            FirebaseAnalytics.Param.METHOD to method
        ))
    }

    fun logSignOut() {
        logEvent("sign_out")
    }

    fun logDeleteAccount() {
        logEvent("delete_account")
    }

    // MARK: - Conversation Events
    fun logConversationStart(levelId: Int?, speed: Double) {
        val params = mutableMapOf<String, Any>(
            "ai_speed" to speed
        )
        if (levelId != null) {
            params["level_id"] = levelId
        }
        logEvent("conversation_start", params)
    }

    fun logConversationEnd(duration: Double, messageCount: Int) {
        logEvent("conversation_end", mapOf(
            "duration" to duration,
            "message_count" to messageCount
        ))
    }

    fun logConversationAnalysisViewed(score: Int?) {
        val params = mutableMapOf<String, Any>()
        if (score != null) {
            params["score"] = score
        }
        logEvent("conversation_analysis_viewed", params)
    }

    // MARK: - Progression Events
    fun logLevelUp(level: Int) {
        logEvent(FirebaseAnalytics.Event.LEVEL_UP, mapOf(
            FirebaseAnalytics.Param.LEVEL to level.toLong()
        ))
    }

    fun logCareerMapViewed() {
        logEvent("career_map_viewed")
    }

    // MARK: - Onboarding Events
    fun logOnboardingStart() {
        logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN)
    }

    fun logOnboardingComplete() {
        logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE)
    }

    // MARK: - Error Events
    fun logError(domain: String, code: Int, description: String) {
        logEvent("app_error", mapOf(
            "error_domain" to domain,
            "error_code" to code,
            "error_description" to description
        ))
    }

    // MARK: - User Properties
    fun setUserProperty(userId: String) {
        analytics.setUserId(userId)
    }

    fun setUserProperty(key: String, value: String?) {
        analytics.setUserProperty(key, value)
    }

    fun logReminderSettings(enabled: Boolean, time: String?) {
        setUserProperty("reminder_enabled", if (enabled) "true" else "false")
        setUserProperty("reminder_time", time ?: "none")
    }
}
