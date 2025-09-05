package com.spikai.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spikai.model.AssessmentQuestion
import com.spikai.model.EnglishLevel
import com.spikai.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// MARK: - Onboarding Step
enum class OnboardingStep {
    WELCOME,
    ASSESSMENT,
    COMPLETION;

    val title: String
        get() = when (this) {
            WELCOME -> "Bienvenido a Spik"
            ASSESSMENT -> "Evaluemos tu inglés"
            COMPLETION -> "¡Todo listo!"
        }

    companion object {
        val allCases: List<OnboardingStep> = values().toList()
    }
}

// MARK: - OnboardingViewModel
class OnboardingViewModel(private val context: Context) : ViewModel() {
    
    // MARK: - Published Properties (StateFlow equivalent to @Published)
    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()
    
    private val _currentAssessmentQuestion = MutableStateFlow(AssessmentQuestion.SPEAKING)
    val currentAssessmentQuestion: StateFlow<AssessmentQuestion> = _currentAssessmentQuestion.asStateFlow()
    
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()
    
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showError = MutableStateFlow(false)
    val showError: StateFlow<Boolean> = _showError.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    // SharedPreferences for persistence
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    }
    
    // JSON serializer
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        @Volatile
        private var INSTANCE: OnboardingViewModel? = null
        
        fun getInstance(context: Context): OnboardingViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingViewModel(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        println("🎯 [OnboardingViewModel] ViewModel initialized")
        loadUserProfile()
    }
    
    // MARK: - Computed Properties
    val canProceed: Boolean
        get() = when (_currentStep.value) {
            OnboardingStep.WELCOME -> !_userProfile.value.name.trim().isEmpty()
            OnboardingStep.ASSESSMENT -> _userProfile.value.assessmentAnswers.size == AssessmentQuestion.allCases.size
            OnboardingStep.COMPLETION -> true
        }
    
    val progress: Double
        get() {
            // Total progress steps: Welcome (1) + Assessment Questions (5) + Completion (1) = 7
            val totalProgressSteps = 6.0
            
            return when (_currentStep.value) {
                OnboardingStep.WELCOME -> {
                    // Welcome step is step 0 out of 7
                    0.0 / totalProgressSteps
                }
                OnboardingStep.ASSESSMENT -> {
                    // Assessment starts at step 1, each question adds 1 step
                    val assessmentProgress = 1.0 + _currentQuestionIndex.value.toDouble()
                    assessmentProgress / totalProgressSteps
                }
                OnboardingStep.COMPLETION -> {
                    // Completion is the final step
                    6.0 / totalProgressSteps
                }
            }
        }
    
    val isLastStep: Boolean
        get() = _currentStep.value == OnboardingStep.COMPLETION
    
    val nextButtonTitle: String
        get() = when (_currentStep.value) {
            OnboardingStep.WELCOME -> "Comenzar"
            OnboardingStep.COMPLETION -> "Comenzar🎙️"
            else -> "Continuar"
        }
    
    // MARK: - Actions
    fun nextStep() {
        if (!canProceed) return
        
        if (isLastStep) {
            completeOnboarding()
            return
        }
        
        val allSteps = OnboardingStep.allCases
        val currentIndex = allSteps.indexOf(_currentStep.value)
        if (currentIndex < allSteps.size - 1) {
            viewModelScope.launch {
                _currentStep.value = allSteps[currentIndex + 1]
                
                // Reset assessment when entering assessment step
                if (_currentStep.value == OnboardingStep.ASSESSMENT) {
                    _currentQuestionIndex.value = 0
                    _currentAssessmentQuestion.value = AssessmentQuestion.allCases[0]
                }
                
                println("🔄 [OnboardingViewModel] Moved to next step: ${_currentStep.value}")
            }
        }
    }
    
    fun previousStep() {
        val allSteps = OnboardingStep.allCases
        val currentIndex = allSteps.indexOf(_currentStep.value)
        if (currentIndex > 0) {
            viewModelScope.launch {
                _currentStep.value = allSteps[currentIndex - 1]
                println("🔄 [OnboardingViewModel] Moved to previous step: ${_currentStep.value}")
            }
        }
    }
    
    fun selectEnglishLevel(level: EnglishLevel) {
        viewModelScope.launch {
            val updatedProfile = _userProfile.value.copy(englishLevel = level)
            _userProfile.value = updatedProfile
            println("📝 [OnboardingViewModel] Selected English level: ${level.rawValue}")
        }
    }
    
    fun selectAssessmentAnswer(question: AssessmentQuestion, level: Int) {
        viewModelScope.launch {
            val currentAnswers = _userProfile.value.assessmentAnswers.toMutableMap()
            currentAnswers[question.value] = level
            
            val updatedProfile = _userProfile.value.copy(assessmentAnswers = currentAnswers)
            _userProfile.value = updatedProfile
            
            println("📝 [OnboardingViewModel] Selected answer for ${question.name}: $level")
            
            // Calculate English level after all questions are answered
            if (updatedProfile.assessmentAnswers.size == AssessmentQuestion.allCases.size) {
                val profileWithLevel = updatedProfile.calculateEnglishLevel()
                _userProfile.value = profileWithLevel
                println("🎯 [OnboardingViewModel] Calculated English level: ${profileWithLevel.englishLevel?.rawValue}")
            }
        }
    }
    
    fun updateUserName(name: String) {
        viewModelScope.launch {
            val updatedProfile = _userProfile.value.copy(name = name)
            _userProfile.value = updatedProfile
            println("📝 [OnboardingViewModel] Updated user name: $name")
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            _isLoading.value = true
            println("🏁 [OnboardingViewModel] Completing onboarding...")
            
            // Simulate saving user profile
            delay(250)
            
            val updatedProfile = _userProfile.value.copy(hasCompletedOnboarding = true)
            _userProfile.value = updatedProfile
            _isLoading.value = false
            
            // Save to SharedPreferences
            saveUserProfile()
            
            println("✅ [OnboardingViewModel] Onboarding completed successfully")
        }
    }
    
    private fun saveUserProfile() {
        try {
            val jsonString = json.encodeToString(_userProfile.value)
            sharedPreferences.edit()
                .putString("userProfile", jsonString)
                .apply()
            
            println("✅ [OnboardingViewModel] User profile saved with level: ${_userProfile.value.englishLevel?.rawValue ?: "unknown"}")
        } catch (e: Exception) {
            println("❌ [OnboardingViewModel] Failed to encode user profile: ${e.message}")
            
            viewModelScope.launch {
                _showError.value = true
                _errorMessage.value = "Failed to save profile: ${e.message}"
            }
        }
    }
    
    fun loadUserProfile() {
        try {
            val jsonString = sharedPreferences.getString("userProfile", null)
            if (jsonString != null) {
                val profile = json.decodeFromString<UserProfile>(jsonString)
                _userProfile.value = profile
                println("✅ [OnboardingViewModel] User profile loaded with level: ${profile.englishLevel?.rawValue ?: "unknown"}")
            } else {
                println("⚠️ [OnboardingViewModel] No user profile found in SharedPreferences")
            }
        } catch (e: Exception) {
            println("❌ [OnboardingViewModel] Failed to load user profile: ${e.message}")
            // Keep default profile on error
        }
    }
    
    fun clearError() {
        viewModelScope.launch {
            _showError.value = false
            _errorMessage.value = ""
        }
    }
    
    // Debug method for development builds
    fun clearAllUserDefaults() {
        sharedPreferences.edit().clear().apply()
        _userProfile.value = UserProfile()
        println("🗑️ [OnboardingViewModel] Cleared all user data")
    }
    // MARK: - Assessment Navigation Methods
    
    val canProceedInAssessment: Boolean
        get() {
            if (_currentStep.value == OnboardingStep.ASSESSMENT) {
                // If it's the last question, need all questions answered
                if (_currentQuestionIndex.value == AssessmentQuestion.allCases.size - 1) {
                    return _userProfile.value.assessmentAnswers.size == AssessmentQuestion.allCases.size
                } else {
                    // For other questions, just need current question answered
                    val currentQuestion = AssessmentQuestion.allCases[_currentQuestionIndex.value]
                    return _userProfile.value.assessmentAnswers[currentQuestion.value] != null
                }
            }
            return canProceed
        }
    
    val assessmentButtonTitle: String
        get() {
            if (_currentStep.value == OnboardingStep.ASSESSMENT) {
                if (_currentQuestionIndex.value == AssessmentQuestion.allCases.size - 1) {
                    return "Finalizar evaluación"
                } else {
                    return "Siguiente pregunta"
                }
            }
            return nextButtonTitle
        }
    
    fun nextAssessmentQuestion() {
        if (_currentStep.value != OnboardingStep.ASSESSMENT) return
        
        viewModelScope.launch {
            if (_currentQuestionIndex.value < AssessmentQuestion.allCases.size - 1) {
                _currentQuestionIndex.value += 1
                _currentAssessmentQuestion.value = AssessmentQuestion.allCases[_currentQuestionIndex.value]
                println("➡️ [OnboardingViewModel] Moved to assessment question ${_currentQuestionIndex.value + 1}")
            } else {
                // Finished all questions, move to next step
                println("🏁 [OnboardingViewModel] Finished all assessment questions")
                nextStep()
            }
        }
    }
    
    fun previousAssessmentQuestion() {
        if (_currentStep.value != OnboardingStep.ASSESSMENT || _currentQuestionIndex.value <= 0) return
        
        viewModelScope.launch {
            _currentQuestionIndex.value -= 1
            _currentAssessmentQuestion.value = AssessmentQuestion.allCases[_currentQuestionIndex.value]
            println("⬅️ [OnboardingViewModel] Moved to assessment question ${_currentQuestionIndex.value + 1}")
        }
    }
}
