package com.spikai.ui.careermap

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.model.CareerLevel

@Composable
fun LevelNodeView(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean,
    isFirstLevel: Boolean,
    isHighlightMode: Boolean,
    onTap: () -> Unit
) {
    // Add breathing animation state for completed levels
    var breathingScale by remember { mutableStateOf(1.0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val animatedBreathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )
    
    LaunchedEffect(level.isCompleted) {
        if (level.isCompleted) {
            breathingScale = animatedBreathingScale
        }
    }
    
    val scaleEffect = if (level.isCompleted) {
        breathingScale * animationScaleEffect(isCurrentLevelAnimating, isNextLevelAnimating)
    } else {
        animationScaleEffect(isCurrentLevelAnimating, isNextLevelAnimating)
    }

    // Highlight mode animation
    val highlightTransition = rememberInfiniteTransition(label = "highlight")
    val highlightShadowRadius by highlightTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "highlight_shadow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleEffect)
            .offset(y = animationOffsetY(isCurrentLevelAnimating, isNextLevelAnimating).dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = level.isUnlocked || isNextLevelAnimating || (isFirstLevel && isHighlightMode)) { onTap() }
                .scale(cardScaleEffect(isCurrentLevelAnimating, isNextLevelAnimating))
                .background(
                    // Extra background layer for first level to ensure it's fully opaque
                    if (isFirstLevel && isHighlightMode) Color(0xFFF2F2F7) else Color.Transparent // BackgroundSecondary
                )
                .zIndex(if (isFirstLevel && isHighlightMode) 1000f else 0f)
                .shadow(
                    elevation = if (isFirstLevel && isHighlightMode) highlightShadowRadius.dp else animationShadowRadius(isCurrentLevelAnimating, isNextLevelAnimating).dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = if (isFirstLevel && isHighlightMode) Color(0xFFFF9500) else animationShadowColor(isCurrentLevelAnimating, isNextLevelAnimating),
                    spotColor = if (isFirstLevel && isHighlightMode) Color(0xFFFF9500) else animationShadowColor(isCurrentLevelAnimating, isNextLevelAnimating)
                )
                .then(
                    if (isFirstLevel && isHighlightMode) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color(0xFFFF9500).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else Modifier
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Level circle icon (80x80 - larger)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(levelBackgroundColor(level, isCurrentLevelAnimating, isNextLevelAnimating))
                    .border(
                        width = 3.dp,
                        color = levelBorderColor(level, isCurrentLevelAnimating, isNextLevelAnimating),
                        shape = CircleShape
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
                    Icon(
                        imageVector = getIconForLevel(level),
                        contentDescription = null,
                        tint = if (level.isUnlocked) Color.White else Color(0xFF8E8E93), // TextSecondary
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Level info card (below circle)
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackgroundColor(level, isCurrentLevelAnimating, isNextLevelAnimating)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFE5E5EA) // BorderLight
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = level.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E), // TextPrimary
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    
                    // Status indicator
                    when {
                        !level.isUnlocked -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93), // TextSecondary
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Bloqueado",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93) // TextSecondary
                                )
                            }
                        }
                        level.isCompleted -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759), // SuccessGreen
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Completado",
                                    fontSize = 14.sp,
                                    color = Color(0xFF34C759) // SuccessGreen
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "Toca para comenzar",
                                fontSize = 14.sp,
                                color = Color(0xFFFF9500) // WarningOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Color Helper Functions

private fun levelBackgroundColor(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        level.isCompleted -> Color(0xFF34C759) // SuccessGreen
        level.isUnlocked -> Color(0xFFFF9500) // WarningOrange
        else -> Color(0xFFF2F2F7) // BackgroundSecondary
    }
}

private fun levelBorderColor(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        level.isCompleted -> Color(0xFF34C759).copy(alpha = 0.8f)
        level.isUnlocked -> Color(0xFFFF9500).copy(alpha = 0.8f)
        else -> Color(0xFFE5E5EA) // BorderLight
    }
}

private fun cardBackgroundColor(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return Color.White
}

private fun cardBorderColor(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return Color(0xFFE5E5EA) // BorderLight
}

// MARK: - Animation Helper Functions

private fun animationScaleEffect(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isNextLevelAnimating -> 1.2f // Scale up for unlock (increased from 1.1f)
        else -> 1.0f
    }
}

private fun animationOffsetY(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isNextLevelAnimating -> -8f // Upward offset for unlock (increased from -1f)
        else -> 0f
    }
}

private fun animationShadowColor(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        isNextLevelAnimating -> Color(0xFFFF9500).copy(alpha = 0.4f)
        else -> Color.Transparent
    }
}

private fun animationShadowRadius(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isNextLevelAnimating -> 20f // Increased from 8f
        else -> 0f
    }
}

private fun cardScaleEffect(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isNextLevelAnimating -> 1.08f // Increased from 1.02f
        else -> 1.0f
    }
}

// Helper function to get icon for level
private fun getIconForLevel(level: CareerLevel): ImageVector {
    // TODO: Map level.iosSymbol to appropriate Material Icon
    return when {
        level.title.contains("Presentación", ignoreCase = true) -> Icons.Default.Person
        level.title.contains("Conversación", ignoreCase = true) -> Icons.Default.Chat
        level.title.contains("Trabajo", ignoreCase = true) -> Icons.Default.Business
        level.title.contains("Viaje", ignoreCase = true) -> Icons.Default.Flight
        level.title.contains("Restaurante", ignoreCase = true) -> Icons.Default.LocalDining
        else -> Icons.Default.MenuBook // Default fallback
    }
}
