package com.spikai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DataCorruptionErrorView(
    errorMessage: String = "Los datos están corruptos",
    onRecoverData: () -> Unit = {},
    onClearData: () -> Unit = {},
    onDismiss: () -> Unit = {},
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF3B30).copy(alpha = 0.1f) // ErrorRed background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF3B30), // ErrorRed
                modifier = Modifier.size(48.dp)
            )
            
            // Error Title
            Text(
                text = "Datos Corruptos Detectados",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E), // TextPrimary
                textAlign = TextAlign.Center
            )
            
            // Error Message
            Text(
                text = errorMessage,
                fontSize = 14.sp,
                color = Color(0xFF8E8E93), // TextSecondary
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            // Recovery Suggestion
            Text(
                text = "Puedes intentar recuperar los datos o limpiar toda la información y empezar de nuevo.",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93), // TextSecondary
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF007AFF) // PrimaryBlue
                )
            } else {
                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recover Data Button (Primary)
                    Button(
                        onClick = onRecoverData,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF) // PrimaryBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Recuperar Datos",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Clear Data Button (Secondary)
                    OutlinedButton(
                        onClick = onClearData,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF3B30) // ErrorRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            Color(0xFFFF3B30) // ErrorRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Limpiar Todo y Empezar de Nuevo",
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Dismiss Button (Tertiary)
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ignorar por ahora",
                            color = Color(0xFF8E8E93), // TextSecondary
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataCorruptionErrorViewPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        DataCorruptionErrorView(
            errorMessage = "No pudimos cargar tu progreso debido a datos corruptos.",
            onRecoverData = { /* Preview action */ },
            onClearData = { /* Preview action */ },
            onDismiss = { /* Preview action */ }
        )
    }
}
