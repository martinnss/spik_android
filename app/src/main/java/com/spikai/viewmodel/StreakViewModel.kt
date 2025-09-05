package com.spikai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spikai.model.DayProgress
import com.spikai.model.StreakData
import com.spikai.service.ErrorHandlingService
import com.spikai.service.StreakService
import com.spikai.service.StreakServiceProtocol
import com.spikai.service.SpikError
import com.spikai.service.SpikErrorException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

class StreakViewModel(
    private val streakService: StreakServiceProtocol,
    private val context: Context
) : ViewModel() {
    
    // MARK: - StateFlow Properties (equivalent to @Published)
    private val _streakData = MutableStateFlow(createInactiveStreak())
    val streakData: StateFlow<StreakData> = _streakData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()
    
    private val _isStreakActive = MutableStateFlow(true) // New property to control streak visual state
    val isStreakActive: StateFlow<Boolean> = _isStreakActive.asStateFlow()
    
    private val errorHandler = ErrorHandlingService.shared
    
    // Dependency injection - never create services directly inside ViewModels
    constructor(context: Context, streakService: StreakServiceProtocol = StreakService.getInstance(context)) : this(streakService, context)
    
    init {
        // Initialize with inactive streak for first-time users
        _streakData.value = createInactiveStreak()
        _isStreakActive.value = false
    }
    
    // MARK: - Public Methods
    fun loadStreak() {
        println("🎯 [StreakVM] loadStreak called")
        viewModelScope.launch {
            _isLoading.value = true
            clearError()
            
            try {
                val loadedStreakData = streakService.getCurrentStreak()
                _streakData.value = loadedStreakData
                println("✅ [StreakVM] Streak loaded - current: ${loadedStreakData.currentStreak}, hasStreak: ${loadedStreakData.hasStreak}")
                println("📅 [StreakVM] Last session date: ${loadedStreakData.lastSessionDate?.toString() ?: "null"}")
                println("📅 [StreakVM] Streak start date: ${loadedStreakData.streakStartDate?.toString() ?: "null"}")
                // Check if streak needs to be "lit" today
                updateStreakActiveState()
                println("🎯 [StreakVM] Final state - streakText: '${streakText}', isStreakActive: ${_isStreakActive.value}")
            } catch (error: Exception) {
                println("❌ [StreakVM] Error loading streak: $error")
                handleError(error)
            }
            
            _isLoading.value = false
        }
    }
    
    fun refreshStreak() {
        loadStreak()
    }
    
    fun markDayComplete(dayIndex: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            clearError()
            
            try {
                _streakData.value = streakService.updateDayProgress(dayIndex = dayIndex, isCompleted = true)
            } catch (error: Exception) {
                handleError(error)
            }
            
            _isLoading.value = false
        }
    }
    
    fun resetStreak() {
        viewModelScope.launch {
            _isLoading.value = true
            clearError()
            
            try {
                _streakData.value = streakService.resetStreak()
            } catch (error: Exception) {
                handleError(error)
            }
            
            _isLoading.value = false
        }
    }
    
    /// Call this when user completes a level to invalidate cache and refresh
    fun refreshAfterLevelCompletion() {
        viewModelScope.launch {
            // Clear cache to force fresh data after level completion
            if (streakService is StreakService) {
                streakService.clearCache()
            }
            loadStreak()
        }
    }
    
    /// Call this when user logs in to refresh streak data
    fun refreshAfterLogin() {
        viewModelScope.launch {
            // Clear cache to force fresh data after login
            if (streakService is StreakService) {
                streakService.clearCache()
            }
            loadStreak()
        }
    }
    
    // MARK: - Error Handling
    fun clearError() {
        _errorMessage.value = null
        _hasError.value = false
    }
    
    private fun handleError(error: Exception) {
        if (error is SpikErrorException) {
            _errorMessage.value = error.spikError.errorDescription
        } else {
            _errorMessage.value = "Error al cargar la racha"
        }
        _hasError.value = true
    }
    
    // MARK: - Streak State Management
    private fun createInactiveStreak(): StreakData {
        return StreakData(
            currentStreak = 0,
            weeklyProgress = listOf(
                DayProgress(dayName = "L", dayIndex = 0, isCompleted = false),
                DayProgress(dayName = "M", dayIndex = 1, isCompleted = false),
                DayProgress(dayName = "X", dayIndex = 2, isCompleted = false),
                DayProgress(dayName = "J", dayIndex = 3, isCompleted = false),
                DayProgress(dayName = "V", dayIndex = 4, isCompleted = false),
                DayProgress(dayName = "S", dayIndex = 5, isCompleted = false),
                DayProgress(dayName = "D", dayIndex = 6, isCompleted = false)
            ),
            hasStreak = false,
            lastSessionDate = null,
            streakStartDate = null
        )
    }
    
    private fun updateStreakActiveState() {
        println("🔍 [StreakVM] updateStreakActiveState called")
        println("🔍 [StreakVM] Current streak: ${_streakData.value.currentStreak}")
        println("🔍 [StreakVM] Has streak: ${_streakData.value.hasStreak}")
        println("🔍 [StreakVM] Last session date: ${_streakData.value.lastSessionDate?.toString() ?: "null"}")
        
        val lastSessionDate = _streakData.value.lastSessionDate
        if (lastSessionDate == null) {
            // No sessions yet, streak is inactive
            println("📭 [StreakVM] No last session date - streak inactive")
            _isStreakActive.value = false
            return
        }
        
        val calendar = Calendar.getInstance()
        val today = Date()
        
        println("📅 [StreakVM] Today: $today")
        println("📅 [StreakVM] Last session: $lastSessionDate")
        
        // Check if user has played today or within the last day
        val isToday = isSameDay(lastSessionDate, today)
        val yesterday = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_MONTH, -1)
        }.time
        val isYesterday = isSameDay(lastSessionDate, yesterday)
        val tomorrow = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_MONTH, 1)
        }.time
        val isTomorrow = isSameDay(lastSessionDate, tomorrow)
        
        println("✅ [StreakVM] Is today: $isToday")
        println("📆 [StreakVM] Is yesterday: $isYesterday")
        println("📅 [StreakVM] Is tomorrow: $isTomorrow")
        
        // FIXED: Trust the backend's streak calculation
        // If backend says user has a streak, show it as active
        // Only deactivate if it's been more than 1 day since last session
        val daysSinceLastSession = daysBetween(lastSessionDate, today)
        val isWithinOneDay = abs(daysSinceLastSession) <= 1
        
        println("📊 [StreakVM] Days since last session: $daysSinceLastSession")
        println("⏰ [StreakVM] Is within one day: $isWithinOneDay")
        
        // Show streak as active if:
        // 1. Backend says user has a streak AND
        // 2. Last session was within 1 day (yesterday, today, or tomorrow)
        _isStreakActive.value = _streakData.value.hasStreak && isWithinOneDay
        
        println("🔥 [StreakVM] Final isStreakActive: ${_isStreakActive.value}")
        
        // Additional logic: If user hasn't played for 2+ days, the backend should handle the reset
        if (!isWithinOneDay && _streakData.value.hasStreak) {
            println("⚠️ [StreakVM] Session is more than 1 day away - backend should handle streak reset")
        }
    }
    
    /// Call this when user completes a level/session to "light up" the streak
    fun lightUpStreak() {
        viewModelScope.launch {
            // Reload streak data which will update the active state
            loadStreak()
        }
    }
    
    // MARK: - Computed Properties
    val completedDaysThisWeek: Int
        get() = _streakData.value.weeklyProgress.count { it.isCompleted }
    
    val streakText: String
        get() {
            // Always show the actual streak count from backend
            val streakCount = _streakData.value.currentStreak
            println("🔍 [StreakVM] streakText - currentStreak: $streakCount, isStreakActive: ${_isStreakActive.value}")
            
            // Always display the streak number if it exists, regardless of active state
            return if (streakCount > 0) streakCount.toString() else "0"
        }
    
    val streakStatusMessage: String
        get() {
            return if (!_isStreakActive.value && _streakData.value.currentStreak > 0) {
                "¡Continúa tu racha de ${_streakData.value.currentStreak} días!"
            } else if (!_isStreakActive.value) {
                "¡Completa tu primer nivel para comenzar tu racha!"
            } else {
                "¡Racha de ${_streakData.value.currentStreak} días activa!"
            }
        }
    
    // MARK: - Helper Methods
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun daysBetween(date1: Date, date2: Date): Int {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        val diffInMillis = cal2.timeInMillis - cal1.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}
