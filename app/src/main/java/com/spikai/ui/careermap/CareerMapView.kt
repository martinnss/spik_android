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
import androidx.compose.ui.draw.alpha
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
import com.spikai.ui.auth.LoginView
import com.spikai.ui.conversation.ConversationView
import com.spikai.viewmodel.CareerMapViewModel
import com.spikai.viewmodel.ConversationViewModel
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
    
    // Authentication state
    var isUserSignedIn by remember { mutableStateOf(false) }
    var isCheckingAuth by remember { mutableStateOf(true) }
    
    // Local state
    var showingProfile by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showingPathSelector by remember { mutableStateOf(false) }
    var showingLanguageSelector by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }
    var showingConversation by remember { mutableStateOf(false) }
    var selectedLevelForConversation by remember { mutableStateOf<Pair<Int, String>?>(null) }
    
    // Node-based animation states
    var isAnimatingUnlock by remember { mutableStateOf(false) }
    var currentLevelAnimating by remember { mutableStateOf(false) }
    var lineAnimating by remember { mutableStateOf(false) }
    var nextLevelAnimating by remember { mutableStateOf(false) }
    var animatingCompletedLevelId by remember { mutableStateOf<Int?>(null) }
    var animatingUnlockedLevelId by remember { mutableStateOf<Int?>(null) }
    
    // TODO: Implement ErrorHandlingService equivalent
    // val errorHandler = ErrorHandlingService.shared
    
    // Check authentication status on launch
    LaunchedEffect(Unit) {
        checkAuthenticationStatus { isSignedIn ->
            isUserSignedIn = isSignedIn
            isCheckingAuth = false
        }
    }
    
    LaunchedEffect(Unit, retryTrigger) {
        if (isUserSignedIn) {
            loadUserProfile { profile ->
                userProfile = profile
            }
            // TODO: Implement level unlock listener
            // setupLevelUnlockListener()
            viewModel.loadCareerLevels()
        }
    }
    
    // Monitor completed levels for animation
    LaunchedEffect(progress.completedLevels) {
        // TODO: Implement animation logic for level completion
        // onChange(of: viewModel.progress.completedLevels) logic here
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isCheckingAuth -> AuthCheckingView()
            !isUserSignedIn -> LoginView(
                onSuccessfulLogin = {
                    isUserSignedIn = true
                    retryTrigger++
                },
                onDismiss = { /* Login is required, no dismiss */ }
            )
            isLoading -> LoadingView()
            !errorMessage.isNullOrEmpty() -> ErrorView(
                message = errorMessage ?: "",
                onRetry = { retryTrigger++ }
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
    
    // Sheets/Dialogs - Only show when user is signed in
    if (isUserSignedIn) {
        selectedLevel?.let { level ->
            if (isLevelDetailShowing) {
                LevelDetailView(
                    level = level,
                    viewModel = viewModel,
                    onDismiss = {
                        viewModel.dismissLevelDetail()
                    },
                    onStartConversation = { levelId, levelTitle ->
                        println("🎮 [CareerMapView] Starting conversation for level $levelId: $levelTitle")
                        selectedLevelForConversation = Pair(levelId, levelTitle)
                        showingConversation = true
                        viewModel.dismissLevelDetail()
                    }
                )
            }
        }
        
        if (showingProfile) {
            ProfileView(
                isSignedIn = isUserSignedIn,
                onDismiss = { showingProfile = false },
                onSignOut = {
                    showingProfile = false
                    isUserSignedIn = false
                    isCheckingAuth = false
                }
            )
        }
        
        if (showingPathSelector) {
            PathSelectorView(
                viewModel = viewModel,
                onDismiss = { showingPathSelector = false }
            )
        }
        
        if (showingLanguageSelector) {
            LanguageSelectorView(
                onDismiss = { showingLanguageSelector = false }
            )
        }
        
        // Conversation View
        if (showingConversation && selectedLevelForConversation != null) {
            ConversationView(
                viewModel = ConversationViewModel(
                    context = LocalContext.current,
                    levelId = selectedLevelForConversation!!.first
                ),
                onBack = {
                    println("🔙 [CareerMapView] Closing conversation, returning to career map")
                    showingConversation = false
                    selectedLevelForConversation = null
                }
            )
        }
    }
    
    // TODO: Implement error alert
    // .errorAlert(...)
}

// MARK: - Dialog Views

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PathSelectorView(
    viewModel: CareerMapViewModel,
    onDismiss: () -> Unit
) {
    val currentSelectedPath by viewModel.currentSelectedPath.collectAsStateWithLifecycle()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = {
            Text(
                text = "Rutas de Aprendizaje",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E) // TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Puedes explorar niveles de rutas anteriores que ya has desbloqueado",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93), // TextSecondary
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Available paths
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // For now, show all paths as available (TODO: implement actual availability logic)
                    EnglishLevel.values().forEach { path ->
                        PathRow(
                            path = path,
                            isAvailable = true, // TODO: implement viewModel.availablePaths logic
                            isSelected = currentSelectedPath == path,
                            onClick = {
                                viewModel.switchToPath(path)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .padding(16.dp)
            .widthIn(min = 400.dp, max = 600.dp)
            .fillMaxWidth(0.95f)
    )
}

@Composable
private fun PathRow(
    path: EnglishLevel,
    isAvailable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = if (isAvailable) onClick else { {} },
        enabled = isAvailable,
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) Color(0xFFF2F2F7) else Color(0xFFF2F2F7).copy(alpha = 0.5f) // BackgroundSecondary
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) pathColor(path) else Color(0xFFD1D1D6) // BorderLight
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Path icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isAvailable) pathColor(path) else Color(0xFF8E8E93),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = pathIcon(path),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Path info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = path.pathDisplayText(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailable) Color(0xFF1C1C1E) else Color(0xFF8E8E93) // TextPrimary/TextSecondary
                    )
                    
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759), // SuccessGreen
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (!isAvailable) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93), // TextSecondary
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Text(
                    text = path.description,
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93) // TextSecondary
                )
            }
            
            if (isAvailable) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93), // TextSecondary
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectorView(
    onDismiss: () -> Unit
) {
    val availableLanguages = listOf(
        Triple("🇺🇸", "English", true)
    )
    
    val comingSoonLanguages = listOf(
        Triple("🇪🇸", "Español", false),
        Triple("🇫🇷", "Français", false),
        Triple("🇩🇪", "Deutsch", false),
        Triple("🇮🇹", "Italiano", false),
        Triple("🇵🇹", "Português", false),
        Triple("🇯🇵", "日本語", false)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = {
            Text(
                text = "Idiomas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E) // TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecciona el Idioma",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1C1E), // TextPrimary
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Available languages section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Disponible",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E) // TextPrimary
                    )
                    
                    availableLanguages.forEach { (flag, name, isAvailable) ->
                        LanguageRow(
                            flag = flag,
                            name = name,
                            isAvailable = isAvailable,
                            isSelected = true // English is selected
                        )
                    }
                }
                
                // Coming soon section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Próximamente",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E8E93) // TextSecondary
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFFF9500).copy(alpha = 0.1f), // WarningOrange background
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COMING SOON",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9500) // WarningOrange
                            )
                        }
                    }
                    
                    comingSoonLanguages.forEach { (flag, name, isAvailable) ->
                        LanguageRow(
                            flag = flag,
                            name = name,
                            isAvailable = isAvailable,
                            isSelected = false
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .padding(16.dp)
            .widthIn(min = 400.dp, max = 600.dp)
            .fillMaxWidth(0.95f)
    )
}

