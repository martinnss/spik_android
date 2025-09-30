package com.spikai.service

import com.spikai.model.CareerLevel
import com.spikai.model.LevelsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class LevelsService {
    
    private val baseURL = "https://us-central1-spik-backend.cloudfunctions.net/articles"
    private val errorHandler = ErrorHandlingService.shared
    private val json = Json { ignoreUnknownKeys = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        val shared = LevelsService()
    }
    
    suspend fun fetchLevels(): List<CareerLevel> {
        return withContext(Dispatchers.IO) {
            val url = "$baseURL/get-levels"
            
            println("🌐 [LevelsService] Starting API call to: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()
            
            try {
                println("📡 [LevelsService] Executing HTTP request...")
                val response = client.newCall(request).execute()
                println("📨 [LevelsService] Received response with status: ${response.code}")
                
                // Check HTTP response status
                when (response.code) {
                    in 200..299 -> {
                        // Success - continue processing
                    }
                    401 -> {
                        val error = SpikError.AUTH_TOKEN_EXPIRED
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                    403 -> {
                        val error = SpikError.PERMISSION_DENIED
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                    404 -> {
                        val error = SpikError.DATA_NOT_FOUND
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                    in 500..599 -> {
                        val error = SpikError.SERVER_UNAVAILABLE
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                    else -> {
                        val error = SpikError.UNKNOWN_ERROR
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                }
                
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    val error = SpikError.DATA_NOT_FOUND
                    errorHandler.showError(error)
                    throw SpikErrorException(error)
                }
                
                try {
                    println("📄 [LevelsService] Raw API response body (first 500 chars):")
                    println(responseBody.take(500))
                    println("📄 [LevelsService] Response body length: ${responseBody.length}")
                    
                    val levelsResponse = json.decodeFromString<LevelsResponse>(responseBody)
                    println("✅ [LevelsService] Successfully decoded ${levelsResponse.levels.size} levels")
                    levelsResponse.levels
                } catch (e: Exception) {
                    println("❌ [LevelsService] JSON decoding failed: ${e.message}")
                    println("📄 [LevelsService] Full response body:")
                    println(responseBody)
                    
                    // Check if it's actually a JSON parsing issue or something else
                    if (responseBody.trim().startsWith("{") || responseBody.trim().startsWith("[")) {
                        // It looks like JSON but failed to parse - this could be DATA_CORRUPTED
                        println("🔍 [LevelsService] Response looks like JSON but failed to parse - potential schema mismatch")
                        val error = SpikError.DATA_CORRUPTED
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    } else {
                        // It doesn't look like JSON - probably server error or HTML error page
                        println("🔍 [LevelsService] Response doesn't look like JSON - likely server error")
                        val error = SpikError.SERVER_UNAVAILABLE
                        errorHandler.showError(error)
                        throw SpikErrorException(error)
                    }
                }
                
            } catch (e: SpikErrorException) {
                throw e
            } catch (e: Exception) {
                val spikError = errorHandler.handleError(e)
                errorHandler.showError(spikError)
                throw SpikErrorException(spikError)
            }
        }
    }
    
    /**
     * Test method to check what the API is actually returning
     */
    suspend fun testAPIEndpoint(): String {
        return withContext(Dispatchers.IO) {
            val url = "$baseURL/get-levels"
            
            println("🧪 [LevelsService] Testing API endpoint: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()
            
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"
                
                println("🧪 [LevelsService] Test API Response:")
                println("   Status Code: ${response.code}")
                println("   Content-Type: ${response.header("Content-Type")}")
                println("   Response Body Length: ${responseBody.length}")
                println("   Response Body (first 1000 chars): ${responseBody.take(1000)}")
                
                responseBody
            } catch (e: Exception) {
                val errorMsg = "Test API call failed: ${e.message}"
                println("❌ [LevelsService] $errorMsg")
                errorMsg
            }
        }
    }
}
