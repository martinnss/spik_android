package com.spikai.ui.careermap

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spikai.model.CareerLevel
import com.spikai.model.EnglishLevel
import com.spikai.model.UserProfile
import com.spikai.service.ErrorHandlingService
import com.spikai.viewmodel.CareerMapViewModel
import kotlinx.coroutines.delay

@Composable
fun CareerMapView(
    viewModel: CareerMapViewModel = CareerMapViewModel.getInstance(LocalContext.current)
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val levels by viewModel.levels.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLevelDetailShowing by viewModel.isLevelDetailShowing.collectAsStateWithLifecycle()
    
    // Local state
    var showingProfile by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showingPathSelector by remember { mutableStateOf(false) }
    var showingLanguageSelector by remember { mutableStateOf(false) }
    
    // Node-based animation states
    var isAnimatingUnlock by remember { mutableStateOf(false) }
    var currentLevelAnimating by remember { mutableStateOf(false) }
    var lineAnimating by remember { mutableStateOf(false) }
    var nextLevelAnimating by remember { mutableStateOf(false) }
    var animatingCompletedLevelId by remember { mutableStateOf<Int?>(null) }
    var animatingUnlockedLevelId by remember { mutableStateOf<Int?>(null) }
    
    // TODO: Implement ErrorHandlingService equivalent
    // val errorHandler = ErrorHandlingService.shared
    
    LaunchedEffect(Unit) {
        loadUserProfile { profile ->
            userProfile = profile
        }
        // TODO: Implement level unlock listener
        // setupLevelUnlockListener()
        viewModel.loadCareerLevels()
    }
    
    // Monitor completed levels for animation
    LaunchedEffect(progress.completedLevels) {
        // TODO: Implement animation logic for level completion
        // onChange(of: viewModel.progress.completedLevels) logic here
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingView()
            !errorMessage.isNullOrEmpty() -> ErrorView(
                message = errorMessage ?: "",
                onRetry = { viewModel.loadCareerLevels() }
            )
            else -> MainContent(
                viewModel = viewModel,
                levels = levels,
                userProfile = userProfile,
                isAnimatingUnlock = isAnimatingUnlock,
                currentLevelAnimating = currentLevelAnimating,
                lineAnimating = lineAnimating,
                nextLevelAnimating = nextLevelAnimating,
                animatingCompletedLevelId = animatingCompletedLevelId,
                animatingUnlockedLevelId = animatingUnlockedLevelId,
                onProfileClick = { showingProfile = true },
                onPathSelectorClick = { showingPathSelector = true },
                onLanguageSelectorClick = { showingLanguageSelector = true }
            )
        }
    }
    
    // Sheets/Dialogs
    if (selectedLevel != null && isLevelDetailShowing) {
        // TODO: Implement as dialog or navigation
        // LevelDetailView equivalent
    }
    
    if (showingProfile) {
        // TODO: Implement ProfileView as dialog
        // ProfileView equivalent
    }
    
    if (showingPathSelector) {
        // TODO: Implement PathSelectorView as dialog
        // PathSelectorView equivalent
    }
    
    if (showingLanguageSelector) {
        // TODO: Implement LanguageSelectorView as dialog
        // LanguageSelectorView equivalent
    }
    
    // TODO: Implement error alert
    // .errorAlert(...)
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF007AFF) // PrimaryBlue
            )
            Text(
                text = "Cargando tu progreso...",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93) // TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF3B30), // ErrorRed
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFF1C1C1E), // TextPrimary
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF) // PrimaryBlue
                )
            ) {
                Text(
                    text = "Reintentar",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    viewModel: CareerMapViewModel,
    levels: List<CareerLevel>,
    userProfile: UserProfile,
    isAnimatingUnlock: Boolean,
    currentLevelAnimating: Boolean,
    lineAnimating: Boolean,
    nextLevelAnimating: Boolean,
    animatingCompletedLevelId: Int?,
    animatingUnlockedLevelId: Int?,
    onProfileClick: () -> Unit,
    onPathSelectorClick: () -> Unit,
    onLanguageSelectorClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            HeaderSection(
                userProfile = userProfile,
                onProfileClick = onProfileClick
            )
        }
        
        item {
            UnitSection(
                userProfile = userProfile,
                onPathSelectorClick = onPathSelectorClick,
                onLanguageSelectorClick = onLanguageSelectorClick
            )
        }
        
        item {
            PathSection(
                viewModel = viewModel,
                levels = levels,
                currentLevelAnimating = currentLevelAnimating,
                nextLevelAnimating = nextLevelAnimating,
                lineAnimating = lineAnimating,
                animatingCompletedLevelId = animatingCompletedLevelId,
                animatingUnlockedLevelId = animatingUnlockedLevelId
            )
        }
    }
}