@Composable
private fun LanguageRow(
    flag: String,
    name: String,
    isAvailable: Boolean,
    isSelected: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) Color(0xFFF2F2F7) else Color(0xFFF2F2F7).copy(alpha = 0.5f) // BackgroundSecondary
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF34C759) else Color(0xFFD1D1D6) // SuccessGreen/BorderLight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isAvailable) 1f else 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flag
            Text(
                text = flag,
                fontSize = 20.sp,
                modifier = Modifier.alpha(if (isAvailable) 1f else 0.5f)
            )
            
            // Language name and status
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isAvailable) Color(0xFF1C1C1E) else Color(0xFF8E8E93) // TextPrimary/TextSecondary
                )
                
                if (!isAvailable) {
                    Text(
                        text = "Próximamente",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93) // TextSecondary
                    )
                }
            }
            
            // Status indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34C759), // SuccessGreen
                    modifier = Modifier.size(20.dp)
                )
            } else if (!isAvailable) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93), // TextSecondary
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
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
                onProfileClick = onProfileClick,
                onLanguageSelectorClick = onLanguageSelectorClick,
                onPathSelectorClick = onPathSelectorClick
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
    onProfileClick: () -> Unit,
    onLanguageSelectorClick: () -> Unit,
    onPathSelectorClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 50.dp, bottom = 8.dp), // Added top padding for safe area
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language Selector Button (US Flag)
        Button(
            onClick = onLanguageSelectorClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                text = "🇺🇸",
                fontSize = 20.sp
            )
        }
        
        // User Level Badge (Orange button)
        Button(
            onClick = onPathSelectorClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9500) // WarningOrange
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = (userProfile.englishLevel ?: EnglishLevel.PRINCIPIANTE).pathDisplayText(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Profile Button (Orange)
        Button(
            onClick = onProfileClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9500) // WarningOrange
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        levels.forEach { level ->
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
    
    // Center the connector line
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
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

private fun EnglishLevel.pathDisplayText(): String {
    return when (this) {
        EnglishLevel.PRINCIPIANTE -> "A1 - Principiante"
        EnglishLevel.BASICO -> "A2 - Básico"
        EnglishLevel.INTERMEDIO -> "B1 - Intermedio"
        EnglishLevel.AVANZADO -> "B2/C1 - Avanzado"
    }
}

private val EnglishLevel.description: String
    get() = when (this) {
        EnglishLevel.PRINCIPIANTE -> "Conceptos básicos y vocabulario fundamental"
        EnglishLevel.BASICO -> "Conversaciones simples y gramática elemental"
        EnglishLevel.INTERMEDIO -> "Comunicación efectiva en situaciones cotidianas"
        EnglishLevel.AVANZADO -> "Fluidez en conversaciones complejas y profesionales"
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

// TODO: Implement authentication status check
private suspend fun checkAuthenticationStatus(onResult: (Boolean) -> Unit) {
    delay(100) // Small delay to ensure Firebase is initialized
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val isSignedIn = auth.currentUser != null
    onResult(isSignedIn)
}

@Composable
private fun AuthCheckingView() {
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
                text = "Verificando autenticación...",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93) // TextSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CareerMapViewPreview() {
    // TODO: Create preview with mock data
    // CareerMapView()
}
