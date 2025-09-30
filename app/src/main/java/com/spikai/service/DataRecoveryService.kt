package com.spikai.service

import android.content.Context
import android.content.SharedPreferences
import com.spikai.model.CareerProgress
import com.spikai.model.UserProfile
import com.spikai.model.EnglishLevel
import com.spikai.service.LocalUserProgress
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * DataRecoveryService - Handles corrupted data detection and recovery
 * Provides methods to detect, clear, and recover from corrupted data in SharedPreferences
 */
class DataRecoveryService(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        @Volatile
        private var INSTANCE: DataRecoveryService? = null
        
        fun getInstance(context: Context): DataRecoveryService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataRecoveryService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Checks all SharedPreferences for corrupted JSON data and provides recovery options
     */
    fun performDataIntegrityCheck(): DataIntegrityReport {
        println("🔍 [DataRecovery] Starting comprehensive data integrity check...")
        
        val corruptedData = mutableListOf<CorruptedDataInfo>()
        val validData = mutableListOf<String>()
        
        // Check Career Map preferences
        checkCareerMapData(corruptedData, validData)
        
        // Check Onboarding preferences  
        checkOnboardingData(corruptedData, validData)
        
        // Check Local Data Service
        checkLocalDataService(corruptedData, validData)
        
        // Check Streak Service
        checkStreakData(corruptedData, validData)
        
        // Check User Preferences
        checkUserPreferences(corruptedData, validData)
        
        val report = DataIntegrityReport(
            corruptedData = corruptedData,
            validData = validData,
            totalChecked = corruptedData.size + validData.size
        )
        
        println("📊 [DataRecovery] Integrity check complete:")
        println("   ✅ Valid data entries: ${validData.size}")
        println("   ❌ Corrupted data entries: ${corruptedData.size}")
        println("   📋 Total checked: ${report.totalChecked}")
        
        return report
    }
    
    /**
     * Clears all corrupted data and resets to safe defaults
     */
    fun clearCorruptedData(): Boolean {
        return try {
            println("🧹 [DataRecovery] Clearing all potentially corrupted data...")
            
            // Clear Career Map data
            val careerMapPrefs = context.getSharedPreferences("career_map_prefs", Context.MODE_PRIVATE)
            clearSharedPrefs(careerMapPrefs, "career_map_prefs")
            
            // Clear Onboarding data
            val onboardingPrefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
            clearSharedPrefs(onboardingPrefs, "onboarding_prefs")
            
            // Clear Local Data Service
            val localDataPrefs = context.getSharedPreferences("local_data_prefs", Context.MODE_PRIVATE)
            clearSharedPrefs(localDataPrefs, "local_data_prefs")
            
            // Clear Streak data
            val streakPrefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
            clearSharedPrefs(streakPrefs, "streak_prefs")
            
            println("✅ [DataRecovery] All corrupted data cleared successfully")
            true
        } catch (e: Exception) {
            println("❌ [DataRecovery] Failed to clear corrupted data: ${e.message}")
            false
        }
    }
    
    /**
     * Attempts to recover corrupted user profile data
     */
    fun recoverUserProfile(): UserProfile? {
        return try {
            println("🔧 [DataRecovery] Attempting to recover user profile...")
            
            val careerMapPrefs = context.getSharedPreferences("career_map_prefs", Context.MODE_PRIVATE)
            val onboardingPrefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
            
            // Try to extract any valid user profile data
            val careerMapProfile = tryDecodeUserProfile(careerMapPrefs, "userProfile")
            val onboardingProfile = tryDecodeUserProfile(onboardingPrefs, "userProfile")
            
            val recoveredProfile = careerMapProfile ?: onboardingProfile
            
            if (recoveredProfile != null) {
                println("✅ [DataRecovery] Successfully recovered user profile: ${recoveredProfile.englishLevel?.rawValue ?: "unknown level"}")
                return recoveredProfile
            } else {
                println("⚠️ [DataRecovery] No recoverable user profile found, creating default")
                return UserProfile().apply {
                    englishLevel = EnglishLevel.PRINCIPIANTE
                    hasCompletedOnboarding = false
                }
            }
        } catch (e: Exception) {
            println("❌ [DataRecovery] Failed to recover user profile: ${e.message}")
            null
        }
    }
    
    /**
     * Attempts to recover career progress data
     */
    fun recoverCareerProgress(): CareerProgress? {
        return try {
            println("🔧 [DataRecovery] Attempting to recover career progress...")
            
            val careerMapPrefs = context.getSharedPreferences("career_map_prefs", Context.MODE_PRIVATE)
            val localDataPrefs = context.getSharedPreferences("local_data_prefs", Context.MODE_PRIVATE)
            
            // Try career map first
            tryDecodeCareerProgress(careerMapPrefs, "careerProgress")?.let { progress ->
                println("✅ [DataRecovery] Recovered career progress from career_map_prefs")
                return progress
            }
            
            // Try local data service
            tryDecodeLocalProgress(localDataPrefs, "local_user_progress")?.let { localProgress ->
                println("✅ [DataRecovery] Recovered career progress from local_data_prefs")
                return CareerProgress(
                    currentLevel = localProgress.currentLevel,
                    totalExperience = localProgress.totalExperience,
                    completedLevels = localProgress.completedLevels,
                    unlockedLevels = localProgress.unlockedLevels
                )
            }
            
            println("⚠️ [DataRecovery] No recoverable career progress found, creating default")
            CareerProgress() // Default progress
            
        } catch (e: Exception) {
            println("❌ [DataRecovery] Failed to recover career progress: ${e.message}")
            null
        }
    }
    
    // MARK: - Private Helper Methods
    
    private fun checkCareerMapData(corruptedData: MutableList<CorruptedDataInfo>, validData: MutableList<String>) {
        val careerMapPrefs = context.getSharedPreferences("career_map_prefs", Context.MODE_PRIVATE)
        
        checkJsonData(careerMapPrefs, "userProfile", "UserProfile", corruptedData, validData) { jsonString ->
            json.decodeFromString<UserProfile>(jsonString)
        }
        
        checkJsonData(careerMapPrefs, "careerProgress", "CareerProgress", corruptedData, validData) { jsonString ->
            json.decodeFromString<CareerProgress>(jsonString)
        }
    }
    
    private fun checkOnboardingData(corruptedData: MutableList<CorruptedDataInfo>, validData: MutableList<String>) {
        val onboardingPrefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        
        checkJsonData(onboardingPrefs, "userProfile", "UserProfile", corruptedData, validData) { jsonString ->
            json.decodeFromString<UserProfile>(jsonString)
        }
    }
    
    private fun checkLocalDataService(corruptedData: MutableList<CorruptedDataInfo>, validData: MutableList<String>) {
        val localDataPrefs = context.getSharedPreferences("local_data_prefs", Context.MODE_PRIVATE)
        
        checkJsonData(localDataPrefs, "local_user_progress", "LocalUserProgress", corruptedData, validData) { jsonString ->
            json.decodeFromString<LocalUserProgress>(jsonString)
        }
    }
    
    private fun checkStreakData(corruptedData: MutableList<CorruptedDataInfo>, validData: MutableList<String>) {
        val streakPrefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
        
        checkJsonData(streakPrefs, "cached_streak_data", "StreakData", corruptedData, validData) { jsonString ->
            json.decodeFromString<com.spikai.model.StreakData>(jsonString)
        }
    }
    
    private fun checkUserPreferences(corruptedData: MutableList<CorruptedDataInfo>, validData: MutableList<String>) {
        val userPrefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        // User preferences typically don't contain JSON, so just check if they exist
        val allPrefs = userPrefs.all
        if (allPrefs.isNotEmpty()) {
            validData.add("user_preferences (${allPrefs.size} entries)")
        }
    }
    
    private fun <T> checkJsonData(
        prefs: SharedPreferences,
        key: String,
        dataType: String,
        corruptedData: MutableList<CorruptedDataInfo>,
        validData: MutableList<String>,
        decoder: (String) -> T
    ) {
        val jsonString = prefs.getString(key, null)
        if (jsonString != null) {
            try {
                decoder(jsonString)
                validData.add("$dataType ($key)")
                println("✅ [DataRecovery] Valid $dataType data found")
            } catch (e: Exception) {
                corruptedData.add(
                    CorruptedDataInfo(
                        key = key,
                        dataType = dataType,
                        prefsFile = prefs.toString(),
                        error = e.message ?: "Unknown error",
                        dataSize = jsonString.length
                    )
                )
                println("❌ [DataRecovery] Corrupted $dataType data found: ${e.message}")
            }
        }
    }
    
    private fun clearSharedPrefs(prefs: SharedPreferences, name: String) {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
        println("🗑️ [DataRecovery] Cleared SharedPreferences: $name")
    }
    
    private fun tryDecodeUserProfile(prefs: SharedPreferences, key: String): UserProfile? {
        return try {
            val jsonString = prefs.getString(key, null)
            if (jsonString != null) {
                json.decodeFromString<UserProfile>(jsonString)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun tryDecodeCareerProgress(prefs: SharedPreferences, key: String): CareerProgress? {
        return try {
            val jsonString = prefs.getString(key, null)
            if (jsonString != null) {
                json.decodeFromString<CareerProgress>(jsonString)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun tryDecodeLocalProgress(prefs: SharedPreferences, key: String): LocalUserProgress? {
        return try {
            val jsonString = prefs.getString(key, null)
            if (jsonString != null) {
                json.decodeFromString<LocalUserProgress>(jsonString)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Test method to simulate data corruption for debugging (only for debug builds)
     */
    fun simulateDataCorruption(dataType: String = "careerProgress") {
        println("🧪 [DataRecovery] SIMULATING DATA CORRUPTION FOR TESTING")
        
        when (dataType) {
            "careerProgress" -> {
                val careerMapPrefs = context.getSharedPreferences("career_map_prefs", Context.MODE_PRIVATE)
                careerMapPrefs.edit().putString("careerProgress", "{invalid_json_data}").apply()
                println("🧪 [DataRecovery] Simulated career progress corruption")
            }
            "userProfile" -> {
                val careerMapPrefs = context.getSharedPreferences("career_map_prefs", Context.MODE_PRIVATE)
                careerMapPrefs.edit().putString("userProfile", "{corrupt:data,missing:}").apply()
                println("🧪 [DataRecovery] Simulated user profile corruption")
            }
            "localProgress" -> {
                val localDataPrefs = context.getSharedPreferences("local_data_prefs", Context.MODE_PRIVATE)
                localDataPrefs.edit().putString("local_user_progress", "{corrupted_local_data}").apply()
                println("🧪 [DataRecovery] Simulated local progress corruption")
            }
            "streakData" -> {
                val streakPrefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
                streakPrefs.edit().putString("cached_streak_data", "{bad_streak_data}").apply()
                println("🧪 [DataRecovery] Simulated streak data corruption")
            }
            else -> {
                println("⚠️ [DataRecovery] Unknown data type for corruption simulation: $dataType")
            }
        }
    }
}

// MARK: - Data Models

data class DataIntegrityReport(
    val corruptedData: List<CorruptedDataInfo>,
    val validData: List<String>,
    val totalChecked: Int
) {
    val hasCorruptedData: Boolean get() = corruptedData.isNotEmpty()
    val corruptionSummary: String get() = "Found ${corruptedData.size} corrupted entries out of $totalChecked total"
}

data class CorruptedDataInfo(
    val key: String,
    val dataType: String,
    val prefsFile: String,
    val error: String,
    val dataSize: Int
)
