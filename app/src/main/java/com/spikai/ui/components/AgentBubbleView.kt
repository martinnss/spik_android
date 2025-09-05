package com.spikai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AgentBubbleView(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation states
    var pulseScale by remember { mutableStateOf(1.0f) }
    var floatOffset by remember { mutableStateOf(0.0f) }
    var rotationAngle by remember { mutableStateOf(0.0f) }
    var eyeHeightScale by remember { mutableStateOf(1.0f) }
    
    val bubbleSize = 120.dp
    
    // Infinite animations
    val infiniteTransition = rememberInfiniteTransition(label = "agent_bubble")
    
    // Float animation
    val animatedFloatOffset by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) -30f else -8f,
        targetValue = if (isSpeaking) -30f else -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 400 else 3000,
                easing = if (isSpeaking) FastOutSlowInEasing else EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    
    // Pulse animation (only when speaking)
    val animatedPulseScale by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 1.0f else 1.0f,
        targetValue = if (isSpeaking) 1.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 300 else 1000,
                easing = if (isSpeaking) FastOutSlowInEasing else LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Rotation animation
    val animatedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 20000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Outer glow scale animation
    val outerGlowScale by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 1.0f else 1.0f,
        targetValue = if (isSpeaking) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_glow"
    )
    
    // Eye blinking state
    var isBlinking by remember { mutableStateOf(false) }
    
    // Eye blinking effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(1000, 1900)) // Random delay between blinks
            isBlinking = true
            delay(200) // Blink duration
            isBlinking = false
            delay(300) // Stay open briefly
        }
    }
    
    Box(
        modifier = modifier.offset(y = animatedFloatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft outer glow/shadow
        Box(
            modifier = Modifier
                .size(bubbleSize + 80.dp)
                .scale(outerGlowScale)
                .blur(12.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF007AFF).copy(alpha = if (isSpeaking) 0.4f else 0.2f), // PrimaryBlue
                            Color(0xFF007AFF).copy(alpha = if (isSpeaking) 0.2f else 0.1f),
                            Color(0xFF007AFF).copy(alpha = if (isSpeaking) 0.1f else 0.05f),
                            Color.Transparent
                        ),
                        radius = (bubbleSize + 80.dp).value / 2
                    ),
                    shape = CircleShape
                )
        )
        
        // Main 3D bubble with depth
        Box(
            modifier = Modifier
                .size(bubbleSize)
                .scale(animatedPulseScale)
                .rotate(animatedRotation)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFF007AFF).copy(alpha = 0.15f), // PrimaryBlue
                            Color(0xFF007AFF).copy(alpha = 0.25f),
                            Color(0xFF007AFF).copy(alpha = 0.4f)
                        ),
                        center = androidx.compose.ui.geometry.Offset(0.3f, 0.25f),
                        radius = bubbleSize.value * 0.8f
                    ),
                    shape = CircleShape
                )
        ) {
            // Bottom-right shadow overlay for 3D depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF007AFF).copy(alpha = 0.15f),
                                Color(0xFF007AFF).copy(alpha = 0.3f)
                            ),
                            center = androidx.compose.ui.geometry.Offset(0.7f, 0.75f),
                            radius = bubbleSize.value * 0.6f
                        ),
                        shape = CircleShape
                    )
            )
            
            // Top-left bright highlight
            Box(
                modifier = Modifier
                    .size(bubbleSize * 0.7f)
                    .offset(
                        x = -bubbleSize * 0.15f,
                        y = -bubbleSize * 0.15f
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            radius = (bubbleSize * 0.35f).value
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        // Two eyes
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .offset(y = (-10).dp)
                .scale(if (isSpeaking) 1.05f else 0.95f)
        ) {
            // Left eye
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(35.dp)
                    .scale(
                        scaleX = 1.0f,
                        scaleY = if (isBlinking) 0.1f else eyeHeightScale
                    )
                    .scale(if (isSpeaking) 1.1f else 1.0f)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            
            // Right eye
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(35.dp)
                    .scale(
                        scaleX = 1.0f,
                        scaleY = if (isBlinking) 0.1f else eyeHeightScale
                    )
                    .scale(if (isSpeaking) 1.1f else 1.0f)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AgentBubbleViewPreview() {
    var isSpeaking by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF), // BackgroundPrimary
                        Color(0xFFF2F2F7)  // BackgroundSecondary
                    )
                )
            )
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        AgentBubbleView(isSpeaking = isSpeaking)
        
        Button(
            onClick = { isSpeaking = !isSpeaking },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF) // PrimaryBlue
            ),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = if (isSpeaking) "Stop Speaking" else "Start Speaking",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White, // TextOnPrimary
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}
