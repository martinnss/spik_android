package com.spikai.ui.conversation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AgentBubbleView(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation for speaking state
    val infiniteTransition = rememberInfiniteTransition(label = "bubble")
    val scale by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 0.9f else 1.0f,
        targetValue = if (isSpeaking) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .size(120.dp)
            .scale(if (isSpeaking) scale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // Agent bubble - placeholder for now
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color(0xFF007AFF), // PrimaryBlue
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.headlineLarge
            )
        }
        
        // Outer ring when speaking
        if (isSpeaking) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = Color(0xFF007AFF).copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AgentBubbleViewPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AgentBubbleView(isSpeaking = false)
        AgentBubbleView(isSpeaking = true)
    }
}
