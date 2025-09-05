package com.spikai.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spikai.model.CareerLevel
import com.spikai.model.CareerProgress
import com.spikai.model.EnglishLevel
import com.spikai.model.UserProfile
import com.spikai.service.ErrorHandlingService
import com.spikai.service.LevelsService
import com.spikai.service.LocalDataService
import com.spikai.service.SpikError
import com.spikai.service.SpikErrorException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * CareerMapViewModel - Manages career level progression and user progress
 * Direct translation of Swift CareerMapViewModel with perfect feature parity
 */
class CareerMapViewModel(private val context: Context) : ViewModel() {
    
    // MARK: - Published Properties (StateFlow equivalent to @Published)
    private val _levels = MutableStateFlow<List<CareerLevel>>(emptyList())
    val levels: StateFlow<List<CareerLevel>> = _levels.asStateFlow()
    
    private val _progress = MutableStateFlow(CareerProgress())
    val progress: StateFlow<CareerProgress> = _progress.asStateFlow()
    
    private val _selectedLevel = MutableStateFlow<CareerLevel?>(null)
    val selectedLevel: StateFlow<CareerLevel?> = _selectedLevel.asStateFlow()
    
    private val _isLevelDetailShowing = MutableStateFlow(false)
    val isLevelDetailShowing: StateFlow<Boolean> = _isLevelDetailShowing.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _currentSelectedPath = MutableStateFlow(EnglishLevel.PRINCIPIANTE)
    val currentSelectedPath: StateFlow<EnglishLevel> = _currentSelectedPath.asStateFlow()
    
    // Private properties
    private val levelsService: LevelsService = LevelsService.shared
    private val errorHandler: ErrorHandlingService = ErrorHandlingService.shared
    private val localDataService: LocalDataService = LocalDataService.getInstance(context)
    private var userProfile: UserProfile? = null
    private var allLevels: List<CareerLevel> = emptyList() // Store all levels to filter by path
    
