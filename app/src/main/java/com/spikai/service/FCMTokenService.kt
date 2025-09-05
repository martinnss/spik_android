package com.spikai.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class FCMTokenService(private val context: Context) {
    
    private val baseURL: String
    private val client: OkHttpClient
    private val errorHandler = ErrorHandlingService.shared
    private val json = Json { ignoreUnknownKeys = true }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: FCMTokenService? = null
        
        fun getInstance(context: Context): FCMTokenService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FCMTokenService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        this.baseURL = NetworkConfig.getBackendURL(context)
        
        this.client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    fun handleFCMTokenReceived(token: String) {
        if (token.isEmpty()) {
            println("⚠️ [FCMTokenService] Invalid or empty FCM token received")
            return
        }
        
        println("📱 [FCMTokenService] FCM token received: ${token.take(20)}...")
        uploadFCMToken(token)
    }
    
    /// Upload FCM token to backend server
    fun uploadFCMToken(token: String) {
        coroutineScope.launch {
            // TODO: Firebase Auth not provided in context - needs to be implemented
            val currentUserId = getCurrentUserId()
            
            if (currentUserId == null) {
                println("⚠️ [FCMTokenService] No authenticated user - storing token for later upload")
                // Store token for when user authenticates
                sharedPreferences.edit().putString("pending_fcm_token", token).apply()
                return@launch
            }
            
            // Clear any pending token since we're uploading now
            sharedPreferences.edit().remove("pending_fcm_token").apply()
            
            val fullURL = "$baseURL/fcm-token"
            println("🌐 [FCMTokenService] Uploading FCM token to: $fullURL")
            
            try {
                val requestBody = mapOf(
                    "userId" to currentUserId,
                    "fcmToken" to token,
                    "platform" to "Android",
                    "timestamp" to (System.currentTimeMillis() / 1000.0)
                )
                
                val jsonBody = json.encodeToString(requestBody)
                println("📝 [FCMTokenService] Request body: $jsonBody")
                
                val request = Request.Builder()
                    .url(fullURL)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                withContext(Dispatchers.Main) {
                    println("📡 [FCMTokenService] Server response status: ${response.code}")
                    
                    when (response.code) {
                        in 200..299 -> {
                            println("✅ [FCMTokenService] FCM token uploaded successfully")
                            response.body?.string()?.let { responseString ->
                                println("📦 [FCMTokenService] Server response: $responseString")
                            }
                        }
                        401 -> {
                            println("❌ [FCMTokenService] Authentication failed")
                            val error = SpikError.AUTH_TOKEN_EXPIRED
                            errorHandler.logError(RuntimeException("Authentication failed"))
                        }
                        403 -> {
                            println("❌ [FCMTokenService] Access forbidden")
                            val error = SpikError.AUTHENTICATION_FAILED
                            errorHandler.logError(RuntimeException("Access forbidden"))
                        }
                        404 -> {
                            println("⚠️ [FCMTokenService] FCM endpoint not implemented on backend (${response.code})")
                            println("📝 [FCMTokenService] Push notifications will not work until backend endpoint is added")
                            // Don't show error to user for 404 - it's a backend configuration issue
                        }
                        in 500..599 -> {
                            println("❌ [FCMTokenService] Server error (${response.code})")
                            val error = SpikError.SERVER_UNAVAILABLE
                            errorHandler.logError(RuntimeException("Server error: ${response.code}"))
                        }
                        else -> {
                            println("❌ [FCMTokenService] Unexpected status code: ${response.code}")
                            response.body?.string()?.let { responseString ->
                                println("📦 [FCMTokenService] Error response: $responseString")
                            }
                            val error = SpikError.SERVER_UNAVAILABLE
                            errorHandler.logError(RuntimeException("Unexpected status: ${response.code}"))
                        }
                    }
                }
                
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    println("❌ [FCMTokenService] Network error: ${error.message}")
                    val spikError = errorHandler.handleError(error)
                    errorHandler.logError(error)
                }
            }
        }
    }
    
    /// Manual token refresh - useful for testing or when user logs in
    fun refreshAndUploadToken() {
        coroutineScope.launch {
            try {
                // TODO: Firebase Messaging not provided in context - needs to be implemented
                val token = getFirebaseMessagingToken()
                
                if (token == null) {
                    println("⚠️ [FCMTokenService] FCM token is null")
                    return@launch
                }
                
                println("🔄 [FCMTokenService] Manual token refresh successful")
                uploadFCMToken(token)
                
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    println("❌ [FCMTokenService] Error fetching FCM token: ${error.message}")
                    val spikError = SpikError.NOTIFICATION_SETUP_FAILED
                    errorHandler.logError(error)
                }
            }
        }
    }
    
    /// Call this method after user successfully authenticates to upload any pending FCM token
    fun uploadPendingTokenIfExists() {
        val pendingToken = sharedPreferences.getString("pending_fcm_token", null)
        
        if (pendingToken == null) {
            println("📱 [FCMTokenService] No pending FCM token to upload")
            return
        }
        
        println("🔄 [FCMTokenService] Uploading pending FCM token after authentication")
        uploadFCMToken(pendingToken)
    }
    
    // TODO: Firebase Auth not provided in context - placeholder implementation
    private fun getCurrentUserId(): String? {
        // This should return Firebase Auth currentUser?.uid
        return null // TODO: Implement Firebase Auth integration
    }
    
    // TODO: Firebase Messaging not provided in context - placeholder implementation
    private suspend fun getFirebaseMessagingToken(): String? {
        // This should return Firebase Messaging token
        return null // TODO: Implement Firebase Messaging integration
    }
}
