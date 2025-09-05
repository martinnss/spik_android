package com.spikai.service

import android.content.Context
import android.content.SharedPreferences
import com.spikai.model.StreakData
import com.spikai.model.StreakResponse
import com.spikai.model.DayProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.TimeUnit

interface StreakServiceProtocol {
    suspend fun getCurrentStreak(): StreakData
    suspend fun updateDayProgress(dayIndex: Int, isCompleted: Boolean): StreakData
    suspend fun resetStreak(): StreakData
}

class StreakService(private val context: Context) : StreakServiceProtocol {
    
    private val baseURL = NetworkConfig.getBackendURL(context)
    private val errorHandler = ErrorHandlingService.shared
    private val json = Json { ignoreUnknownKeys = true }
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
    }
    
    private val streakKey = "cached_streak_data"
    private val lastUpdateKey = "streak_last_update"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        @Volatile
        private var INSTANCE: StreakService? = null
        
        fun getInstance(context: Context): StreakService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StreakService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        println("🎯 [StreakService] Service initialized")
        println("🌐 [StreakService] Base URL: $baseURL")
        println("🔑 [StreakService] Cache keys - streak: '$streakKey', lastUpdate: '$lastUpdateKey'")
    }
    
    override suspend fun getCurrentStreak(): StreakData {
        return withContext(Dispatchers.IO) {
            println("🎯 [StreakService] getCurrentStreak() called")
            
            // For debugging: always clear cache to get fresh data
            // TODO: Remove this in production
            clearCache()
            println("🗑️ [StreakService] Cleared cache for debugging")
            
            // Try to load from cache first for fast loading
            val cachedStreak = loadCachedStreak()
            if (cachedStreak != null && isCacheValid()) {
                println("📱 [StreakService] Using cached streak data - current streak: ${cachedStreak.currentStreak}")
                return@withContext cachedStreak
            } else if (cachedStreak != null) {
                println("⏰ [StreakService] Cache exists but expired, fetching fresh data")
            } else {
                println("💾 [StreakService] No cache found, fetching from server")
            }
            
            // TODO: Firebase Auth not provided in context - needs to be implemented
            val currentUserId = getCurrentUserId()
            
            if (currentUserId == null) {
                // User not authenticated yet - return default empty streak
                // This is normal for users who haven't logged in yet
                println("👤 [StreakService] User not authenticated - returning default streak")
                return@withContext createDefaultStreak()
            }
            
            println("👤 [StreakService] Authenticated user ID: $currentUserId")
            
            val url = "$baseURL/user-streak/$currentUserId"
            println("🌐 [StreakService] Making request to: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()
            
            println("📡 [StreakService] Request configured - method: GET, timeout: 30s")
            
            try {
                println("⏳ [StreakService] Starting network request...")
                val response = client.newCall(request).execute()
                println("✅ [StreakService] Network request completed")
                
                val responseBody = response.body?.string() ?: ""
                println("📊 [StreakService] Response data size: ${responseBody.length} bytes")
                
                // Check HTTP response status
                println("🌐 [StreakService] HTTP Status Code: ${response.code}")
                println("📋 [StreakService] Response Headers: ${response.headers}")
                
                when (response.code) {
                    in 200..299 -> {
                        println("✅ [StreakService] Success response received")
                        // Success - continue processing
                    }
                    401 -> {
                        println("🔐 [StreakService] Authentication token expired (401)")
                        val error = SpikError.AUTH_TOKEN_EXPIRED
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                    403 -> {
                        println("🚫 [StreakService] Permission denied (403)")
                        val error = SpikError.PERMISSION_DENIED
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                    404 -> {
                        println("👤 [StreakService] User not found (404) - new user detected")
                        // User not found is normal for new users - don't show error
                        // Return a default empty streak instead of throwing
                        val defaultStreak = createDefaultStreak()
                        println("📱 [StreakService] New user detected - returning default streak")
                        return@withContext defaultStreak
                    }
                    in 500..599 -> {
                        println("🔥 [StreakService] Server error (${response.code})")
                        
                        // Log the actual server error response
                        if (responseBody.isNotEmpty()) {
                            println("📄 [StreakService] Server error response: $responseBody")
                        }
                        
                        val error = SpikError.SERVER_UNAVAILABLE
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                    else -> {
                        println("❓ [StreakService] Unexpected status code: ${response.code}")
                        val error = SpikError.UNKNOWN_ERROR
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                }
                
                if (responseBody.isEmpty()) {
                    println("📭 [StreakService] Empty response data received")
                    val error = SpikError.DATA_NOT_FOUND
                    errorHandler.showError(error)
                    throw SpikErrorException(error)
                }
                
                println("🔍 [StreakService] Attempting to decode response data...")
                println("📄 [StreakService] Raw JSON response: $responseBody")
                
                try {
                    val streakResponse = json.decodeFromString<StreakResponse>(responseBody)
                    println("🎯 [StreakService] Successfully decoded StreakResponse")
                    
                    val streakData = StreakData.fromResponse(streakResponse)
                    println("✨ [StreakService] Created StreakData - current streak: ${streakData.currentStreak}, has streak: ${streakData.hasStreak}")
                    
                    // Cache the data for fast loading next time
                    cacheStreak(streakData)
                    
                    println("🎉 [StreakService] Successfully returning streak data")
                    streakData
                } catch (e: Exception) {
                    println("🔍 [StreakService] JSON decoding failed")
                    val error = SpikError.DATA_CORRUPTED
                    errorHandler.showError(error)
                    throw SpikErrorException(error)
                }
                
            } catch (e: SpikErrorException) {
                throw e
            } catch (e: Exception) {
                println("❌ [StreakService] Network/General error: ${e.message}")
                println("🔍 [StreakService] Error type: ${e::class.java}")
                val spikError = errorHandler.handleError(e)
                errorHandler.showError(spikError)
                throw SpikErrorException(spikError)
            }
        }
    }
    
    override suspend fun updateDayProgress(dayIndex: Int, isCompleted: Boolean): StreakData {
        println("📅 [StreakService] updateDayProgress called - dayIndex: $dayIndex, isCompleted: $isCompleted")
        // This method is not yet implemented in the backend
        // For now, return current streak data as this functionality 
        // might be handled differently (through session completion)
        println("⚠️ [StreakService] updateDayProgress not implemented, returning current streak")
        return getCurrentStreak()
    }
    
    override suspend fun resetStreak(): StreakData {
        println("🔄 [StreakService] resetStreak called")
        // This method is not yet implemented in the backend
        // For now, return mock data indicating no streak
        // In the future, this would make an API call to reset the user's streak
        println("⚠️ [StreakService] resetStreak not implemented, returning default empty streak")
        return createDefaultStreak()
    }
    
    // MARK: - Local Caching Methods
    private fun cacheStreak(streakData: StreakData) {
        println("💾 [StreakService] Attempting to cache streak data...")
        try {
            val jsonString = json.encodeToString(streakData)
            sharedPreferences.edit()
                .putString(streakKey, jsonString)
                .putLong(lastUpdateKey, Date().time)
                .apply()
            println("✅ [StreakService] Successfully cached streak data locally - size: ${jsonString.length} bytes")
        } catch (e: Exception) {
            println("❌ [StreakService] Failed to cache streak: ${e.message}")
        }
    }
    
    private fun loadCachedStreak(): StreakData? {
        println("🔍 [StreakService] Attempting to load cached streak...")
        val jsonString = sharedPreferences.getString(streakKey, null)
        
        if (jsonString == null) {
            println("📭 [StreakService] No cached data found")
            return null
        }
        
        println("📄 [StreakService] Found cached data - size: ${jsonString.length} bytes")
        
        return try {
            val streakData = json.decodeFromString<StreakData>(jsonString)
            println("✅ [StreakService] Successfully loaded cached streak - current: ${streakData.currentStreak}")
            streakData
        } catch (e: Exception) {
            println("❌ [StreakService] Failed to decode cached streak: ${e.message}")
            // Clear corrupted cache
            sharedPreferences.edit()
                .remove(streakKey)
                .remove(lastUpdateKey)
                .apply()
            println("🗑️ [StreakService] Cleared corrupted cache")
            null
        }
    }
    
    private fun isCacheValid(): Boolean {
        val lastUpdateTimestamp = sharedPreferences.getLong(lastUpdateKey, 0)
        
        if (lastUpdateTimestamp == 0L) {
            println("⏰ [StreakService] No last update timestamp found")
            return false
        }
        
        val lastUpdate = Date(lastUpdateTimestamp)
        
        // Cache is valid for 1 hour
        val cacheValidityDuration: Long = 60 * 60 * 1000 // 1 hour in milliseconds
        val timeSinceUpdate = Date().time - lastUpdate.time
        val isValid = timeSinceUpdate < cacheValidityDuration
        
        println("⏰ [StreakService] Cache age: ${timeSinceUpdate / 1000}s, valid: $isValid")
        
        return isValid
    }
    
    fun clearCache() {
        println("🗑️ [StreakService] clearCache called")
        sharedPreferences.edit()
            .remove(streakKey)
            .remove(lastUpdateKey)
            .apply()
        println("✅ [StreakService] Cleared streak cache successfully")
    }
    
    // MARK: - Helper Methods
    
    private fun createDefaultStreak(): StreakData {
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
    
    // TODO: Firebase Auth not provided in context - placeholder implementation
    private fun getCurrentUserId(): String? {
        // This should return Firebase Auth currentUser?.uid
        return null // TODO: Implement Firebase Auth integration
    }
    
    // MARK: - Debug Methods
    
    /// Debug method to check service status and configuration
    fun debugServiceStatus() {
        println("🔍 [StreakService] === DEBUG SERVICE STATUS ===")
        println("🌐 Base URL: $baseURL")
        println("🔐 Firebase Auth User: ${getCurrentUserId() ?: "Not authenticated"}")
        println("📱 Has cached data: ${sharedPreferences.getString(streakKey, null) != null}")
        
        val lastUpdateTimestamp = sharedPreferences.getLong(lastUpdateKey, 0)
        val lastUpdateDate = if (lastUpdateTimestamp > 0) Date(lastUpdateTimestamp) else Date(0)
        println("⏰ Cache last update: $lastUpdateDate")
        println("✅ Cache is valid: ${isCacheValid()}")
        
        loadCachedStreak()?.let { cachedData ->
            println("💾 Cached streak: ${cachedData.currentStreak}")
            println("🎯 Cached has streak: ${cachedData.hasStreak}")
        } ?: run {
            println("📭 No valid cached data")
        }
        println("===========================================")
    }
}
