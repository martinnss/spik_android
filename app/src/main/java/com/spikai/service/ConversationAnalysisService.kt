package com.spikai.service

import android.content.Context
import com.spikai.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// MARK: - ConversationAnalysisService
class ConversationAnalysisService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    private val errorHandler = ErrorHandlingService.shared
    
    companion object {
        val shared = ConversationAnalysisService()
    }
    
    // MARK: - Analyze Conversation Flow
    suspend fun analyzeConversationFlow(
        conversation: List<ConversationItem>,
        levelId: Int,
        context: Context
    ): Result<ConversationFlowAnalysis> {
        return withContext(Dispatchers.IO) {
            try {
                val messages = conversation.map { it.toConversationMessage() }
                val request = ConversationAnalysisRequest(conversation = messages, levelId = levelId)
                
                val url = "${NetworkConfig.getBackendURL(context)}/analyze-conversation-flow"
                
                performRequest<ConversationAnalysisRequest, ConversationFlowAnalysis>(
                    url = url,
                    body = request
                )
            } catch (e: Exception) {
                val spikError = errorHandler.handleError(e)
                errorHandler.showError(spikError)
                Result.failure(Exception(spikError.errorDescription))
            }
        }
    }
    
    // MARK: - Evaluate Level Progression
    suspend fun evaluateLevelProgression(
        conversation: List<ConversationItem>,
        currentLevelId: Int,
        context: Context
    ): Result<LevelProgressionEvaluation> {
        return withContext(Dispatchers.IO) {
            try {
                val messages = conversation.map { it.toConversationMessage() }
                val request = LevelProgressionRequest(conversation = messages, currentLevelId = currentLevelId)
                
                val url = "${NetworkConfig.getBackendURL(context)}/evaluate-level-progression"
                
                performRequest<LevelProgressionRequest, LevelProgressionEvaluation>(
                    url = url,
                    body = request
                )
            } catch (e: Exception) {
                val spikError = errorHandler.handleError(e)
                errorHandler.showError(spikError)
                Result.failure(Exception(spikError.errorDescription))
            }
        }
    }
    
    // MARK: - Generic Request Method
    private suspend inline fun <reified T, reified R> performRequest(
        url: String,
        body: T
    ): Result<R> where T : Any, R : Any {
        return try {
            val jsonBody = json.encodeToString(body)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                in 200..299 -> {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        try {
                            val result = json.decodeFromString<R>(responseBody)
                            Result.success(result)
                        } catch (e: Exception) {
                            val spikError = SpikError.DATA_CORRUPTED
                            errorHandler.showError(spikError)
                            Result.failure(Exception(spikError.errorDescription))
                        }
                    } else {
                        val error = SpikError.DATA_NOT_FOUND
                        errorHandler.showError(error)
                        Result.failure(Exception(error.errorDescription))
                    }
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
        } catch (e: Exception) {
            val spikError = errorHandler.handleError(e)
            errorHandler.showError(spikError)
            Result.failure(Exception(spikError.errorDescription))
        }
    }
}
