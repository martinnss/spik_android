package com.spikai.ui.careermap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.model.CareerLevel
import com.spikai.viewmodel.CareerMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelDetailView(
    level: CareerLevel,
    viewModel: CareerMapViewModel,
    onDismiss: () -> Unit,
    onStartConversation: (Int, String) -> Unit
) {
    var showingConversation by remember { mutableStateOf(false) }
    
    // TODO: Implement NavigationCoordinator equivalent
    // val navigationCoordinator = NavigationCoordinator.shared
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { 
                Text(
                    text = level.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cerrar",
                        color = Color(0xFF007AFF) // PrimaryBlue
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFF2F2F7) // BackgroundSecondary
            )
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 100.dp), // Space for fixed button
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header del nivel
                    LevelHeader(level = level)
                    
                    // Descripción detallada
                    DescriptionSection(level = level)
                    
                    // Habilidades a desarrollar
                    SkillsSection(level = level)
                }
            }
            
            // Fixed button at bottom
            ActionButton(
                level = level,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                onStartLevel = {
                    startLevel(level, onStartConversation)
                }
            )
        }
    }
    
    // TODO: Implement fullScreenCover equivalent for conversation
    if (showingConversation) {
        // ConversationView equivalent
        // onStartConversation(level.levelId, level.title)
    }
    
    // TODO: Implement NavigationCoordinator onChange equivalent
    // LaunchedEffect(navigationCoordinator.shouldDismissToCareerMap) { ... }
}

@Composable
private fun LevelHeader(level: CareerLevel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // secondarySystemBackground equivalent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ícono del nivel
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (level.isCompleted) Color(0xFF34C759) // StatusConnected
                        else Color(0xFF007AFF) // PrimaryBlue
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (level.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    // Alternative: Use Text with emoji if icons fail
                    Text(
                        text = getEmojiForLevel(level),
                        fontSize = 32.sp,
                        color = Color.White
                    )
                    /* Original icon approach:
                    Icon(
                        imageVector = getIconForLevel(level),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    */
                }
            }
            
            // Título y estado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = level.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1C1C1E) // TextPrimary
                )
                
                // Status row placeholder
                Row {
                    Spacer(modifier = Modifier)
                }
            }
        }
    }
}

@Composable
private fun DescriptionSection(level: CareerLevel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // secondarySystemBackground equivalent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Descripción",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E) // primary
            )
            
            Text(
                text = level.description,
                fontSize = 16.sp,
                color = Color(0xFF8E8E93), // secondary
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SkillsSection(level: CareerLevel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // secondarySystemBackground equivalent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Habilidades a Desarrollar",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E) // primary
            )
            
            // Skills grid using regular Column and Row layout
            val skillChunks = level.skills.chunked(2) // Group skills in pairs
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skillChunks.forEach { rowSkills ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowSkills.forEach { skill ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF007AFF).copy(alpha = 0.1f) // PrimaryBlue with opacity
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = skill,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF007AFF), // PrimaryBlue
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // Add spacer if odd number of skills in last row
                        if (rowSkills.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    level: CareerLevel,
    modifier: Modifier = Modifier,
    onStartLevel: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (level.isUnlocked) {
            Button(
                onClick = onStartLevel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (level.isCompleted) Color(0xFFFF9500) // StatusConnecting
                    else Color(0xFF007AFF) // PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (level.isCompleted) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = if (level.isCompleted) "Repetir Nivel" else "Comenzar Nivel",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            
            if (!level.isCompleted) {
                Text(
                    text = "Practica conversaciones y ejercicios interactivos",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93), // secondary
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color(0xFF8E8E93) // TextSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "Nivel Bloqueado",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// MARK: - Private Methods
private fun startLevel(
    level: CareerLevel,
    onStartConversation: (Int, String) -> Unit
) {
    println("🎮 [LevelDetail] Starting level ${level.levelId}")
    println("▶️ [LevelDetail] Starting conversation directly")
    onStartConversation(level.levelId, level.title)
}

// Helper function to get icon for level (same as LevelNodeView)
private fun getIconForLevel(level: CareerLevel): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        level.title.contains("Presentación", ignoreCase = true) -> Icons.Default.Person
        level.title.contains("Conversación", ignoreCase = true) -> Icons.Default.Chat
        level.title.contains("Trabajo", ignoreCase = true) -> Icons.Default.Work
        level.title.contains("Viaje", ignoreCase = true) -> Icons.Default.Flight
        level.title.contains("Restaurante", ignoreCase = true) -> Icons.Default.Restaurant
        else -> Icons.Default.School // Default fallback
    }
}

// Alternative: Emoji-based icons (fallback if Material Icons fail)
private fun getEmojiForLevel(level: CareerLevel): String {
    return when {
        level.title.contains("Presentación", ignoreCase = true) -> "👤"
        level.title.contains("Conversación", ignoreCase = true) -> "💬"
        level.title.contains("Trabajo", ignoreCase = true) -> "💼"
        level.title.contains("Viaje", ignoreCase = true) -> "✈️"
        level.title.contains("Restaurante", ignoreCase = true) -> "🍽️"
        else -> "📚" // Default fallback
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelDetailViewPreview() {
    // TODO: Create preview with sample data
    // LevelDetailView(
    //     level = CareerLevel.sampleLevels[0],
    //     viewModel = CareerMapViewModel(),
    //     onDismiss = { },
    //     onStartConversation = { _, _ -> }
    // )
}