    // SharedPreferences for persistence
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("career_map_prefs", Context.MODE_PRIVATE)
    }
    
    // JSON serializer
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        @Volatile
        private var INSTANCE: CareerMapViewModel? = null
        
        fun getInstance(context: Context): CareerMapViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CareerMapViewModel(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        println("🎯 [CareerMapViewModel] ViewModel initialized")
        loadUserProfile()  // Load user profile first
        setupNotificationObserver()
        loadProgressFromLocalData()  // Load from local data for fast startup
        
        viewModelScope.launch {
            loadCareerLevels()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // TODO: Remove notification observer when proper notification system is implemented
        println("🗑️ [CareerMapViewModel] ViewModel cleared")
    }
    
    private fun loadUserProfile() {
        val jsonString = sharedPreferences.getString("userProfile", null)
        if (jsonString != null) {
            println("📦 [CareerMapViewModel] Found userProfile data, size: ${jsonString.length} bytes")
            
            try {
                val profile = json.decodeFromString<UserProfile>(jsonString)
                userProfile = profile
                println("✅ [CareerMapViewModel] Loaded user profile with level: ${profile.englishLevel?.rawValue ?: "unknown"} (base ID: ${profile.englishLevel?.baseLevelId ?: 0})")
            } catch (e: Exception) {
                println("❌ [CareerMapViewModel] Failed to decode user profile: ${e.message}")
                // Set user-friendly error message
                viewModelScope.launch {
                    _errorMessage.value = "Hubo un problema al cargar tu perfil. Usando configuración por defecto."
                }
                
                // Print raw JSON data for debugging
                println("📄 [CareerMapViewModel] Raw JSON data: $jsonString")
                
                // Set fallback profile
                userProfile = UserProfile()
                userProfile?.englishLevel = EnglishLevel.PRINCIPIANTE
            }
        } else {
            println("⚠️ [CareerMapViewModel] No user profile found in SharedPreferences, showing all levels")
            val allKeys = sharedPreferences.all.keys
            println("📋 [CareerMapViewModel] SharedPreferences keys: $allKeys")
            
            // Set default profile for new users
            userProfile = UserProfile()
            userProfile?.englishLevel = EnglishLevel.PRINCIPIANTE
        }
    }
    
    private fun setupNotificationObserver() {
        // TODO: Implement proper notification observer when notification system is available
        // For now, this is a placeholder that would listen for level completion notifications
        println("📡 [CareerMapViewModel] Notification observer setup (placeholder)")
    }
    
    private fun loadProgressFromLocalData() {
        val localProgress = localDataService.loadUserProgress()
        
        // Convert local progress to CareerProgress
        val careerProgress = CareerProgress()
        careerProgress.completedLevels = localProgress.completedLevels
        careerProgress.unlockedLevels = localProgress.unlockedLevels
        careerProgress.totalExperience = localProgress.totalExperience
        careerProgress.currentLevel = localProgress.currentLevel
        
        _progress.value = careerProgress
        
        println("✅ [CareerMapViewModel] Loaded progress from local data - Completed: ${localProgress.completedLevels.size}, Unlocked: ${localProgress.unlockedLevels.size}")
        
        // Also load from SharedPreferences as backup for compatibility
        loadProgressFromSharedPreferences()
    }
    
    private fun loadProgressFromSharedPreferences() {
        val jsonString = sharedPreferences.getString("careerProgress", null)
        if (jsonString != null) {
            try {
                val savedProgress = json.decodeFromString<CareerProgress>(jsonString)
                
                // Merge with local data (local data takes precedence)
                val localProgress = localDataService.loadUserProgress()
                if (localProgress.completedLevels.isEmpty() && savedProgress.completedLevels.isNotEmpty()) {
                    // If local data is empty but SharedPreferences has data, use SharedPreferences
                    _progress.value = savedProgress
                    println("✅ [CareerMapViewModel] Loaded progress from SharedPreferences (fallback)")
                }
            } catch (e: Exception) {
                println("❌ [CareerMapViewModel] Error decoding saved progress: ${e.message}")
                initializeFreshProgress()
            }
        } else if (localDataService.loadUserProgress().completedLevels.isEmpty()) {
            initializeFreshProgress()
        }
    }
    
    private fun initializeFreshProgress() {
        // Initialize progress based on user's English level if available
        val progressValue = if (userProfile?.englishLevel != null) {
            CareerProgress(basedOnEnglishLevel = userProfile!!.englishLevel!!)
        } else {
            CareerProgress() // Default initialization
        }
        
        _progress.value = progressValue
        
        if (userProfile?.englishLevel != null) {
            println("🎯 [CareerMapViewModel] Initialized progress for ${userProfile!!.englishLevel!!.rawValue} level")
        } else {
            println("⚠️ [CareerMapViewModel] Initialized default progress")
        }
    }
    
    private fun saveProgressToSharedPreferences() {
        // Save to both SharedPreferences and LocalDataService
        try {
            val jsonString = json.encodeToString(_progress.value)
            sharedPreferences.edit().putString("careerProgress", jsonString).apply()
        } catch (e: Exception) {
            println("❌ [CareerMapViewModel] Error saving progress to SharedPreferences: ${e.message}")
        }
        
        // Also save to LocalDataService
        val currentProgress = _progress.value
        var localProgress = localDataService.loadUserProgress()
        localProgress.completedLevels = currentProgress.completedLevels
        localProgress.unlockedLevels = currentProgress.unlockedLevels
        localProgress.totalExperience = currentProgress.totalExperience
        localProgress.currentLevel = currentProgress.currentLevel
        localProgress.lastSync = java.util.Date()
        localDataService.saveUserProgress(localProgress)
    }
    
    suspend fun loadCareerLevels() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val currentProgress = _progress.value
            println("🔄 [CareerMapViewModel] Loading levels. Current progress - Completed: ${currentProgress.completedLevels}, Unlocked: ${currentProgress.unlockedLevels}")
            
            try {
                val apiLevels = levelsService.fetchLevels()
                
                // Store all levels for path switching
                allLevels = apiLevels
                
                // Set initial path if not set
                val userLevel = userProfile?.englishLevel
                if (userLevel != null) {
                    _currentSelectedPath.value = userLevel
                } else {
                    _currentSelectedPath.value = EnglishLevel.PRINCIPIANTE
                }
                
                // Update levels based on selected path
                updateLevelsForSelectedPath()
                
                println("✅ [CareerMapViewModel] Loaded ${_levels.value.size} levels from API for path: ${_currentSelectedPath.value.rawValue} (filtered from ${apiLevels.size} total)")
                
                // Clear any previous errors on successful load
                _errorMessage.value = null
                
            } catch (e: Exception) {
                // Set user-friendly error message for the UI
                val errorMsg = when (e) {
                    is SpikErrorException -> e.spikError.errorDescription
                    else -> "No pudimos cargar los niveles. Usando contenido de ejemplo."
                }
                _errorMessage.value = errorMsg
                
                println("❌ [CareerMapViewModel] Error loading levels: ${e.message}")
                // Fallback to sample data in case of error
                loadSampleLevels()
            }
            
            _isLoading.value = false
        }
    }
    
    private fun filterLevelsForUser(allLevels: List<CareerLevel>): List<CareerLevel> {
        val userProfile = this.userProfile
        val englishLevel = userProfile?.englishLevel
        
        if (userProfile == null || englishLevel == null) {
            println("⚠️ [CareerMapViewModel] No user profile or English level found, showing all levels")
            this.allLevels = allLevels
            _currentSelectedPath.value = EnglishLevel.PRINCIPIANTE
            return allLevels
        }
        
        // Store all levels for path switching
        this.allLevels = allLevels
        
        // Set current selected path to user's level
        _currentSelectedPath.value = englishLevel
        
        val baseLevelId = englishLevel.baseLevelId
        val filteredLevels = allLevels.filter { level ->
            level.levelId >= baseLevelId && level.levelId < baseLevelId + 1000
        }
        
        println("🎯 [CareerMapViewModel] User level: ${englishLevel.rawValue} (base ID: $baseLevelId)")
        println("📋 [CareerMapViewModel] Filtered ${filteredLevels.size} levels from ${allLevels.size} total")
        
        return filteredLevels
    }
    
    private fun loadSampleLevels() {
        // Fallback method using sample data
        println("🔄 [CareerMapViewModel] Loading sample levels as fallback")
        val sampleLevels = com.spikai.model.CareerLevelSamples.sampleLevels
        allLevels = sampleLevels
        
        // Set current selected path
        val userLevel = userProfile?.englishLevel
        if (userLevel != null) {
            _currentSelectedPath.value = userLevel
        } else {
            _currentSelectedPath.value = EnglishLevel.PRINCIPIANTE
        }
        
        val filteredSampleLevels = filterLevelsForUser(sampleLevels)
        
        val levelsWithProgress = filteredSampleLevels.map { level ->
            CareerLevel(
                levelId = level.levelId,
                title = level.title,
                description = level.description,
                toLearn = level.toLearn,
                iosSymbol = level.iosSymbol, // Include the iosSymbol from sample data
                isUnlocked = shouldLevelBeUnlocked(level.levelId),
                isCompleted = _progress.value.completedLevels.contains(level.levelId),
                experience = level.experience,
                totalExperience = level.totalExperience
            )
        }
        
        _levels.value = levelsWithProgress
        println("✅ [CareerMapViewModel] Loaded ${levelsWithProgress.size} sample levels")
    }
    
    private fun getExperienceForLevel(levelId: Int): Int {
        // In the future, this could come from local storage or user progress API
        return 0
    }
    
    private fun getTotalExperienceForLevel(levelId: Int): Int {
        // Calculate based on level difficulty or get from API
        return 100 + (levelId - 1001) * 50 // Adjust for 1001-based IDs
    }
    
    suspend fun refreshLevels() {
        loadCareerLevels()
    }
    
    fun selectLevel(level: CareerLevel) {
        if (!level.isUnlocked) return
        
        viewModelScope.launch {
            _selectedLevel.value = level
            _isLevelDetailShowing.value = true
        }
    }
    
    fun startLevel(level: CareerLevel) {
        if (!level.isUnlocked) return
        
        viewModelScope.launch {
            // Aquí se iniciaría el nivel seleccionado
            // Por ahora solo cerramos el modal
            _isLevelDetailShowing.value = false
        }
    }
    
    fun completeLevel(levelNumber: Int) {
        viewModelScope.launch {
            println("🎯 [CareerMapViewModel] Completing level: $levelNumber")
            
            // Mark as completed in local data service (fast local storage)
            localDataService.markLevelCompleted(levelNumber)
            
            // Also update in-memory progress
            val currentProgress = _progress.value
            val updatedProgress = currentProgress.copy(
                completedLevels = currentProgress.completedLevels + levelNumber
            )
            _progress.value = updatedProgress
            
            // Desbloquear el siguiente nivel
            val sortedLevels = _levels.value.sortedBy { it.levelId }
            println("📋 [CareerMapViewModel] Available levels: ${sortedLevels.map { it.levelId }}")
            
            var unlockedLevelId: Int? = null
            val currentIndex = sortedLevels.indexOfFirst { it.levelId == levelNumber }
            if (currentIndex >= 0 && currentIndex + 1 < sortedLevels.size) {
                val nextLevelId = sortedLevels[currentIndex + 1].levelId
                val updatedProgressWithUnlock = updatedProgress.copy(
                    unlockedLevels = updatedProgress.unlockedLevels + nextLevelId,
                    currentLevel = maxOf(updatedProgress.currentLevel, nextLevelId)
                )
                _progress.value = updatedProgressWithUnlock
                unlockedLevelId = nextLevelId
                println("🔓 [CareerMapViewModel] Unlocked next level: $nextLevelId")
            } else {
                println("📝 [CareerMapViewModel] No next level to unlock. Current index: $currentIndex, levels count: ${sortedLevels.size}")
            }
            
            // Actualizar la experiencia total
            val level = _levels.value.firstOrNull { it.levelId == levelNumber }
            if (level != null) {
                val finalProgress = _progress.value.copy(
                    totalExperience = _progress.value.totalExperience + level.totalExperience
                )
                _progress.value = finalProgress
                println("💰 [CareerMapViewModel] Added ${level.totalExperience} experience, total: ${finalProgress.totalExperience}")
            }
            
            // Guardar el progreso actualizado
            saveProgressToSharedPreferences()
            println("💾 [CareerMapViewModel] Progress saved. Completed: ${_progress.value.completedLevels}, Unlocked: ${_progress.value.unlockedLevels}")
            
            // Recargar los niveles con el nuevo estado
            loadCareerLevels()
            
            // Trigger unlock animation if a new level was unlocked
            unlockedLevelId?.let { newLevelId ->
                delay(1000)
                println("🎬 [CareerMapViewModel] Triggering unlock animation for level: $newLevelId")
                // TODO: Post notification when notification system is implemented
                // NotificationCenter.default.post(name: "ShowLevelUnlockAnimation", userInfo: ["levelId": newLevelId])
            }
        }
    }
    
    fun getCurrentLevelInfo(): Triple<Int, Int, Double> {
        val currentProgress = _progress.value
        val currentLevel = currentProgress.currentLevel
        val totalLevels = _levels.value.size
        val percentage = if (totalLevels > 0) {
            (currentLevel - 1).toDouble() / totalLevels.toDouble() * 100
        } else {
            0.0
        }
        return Triple(currentLevel, totalLevels, percentage)
    }
    
    fun getTotalExperience(): Int {
        return _progress.value.totalExperience
    }
    
    fun getCompletedLevelsCount(): Int {
        return _progress.value.completedLevels.size
    }
    
    /**
     * Update user profile and reload levels with new filtering
     */
    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            println("🔄 [CareerMapViewModel] Updating user profile with level: ${profile.englishLevel?.rawValue ?: "unknown"}")
            userProfile = profile
            
            // Update current selected path to user's level
            profile.englishLevel?.let { englishLevel ->
                _currentSelectedPath.value = englishLevel
            }
            
            // Re-initialize progress if needed based on new profile
            profile.englishLevel?.let { englishLevel ->
                val currentProgress = _progress.value
                // Only update progress if it's still default (no saved progress exists)
                if (currentProgress.completedLevels.isEmpty() && 
                    currentProgress.unlockedLevels.size == 1 && 
                    currentProgress.unlockedLevels.contains(1001)) {
                    
                    val newProgress = CareerProgress(basedOnEnglishLevel = englishLevel)
                    _progress.value = newProgress
                    saveProgressToSharedPreferences()
                    println("🎯 [CareerMapViewModel] Updated progress for ${englishLevel.rawValue} level")
                }
            }
            
            // Reload levels with new filtering
            loadCareerLevels()
        }
    }
    
    // MARK: - Error Handling
    
    /**
     * Clear the current error message
     */
    fun clearError() {
        viewModelScope.launch {
            _errorMessage.value = null
        }
    }
    
    /**
     * Check if there's an active error
     */
    val hasError: Boolean
        get() {
            val errorMsg = _errorMessage.value
            return errorMsg != null && errorMsg.isNotEmpty()
        }
    
    // MARK: - Path Selection Methods
    
    /**
     * Get available paths for the user (only current and previous levels)
     */
    val availablePaths: List<EnglishLevel>
        get() {
            val userLevel = userProfile?.englishLevel ?: return listOf(EnglishLevel.PRINCIPIANTE)
            
            return when (userLevel) {
                EnglishLevel.PRINCIPIANTE -> listOf(EnglishLevel.PRINCIPIANTE)
                EnglishLevel.BASICO -> listOf(EnglishLevel.PRINCIPIANTE, EnglishLevel.BASICO)
                EnglishLevel.INTERMEDIO -> listOf(EnglishLevel.PRINCIPIANTE, EnglishLevel.BASICO, EnglishLevel.INTERMEDIO)
                EnglishLevel.AVANZADO -> listOf(EnglishLevel.PRINCIPIANTE, EnglishLevel.BASICO, EnglishLevel.INTERMEDIO, EnglishLevel.AVANZADO)
            }
        }
    
    /**
     * Get locked paths (future levels the user hasn't reached yet)
     */
    val lockedPaths: List<EnglishLevel>
        get() {
            val userLevel = userProfile?.englishLevel ?: return listOf(EnglishLevel.BASICO, EnglishLevel.INTERMEDIO, EnglishLevel.AVANZADO)
            
            val allPaths = listOf(EnglishLevel.PRINCIPIANTE, EnglishLevel.BASICO, EnglishLevel.INTERMEDIO, EnglishLevel.AVANZADO)
            return allPaths.filter { path ->
                path.baseLevelId > userLevel.baseLevelId
            }
        }
    
    /**
     * Switch to a different learning path
     */
    fun switchToPath(path: EnglishLevel) {
        if (!availablePaths.contains(path)) {
            println("⚠️ [CareerMapViewModel] Cannot switch to locked path: ${path.rawValue}")
            return
        }
        
        viewModelScope.launch {
            println("🔄 [CareerMapViewModel] Switching to path: ${path.rawValue}")
            _currentSelectedPath.value = path
            updateLevelsForSelectedPath()
        }
    }
    
    /**
     * Update the displayed levels based on the selected path
     */
    private fun updateLevelsForSelectedPath() {
        val pathBaseLevelId = _currentSelectedPath.value.baseLevelId
        val pathLevels = allLevels.filter { level ->
            level.levelId >= pathBaseLevelId && level.levelId < pathBaseLevelId + 1000
        }
        
        val levelsWithProgress = pathLevels.map { level ->
            CareerLevel(
                levelId = level.levelId,
                title = level.title,
                description = level.description,
                toLearn = level.toLearn,
                iosSymbol = level.iosSymbol,
                isUnlocked = shouldLevelBeUnlocked(level.levelId),
                isCompleted = _progress.value.completedLevels.contains(level.levelId),
                experience = getExperienceForLevel(level.levelId),
                totalExperience = getTotalExperienceForLevel(level.levelId)
            )
        }
        
        _levels.value = levelsWithProgress
        
        println("✅ [CareerMapViewModel] Updated to ${levelsWithProgress.size} levels for path: ${_currentSelectedPath.value.rawValue}")
    }
    
    /**
     * Determine if a level should be unlocked based on user's overall progress
     */
    private fun shouldLevelBeUnlocked(levelId: Int): Boolean {
        val userLevel = userProfile?.englishLevel ?: return false
        
        // If the level is from a path lower than user's level, unlock all levels in that path
        val userBaseLevelId = userLevel.baseLevelId
        val levelPath = levelId / 1000 * 1000 // Get the path base (1000, 2000, 3000, 4000)
        
        return when {
            levelPath < userBaseLevelId -> {
                // All levels in previous paths are unlocked
                true
            }
            levelPath == userBaseLevelId -> {
                // For current path, use normal unlock logic
                _progress.value.unlockedLevels.contains(levelId)
            }
            else -> {
                // Future paths are locked
                false
            }
        }
    }
}
