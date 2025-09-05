package com.spikai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// TODO: Re-enable BuildConfig import when compilation issues are resolved
// import com.spikai.BuildConfig
import com.spikai.service.GoogleSignInManager
import com.spikai.service.PreferencesManager

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesManager = PreferencesManager.getInstance(application)
    private val googleSignInManager = GoogleSignInManager.getInstance(application)
    
    // Onboarding state
    val hasCompletedOnboarding: StateFlow<Boolean> = preferencesManager.hasCompletedOnboarding
    
    // Sign-in state - directly use GoogleSignInManager's state
    val isSignedIn: StateFlow<Boolean> = googleSignInManager.isSignedIn
    
    init {
        // In debug mode, clear all data on app launch
        // TODO: Re-enable debug data clearing when BuildConfig is available
        // if (BuildConfig.DEBUG) {
        //     clearDebugData()
        // }
    }
    
    fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }
    
    fun refreshSignInStatus() {
        // GoogleSignInManager automatically manages its state through Firebase auth listener
        // No explicit refresh needed since we're using its StateFlow directly
    }
    
    private fun clearDebugData() {
        // TODO: Re-enable debug data clearing when BuildConfig is available
        // if (BuildConfig.DEBUG) {
            preferencesManager.clearAllData()
            // TODO: Clear Google Sign-In data in debug mode if needed
        // }
    }
}
