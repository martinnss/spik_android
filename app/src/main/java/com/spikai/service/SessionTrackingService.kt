package com.spikai.service

import android.content.Context
import com.spikai.model.UserSession
import com.spikai.model.SessionLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

// MARK: - SessionTrackingService
class SessionTrackingService(private val context: Context) {
    
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()
    
    private var currentSession: UserSession? = null
    private val sessionReportingService = SessionReportingService.shared
    private val errorHandler = ErrorHandlingService.shared
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        @Volatile
        private var INSTANCE: SessionTrackingService? = null
        
        fun getInstance(context: Context): SessionTrackingService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionTrackingService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // MARK: - Level-Based Session Management
    
    /// Start a new session when a level begins
    fun startLevelSession(levelId: Int) {
        // TODO: Firebase Auth not provided in context - needs to be implemented
        val userId = getCurrentUserId()
        
        if (userId == null) {
            println("❌ [SessionTracker] Cannot start level session: User not authenticated")
            return
        }
        
        // End any existing session before starting a new one
        if (currentSession != null) {
            println("⚠️ [SessionTracker] Ending previous session before starting level $levelId")
            endCurrentSession()
        }
        
        // Create new session
        currentSession = UserSession(userId)
        _isSessionActive.value = true
        
        println("✅ [SessionTracker] Level session started:")
        println("   🎯 Level ID: $levelId")
        println("   📱 Session ID: ${currentSession?.sessionId ?: "Unknown"}")
        println("   👤 User ID: $userId")
        println("   🕐 Start Time: ${currentSession?.startTime ?: Date()}")
        
        // Immediately add this level to the session
        startLevel(levelId)
    }
    
    /// End the current session when a level is completed
    fun endLevelSession(levelId: Int, completed: Boolean) {
        var session = currentSession
        
        if (session == null) {
            println("⚠️ [SessionTracker] No active session to end for level $levelId")
            return
        }
        
        println("🔚 [SessionTracker] Ending level session:")
        println("   🎯 Level ID: $levelId")
        println("   ✅ Completed: $completed")
        
        // Mark the level as completed if it was passed
        if (completed) {
            completeLevel(levelId)
            // Get the updated session after completion
            session = currentSession ?: session
            println("🔄 [SessionTracker] Updated session after completion - checking level status:")
            session.levels.forEach { level ->
                println("     - Level ${level.levelId}: ${level.tries} tries, completed: ${level.completed}")
            }
        } else {
            failLevel(levelId)
        }
        
        // End the session
        val updatedSession = session.copy(endTime = Date())
        _isSessionActive.value = false
        
        println("   📱 Session ID: ${updatedSession.sessionId}")
        println("   🕐 Start Time: ${updatedSession.startTime}")
        println("   🕐 End Time: ${updatedSession.endTime ?: Date()}")
        println("   ⏱️ Duration: ${updatedSession.endTime?.let { (it.time - updatedSession.startTime.time) / 1000.0 } ?: 0} seconds")
        println("   📊 Levels attempted: ${updatedSession.levels.size}")
        updatedSession.levels.forEach { level ->
            println("     - Level ${level.levelId}: ${level.tries} tries, completed: ${level.completed}")
        }
        
        // Save session to backend
        saveSessionToBackend(session = updatedSession)
        
        // Clear current session
        currentSession = null
        
        println("✅ [SessionTracker] Level session ended and queued for backend reporting")
    }
    
    /// Force end the current session (for cleanup or emergency cases)
    fun endCurrentSession() {
        val session = currentSession
        
        if (session == null) {
            println("⚠️ [SessionTracker] No active session to force end")
            return
        }
        
        println("🚨 [SessionTracker] Force ending current session ${session.sessionId}")
        
        // If there are levels in progress, mark the most recent as failed
        val lastLevel = session.levels.lastOrNull()
        if (lastLevel != null && !lastLevel.completed) {
            endLevelSession(levelId = lastLevel.levelId, completed = false)
        } else {
            // Just end the session without level completion
            val updatedSession = session.copy(endTime = Date())
            saveSessionToBackend(session = updatedSession)
            currentSession = null
            _isSessionActive.value = false
        }
    }
    
    // MARK: - Level Tracking Methods (Internal)
    
    private fun startLevel(levelId: Int) {
        var session = currentSession
        
        if (session == null) {
            println("❌ [SessionTracker] Cannot start level $levelId: No active session")
            return
        }
        
        // Check if level already exists in current session
        val existingLevelIndex = session.levels.indexOfFirst { it.levelId == levelId }
        
        val updatedLevels = if (existingLevelIndex != -1) {
            // Level already exists, increment tries
            val updatedLevel = session.levels[existingLevelIndex].copy(tries = session.levels[existingLevelIndex].tries + 1)
            val newLevels = session.levels.toMutableList().apply { set(existingLevelIndex, updatedLevel) }
            println("🔄 [SessionTracker] Level $levelId - Try #${updatedLevel.tries}")
            newLevels
        } else {
            // New level
            val newLevel = SessionLevel(levelId).copy(tries = 1)
            val newLevels = session.levels + newLevel
            println("🆕 [SessionTracker] Started new level: $levelId")
            newLevels
        }
        
        currentSession = session.copy(levels = updatedLevels)
        println("📊 [SessionTracker] Current session has ${updatedLevels.size} levels")
    }
    
