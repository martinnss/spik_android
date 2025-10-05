package com.spikai.ui.conversation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.spikai.viewmodel.ConversationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationView(
    viewModel: ConversationViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    // Use only state exposed by ConversationViewModel
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val messages by viewModel.conversation.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val listState = rememberLazyListState()

    // Request microphone permission for WebRTC audio recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            println("⚠️ [ConversationView] Microphone permission denied")
        }
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Auto-start session when view appears (if not already connected)
    LaunchedEffect(connectionStatus) {
        if (connectionStatus == ConnectionStatus.DISCONNECTED) {
            println("🎬 [ConversationView] Auto-starting session")
            viewModel.startSession()
        }
    }

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
                        Color(0xFF1C1C1E).copy(alpha = 0.9f), // Dark background
                        Color(0xFFFF9500).copy(alpha = 0.8f), // Orange primary color
                        Color(0xFFFF9500).copy(alpha = 0.6f)  // Orange primary color lighter
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures {
                    // No-op (kept for parity with original tap-to-dismiss behavior)
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            ConversationTopBar(
                onBack = onBack
            )

            // Optional error banner
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB00020))
                        .padding(8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            }

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
                // Show "Start speaking..." hint when connected but no messages yet
                if (messages.isEmpty() && connectionStatus == ConnectionStatus.CONNECTED) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🎤 Listening...",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start speaking to begin the conversation",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                items(messages) { item: ConversationItem ->
                    ConversationBubble(
                        item = item
                    )
                }
            }

            // Bottom Controls (start/end, mute)
            ConversationBottomControls(
                connectionStatus = connectionStatus,
                isMuted = isMuted,
                onStart = { viewModel.startSession() },
                onEnd = { viewModel.endSession() },
                onToggleMute = { viewModel.toggleMute() }
            )
        }
    }
}

@Composable
private fun ConversationTopBar(
    onBack: () -> Unit
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

            Text(
                text = "Conversation",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.size(40.dp)) // balance layout
        }
    }
}

@Composable
private fun ConversationBottomControls(
    connectionStatus: ConnectionStatus,
    isMuted: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (connectionStatus) {
                ConnectionStatus.CONNECTED -> {
                    OutlinedButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isMuted) "Unmute" else "Mute")
                    }
                    Button(onClick = onEnd) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("End")
                    }
                }
                ConnectionStatus.CONNECTING -> {
                    CircularProgressIndicator(color = Color.White)
                }
                ConnectionStatus.DISCONNECTED, ConnectionStatus.FAILED -> {
                    Button(onClick = onStart) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start")
                    }
                }
            }
        }
    }
}
