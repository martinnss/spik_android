package com.spikai.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConversationSettingsMenuView(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    aiSpeakingSpeed: Double,
    onSpeedChange: (Double) -> Unit
) {
    if (!isPresented) return
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Settings menu content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 34.dp) // Safe area padding
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                // Improved glass effect background with better contrast
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            // Base solid background for better text contrast
                            color = Color(0xFFFFFFFF).copy(alpha = 0.95f), // BackgroundPrimary
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(
                            // Gradient border overlay
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configuración",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E) // TextPrimary
                        )
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF8E8E93), // TextSecondary
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        color = Color(0xFFE5E5EA) // BorderLight
                    )
                    
                    // Information notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF007AFF).copy(alpha = 0.05f)) // PrimaryBlue with opacity
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF007AFF), // PrimaryBlue
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Text(
                            text = "La configuración se guarda automáticamente y se aplica a futuras llamadas",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93), // TextSecondary
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    HorizontalDivider(
                        color = Color(0xFFE5E5EA) // BorderLight
                    )
                    
                    // AI Speaking Speed Setting
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Velocidad de Habla de la IA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1C1E) // TextPrimary
                            )
                            
                            Text(
                                text = "Ajusta qué tan rápido habla la IA (se guarda automáticamente para futuras llamadas)",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93) // TextSecondary
                            )
                        }
                        
                        // Speed slider
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Lenta",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93) // TextSecondary
                                )
                                
                                Text(
                                    text = "Normal",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93) // TextSecondary
                                )
                                
                                Text(
                                    text = "Rápida",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93) // TextSecondary
                                )
                            }
                            
                            Slider(
                                value = aiSpeakingSpeed.toFloat(),
                                onValueChange = { onSpeedChange(it.toDouble()) },
                                valueRange = 0.5f..2.0f,
                                steps = 14, // (2.0 - 0.5) / 0.1 - 1 = 14 steps for 0.1 increments
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF007AFF), // PrimaryBlue
                                    activeTrackColor = Color(0xFF007AFF), // PrimaryBlue
                                    inactiveTrackColor = Color(0xFFE5E5EA) // BorderLight
                                )
                            )
                            
                            // Current speed value
                            Text(
                                text = "Velocidad: ${String.format("%.1f", aiSpeakingSpeed)}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1C1E), // TextPrimary
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Preview(showBackground = true)
@Composable
private fun ConversationSettingsMenuViewPreview() {
    ConversationSettingsMenuView(
        isPresented = true,
        onDismiss = { },
        aiSpeakingSpeed = 1.0,
        onSpeedChange = { }
    )
}