@Composable
private fun HeaderSection(
    userProfile: UserProfile,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "¡Hola, ${userProfile.name}!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E) // TextPrimary
            )
            Text(
                text = "Continúa tu progreso",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93) // TextSecondary
            )
        }
        
        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = Color(0xFF007AFF), // PrimaryBlue
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun UnitSection(
    userProfile: UserProfile,
    onPathSelectorClick: () -> Unit,
    onLanguageSelectorClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Unidad Actual",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93), // TextSecondary
                fontWeight = FontWeight.Medium
            )
            Text(
                text = pathDisplayText(userProfile.englishLevel ?: EnglishLevel.PRINCIPIANTE),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E) // TextPrimary
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onPathSelectorClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cambiar nivel",
                    tint = Color(0xFF8E8E93) // TextSecondary
                )
            }
            
            IconButton(onClick = onLanguageSelectorClick) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Cambiar idioma",
                    tint = Color(0xFF8E8E93) // TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PathSection(
    viewModel: CareerMapViewModel,
    levels: List<CareerLevel>,
    currentLevelAnimating: Boolean,
    nextLevelAnimating: Boolean,
    lineAnimating: Boolean,
    animatingCompletedLevelId: Int?,
    animatingUnlockedLevelId: Int?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(levels) { level ->
            val index = levels.indexOf(level)
            val isCurrentLevelAnimating = currentLevelAnimating && animatingCompletedLevelId == level.levelId
            val isNextLevelAnimating = nextLevelAnimating && animatingUnlockedLevelId == level.levelId
            
            Column {
                LevelNodeView(
                    level = level,
                    isCurrentLevelAnimating = isCurrentLevelAnimating,
                    isNextLevelAnimating = isNextLevelAnimating,
                    onTap = { viewModel.selectLevel(level) }
                )
                
                // Path connector to next level
                if (index < levels.size - 1) {
                    val nextLevel = levels[index + 1]
                    PathConnector(
                        nextLevel = nextLevel,
                        lineAnimating = lineAnimating,
                        animatingUnlockedLevelId = animatingUnlockedLevelId
                    )
                }
            }
        }
    }
}

@Composable
private fun PathConnector(
    nextLevel: CareerLevel,
    lineAnimating: Boolean,
    animatingUnlockedLevelId: Int?
) {
    val width = connectorWidth(nextLevel, lineAnimating, animatingUnlockedLevelId)
    val color = connectorColor(nextLevel, lineAnimating, animatingUnlockedLevelId)
    val shadowColor = connectorShadowColor(nextLevel, lineAnimating, animatingUnlockedLevelId)
    val shadowRadius = connectorShadowRadius(nextLevel, lineAnimating, animatingUnlockedLevelId)
    
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(40.dp)
            .shadow(
                elevation = shadowRadius.dp,
                shape = RoundedCornerShape(width.dp / 2),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(
                color = color,
                shape = RoundedCornerShape(width.dp / 2)
            )
    )
}

// Helper functions for connector styling
private fun connectorWidth(
    nextLevel: CareerLevel,
    lineAnimating: Boolean,
    animatingUnlockedLevelId: Int?
): Float {
    return if (lineAnimating && animatingUnlockedLevelId == nextLevel.levelId) {
        8f // Animated width
    } else {
        3f // Normal width
    }
}

private fun connectorColor(
    nextLevel: CareerLevel,
    lineAnimating: Boolean,
    animatingUnlockedLevelId: Int?
): Color {
    return if (lineAnimating && animatingUnlockedLevelId == nextLevel.levelId) {
        Color(0xFFFF9500) // Animated orange
    } else if (nextLevel.isCompleted) {
        Color(0xFF34C759) // SuccessGreen
    } else if (nextLevel.isUnlocked) {
        Color(0xFFFF9500) // WarningOrange
    } else {
        Color(0xFFE5E5EA) // BorderLight
    }
}

private fun connectorShadowColor(
    nextLevel: CareerLevel,
    lineAnimating: Boolean,
    animatingUnlockedLevelId: Int?
): Color {
    return if (lineAnimating && animatingUnlockedLevelId == nextLevel.levelId) {
        Color(0xFFFF9500).copy(alpha = 0.4f)
    } else if (nextLevel.isCompleted || nextLevel.isUnlocked) {
        Color(0xFF000000).copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
}

private fun connectorShadowRadius(
    nextLevel: CareerLevel,
    lineAnimating: Boolean,
    animatingUnlockedLevelId: Int?
): Float {
    return if (lineAnimating && animatingUnlockedLevelId == nextLevel.levelId) {
        6f
    } else if (nextLevel.isCompleted || nextLevel.isUnlocked) {
        2f
    } else {
        0f
    }
}

private fun pathDisplayText(path: EnglishLevel): String {
    return when (path) {
        EnglishLevel.PRINCIPIANTE -> "A1 - Principiante"
        EnglishLevel.BASICO -> "A2 - Básico"
        EnglishLevel.INTERMEDIO -> "B1 - Intermedio"
        EnglishLevel.AVANZADO -> "B2 - Avanzado"
    }
}

private fun pathColor(path: EnglishLevel): Color {
    return when (path) {
        EnglishLevel.PRINCIPIANTE -> Color(0xFF34C759) // SuccessGreen
        EnglishLevel.BASICO -> Color(0xFF007AFF) // PrimaryBlue
        EnglishLevel.INTERMEDIO -> Color(0xFFFF9500) // WarningOrange
        EnglishLevel.AVANZADO -> Color(0xFF6B46C1) // PrimaryPurple
    }
}

private fun pathIcon(path: EnglishLevel): ImageVector {
    return when (path) {
        EnglishLevel.PRINCIPIANTE -> Icons.Default.PlayArrow
        EnglishLevel.BASICO -> Icons.Default.Book
        EnglishLevel.INTERMEDIO -> Icons.Default.Star
        EnglishLevel.AVANZADO -> Icons.Default.Grade
    }
}

// TODO: Implement user profile loading
private suspend fun loadUserProfile(onLoaded: (UserProfile) -> Unit) {
    // Load from SharedPreferences equivalent
    delay(100) // Simulate loading
    onLoaded(UserProfile()) // Default profile for now
}

@Preview(showBackground = true)
@Composable
private fun CareerMapViewPreview() {
    // TODO: Create preview with mock data
    // CareerMapView()
}
