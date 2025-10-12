package com.spikai.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spikai.model.ConnectionStatus
import com.spikai.model.ConversationFlowAnalysis
import com.spikai.model.ConversationItem
import com.spikai.model.LevelProgressionEvaluation
import com.spikai.service.ConversationAnalysisService
import com.spikai.service.ErrorHandlingService
import com.spikai.service.SessionTrackingService
import com.spikai.service.UserPreferencesService
import com.spikai.service.WebRTCManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// MARK: - ConversationViewModel
class ConversationViewModel(
    private val context: Context,
    private val levelId: Int? = null,
    private val userPreferences: UserPreferencesService = UserPreferencesService.getInstance(context),
    private val sessionTracker: SessionTrackingService = SessionTrackingService.getInstance(context),
    private val analysisService: ConversationAnalysisService = ConversationAnalysisService.shared,
    private val errorHandler: ErrorHandlingService = ErrorHandlingService.shared
) : ViewModel() {
    
    // MARK: - Published Properties (StateFlow equivalent to @Published)
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _conversation = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversation: StateFlow<List<ConversationItem>> = _conversation.asStateFlow()
    
    private val _outgoingMessage = MutableStateFlow("")
    val outgoingMessage: StateFlow<String> = _outgoingMessage.asStateFlow()
    
    private val _eventTypeStr = MutableStateFlow("")
    val eventTypeStr: StateFlow<String> = _eventTypeStr.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    // MARK: - Conversation Analysis Properties
    private val _isAnalyzingConversation = MutableStateFlow(false)
    val isAnalyzingConversation: StateFlow<Boolean> = _isAnalyzingConversation.asStateFlow()
    
    private val _isEvaluatingLevel = MutableStateFlow(false)
    val isEvaluatingLevel: StateFlow<Boolean> = _isEvaluatingLevel.asStateFlow()
    
    private val _showEvaluationPopup = MutableStateFlow(false)
    val showEvaluationPopup: StateFlow<Boolean> = _showEvaluationPopup.asStateFlow()
    
    private val _levelEvaluation = MutableStateFlow<LevelProgressionEvaluation?>(null)
    val levelEvaluation: StateFlow<LevelProgressionEvaluation?> = _levelEvaluation.asStateFlow()
    
    // MARK: - Settings Properties
    /// AI speaking speed multiplier (0.5 = slow, 1.0 = normal, 2.0 = fast)
    /// This setting is only applied when starting a new session and is sent to the backend
    /// as a parameter when requesting the ephemeral token
    private val _aiSpeakingSpeed = MutableStateFlow(1.0)
    val aiSpeakingSpeed: StateFlow<Double> = _aiSpeakingSpeed.asStateFlow()
    
    // MARK: - Private Properties
    private val webRTCManager: WebRTCManager
    private var bindingJobs: List<Job> = emptyList()
    
    // Track when AI last spoke to trigger analysis
    private var lastAIMessageCount = 0
    
    // MARK: - Initialization
    init {
        this.webRTCManager = WebRTCManager(context)
        
        // Load saved speed preference
        _aiSpeakingSpeed.value = userPreferences.aiSpeakingSpeed
        
        setupBindings()
        
        // Listen for aiSpeakingSpeed changes to save preference
        viewModelScope.launch {
            _aiSpeakingSpeed.collect { speed ->
                userPreferences.aiSpeakingSpeed = speed
            }
        }
    }
    
    /// Initialize with custom backend URL
    constructor(
        context: Context,
        backendURL: String,
        levelId: Int? = null,
        userPreferences: UserPreferencesService = UserPreferencesService.getInstance(context),
        sessionTracker: SessionTrackingService = SessionTrackingService.getInstance(context),
        analysisService: ConversationAnalysisService = ConversationAnalysisService.shared,
        errorHandler: ErrorHandlingService = ErrorHandlingService.shared
    ) : this(context, levelId, userPreferences, sessionTracker, analysisService, errorHandler) {
        // TODO: Implement custom backend URL for WebRTCManager
        // this.webRTCManager = WebRTCManager(context, backendURL)
    }
    
    // MARK: - Cleanup
    override fun onCleared() {
        super.onCleared()
        println("🧹 [ConversationVM] Cleaning up conversation view model")
        stopRecording()
        webRTCManager.stopConnection()
        bindingJobs.forEach { it.cancel() }
    }
    
    // MARK: - Setup Methods
    private fun setupBindings() {
        // Bind WebRTC manager properties to view model
        val jobs = listOf(
            viewModelScope.launch {
                webRTCManager.connectionStatus.collect { status ->
                    _connectionStatus.value = status
                }
            },
            viewModelScope.launch {
                webRTCManager.conversation.collect { conversation ->
                    _conversation.value = conversation
                    handleConversationUpdate(conversation)
                }
            },
            viewModelScope.launch {
                webRTCManager.outgoingMessage.collect { message ->
                    _outgoingMessage.value = message
                }
            },
            viewModelScope.launch {
                webRTCManager.eventTypeStr.collect { eventType ->
                    _eventTypeStr.value = eventType
                }
            },
            viewModelScope.launch {
                webRTCManager.errorMessage.collect { error ->
                    _errorMessage.value = error
                }
            },
            viewModelScope.launch {
                webRTCManager.isMuted.collect { muted ->
                    _isMuted.value = muted
                }
            },
            // Bind outgoing message changes back to WebRTC manager
            viewModelScope.launch {
                _outgoingMessage.collect { message ->
                    // TODO: Implement setOutgoingMessage in WebRTCManager
                    // webRTCManager.setOutgoingMessage(message)
                }
            }
        )
        bindingJobs = jobs
    }
    
    // MARK: - Computed Properties
    val connectionStatusColor: Color
        get() = when (_connectionStatus.value) {
            ConnectionStatus.CONNECTED -> Color(0xFF34C759) // StatusConnected
            ConnectionStatus.CONNECTING -> Color(0xFFFF9500) // StatusConnecting
            ConnectionStatus.DISCONNECTED -> Color(0xFF8E8E93) // StatusDisconnected
            ConnectionStatus.FAILED -> Color(0xFFFF3B30) // StatusFailed
        }
    
    val connectionStatusText: String
        get() = when (_connectionStatus.value) {
            ConnectionStatus.CONNECTED -> "Connected"
            ConnectionStatus.CONNECTING -> "Connecting..."
            ConnectionStatus.DISCONNECTED -> "Disconnected"
            ConnectionStatus.FAILED -> "Failed"
        }
    
    val isConnected: Boolean
        get() = _connectionStatus.value == ConnectionStatus.CONNECTED
    
    val isDisconnected: Boolean
        get() = _connectionStatus.value == ConnectionStatus.DISCONNECTED
    
    val canSendMessage: Boolean
        get() = _outgoingMessage.value.trim().isNotEmpty() && isConnected
    
    val isConversationEmpty: Boolean
        get() = _conversation.value.isEmpty()
    
    val hasEventType: Boolean
        get() = _eventTypeStr.value.isNotEmpty()
    
    val hasError: Boolean
        get() = _errorMessage.value.isNotEmpty()
    
    val canChangeSettings: Boolean
        get() = isDisconnected
    
    // MARK: - Action Methods
    fun startSession() {
        viewModelScope.launch {
            // Ensure we're on the main thread for UI updates
            _connectionStatus.value = ConnectionStatus.CONNECTING
            _errorMessage.value = "" // Clear any previous errors
            
            // Add a delay to prevent race conditions on device
            delay(100)
            
            // Start level-based session if levelId is available
            levelId?.let { id ->
                println("🎯 [ConversationVM] Starting level session for level: $id")
                sessionTracker.startLevelSession(id)
            } ?: run {
                println("⚠️ [ConversationVM] Cannot start level session - levelId is nil")
            }
            
            println("Starting session with ephemeral token and speed: ${_aiSpeakingSpeed.value}")
            
            // Wrap WebRTC start in error handling
            try {
                webRTCManager.fetchSessionConfigAndStartConnection(levelId = levelId, speed = _aiSpeakingSpeed.value)
            } catch (error: Exception) {
                println("❌ [ConversationVM] Error starting WebRTC session: $error")
                _connectionStatus.value = ConnectionStatus.FAILED
                _errorMessage.value = "Failed to start conversation: ${error.localizedMessage ?: error.message}"
            }
        }
    }
    
    fun endSession() {
        webRTCManager.stopConnection()
        
        // End the level session without completion (user manually ended)
        levelId?.let { id ->
            println("🛑 [ConversationVM] Manually ending session for level: $id")
            sessionTracker.endLevelSession(levelId = id, completed = false)
        }
    }
    
    /// End WebRTC session without affecting session tracking (used when level completion is handled separately)
    private fun endSessionWithoutTracking() {
        webRTCManager.stopConnection()
        println("🛑 [ConversationVM] Ending WebRTC connection (session tracking handled separately)")
    }
    
    fun toggleMute() {
        webRTCManager.toggleMute()
    }
    
    fun stopRecording() {
        // Stop any ongoing recording or audio processing
        _isRecording.value = false
        println("🎤 [ConversationVM] Recording stopped")
    }
    
    fun sendMessage() {
        if (!canSendMessage) return
        webRTCManager.sendMessage()
    }
    
    fun updateOutgoingMessage(message: String) {
        _outgoingMessage.value = message
    }
    
    fun updateAiSpeakingSpeed(speed: Double) {
        _aiSpeakingSpeed.value = speed
    }
    
    // MARK: - Level Tracking Methods
    fun completeLevel() {
        levelId?.let { id ->
            println("🎉 [ConversationVM] Completing level: $id")
            sessionTracker.endLevelSession(levelId = id, completed = true)
        } ?: run {
            println("⚠️ [ConversationVM] Cannot complete level - levelId is nil")
        }
    }
    
    fun failLevel() {
        levelId?.let { id ->
            println("❌ [ConversationVM] Failing level: $id")
            sessionTracker.endLevelSession(levelId = id, completed = false)
        } ?: run {
            println("⚠️ [ConversationVM] Cannot fail level - levelId is nil")
        }
    }
    
    // MARK: - Conversation Analysis Methods
    
    private fun handleConversationUpdate(conversation: List<ConversationItem>) {
        // Only trigger analysis if we have a level to analyze and conversation is not empty
        val currentLevelId = levelId
        if (currentLevelId == null || conversation.isEmpty()) {
            println("🔍 [ConversationVM] Skipping analysis - levelId: ${currentLevelId?.toString() ?: "null"}, conversation count: ${conversation.size}")
            return
        }
        
        // Count AI messages
        val aiMessageCount = conversation.count { it.role != "user" }
        
        //println("🔍 [ConversationVM] Conversation update:")
        //println("   Total messages: ${conversation.size}")
        //println("   AI messages: $aiMessageCount")
        //println("   Last AI count: $lastAIMessageCount")
        
        // Trigger analysis only when AI sends a new message (not when user speaks)
        if (aiMessageCount > lastAIMessageCount) {
            lastAIMessageCount = aiMessageCount
            println("🚀 [ConversationVM] New AI message detected, triggering analysis...")
            analyzeConversationFlow()
        } else {
            //println("🔍 [ConversationVM] No new AI messages, skipping analysis")
        }
    }
    
    private fun analyzeConversationFlow() {
        val currentLevelId = levelId
        if (currentLevelId == null || _conversation.value.isEmpty() || _isAnalyzingConversation.value) {
            println("🔍 [ConversationVM] Skipping flow analysis - levelId: ${currentLevelId?.toString() ?: "null"}, conversation empty: ${_conversation.value.isEmpty()}, already analyzing: ${_isAnalyzingConversation.value}")
            return
        }
        
        println("🔍 [ConversationVM] Starting conversation flow analysis...")
        _isAnalyzingConversation.value = true
        
        viewModelScope.launch {
            try {
                val result = analysisService.analyzeConversationFlow(
                    conversation = _conversation.value,
                    levelId = currentLevelId,
                    context = context
                )
                
                _isAnalyzingConversation.value = false
                
                result.fold(
                    onSuccess = { analysis ->
                        println("✅ [ConversationVM] Flow analysis completed successfully")
                        handleConversationFlowAnalysis(analysis)
                    },
                    onFailure = { error ->
                        println("❌ [ConversationVM] Flow analysis failed: ${error.localizedMessage}")
                        // Error is already handled by the service, continue normally
                    }
                )
            } catch (error: Exception) {
                _isAnalyzingConversation.value = false
                println("❌ [ConversationVM] Flow analysis exception: ${error.message}")
            }
        }
    }
    
    private fun handleConversationFlowAnalysis(analysis: ConversationFlowAnalysis) {
        println("🔍 [ConversationVM] Handling flow analysis result:")
        println("   Should continue: ${analysis.shouldContinue}")
        println("   Reason: ${analysis.reason}")
        
        if (!analysis.shouldContinue) {
            println("🏁 [ConversationVM] Conversation should end, triggering level evaluation...")
            // Conversation should end, trigger level evaluation
            evaluateLevelProgression()
        } else {
            println("➡️ [ConversationVM] Conversation should continue...")
        }
        // If shouldContinue is true, do nothing - let conversation continue
    }
    
    private fun evaluateLevelProgression() {
        val currentLevelId = levelId
        if (currentLevelId == null || _conversation.value.isEmpty() || _isEvaluatingLevel.value) {
            println("📊 [ConversationVM] Skipping level evaluation - levelId: ${currentLevelId?.toString() ?: "null"}, conversation empty: ${_conversation.value.isEmpty()}, already evaluating: ${_isEvaluatingLevel.value}")
            return
        }
        
        println("📊 [ConversationVM] Starting level progression evaluation...")
        _isEvaluatingLevel.value = true
        
        viewModelScope.launch {
            try {
                val result = analysisService.evaluateLevelProgression(
                    conversation = _conversation.value,
                    currentLevelId = currentLevelId,
                    context = context
                )
                
                _isEvaluatingLevel.value = false
                
                result.fold(
                    onSuccess = { evaluation ->
                        println("✅ [ConversationVM] Level evaluation completed successfully")
                        handleLevelEvaluation(evaluation)
                    },
                    onFailure = { error ->
                        println("❌ [ConversationVM] Level evaluation failed: ${error.localizedMessage}")
                        // Error is already handled by the service
                        // Set a local message for user feedback on evaluation failure
                        _errorMessage.value = "No pudimos evaluar tu progreso. Por favor intenta nuevamente."
                    }
                )
            } catch (error: Exception) {
                _isEvaluatingLevel.value = false
                println("❌ [ConversationVM] Level evaluation exception: ${error.message}")
                _errorMessage.value = "No pudimos evaluar tu progreso. Por favor intenta nuevamente."
            }
        }
    }
    
    private fun handleLevelEvaluation(evaluation: LevelProgressionEvaluation) {
        _levelEvaluation.value = evaluation
        
        println("📊 [ConversationVM] Processing evaluation for level ${levelId ?: -1}: passed = ${evaluation.passed}")
        
        // Track completion in session service BEFORE ending WebRTC session
        if (evaluation.passed) {
            println("✅ [ConversationVM] Level passed - calling completeLevel()")
            completeLevel()
        } else {
            println("❌ [ConversationVM] Level failed - calling failLevel()")
            failLevel()
        }
        
        // End the WebRTC session AFTER tracking completion
        endSessionWithoutTracking()
        
        // Show evaluation popup
        _showEvaluationPopup.value = true
        println("🎉 [ConversationVM] Evaluation popup state set to TRUE - popup should now be visible")
        println("📊 [ConversationVM] Evaluation details: score=${evaluation.score}, passed=${evaluation.passed}")
    }
    
    // MARK: - Evaluation Popup Actions
    
    fun onEvaluationContinue() {
        _showEvaluationPopup.value = false
        _levelEvaluation.value = null
        // The view should handle navigation to next level
    }
    
    fun onEvaluationRetry() {
        _showEvaluationPopup.value = false
        _levelEvaluation.value = null
        // Clear conversation and restart
        _conversation.value = emptyList()
        lastAIMessageCount = 0
        startSession()
    }
    
    fun onEvaluationClose() {
        _showEvaluationPopup.value = false
        _levelEvaluation.value = null
        // The view should handle navigation back
    }
    
    // MARK: - Settings Management
    
    fun resetUserPreferences() {
        userPreferences.resetToDefaults()
        _aiSpeakingSpeed.value = 1.0 // Reset to default
        println("🔄 User preferences reset to defaults")
    }
    
    val hasCustomPreferences: Boolean
        get() = userPreferences.hasCustomPreferences
    
    companion object {
        @Volatile
        private var INSTANCE: ConversationViewModel? = null
        
        fun getInstance(
            context: Context,
            levelId: Int? = null
        ): ConversationViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConversationViewModel(context, levelId).also { INSTANCE = it }
            }
        }
    }
}
