package com.spikai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spikai.ui.auth.LoginView
import com.spikai.ui.careermap.CareerMapView
import com.spikai.ui.onboarding.OnboardingView
import com.spikai.viewmodel.AppViewModel

@Composable
fun SpikAIApp(
    appViewModel: AppViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        // Log initial user properties
        com.spikai.service.UserPreferencesService.getInstance(context).logCurrentReminderSettings()
    }

    val hasCompletedOnboarding by appViewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val isSignedIn by appViewModel.isSignedIn.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            !hasCompletedOnboarding -> {
                OnboardingView(
                    onOnboardingComplete = {
                        appViewModel.setOnboardingCompleted(true)
                    }
                )
            }
            !isSignedIn -> {
                LoginView(
                    onSuccessfulLogin = {
                        // FCM token upload is handled automatically in GoogleSignInManager
                        appViewModel.refreshSignInStatus()
                    }
                )
            }
            else -> {
                CareerMapView()
            }
        }
    }
}
