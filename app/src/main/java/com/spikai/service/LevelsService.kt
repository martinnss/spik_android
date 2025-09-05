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
            
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()
            
            try {
                val response = client.newCall(request).execute()
                
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
                    val levelsResponse = json.decodeFromString<LevelsResponse>(responseBody)
                    levelsResponse.levels
                } catch (e: Exception) {
                    val error = SpikError.DATA_CORRUPTED
                    errorHandler.showError(error)
                    throw SpikErrorException(error)
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
}
