package com.spikai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spikai.viewmodel.StreakViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun StreakCounterView(
    refreshTrigger: Flow<Unit>? = null,
    viewModel: StreakViewModel = StreakViewModel(
        context = LocalContext.current
    )
) {
    // State collection from ViewModel
    val streakData by viewModel.streakData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isStreakActive by viewModel.isStreakActive.collectAsStateWithLifecycle()
    
    // Handle refresh trigger
    LaunchedEffect(refreshTrigger) {
        refreshTrigger?.collect {
            println("🔄 [StreakCounterView] Refresh trigger received - refreshing streak")
            viewModel.refreshAfterLevelCompletion()
        }
    }
    
    // Load streak on appear
    LaunchedEffect(Unit) {
        viewModel.loadStreak()
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasError) {
            ErrorView(
                errorMessage = errorMessage,
                onRetry = { viewModel.loadStreak() }
            )
        } else {
            StreakContentView(
                streakData = streakData,
                isLoading = isLoading,
                isStreakActive = isStreakActive,
                streakText = viewModel.streakText
            )
            
            // Show status message when streak is inactive
            if (!isStreakActive) {
                StatusMessageView(
                    message = viewModel.streakStatusMessage,
                    isStreakActive = isStreakActive
                )
            }
        }
    }
}

@Composable
private fun StatusMessageView(
    message: String,
    isStreakActive: Boolean
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (!isStreakActive) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = EaseInOut),
        label = "status_message_alpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (!isStreakActive) 1f else 0.95f,
        animationSpec = tween(durationMillis = 300, easing = EaseInOut),
        label = "status_message_scale"
    )
    
    Text(
        text = message,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF8E8E93).copy(alpha = 0.8f), // TextSecondary
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .scale(animatedScale)
        // Note: Using scale instead of transition/opacity as Compose doesn't have direct equivalent
    )
}

@Composable
private fun StreakContentView(
    streakData: com.spikai.model.StreakData,
    isLoading: Boolean,
    isStreakActive: Boolean,
    streakText: String
) {
    val animatedOpacity by animateFloatAsState(
        targetValue = if (isLoading) 0.7f else (if (isStreakActive) 1.0f else 0.6f),
        animationSpec = tween(durationMillis = 300, easing = EaseInOut),
        label = "content_opacity"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isStreakActive) 1.0f else 1.02f,
        animationSpec = tween(durationMillis = 500, easing = EaseInOut),
        label = "content_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .shadow(
                elevation = if (isStreakActive) 8.dp else 10.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = if (isStreakActive) 
                    Color(0xFF000000).copy(alpha = 0.08f) 
                else 
                    Color(0xFFFF3B30).copy(alpha = 0.15f) // ErrorRed
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isStreakActive) listOf(
                            Color(0xFFF2F2F7).copy(alpha = 0.6f), // BackgroundSecondary
                            Color(0xFFFFFFFF).copy(alpha = 0.4f)  // BackgroundTertiary
                        ) else listOf(
                            Color(0xFFF2F2F7).copy(alpha = 0.3f), // BackgroundSecondary
                            Color(0xFFFFFFFF).copy(alpha = 0.2f)  // BackgroundTertiary
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isStreakActive) 1.dp else 1.5.dp,
                    color = if (isStreakActive) 
                        Color(0xFF34C759).copy(alpha = 0.6f) // SuccessGreen
                    else 
                        Color(0xFFFF3B30).copy(alpha = 0.8f), // ErrorRed
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Compact flame and streak counter
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isStreakActive) "🔥" else "🌫️",
                        fontSize = 20.sp
                    )
                    
                    Text(
                        text = streakText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Transparent, // Will be overridden by brush
                        modifier = Modifier.background(
                            brush = if (isStreakActive) Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF007AFF), // PrimaryBlue
                                    Color(0xFF6B46C1)  // PrimaryPurple
                                )
                            ) else Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8E8E93).copy(alpha = 0.6f), // TextSecondary
                                    Color(0xFF8E8E93).copy(alpha = 0.4f)  // TextSecondary
                                )
                            )
                        )
                    )
                    
                    Text(
                        text = "días",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isStreakActive) 
                            Color(0xFF8E8E93) // TextSecondary
                        else 
                            Color(0xFF8E8E93).copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Right side - Compact days of the week
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    streakData.weeklyProgress.forEach { dayProgress ->
                        DayProgressItem(
                            dayProgress = dayProgress,
                            isStreakActive = isStreakActive
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayProgressItem(
    dayProgress: com.spikai.model.DayProgress,
    isStreakActive: Boolean
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (dayProgress.isCompleted && isStreakActive) 1.0f else 0.9f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "day_scale"
    )
    
    Box(
        modifier = Modifier
            .size(20.dp)
            .scale(animatedScale)
            .background(
                color = if (dayProgress.isCompleted && isStreakActive)
                    Color(0xFF34C759) // SuccessGreen
                else
                    Color(0xFFE5E5EA).copy(alpha = if (isStreakActive) 0.4f else 0.2f), // BorderLight
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (dayProgress.isCompleted && isStreakActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(8.dp)
            )
        } else {
            Text(
                text = dayProgress.dayName,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isStreakActive)
                    Color(0xFF8E8E93) // TextSecondary
                else
                    Color(0xFF8E8E93).copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun ErrorView(
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF3B30).copy(alpha = 0.1f) // ErrorRed
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFFFF3B30) // ErrorRed
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF3B30), // ErrorRed
                modifier = Modifier.size(16.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Error al cargar racha",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1C1E) // TextPrimary
                )
                
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93), // TextSecondary
                        maxLines = 2
                    )
                }
            }
            
            TextButton(
                onClick = onRetry
            ) {
                Text(
                    text = "Reintentar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF007AFF) // PrimaryBlue
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreakCounterViewPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // BackgroundPrimary
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TODO: Create mock StreakViewModel for preview
        // StreakCounterView()
        
        Spacer(modifier = Modifier.weight(1f))
    }
}
