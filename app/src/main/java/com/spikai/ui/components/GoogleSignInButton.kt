package com.spikai.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.service.GoogleSignInManager

@Composable
fun GoogleSignInButton(
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val googleSignInManager = remember { GoogleSignInManager.getInstance(context) }
    
    val isLoading by googleSignInManager.isLoading.collectAsState()
    val errorMessage by googleSignInManager.errorMessage.collectAsState()
    
    // Show error if any
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // TODO: Show proper error dialog or snackbar
            println("Sign-in error: $it")
        }
    }
    
    Button(
        onClick = {
            println("🔘 GoogleSignInButton clicked!")
            println("🔍 Context type: ${context::class.simpleName}")
            println("🔍 Is loading: $isLoading")
            
            if (context is Activity) {
                println("✅ Context is Activity - proceeding with sign-in")
                googleSignInManager.signInWithGoogle(context) { success ->
                    println("📝 Sign-in callback received: success = $success")
                    if (success) {
                        println("🎉 Calling action() callback")
                        action()
                    } else {
                        println("❌ Sign-in failed, not calling action()")
                    }
                }
            } else {
                println("❌ Context is not an Activity - cannot perform Google Sign-In")
                println("   Context class: ${context::class.java.name}")
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color(0xFF000000).copy(alpha = 0.1f), // ShadowColor
                spotColor = Color(0xFF000000).copy(alpha = 0.1f)
            ),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White, // TextInverse
            contentColor = Color(0xFF1C1C1E), // TextPrimary
            disabledContainerColor = Color.White.copy(alpha = 0.8f),
            disabledContentColor = Color(0xFF1C1C1E).copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE5E5EA) // BorderLight
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF4285F4) // GoogleBlue
                )
            } else {
                // Google "G" logo placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = Color.White, // TextInverse
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4) // GoogleBlue
                    )
                }
            }
            
            Text(
                text = if (isLoading) "Iniciando sesión..." else "Continuar con Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1C1E) // TextPrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoogleSignInButtonPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // BackgroundSecondary
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GoogleSignInButton(
            action = {
                println("Google Sign-In tapped")
            }
        )
    }
}
