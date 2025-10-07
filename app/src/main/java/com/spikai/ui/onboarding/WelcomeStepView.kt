package com.spikai.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.ui.theme.SpikAIColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spikai.viewmodel.OnboardingViewModel
import com.spikai.R

@Composable
fun WelcomeStepView(
    viewModel: OnboardingViewModel
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // App icon and welcome message
        Column(
            modifier = Modifier.padding(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SpikAI Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.spik_logo),
                    contentDescription = "SpikAI Logo",
                    modifier = Modifier.size(130.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bienvenido a Spik",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1C1C1E) // TextPrimary equivalent
                )
                
                Text(
                    text = "Aprende inglés a través de conversaciones realistas",
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93), // TextSecondary equivalent
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
        
        // Name input
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "¿Cómo te llamas?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            
            OutlinedTextField(
                value = userProfile.name,
                onValueChange = { viewModel.updateUserName(it) },
                placeholder = { 
                    Text(
                        text = "Ingresa tu nombre",
                        color = Color(0xFF8E8E93)
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9500),
                    unfocusedBorderColor = Color(0xFFE5E5EA),
                    focusedContainerColor = Color(0xFFF2F2F7),
                    unfocusedContainerColor = Color(0xFFF2F2F7),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeStepViewPreview() {
    // TODO: Create preview with mock ViewModel
    // WelcomeStepView(viewModel = OnboardingViewModel())
}
