package com.spikai.ui.conversation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spikai.viewmodel.ConversationViewModel
import com.spikai.ui.components.StreakCounterView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationView(
    viewModel: ConversationViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val currentQuestion by viewModel.currentQuestion.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isAISpeaking by viewModel.isAISpeaking.collectAsStateWithLifecycle()
    val speechToTextResult by viewModel.speechToTextResult.collectAsStateWithLifecycle()
    val showLevelEvaluationPopup by viewModel.showLevelEvaluationPopup.collectAsStateWithLifecycle()
    val showConversationSettingsMenu by viewModel.showConversationSettingsMenu.collectAsStateWithLifecycle()
    val showCloseConversationAlert by viewModel.showCloseConversationAlert.collectAsStateWithLifecycle()
    val conversationProgress by viewModel.conversationProgress.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures { 
                    // Dismiss any open menus/popups when tapping background
                    if (showConversationSettingsMenu) {
                        viewModel.hideConversationSettingsMenu()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            ConversationTopBar(
                conversationProgress = conversationProgress,
                streak = streak,
                onBack = { viewModel.showCloseConversationAlert() },
                onSettings = { viewModel.showConversationSettingsMenu() }
            )
            
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ConversationBubble(
                        message = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    )
                }
                
                // Current question display
                if (currentQuestion.isNotEmpty()) {
                    item {
                        ConversationBubble(
                            message = ConversationMessage(
                                id = "current",
                                content = currentQuestion,
                                role = ConversationRole.AI,
                                timestamp = System.currentTimeMillis()
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement()
                        )
                    }
                }
                
                // Speech-to-text result display
                if (speechToTextResult.isNotEmpty()) {
                    item {
                        ConversationBubble(
                            message = ConversationMessage(
                                id = "speech",
                                content = speechToTextResult,
                                role = ConversationRole.User,
                                timestamp = System.currentTimeMillis()
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(0.7f)
                                .animateItemPlacement()
                        )
                    }
                }
            }
            
            // Bottom Controls
            ConversationBottomControls(
                isListening = isListening,
                isAISpeaking = isAISpeaking,
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() },
                onStopSpeaking = { viewModel.stopAISpeaking() }
            )
        }
        
        // AI Agent Bubble Overlay
        AgentBubbleView(
            isVisible = isAISpeaking,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        
        // Level Evaluation Popup Overlay
        AnimatedVisibility(
            visible = showLevelEvaluationPopup,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            LevelEvaluationPopupView(
                onDismiss = { viewModel.hideLevelEvaluationPopup() },
                onContinue = { 
                    viewModel.hideLevelEvaluationPopup()
                    // TODO: Navigate to next level or continue conversation
                }
            )
        }
        
        // Conversation Settings Menu Overlay
        AnimatedVisibility(
            visible = showConversationSettingsMenu,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ConversationSettingsMenuView(
                onDismiss = { viewModel.hideConversationSettingsMenu() }
            )
        }
        
        // Close Conversation Alert Dialog
        if (showCloseConversationAlert) {
            CloseConversationAlertDialog(
                onDismiss = { viewModel.hideCloseConversationAlert() },
                onConfirm = { 
                    viewModel.hideCloseConversationAlert()
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun ConversationTopBar(
    conversationProgress: Float,
    streak: Int,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Progress Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                LinearProgressIndicator(
                    progress = conversationProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF4facfe),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                
                Text(
                    text = "${(conversationProgress * 100).toInt()}% Complete",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Settings and Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakCounterView(
                    streak = streak,
                    modifier = Modifier.size(32.dp)
                )
                
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationBottomControls(
    isListening: Boolean,
    isAISpeaking: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isAISpeaking -> {
                    // Stop Speaking Button
                    FloatingActionButton(
                        onClick = onStopSpeaking,
                        modifier = Modifier.size(72.dp),
                        containerColor = Color(0xFFff4757),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Speaking",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                isListening -> {
                    // Stop Listening Button with pulsing animation
                    val infiniteTransition = rememberInfiniteTransition(label = "listening")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    
                    FloatingActionButton(
                        onClick = onStopListening,
                        modifier = Modifier
                            .size((72 * pulseScale).dp),
                        containerColor = Color(0xFFff4757),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Stop Listening",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                else -> {
                    // Start Listening Button
                    FloatingActionButton(
                        onClick = onStartListening,
                        modifier = Modifier.size(72.dp),
                        containerColor = Color(0xFF4facfe),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start Listening",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloseConversationAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1e1e2e)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFffa502),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "End Conversation?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your progress will be saved, but you'll need to restart this conversation.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFff4757)
                        )
                    ) {
                        Text("End", color = Color.White)
                    }
                }
            }
        }
    }
}

// Data classes for conversation messages (these should match your existing models)
data class ConversationMessage(
    val id: String,
    val content: String,
    val role: ConversationRole,
    val timestamp: Long
)

enum class ConversationRole {
    User, AI
}
