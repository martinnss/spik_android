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
    
    LaunchedEffect(level.isCompleted, isCurrentLevelAnimating) {
        if (level.isCompleted && !isCurrentLevelAnimating) {
            breathingScale = animatedBreathingScale
        }
    }
    
    val scaleEffect = if (level.isCompleted && !isCurrentLevelAnimating) breathingScale else 1.0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleEffect)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = level.isUnlocked || isNextLevelAnimating) { onTap() }
                .scale(cardScaleEffect(isCurrentLevelAnimating, isNextLevelAnimating))
                .shadow(
                    elevation = cardShadowRadius(isCurrentLevelAnimating, isNextLevelAnimating).dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = cardShadowColor(isCurrentLevelAnimating, isNextLevelAnimating),
                    spotColor = cardShadowColor(isCurrentLevelAnimating, isNextLevelAnimating)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBackgroundColor(level, isCurrentLevelAnimating, isNextLevelAnimating)
            ),
            border = BorderStroke(
                width = 2.dp,
                color = cardBorderColor(level, isCurrentLevelAnimating, isNextLevelAnimating)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level circle icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(animationScaleEffect(isCurrentLevelAnimating, isNextLevelAnimating))
                        .offset(
                            x = animationOffsetX(isCurrentLevelAnimating, isNextLevelAnimating).dp,
                            y = animationOffsetY(isCurrentLevelAnimating, isNextLevelAnimating).dp
                        )
                        .shadow(
                            elevation = animationShadowRadius(isCurrentLevelAnimating, isNextLevelAnimating).dp,
                            shape = CircleShape,
                            ambientColor = animationShadowColor(isCurrentLevelAnimating, isNextLevelAnimating),
                            spotColor = animationShadowColor(isCurrentLevelAnimating, isNextLevelAnimating)
                        )
                        .clip(CircleShape)
                        .background(levelBackgroundColor(level, isCurrentLevelAnimating, isNextLevelAnimating))
                        .border(
                            width = 2.dp,
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
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = getIconForLevel(level),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Level information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = level.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E), // TextPrimary
                        maxLines = 2
                    )
                }
                
                // Status indicator
                when {
                    level.isCompleted -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completado",
                            tint = Color(0xFF34C759), // SuccessGreen
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    level.isUnlocked -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Disponible",
                            tint = Color(0xFFFF9500), // WarningOrange
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = Color(0xFF8E8E93), // TextSecondary
                            modifier = Modifier.size(20.dp)
                        )
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
        isCurrentLevelAnimating -> Color(0xFF34C759) // Animated completion green
        isNextLevelAnimating -> Color(0xFFFF9500) // Animated unlock orange
        level.isCompleted -> Color(0xFF34C759) // SuccessGreen
        level.isUnlocked -> Color(0xFFFF9500) // WarningOrange
        else -> Color(0xFF8E8E93) // TextSecondary (locked)
    }
}

private fun levelBorderColor(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        isCurrentLevelAnimating -> Color(0xFF34C759).copy(alpha = 0.8f)
        isNextLevelAnimating -> Color(0xFFFF9500).copy(alpha = 0.8f)
        level.isCompleted -> Color(0xFF34C759).copy(alpha = 0.8f)
        level.isUnlocked -> Color(0xFFFF9500).copy(alpha = 0.8f)
        else -> Color(0xFF8E8E93).copy(alpha = 0.5f)
    }
}

private fun cardBackgroundColor(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        isCurrentLevelAnimating -> Color(0xFFF0FFF4) // Light green tint
        isNextLevelAnimating -> Color(0xFFFFF4E6) // Light orange tint
        level.isCompleted -> Color(0xFFF0FFF4) // Light green
        level.isUnlocked -> Color(0xFFFFF4E6) // Light orange
        else -> Color(0xFFF2F2F7) // BackgroundSecondary
    }
}

private fun cardBorderColor(
    level: CareerLevel,
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        isCurrentLevelAnimating -> Color(0xFF34C759) // Animated green border
        isNextLevelAnimating -> Color(0xFFFF9500) // Animated orange border
        level.isCompleted -> Color(0xFF34C759).copy(alpha = 0.3f)
        level.isUnlocked -> Color(0xFFFF9500).copy(alpha = 0.3f)
        else -> Color(0xFFE5E5EA) // BorderLight
    }
}

// MARK: - Animation Helper Functions

private fun animationScaleEffect(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isCurrentLevelAnimating -> 1.2f // Scale up for completion
        isNextLevelAnimating -> 1.1f // Scale up for unlock
        else -> 1.0f
    }
}

private fun animationOffsetX(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isCurrentLevelAnimating -> 2f // Slight offset for completion
        isNextLevelAnimating -> 1f // Slight offset for unlock
        else -> 0f
    }
}

private fun animationOffsetY(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isCurrentLevelAnimating -> -2f // Slight upward offset
        isNextLevelAnimating -> -1f // Slight upward offset
        else -> 0f
    }
}

private fun animationShadowColor(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        isCurrentLevelAnimating -> Color(0xFF34C759).copy(alpha = 0.4f)
        isNextLevelAnimating -> Color(0xFFFF9500).copy(alpha = 0.4f)
        else -> Color.Transparent
    }
}

private fun animationShadowRadius(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isCurrentLevelAnimating -> 12f
        isNextLevelAnimating -> 8f
        else -> 0f
    }
}

private fun cardScaleEffect(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isCurrentLevelAnimating -> 1.05f
        isNextLevelAnimating -> 1.02f
        else -> 1.0f
    }
}

private fun cardShadowColor(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Color {
    return when {
        isCurrentLevelAnimating -> Color(0xFF34C759).copy(alpha = 0.3f)
        isNextLevelAnimating -> Color(0xFFFF9500).copy(alpha = 0.3f)
        else -> Color(0xFF000000).copy(alpha = 0.1f)
    }
}

private fun cardShadowRadius(
    isCurrentLevelAnimating: Boolean,
    isNextLevelAnimating: Boolean
): Float {
    return when {
        isCurrentLevelAnimating -> 16f
        isNextLevelAnimating -> 12f
        else -> 4f
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
