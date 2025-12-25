package com.spikai.ui.conversation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spikai.model.ConnectionStatus
import com.spikai.model.ConversationItem
import com.spikai.ui.components.LevelEvaluationPopupView
import com.spikai.viewmodel.ConversationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationView(
    viewModel: ConversationViewModel = viewModel(),
    levelId: Int? = null,
    levelTitle: String? = null,
    onBack: () -> Unit = {}
) {
    // State from ViewModel
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val messages by viewModel.conversation.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val aiSpeakingSpeed by viewModel.aiSpeakingSpeed.collectAsStateWithLifecycle()
    val showEvaluationPopup by viewModel.showEvaluationPopup.collectAsStateWithLifecycle()
    val levelEvaluation by viewModel.levelEvaluation.collectAsStateWithLifecycle()

    // Derived state for progress
    val userMessageCount = remember(messages) {
        messages.count { it.role == "user" }
    }
    val canManuallyFinishLevel = remember(userMessageCount) {
        userMessageCount >= 10
    }

    // Debug logging for popup state
    LaunchedEffect(showEvaluationPopup) {
        println("🔍 [ConversationView] showEvaluationPopup state changed to: $showEvaluationPopup")
        if (showEvaluationPopup && levelEvaluation != null) {
            println("✅ [ConversationView] Popup should be visible now with evaluation: score=${levelEvaluation?.score}, passed=${levelEvaluation?.passed}")
        }
    }

    // UI state
    val listState = rememberLazyListState()
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showCloseConfirmation by remember { mutableStateOf(false) }
    
    // Keyboard/Text Input state
    var isKeyboardMode by remember { mutableStateOf(false) }
    var textInputMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Text input helpers
    val canSendTextMessage = textInputMessage.trim().isNotEmpty() && connectionStatus == ConnectionStatus.CONNECTED

    // Request microphone permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            println("⚠️ [ConversationView] Microphone permission denied")
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        
        // Auto-start session ONLY on initial load
        if (connectionStatus == ConnectionStatus.DISCONNECTED) {
            println("🎬 [ConversationView] Auto-starting session on view load")
            viewModel.startSession()
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1C1C1E),
                        Color(0xFF2C2C2E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Navigation Bar
            TopNavigationBar(
                onBack = { showCloseConfirmation = true },
                onSettings = { showSettingsMenu = true }
            )

            // Level Title Section (if provided)
            levelTitle?.let {
                Text(
                    text = it,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Progress Section
            if (levelId != null || viewModel.isLevelSession) {
                ProgressSection(
                    userMessageCount = userMessageCount,
                    canManuallyFinishLevel = canManuallyFinishLevel,
                    onManualFinish = { viewModel.triggerManualEvaluation() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Conversation Area
            if (messages.isEmpty()) {
                // Empty state - preparing scenario
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "🎬",
                        fontSize = 64.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Preparando el escenario...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages.reversed()) { item: ConversationItem ->
                        ConversationBubble(item = item)
                    }
                }
            }

            // Bottom Control Bar (Call or Keyboard)
            AnimatedContent(
                targetState = isKeyboardMode,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()).togetherWith(
                        slideOutVertically { it } + fadeOut()
                    )
                },
                label = "BottomBarAnimation"
            ) { keyboardMode ->
                if (keyboardMode) {
                    KeyboardInputBar(
                        textInputMessage = textInputMessage,
                        onTextChange = { textInputMessage = it },
                        canSend = canSendTextMessage,
                        focusRequester = focusRequester,
                        onSend = {
                            if (canSendTextMessage) {
                                val messageToSend = textInputMessage.trim()
                                textInputMessage = ""
                                viewModel.sendTextMessage(messageToSend)
                            }
                        },
                        onSwitchToVoice = {
                            viewModel.setMuted(false)
                            isKeyboardMode = false
                        }
                    )
                } else {
                    CallControlBar(
                        connectionStatus = connectionStatus,
                        isMuted = isMuted,
                        onHangUp = {
                            if (connectionStatus == ConnectionStatus.CONNECTED ||
                                connectionStatus == ConnectionStatus.CONNECTING) {
                                viewModel.endSession()
                            }
                            onBack()
                        },
                        onToggleMute = { viewModel.toggleMute() },
                        onSwitchToKeyboard = {
                            viewModel.setMuted(true)
                            isKeyboardMode = true
                        }
                    )
                }
            }
        }

        // Settings Menu Overlay
        if (showSettingsMenu) {
            ConversationSettingsMenuView(
                isPresented = showSettingsMenu,
                onDismiss = { showSettingsMenu = false },
                aiSpeakingSpeed = aiSpeakingSpeed,
                onSpeedChange = { viewModel.updateAiSpeakingSpeed(it) }
            )
        }

        // Close Confirmation Dialog
        if (showCloseConfirmation) {
            CloseConfirmationDialog(
                onDismiss = { showCloseConfirmation = false },
                onConfirm = {
                    showCloseConfirmation = false
                    viewModel.endSession()
                    onBack()
                }
            )
        }
        
        // Level Evaluation Popup
        if (showEvaluationPopup && levelEvaluation != null) {
            LevelEvaluationPopupView(
                evaluation = levelEvaluation!!,
                onContinue = {
                    viewModel.onEvaluationContinue()
                    onBack()
                },
                onRetry = {
                    viewModel.onEvaluationRetry()
                },
                onClose = {
                    viewModel.onEvaluationClose()
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun TopNavigationBar(
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
                .padding(top = 48.dp) // Status bar padding
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back/Close Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CallControlBar(
    connectionStatus: ConnectionStatus,
    isMuted: Boolean,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onSwitchToKeyboard: () -> Unit = {}
) {
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 34.dp), // Navigation bar padding
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Control buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Microphone button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isConnected -> Color(0xFFF2F2F7) // BackgroundSecondary
                                    isMuted -> Color(0xFFFF3B30) // ErrorRed
                                    else -> Color.White.copy(alpha = 0.2f)
                                }
                            )
                            .clickable(enabled = isConnected) { onToggleMute() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                !isConnected -> Icons.Default.MicOff
                                isMuted -> Icons.Default.MicOff
                                else -> Icons.Default.Mic
                            },
                            contentDescription = "Microphone",
                            tint = when {
                                !isConnected -> Color(0xFF8E8E93) // TextSecondary
                                isMuted -> Color.White
                                else -> Color.White
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Text(
                        text = when {
                            !isConnected -> "mute"
                            isMuted -> "unmute"
                            else -> "mute"
                        },
                        fontSize = 12.sp,
                        color = when {
                            !isConnected -> Color(0xFF8E8E93)
                            else -> Color.White
                        }
                    )
                }
                
                // Keyboard button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) Color.White.copy(alpha = 0.2f)
                                else Color(0xFFF2F2F7)
                            )
                            .clickable(enabled = isConnected) { onSwitchToKeyboard() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Keyboard",
                            tint = if (isConnected) Color.White else Color(0xFF8E8E93),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Text(
                        text = "teclado",
                        fontSize = 12.sp,
                        color = if (isConnected) Color.White else Color(0xFF8E8E93)
                    )
                }
            }

            // Hang up button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected || connectionStatus == ConnectionStatus.CONNECTING) {
                            Color(0xFFFF3B30) // ErrorRed
                        } else {
                            Color(0xFF34C759) // SuccessGreen
                        }
                    )
                    .clickable { onHangUp() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnected || connectionStatus == ConnectionStatus.CONNECTING) {
                        Icons.Default.CallEnd
                    } else {
                        Icons.Default.Call
                    },
                    contentDescription = "Hang Up",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun CloseConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C2E)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "¿Salir de la conversación?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Si sales ahora, perderás el progreso de esta conversación.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Sí, salir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressSection(
    userMessageCount: Int,
    canManuallyFinishLevel: Boolean,
    onManualFinish: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = canManuallyFinishLevel,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + scaleIn()).togetherWith(
                    fadeOut(animationSpec = tween(300)) + scaleOut()
                )
            },
            label = "ProgressSectionAnimation"
        ) { canFinish ->
            if (canFinish) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onManualFinish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759) // SuccessGreen
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp
                    ),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0xFF34C759).copy(alpha = 0.3f)
                        )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terminar y evaluar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 40.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$userMessageCount/10",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93), // TextSecondary
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                    
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF3A3A3C)) // BackgroundTertiary
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (userMessageCount.coerceAtMost(10) / 10f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF0A84FF), // PrimaryBlue
                                            Color(0xFFBF5AF2)  // PrimaryPurple
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardInputBar(
    textInputMessage: String,
    onTextChange: (String) -> Unit,
    canSend: Boolean,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
    onSwitchToVoice: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 34.dp),
        color = Color(0xFF1C1C1E).copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Text Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF2C2C2E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = textInputMessage,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { onSend() }
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (textInputMessage.isEmpty()) {
                                    Text(
                                        text = "Escribe tu mensaje...",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    // Clear button
                    if (textInputMessage.isNotEmpty()) {
                        IconButton(
                            onClick = { onTextChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // Send Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSend()
                },
                enabled = canSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) Color(0xFF0A84FF) else Color(0xFF3A3A3C)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Switch to Voice Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwitchToVoice()
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E))
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Switch to voice",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
