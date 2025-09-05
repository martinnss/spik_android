package com.spikai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.service.SpikError

@Composable
fun ErrorAlertView(
    error: SpikError,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Add logging equivalent
    // println("🚨 [ErrorAlertView] Showing critical error: $error - ${error.localizedDescription}")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0xFF000000).copy(alpha = 0.3f), // ShadowColor
                spotColor = Color(0xFF000000).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF) // BackgroundPrimary
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFF3B30) // ErrorRed
            )
            
            // Error Title
            Text(
                text = "¡Ups! Algo salió mal",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E), // TextPrimary
                textAlign = TextAlign.Center
            )
            
            // Error Description
            Text(
                text = error.errorDescription,
                fontSize = 16.sp,
                color = Color(0xFF8E8E93), // TextSecondary
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            // Recovery Suggestion
            if (error.recoverySuggestion.isNotEmpty()) {
                Text(
                    text = error.recoverySuggestion,
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93), // TextSecondary
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    lineHeight = 16.sp
                )
            }
            
            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Retry Button (if applicable)
                if (error.isRetryable && onRetry != null) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF) // PrimaryBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Reintentar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White, // TextOnPrimary
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
                
                // Dismiss Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF2F2F7), // BackgroundSecondary
                        contentColor = Color(0xFF007AFF) // PrimaryBlue
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFE5E5EA) // BorderDefault
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (error.isRetryable) "Cancelar" else "Aceptar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorAlertViewPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // BackgroundSecondary
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ErrorAlertView(
            error = SpikError.NO_INTERNET_CONNECTION,
            onRetry = {
                println("Retry tapped")
            },
            onDismiss = {
                println("Dismiss tapped")
            }
        )
    }
}
