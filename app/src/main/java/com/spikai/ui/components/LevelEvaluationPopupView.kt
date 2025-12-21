package com.spikai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.model.LevelProgressionEvaluation
import kotlinx.coroutines.delay

@Composable
fun LevelEvaluationPopupView(
    evaluation: LevelProgressionEvaluation,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    // Animation states
    var animateScore by remember { mutableStateOf(false) }
    var animateAppearance by remember { mutableStateOf(false) }
    var animateButtons by remember { mutableStateOf(false) }
    var isNavigatingToCareerMap by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Analytics
        com.spikai.service.AnalyticsService.logConversationAnalysisViewed(evaluation.score)

        // Stagger the animations for a nice effect
        animateAppearance = true
        delay(300)
        animateScore = true
        delay(800)
        animateButtons = true
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (animateAppearance) 0.5f else 0f))
                .clickable(enabled = false) { /* Prevent dismissing by tapping background */ }
        )
        
        // Main popup
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(if (animateAppearance) 1.0f else 0.7f)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF) // BackgroundPrimary
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header section
                    HeaderSection(
                        evaluation = evaluation,
                        animateScore = animateScore
                    )
                    
                    // Content section
                    ContentSection(
                        evaluation = evaluation
                    )
                    
                    // Action buttons
                    ActionButtons(
                        evaluation = evaluation,
                        animateButtons = animateButtons,
                        isNavigatingToCareerMap = isNavigatingToCareerMap,
                        onContinue = {
                            // TODO: Implement haptic feedback
                            // val impactFeedback = UIImpactFeedbackGenerator(style = .heavy)
                            // impactFeedback.impactOccurred()
                            
                            // TODO: Implement success notification haptic
                            // val notificationFeedback = UINotificationFeedbackGenerator()
                            // notificationFeedback.notificationOccurred(.success)
                            
                            isNavigatingToCareerMap = true
                            // Delay the actual navigation to allow animation
                            // In a real app, this would be handled by the caller
                            onContinue()
                        },
                        onRetry = onRetry,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    evaluation: LevelProgressionEvaluation,
    animateScore: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score circle
        ScoreCircle(
            score = evaluation.score,
            animateScore = animateScore
        )
        
        // Result title
        Text(
            text = if (evaluation.passed) "¡Felicitaciones!" else "Continúa practicando",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E), // TextPrimary
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(if (animateScore) 1.0f else 0.8f)
        )
        
        // Status subtitle
        Text(
            text = if (evaluation.passed) "Has aprobado este nivel" else "Necesitas más práctica",
            fontSize = 16.sp,
            color = Color(0xFF8E8E93), // TextSecondary
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScoreCircle(
    score: Int,
    animateScore: Boolean
) {
    val scoreColor = when (score) {
        5 -> Color(0xFF34C759) // SuccessGreen
        4 -> Color(0xFF30D158) // AccentMint
        3 -> Color(0xFFFF9500) // WarningOrange
        else -> Color(0xFFFF3B30) // ErrorRed
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (animateScore) score / 5.0f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = EaseInOut),
        label = "score_progress"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (animateScore) 1.0f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "score_scale"
    )
    
    val numberScale by animateFloatAsState(
        targetValue = if (animateScore) 1.0f else 0.5f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 500,
            easing = EaseInOut
        ),
        label = "number_scale"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(animatedScale),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 8.dp.toPx()
            
            // Background stroke
            drawCircle(
                color = scoreColor.copy(alpha = 0.2f),
                style = Stroke(width = strokeWidth)
            )
            
            // Progress stroke
            drawArc(
                color = scoreColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Score text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = score.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor,
                modifier = Modifier.scale(numberScale)
            )
            
            Text(
                text = "/ 5",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8E8E93) // TextSecondary
            )
        }
    }
}

@Composable
private fun ContentSection(
    evaluation: LevelProgressionEvaluation
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Main reasoning
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF007AFF), // PrimaryBlue
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Evaluación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E) // TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Text(
                text = evaluation.reasoning,
                fontSize = 16.sp,
                color = Color(0xFF8E8E93), // TextSecondary
                lineHeight = 20.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ActionButtons(
    evaluation: LevelProgressionEvaluation,
    animateButtons: Boolean,
    isNavigatingToCareerMap: Boolean,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE5E5EA) // BorderLight
        )
        
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (evaluation.passed) {
                // Continue to next level button
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .scale(if (animateButtons) (if (isNavigatingToCareerMap) 1.05f else 1.0f) else 0.9f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNavigatingToCareerMap) 
                            Color(0xFF34C759).copy(alpha = 0.8f) 
                        else 
                            Color(0xFF34C759) // SuccessGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isNavigatingToCareerMap
                ) {
                    if (isNavigatingToCareerMap) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continuar al siguiente nivel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                // Retry level button
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .scale(if (animateButtons) 1.0f else 0.9f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF) // PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Practicar de nuevo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LevelEvaluationPopupViewPreview() {
    LevelEvaluationPopupView(
        evaluation = LevelProgressionEvaluation.preview(),
        onContinue = {},
        onRetry = {},
        onClose = {}
    )
}