    private fun completeLevel(levelId: Int) {
        var session = currentSession
        
        if (session == null) {
            println("❌ [SessionTracker] Cannot complete level: No active session")
            return
        }
        
        println("🔍 [SessionTracker] Attempting to complete level $levelId")
        println("   📋 Current session levels before completion:")
        session.levels.forEachIndexed { index, level ->
            println("     Level ${index + 1}: ID=${level.levelId}, tries=${level.tries}, completed=${level.completed}")
        }
        
        val levelIndex = session.levels.indexOfFirst { it.levelId == levelId }
        
        if (levelIndex != -1) {
            println("✅ [SessionTracker] Found level $levelId at index $levelIndex, marking as completed")
            val updatedLevel = session.levels[levelIndex].copy(
                completed = true,
                endTime = Date()
            )
            val updatedLevels = session.levels.toMutableList().apply { set(levelIndex, updatedLevel) }
            currentSession = session.copy(levels = updatedLevels)
            println("✅ [SessionTracker] Level $levelId marked as completed!")
            
            println("   📋 Current session levels after completion:")
            updatedLevels.forEachIndexed { index, level ->
                println("     Level ${index + 1}: ID=${level.levelId}, tries=${level.tries}, completed=${level.completed}")
            }
        } else {
            println("⚠️ [SessionTracker] Level $levelId not found in current session")
            println("   📋 Available levels:")
            session.levels.forEachIndexed { index, level ->
                println("     Level ${index + 1}: ID=${level.levelId}, tries=${level.tries}, completed=${level.completed}")
            }
        }
        
        // Notify the career map to update progress
        println("📡 [SessionTracker] Sending level completion notification for level $levelId")
        // TODO: Implement notification system when needed
        // In Android, this would typically be done through callbacks, LiveData, or StateFlow
        notifyLevelCompleted(levelId)
    }
    
    private fun failLevel(levelId: Int) {
        val session = currentSession
        
        if (session == null) {
            println("❌ [SessionTracker] Cannot fail level: No active session")
            return
        }
        
        val level = session.levels.firstOrNull { it.levelId == levelId }
        if (level != null) {
            println("❌ [SessionTracker] Level $levelId failed (tries: ${level.tries})")
        } else {
            println("❌ [SessionTracker] Level $levelId failed - not found in session")
        }
        
        // Note: We don't modify the level completion status for failures
        // The level remains incomplete in the session data
    }
    
    // MARK: - Backend Reporting Methods
    
    private fun saveSessionToBackend(session: UserSession) {
        println("📤 [SessionTracker] Preparing to send session to backend...")
        val sessionReportData = session.toSessionReportData()
        
        coroutineScope.launch {
            val result = sessionReportingService.reportSession(sessionReportData)
            
            result.fold(
                onSuccess = { _: Unit ->
                    println("✅ [SessionTracker] Session reported successfully to backend: ${session.sessionId}")
                },
                onFailure = { error: Throwable ->
                    println("❌ [SessionTracker] Error reporting session to backend:")
                    println("   📱 Session ID: ${session.sessionId}")
                    println("   ⚠️ Error: ${error.message}")
                    
                    // Error is already handled by SessionReportingService
                    // No need to duplicate error handling here
                }
            )
        }
    }
    
    // MARK: - Public Helper Methods
    
    fun getCurrentSessionId(): String? {
        return currentSession?.sessionId
    }
    
    fun getCurrentSessionLevels(): List<SessionLevel> {
        return currentSession?.levels ?: emptyList()
    }
    
    /// Get the current level being attempted (for debugging)
    fun getCurrentLevelId(): Int? {
        return currentSession?.levels?.lastOrNull()?.levelId
    }
    
    // MARK: - Debug/Test Methods
    
    /// Test method to create and immediately send a sample session (for debugging)
    fun testSessionReporting() {
        // TODO: Firebase Auth not provided in context - needs to be implemented
        val userId = getCurrentUserId()
        
        if (userId == null) {
            println("❌ [SessionTracker] Cannot test session: User not authenticated")
            return
        }
        
        println("🧪 [SessionTracker] Testing session reporting...")
        
        // Create a test session
        val testSession = UserSession(userId)
        
        // Add a test level
        val testLevel = SessionLevel(1).copy(
            tries = 1,
            completed = true,
            endTime = Date()
        )
        
        val finalTestSession = testSession.copy(
            levels = listOf(testLevel),
            endTime = Date()
        )
        
        println("🧪 [SessionTracker] Sending test session to backend...")
        saveSessionToBackend(session = finalTestSession)
    }
    
    // TODO: Firebase Auth not provided in context - placeholder implementation
    private fun getCurrentUserId(): String? {
        // This should return Firebase Auth currentUser?.uid
        return null // TODO: Implement Firebase Auth integration
    }
    
    // TODO: Implement notification system when needed
    private fun notifyLevelCompleted(levelId: Int) {
        // In Android, this would typically be done through:
        // - Callbacks to registered listeners
        // - Broadcasting through StateFlow/LiveData
        // - Event bus systems
        println("📡 [SessionTracker] Level $levelId completion notification sent")
    }
}
