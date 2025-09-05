package com.spikai.service

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val _hasCompletedOnboarding = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()
    
    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed)
        }
        _hasCompletedOnboarding.value = completed
    }
    
    fun clearAllData() {
        sharedPreferences.edit {
            clear()
        }
        _hasCompletedOnboarding.value = false
    }
    
    companion object {
        private const val PREFS_NAME = "spik_ai_prefs"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "hasCompletedOnboarding"
        
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
