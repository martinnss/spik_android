package com.spikai.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.R

@Composable
fun LoginView(
    onSuccessfulLogin: (() -> Unit)? = null,
    onDismiss: () -> Unit = {}
) {
    LocalContext.current
    val scrollState = rememberScrollState()
    

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1C1C1E).copy(alpha = 0.8f), // TextPrimary with opacity
                        Color(0xFFFF9500).copy(alpha = 0.6f)  // Orange primary color with opacity
                    )
                )
            )
    ) {
        val maxHeight = maxHeight
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Section
            HeaderSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight * 0.4f)
            )
            
            // Content Section  
            ContentSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight * 0.6f),
                onSuccessfulLogin = onSuccessfulLogin,
                onDismiss = onDismiss
            )
        }
    }
    
    // TODO: Implement error alert handling
    // .errorAlert(
    //     isPresented = errorHandler.isShowingError,
    //     error = errorHandler.currentError,
    //     onRetry = { ... }
    // )
}

@Composable
private fun HeaderSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Spacer(modifier = Modifier.height(1.dp))
        
        // App Logo/Icon
        Box {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF000000).copy(alpha = 0.1f),
                        spotColor = Color(0xFF000000).copy(alpha = 0.1f)
                    )
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.spik_logo),
                    contentDescription = "Spik Logo",
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        
        // Welcome Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "¡Felicidades! 🚀",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White, // TextInverse
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Completaste el cuestionario. Ahora inicia sesión para continuar",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f), // TextInverse with opacity
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
        
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@Composable
private fun ContentSection(
    modifier: Modifier = Modifier,
    onSuccessfulLogin: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFFFF) // BackgroundPrimary
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 20.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Benefits Section
                BenefitsSection()
                
                // Sign In Section
                SignInSection(
                    onSuccessfulLogin = onSuccessfulLogin,
                    onDismiss = onDismiss
                )
                
                // Terms Section
                TermsSection()
            }
        }
    }
}

@Composable
private fun BenefitsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Accede a tu experiencia personalizada",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E), // TextPrimary
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BenefitRow(
                icon = Icons.Default.Shield,
                title = "Guarda tu progreso",
                description = "Nunca pierdas tu avance en los niveles",
                color = Color(0xFF007AFF) // PrimaryBlue
            )
            
            BenefitRow(
                icon = Icons.Default.Notifications,
                title = "Recibe notificaciones", 
                description = "Mantente al día con nuevos contenidos",
                color = Color(0xFF34C759) // SuccessGreen
            )
            
            BenefitRow(
                icon = Icons.Default.EmojiEvents,
                title = "Desbloquea logros",
                description = "Obtén recompensas por tu dedicación",
                color = Color(0xFFFF9500) // WarningOrange
            )
        }
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // BackgroundSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E) // TextPrimary
                )
                
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93), // TextSecondary
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun SignInSection(
    onSuccessfulLogin: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Inicia sesión para comenzar tu aprendizaje",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E), // TextPrimary
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Use the proper GoogleSignInButton component
            com.spikai.ui.components.GoogleSignInButton(
                action = {
                    handleSuccessfulSignIn(onSuccessfulLogin, onDismiss)
                }
            )
            
            // TODO: Apple Sign In not implemented as per instructions
            // Note: Apple Sign In would require platform-specific implementation
        }
    }
}

@Composable
private fun TermsSection() {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Al continuar, aceptas nuestros",
            fontSize = 12.sp,
            color = Color(0xFF8E8E93), // TextSecondary
            textAlign = TextAlign.Center
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.spik.cl/terms"))
                    context.startActivity(intent)
                }
            ) {
                Text(
                    text = "Términos de Servicio",
                    fontSize = 12.sp,
                    color = Color(0xFF007AFF) // PrimaryBlue
                )
            }
            
            Text(
                text = "y",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93) // TextSecondary
            )
            
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.spik.cl/privacy"))
                    context.startActivity(intent)
                }
            ) {
                Text(
                    text = "Política de Privacidad",
                    fontSize = 12.sp,
                    color = Color(0xFF007AFF) // PrimaryBlue
                )
            }
        }
    }
}

// MARK: - Actions
private fun handleSuccessfulSignIn(
    onSuccessfulLogin: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    // Navigate back to continue the learning journey
    println("Usuario autenticado exitosamente - continuando progreso")
    onSuccessfulLogin?.invoke()
    onDismiss()
}

@Preview(showBackground = true)
@Composable
private fun LoginViewPreview() {
    LoginView(
        onSuccessfulLogin = { },
        onDismiss = { }
    )
}
