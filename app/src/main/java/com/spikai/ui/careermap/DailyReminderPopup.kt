package com.spikai.ui.careermap

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spikai.viewmodel.DailyReminderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReminderPopup(
    onDismiss: () -> Unit,
    viewModel: DailyReminderViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedHour by viewModel.selectedHour.collectAsState()
    val selectedMinute by viewModel.selectedMinute.collectAsState()
    val showPermissionAlert by viewModel.showPermissionAlert.collectAsState()
    
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )
    
    // Refresh settings when popup opens to ensure we show saved time
    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }
    
    // Sync state from ViewModel to TimePicker (in case refresh changed it)
    LaunchedEffect(selectedHour, selectedMinute) {
        if (timePickerState.hour != selectedHour) timePickerState.hour = selectedHour
        if (timePickerState.minute != selectedMinute) timePickerState.minute = selectedMinute
    }
    
    // Sync state from TimePicker to ViewModel
    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        viewModel.updateTime(timePickerState.hour, timePickerState.minute)
    }
    
    if (showPermissionAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionAlert() },
            title = { Text("Notificaciones Desactivadas") },
            text = { Text("Por favor activa las notificaciones en Configuración para recibir recordatorios.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissPermissionAlert()
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionAlert() }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    Dialog(
        onDismissRequest = { 
            viewModel.skipReminder()
            onDismiss() 
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { 
                    viewModel.skipReminder()
                    onDismiss() 
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) {}, // Prevent click through
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Icon with Gradient Background
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF9500).copy(alpha = 0.15f),
                                            Color(0xFFFF5E3A).copy(alpha = 0.15f)
                                        )
                                    )
                                )
                        )
                        
                        Text(
                            text = "⏰",
                            fontSize = 40.sp
                        )
                    }
                    
                    // Title & Description
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "¡Hazlo un hábito!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E),
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Elige una hora y Spik te lo recordará cada día",
                            fontSize = 15.sp,
                            color = Color(0xFF8E8E93),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                    
                    // Time Picker with cleaner design
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F8F8)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors(
                                    clockDialColor = Color(0xFFF8F8F8),
                                    clockDialSelectedContentColor = Color.White,
                                    clockDialUnselectedContentColor = Color(0xFF1C1C1E),
                                    selectorColor = Color(0xFFFF9500),
                                    containerColor = Color(0xFFF8F8F8),
                                    timeSelectorSelectedContainerColor = Color(0xFFFF9500),
                                    timeSelectorSelectedContentColor = Color.White,
                                    timeSelectorUnselectedContainerColor = Color(0xFFE8E8E8),
                                    timeSelectorUnselectedContentColor = Color(0xFF1C1C1E),
                                    periodSelectorSelectedContainerColor = Color(0xFFFF9500),
                                    periodSelectorSelectedContentColor = Color.White,
                                    periodSelectorUnselectedContainerColor = Color(0xFFE8E8E8),
                                    periodSelectorUnselectedContentColor = Color(0xFF8E8E93)
                                )
                            )
                        }
                    }
                    
                    // Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.scheduleReminder(context)) {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFFF9500),
                                                Color(0xFFFF5E3A)
                                            )
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Guardar Recordatorio",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = {
                                viewModel.skipReminder()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Quizás después",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
        }
    }
}
