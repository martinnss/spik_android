package com.spikai.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spikai.model.AssessmentOption
import com.spikai.model.AssessmentQuestion
import com.spikai.viewmodel.OnboardingViewModel

@Composable
fun AssessmentStepView(
    viewModel: OnboardingViewModel
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    
    val currentQuestion = AssessmentQuestion.allCases[currentQuestionIndex]
    val isLastQuestion = currentQuestionIndex == AssessmentQuestion.allCases.size - 1
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // Question
        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = currentQuestion.question,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1C1E), // TextPrimary
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 28.sp
            )
            
            Text(
                text = "Selecciona la opción que mejor te describa",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93), // TextSecondary
                textAlign = TextAlign.Center
            )
            
            // Answer options
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currentQuestion.options.forEach { option ->
                    AnswerOptionCard(
                        option = option,
                        isSelected = userProfile.assessmentAnswers[currentQuestion.value] == option.level,
                        onSelected = {
                            viewModel.selectAssessmentAnswer(currentQuestion, option.level)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AnswerOptionCard(
    option: AssessmentOption,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onSelected() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // BackgroundSecondary
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF007AFF)) // PrimaryBlue
        } else {
            BorderStroke(1.dp, Color(0xFFE5E5EA)) // BorderLight
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF007AFF) else Color(0xFFE5E5EA)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForName(option.iconName),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF8E8E93),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = option.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E), // TextPrimary
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = option.description,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93), // TextSecondary
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            
            // Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (isSelected) Color(0xFF007AFF) else Color(0xFFBEBEC0),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                    )
                }
            }
        }
    }
}

// Helper function to map string icon names to Material Icons
@Composable
private fun getIconForName(iconName: String): ImageVector {
    return when (iconName) {
        "face.dashed" -> Icons.Default.Face
        "bubble.left" -> Icons.Default.ChatBubble
        "person.2" -> Icons.Default.People
        "star" -> Icons.Default.Star
        "xmark.circle" -> Icons.Default.Cancel
        "ear" -> Icons.Default.Hearing
        "ear.trianglebadge.exclamationmark" -> Icons.Default.VolumeUp
        "waveform" -> Icons.Default.GraphicEq
        "hand.wave" -> Icons.Default.Face // Using Face as WavingHand equivalent
        "questionmark.bubble" -> Icons.Default.Help
        "text.bubble" -> Icons.Default.Message
        "brain.head.profile" -> Icons.Default.Psychology
        "exclamationmark.triangle" -> Icons.Default.Warning
        "pause.circle" -> Icons.Default.Pause
        "clock" -> Icons.Default.AccessTime
        "checkmark.circle" -> Icons.Default.CheckCircle
        "airplane" -> Icons.Default.Flight
        "message" -> Icons.Default.Message
        "briefcase" -> Icons.Default.Work
        "crown" -> Icons.Default.Star // Using star as crown equivalent
        else -> Icons.Default.Circle // Default fallback
    }
}

@Preview(showBackground = true)
@Composable
private fun AssessmentStepViewPreview() {
    // TODO: Create preview with mock ViewModel
    // AssessmentStepView(viewModel = OnboardingViewModel())
}
