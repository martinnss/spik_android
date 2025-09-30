package com.spikai.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlinx.serialization.decodeFromString

// MARK: - Local Data Models
@Serializable
data class LocalUserProgress(
    var completedLevels: Set<Int> = emptySet(),
    var unlockedLevels: Set<Int> = setOf(1001), // First level unlocked by default
    var totalExperience: Int = 0,
    var currentLevel: Int = 1001,
    var achievements: List<String> = emptyList(), // Achievement IDs
    @Serializable(with = DateSerializer::class)
    var lastSync: Date = Date()
)

// MARK: - Local Data Service
class LocalDataService(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("local_data_prefs", Context.MODE_PRIVATE)
    }
    
    private val progressKey = "local_user_progress"
    private val levelsKey = "cached_levels"
    private val achievementsKey = "cached_achievements"
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        @Volatile
        private var INSTANCE: LocalDataService? = null
        
        fun getInstance(context: Context): LocalDataService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDataService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // MARK: - User Progress
    fun saveUserProgress(progress: LocalUserProgress) {
        try {
            val jsonString = json.encodeToString(progress)
            sharedPreferences.edit().putString(progressKey, jsonString).apply()
            println("💾 [LocalData] Saved user progress locally")
        } catch (error: Exception) {
            println("❌ [LocalData] Failed to save progress: $error")
        }
    }
    
    fun loadUserProgress(): LocalUserProgress {
        val jsonString = sharedPreferences.getString(progressKey, null)
        
        if (jsonString == null) {
            println("📱 [LocalData] No cached progress found, creating new")
            return LocalUserProgress()
        }
        
        return try {
            val progress = json.decodeFromString<LocalUserProgress>(jsonString)
            println("📱 [LocalData] Loaded cached user progress")
            
            // Validate the data integrity
            validateUserProgress(progress)
            
            progress
        } catch (error: Exception) {
            println("❌ [LocalData] Failed to decode progress: $error")
            
            // Clear corrupted data and return default
            println("🗑️ [LocalData] Clearing corrupted progress data")
            sharedPreferences.edit().remove(progressKey).apply()
            
            // Create default progress
            val defaultProgress = LocalUserProgress()
            
            // Try to save the default to ensure the storage works
            try {
                saveUserProgress(defaultProgress)
                println("✅ [LocalData] Saved default progress after corruption")
            } catch (saveError: Exception) {
                println("❌ [LocalData] Failed to save default progress: $saveError")
            }
            
            defaultProgress
        }
    }
    
    // MARK: - Level Completion
    fun markLevelCompleted(levelId: Int) {
        val progress = loadUserProgress()
        val updatedProgress = progress.copy(
            completedLevels = progress.completedLevels + levelId,
            lastSync = Date()
        )
        
        // Unlock next level if it exists
        val nextLevelId = getNextLevelId(levelId)
        val finalProgress = if (nextLevelId > 0) {
            updatedProgress.copy(unlockedLevels = updatedProgress.unlockedLevels + nextLevelId)
        } else {
            updatedProgress
        }
        
        saveUserProgress(finalProgress)
        println("✅ [LocalData] Marked level $levelId as completed")
    }
    
    fun isLevelCompleted(levelId: Int): Boolean {
        val progress = loadUserProgress()
        return progress.completedLevels.contains(levelId)
    }
    
    fun isLevelUnlocked(levelId: Int): Boolean {
        val progress = loadUserProgress()
        return progress.unlockedLevels.contains(levelId)
    }
    
    fun getCompletedLevelsCount(): Int {
        val progress = loadUserProgress()
        return progress.completedLevels.size
    }
    
    // MARK: - Achievements
    fun unlockAchievement(achievementId: String) {
        val progress = loadUserProgress()
        if (!progress.achievements.contains(achievementId)) {
            val updatedProgress = progress.copy(
                achievements = progress.achievements + achievementId,
                lastSync = Date()
            )
            saveUserProgress(updatedProgress)
            println("🏆 [LocalData] Unlocked achievement: $achievementId")
        }
    }
    
    fun isAchievementUnlocked(achievementId: String): Boolean {
        val progress = loadUserProgress()
        return progress.achievements.contains(achievementId)
    }
    
    fun getUnlockedAchievements(): List<String> {
        val progress = loadUserProgress()
        return progress.achievements
    }
    
    // MARK: - Cache Management
    fun clearAllCache() {
        sharedPreferences.edit()
            .remove(progressKey)
            .remove(levelsKey)
            .remove(achievementsKey)
            .apply()
        println("🗑️ [LocalData] Cleared all local cache")
    }
    
    fun needsSync(): Boolean {
        val progress = loadUserProgress()
        val hoursSinceSync = (Date().time - progress.lastSync.time) / (1000 * 60 * 60) // Convert to hours
        return hoursSinceSync > 24 // Sync every 24 hours
    }
    
    // MARK: - Private Helpers
    private fun getNextLevelId(levelId: Int): Int {
        // Simple logic: increment by 1 within the same tier
        // 1001 -> 1002, 2001 -> 2002, etc.
        val tier = levelId / 1000
        val levelInTier = levelId % 1000
        val nextInTier = levelInTier + 1
        
        // Check if next level in tier exists (assuming max 10 levels per tier)
        return if (nextInTier <= 10) {
            tier * 1000 + nextInTier
        } else {
            // Move to next tier
            (tier + 1) * 1000 + 1
        }
    }
    
    // MARK: - Data Validation
    
    private fun validateUserProgress(progress: LocalUserProgress) {
        // Validate that completed levels are also in unlocked levels
        val invalidCompletedLevels = progress.completedLevels.filter { levelId ->
            !progress.unlockedLevels.contains(levelId)
        }
        
        if (invalidCompletedLevels.isNotEmpty()) {
            println("⚠️ [LocalData] Found completed levels that are not unlocked: $invalidCompletedLevels")
            // Auto-fix by adding them to unlocked levels
            val fixedProgress = progress.copy(
                unlockedLevels = progress.unlockedLevels + invalidCompletedLevels
            )
            saveUserProgress(fixedProgress)
            println("🔧 [LocalData] Auto-fixed progress by unlocking completed levels")
        }
        
        // Validate experience is not negative
        if (progress.totalExperience < 0) {
            println("⚠️ [LocalData] Invalid total experience: ${progress.totalExperience}")
            val fixedProgress = progress.copy(totalExperience = 0)
            saveUserProgress(fixedProgress)
            println("🔧 [LocalData] Auto-fixed negative experience")
        }
        
        // Validate current level is unlocked
        if (!progress.unlockedLevels.contains(progress.currentLevel)) {
            println("⚠️ [LocalData] Current level ${progress.currentLevel} is not in unlocked levels")
            val fixedProgress = progress.copy(
                unlockedLevels = progress.unlockedLevels + progress.currentLevel
            )
            saveUserProgress(fixedProgress)
            println("🔧 [LocalData] Auto-fixed by unlocking current level")
        }
    }
    
    // MARK: - Corruption Recovery
    
    fun validateAndRepairData(): Boolean {
        return try {
            println("🔧 [LocalData] Performing data validation and repair...")
            
            val progress = loadUserProgress()
            validateUserProgress(progress)
            
            println("✅ [LocalData] Data validation and repair completed successfully")
            true
        } catch (e: Exception) {
            println("❌ [LocalData] Data validation and repair failed: ${e.message}")
            false
        }
    }
}
