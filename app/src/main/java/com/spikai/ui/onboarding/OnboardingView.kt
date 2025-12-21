package com.spikai.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spikai.viewmodel.OnboardingStep
import com.spikai.viewmodel.OnboardingViewModel

@Composable
fun OnboardingView(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = OnboardingViewModel.getInstance(LocalContext.current)
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    
    // Computed properties
    val progress = viewModel.progress
    val canProceed = viewModel.canProceed
    val canProceedInAssessment = viewModel.canProceedInAssessment
    val nextButtonTitle = viewModel.nextButtonTitle
    val assessmentButtonTitle = viewModel.assessmentButtonTitle
    
    // Observe completion state
    LaunchedEffect(userProfile.hasCompletedOnboarding) {
        if (userProfile.hasCompletedOnboarding) {
            com.spikai.service.AnalyticsService.logOnboardingComplete()
            onOnboardingComplete()
        }
    }
    
    // Load user profile on appear
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
        if (currentStep == OnboardingStep.WELCOME) {
            com.spikai.service.AnalyticsService.logOnboardingStart()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF), // BackgroundPrimary
                        Color(0xFFF2F2F7).copy(alpha = 0.3f) // BackgroundSecondary with opacity
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress bar
            if (progress != 0.0) {
                ProgressBarSection(progress = progress.toFloat())
            }
            
            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                when (currentStep) {
                    OnboardingStep.WELCOME -> WelcomeStepView(viewModel = viewModel)
                    OnboardingStep.ASSESSMENT -> AssessmentStepView(viewModel = viewModel)
                    OnboardingStep.COMPLETION -> CompletionStepView(viewModel = viewModel)
                }
            }
            
            // Navigation buttons
            NavigationButtonsSection(
                currentStep = currentStep,
                currentQuestionIndex = currentQuestionIndex,
                canProceed = canProceed,
                canProceedInAssessment = canProceedInAssessment,
                isLoading = isLoading,
                nextButtonTitle = nextButtonTitle,
                assessmentButtonTitle = assessmentButtonTitle,
                onBackClick = {
                    if (currentStep == OnboardingStep.ASSESSMENT) {
                        viewModel.previousAssessmentQuestion()
                    } else {
                        viewModel.previousStep()
                    }
                },
                onNextClick = {
                    if (currentStep == OnboardingStep.ASSESSMENT) {
                        viewModel.nextAssessmentQuestion()
                    } else {
                        viewModel.nextStep()
                    }
                }
            )
        }
    }
}

@Composable
private fun ProgressBarSection(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progress"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp, bottom = 16.dp), // Increased top padding for better safe area handling
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFFFF9500), // WarningOrange
            trackColor = Color(0xFFE5E5EA),
        )
    }
}

@Composable
private fun NavigationButtonsSection(
    currentStep: OnboardingStep,
    currentQuestionIndex: Int,
    canProceed: Boolean,
    canProceedInAssessment: Boolean,
    isLoading: Boolean,
    nextButtonTitle: String,
    assessmentButtonTitle: String,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val shouldShowBackButton = when (currentStep) {
        OnboardingStep.ASSESSMENT -> currentQuestionIndex > 0
        else -> currentStep != OnboardingStep.WELCOME
    }
    
    val currentCanProceed = when (currentStep) {
        OnboardingStep.ASSESSMENT -> canProceedInAssessment
        else -> canProceed
    }
    
    val buttonTitle = when (currentStep) {
        OnboardingStep.ASSESSMENT -> assessmentButtonTitle
        else -> nextButtonTitle
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (shouldShowBackButton) {
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp), // Fixed height for consistency
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9500).copy(alpha = 0.1f), // OrangeTransparent
                    contentColor = Color(0xFFFF9500)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Atrás",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Button(
            onClick = onNextClick,
            enabled = currentCanProceed && !isLoading,
            modifier = Modifier
                .weight(if (shouldShowBackButton) 1f else 2f)
                .height(56.dp), // Fixed height for consistency
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentCanProceed) Color(0xFFFF9500) else Color(0xFFBEBEC0),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFBEBEC0),
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = buttonTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingViewPreview() {
    OnboardingView(
        onOnboardingComplete = { }
        // TODO: Add mock viewModel for preview
    )
}
