package com.spikai.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spikai.viewmodel.OnboardingViewModel

@Composable
fun CompletionStepView(
    viewModel: OnboardingViewModel
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // Animation for loading state
    val infiniteTransition = rememberInfiniteTransition(label = "scale")
    val scale by infiniteTransition.animateFloat(
        initialValue = if (isLoading) 0.8f else 1.0f,
        targetValue = if (isLoading) 1.0f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // Success animation and message
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                tint = Color.Unspecified // This allows the gradient to show through
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¡Todo listo!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1C1C1E) // TextPrimary
                )
                
                Text(
                    text = "¡Bienvenido a tu viaje de aprendizaje de oratoria, ${userProfile.name}!",
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93), // TextSecondary
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
        
        // Summary of selections
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Tu Perfil",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF2F2F7) // BackgroundSecondary
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileSummaryRow(
                        icon = Icons.Default.Person,
                        title = "Nombre",
                        value = userProfile.name
                    )
                    
                    userProfile.englishLevel?.let { level ->
                        ProfileSummaryRow(
                            icon = Icons.Default.TrendingUp,
                            title = "Nivel de Oratoria",
                            value = level.rawValue
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProfileSummaryRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF007AFF) // PrimaryBlue
        )
        
        Text(
            text = title,
            fontSize = 14.sp,
            color = Color(0xFF8E8E93) // TextSecondary
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1C1C1E) // TextPrimary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompletionStepViewPreview() {
    // TODO: Create preview with mock ViewModel
    // CompletionStepView(viewModel = OnboardingViewModel())
}
