package com.spikai.ui.conversation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.model.ConversationItem

@Composable
fun ConversationBubble(
    item: ConversationItem
) {
    val isUser = item.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isUser) {
            Spacer(modifier = Modifier.widthIn(min = 60.dp))
        }
        
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Message bubble
            Text(
                text = if (item.text.isEmpty()) "..." else item.text,
                fontSize = 16.sp,
                color = if (isUser) Color.White else Color(0xFF1C1C1E), // TextInverse : TextPrimary
                modifier = Modifier
                    .background(
                        brush = if (isUser) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF007AFF), // PrimaryBlue
                                    Color(0xFF6B46C1)  // PrimaryPurple
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF2F2F7), // BackgroundSecondary
                                    Color(0xFFFFFFFF)  // BackgroundTertiary (using white as tertiary)
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                            )
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            // Role label
            Text(
                text = if (isUser) "You" else "AI Assistant",
                fontSize = 10.sp,
                color = Color(0xFF8E8E93), // TextSecondary
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        if (!isUser) {
            Spacer(modifier = Modifier.widthIn(min = 60.dp))
        }
    }
}

@Preview
@Composable
private fun ConversationBubblePreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConversationBubble(
            item = ConversationItem(
                id = "1",
                role = "user",
                text = "Hello! How are you?"
            )
        )
        
        ConversationBubble(
            item = ConversationItem(
                id = "2",
                role = "assistant",
                text = "I'm doing great, thank you! How can I help you today?"
            )
        )
    }
}
