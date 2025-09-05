package com.spikai.service

import com.spikai.model.UserSession
import com.spikai.model.SessionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// MARK: - SessionReportingService
class SessionReportingService {
    
    private val baseURL: String
    private val client: OkHttpClient
    private val errorHandler = ErrorHandlingService.shared
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        val shared = SessionReportingService()
    }
    
    init {
        this.baseURL = NetworkConfig.getBackendURL()
        
        this.client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // MARK: - Session Reporting
    
    /// Report user session data to backend
    suspend fun reportSession(sessionData: SessionReportData): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val fullURL = "$baseURL/openai-realtime-usage"
            println("🌐 [SessionReportingService] Attempting to send session data to: $fullURL")
            
            // Debug the exact data being sent
            println("📊 [SessionReportingService] Detailed session data:")
            println("   📱 Session ID: ${sessionData.sessionId}")
            println("   👤 User ID: ${sessionData.userId}")
            println("   🕐 Start Time: ${sessionData.startTime}")
            println("   🕐 End Time: ${sessionData.endTime ?: "null"}")
            println("   🎯 Level Tried: ${sessionData.levelTried?.toString() ?: "null"}")
            println("   ✅ Level Passed: ${sessionData.levelPassed}")
            println("   ⏱️ Session Duration: ${sessionData.sessionDuration}")
            println("   🔢 Tokens Used: ${sessionData.tokensUsed}")
            println("   🎤 Audio Duration: ${sessionData.audioDuration}")
            println("   💬 Conversation Turns: ${sessionData.conversationTurns}")
            
            try {
                val jsonData = json.encodeToString(sessionData)
                println("📝 [SessionReportingService] JSON payload: $jsonData")
                
                val requestBody = jsonData.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(fullURL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                println("🚀 [SessionReportingService] Sending POST request...")
                val response = client.newCall(request).execute()
                
                withContext(Dispatchers.Main) {
                    println("📡 [SessionReportingService] HTTP Status Code: ${response.code}")
                    
                    response.body?.string()?.let { responseString ->
                        println("📄 [SessionReportingService] Response body: $responseString")
                    }
                    
                    when (response.code) {
                        in 200..299 -> {
                            println("✅ [SessionReportingService] Session data sent successfully!")
                            Result.success(Unit)
                        }
                        401 -> {
                            val error = SpikError.AUTH_TOKEN_EXPIRED
                            errorHandler.showError(error)
                            Result.failure(Exception(error.errorDescription))
                        }
                        403 -> {
                            val error = SpikError.PERMISSION_DENIED
                            errorHandler.showError(error)
                            Result.failure(Exception(error.errorDescription))
                        }
                        404 -> {
                            val error = SpikError.DATA_NOT_FOUND
                            errorHandler.showError(error)
                            Result.failure(Exception(error.errorDescription))
                        }
                        in 400..499 -> {
                            val error = SpikError.INVALID_DATA
                            errorHandler.showError(error)
                            Result.failure(Exception(error.errorDescription))
                        }
                        in 500..599 -> {
                            val error = SpikError.SERVER_UNAVAILABLE
                            errorHandler.showError(error)
                            Result.failure(Exception(error.errorDescription))
                        }
                        else -> {
                            val error = SpikError.UNKNOWN_ERROR
                            errorHandler.showError(error)
                            Result.failure(Exception(error.errorDescription))
                        }
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val spikError = SpikError.INVALID_DATA
                    errorHandler.showError(spikError)
                    Result.failure(Exception(spikError.errorDescription))
                }
            }
        }
    }
}

// MARK: - Session Report Data Model
@Serializable
data class SessionReportData(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("start_time")
    val startTime: String, // ISO 8601 format
    @SerialName("end_time")
    val endTime: String? = null, // ISO 8601 format
    @SerialName("level_tried")
    val levelTried: Int? = null, // Most recent level ID that was attempted, null if none
    @SerialName("level_passed")
    val levelPassed: Boolean, // Boolean indicating if any level was passed in this session
    @SerialName("tokens_used")
    val tokensUsed: Int, // For now, we'll set this to 0 or track if available
    @SerialName("user_id")
    val userId: String,
    @SerialName("session_duration")
    val sessionDuration: Double, // Duration in seconds
    @SerialName("audio_duration")
    val audioDuration: Double, // For now, we'll set this to 0 or track if available
    @SerialName("conversation_turns")
    val conversationTurns: Int // For now, we'll set this to 0 or track if available
)

// MARK: - UserSession Extension for Backend Reporting
fun UserSession.toSessionReportData(): SessionReportData {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    println("🔍 [SessionReportData] Raw session data before conversion:")
    println("   📱 Session ID: $sessionId")
    println("   👤 User ID: $userId")
    println("   🕐 Start Time: $startTime")
    println("   🕐 End Time: ${endTime?.toString() ?: "null"}")
    println("   📋 Total levels: ${levels.size}")
    
    levels.forEachIndexed { index, level ->
        println("   📊 Level ${index + 1}: ID=${level.levelId}, tries=${level.tries}, completed=${level.completed}")
    }
    
    // Get the most recent level tried (last one in the array)
    val mostRecentLevelTried = levels.lastOrNull()?.levelId
    
    // Check if any level was completed in this session
    val anyLevelPassed = levels.any { it.completed }
    
    val duration = endTime?.let { (it.time - startTime.time) / 1000.0 } ?: 0.0
    
    println("🔄 [SessionReportData] Converting session to backend format:")
    println("   📱 Session ID: $sessionId")
    println("   👤 User ID: $userId")
    println("   🕐 Start: ${formatter.format(startTime)}")
    println("   🕐 End: ${endTime?.let { formatter.format(it) } ?: "null"}")
    println("   ⏱️ Duration: $duration seconds")
    println("   📊 Most recent level tried: ${mostRecentLevelTried?.toString() ?: "none"}")
    println("   ✅ Any level passed: $anyLevelPassed")
    println("   📋 Total levels in session: ${levels.size}")
    
    return SessionReportData(
        sessionId = sessionId,
        startTime = formatter.format(startTime),
        endTime = endTime?.let { formatter.format(it) },
        levelTried = mostRecentLevelTried,
        levelPassed = anyLevelPassed,
        tokensUsed = 0, // TODO: Track tokens when available
        userId = userId,
        sessionDuration = duration,
        audioDuration = 0.0, // TODO: Track audio duration when available
        conversationTurns = 0 // TODO: Track conversation turns when available
    )
}
