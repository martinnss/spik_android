package com.spikai.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spikai.receiver.ReminderReceiver
import com.spikai.service.UserPreferencesService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyReminderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userPreferencesService = UserPreferencesService.getInstance(application)
    
    private val _selectedHour = MutableStateFlow(9)
    val selectedHour: StateFlow<Int> = _selectedHour.asStateFlow()
    
    private val _selectedMinute = MutableStateFlow(0)
    val selectedMinute: StateFlow<Int> = _selectedMinute.asStateFlow()
    
    private val _isReminderSet = MutableStateFlow(false)
    val isReminderSet: StateFlow<Boolean> = _isReminderSet.asStateFlow()
    
    private val _showPermissionAlert = MutableStateFlow(false)
    val showPermissionAlert: StateFlow<Boolean> = _showPermissionAlert.asStateFlow()

    init {
        checkCurrentSettings()
    }
    
    fun refreshSettings() {
        checkCurrentSettings()
    }
    
    private fun checkCurrentSettings() {
        val (hour, minute) = userPreferencesService.getReminderTimeComponents()
        _selectedHour.value = hour
        _selectedMinute.value = minute
        _isReminderSet.value = userPreferencesService.isReminderEnabled
    }
    
    fun updateTime(hour: Int, minute: Int) {
        _selectedHour.value = hour
        _selectedMinute.value = minute
    }
    
    fun scheduleReminder(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                _showPermissionAlert.value = true
                return false
            }
        }
        
        scheduleNotification(context)
        return true
    }
    
    private fun scheduleNotification(context: Context) {
        val hour = _selectedHour.value
        val minute = _selectedMinute.value
        
        // Create Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Reminder"
            val descriptionText = "Reminders to practice Spik"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("daily_reminder_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        // Schedule Alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        
        // If time is in the past, add a day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback or request permission (not handling request here for simplicity)
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            viewModelScope.launch {
                _isReminderSet.value = true
                userPreferencesService.saveReminderTime(hour, minute)
                userPreferencesService.hasShownDailyReminderPopup = true
            }
            
            println("⏰ [DailyReminderViewModel] Reminder scheduled for $hour:$minute")
            
        } catch (e: SecurityException) {
            println("❌ [DailyReminderViewModel] Failed to schedule alarm: ${e.message}")
        }
    }
    
    fun skipReminder() {
        userPreferencesService.hasShownDailyReminderPopup = true
    }
    
    fun dismissPermissionAlert() {
        _showPermissionAlert.value = false
    }
}
