package com.spikai.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy Material colors (keeping for compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// SpikAI Brand Colors - matching iOS app exactly
val PrimaryBlue = Color(0xFF007AFF)        // iOS System Blue
val PrimaryPurple = Color(0xFF6B46C1)      // Custom purple for gradients
val GoogleBlue = Color(0xFF4285F4)         // Google brand blue

// Background Colors
val BackgroundPrimary = Color(0xFF1a1a2e)     // Dark background (Conversation view)
val BackgroundSecondary = Color(0xFF16213e)   // Slightly darker background
val BackgroundTertiary = Color(0xFFFFFFFF)    // White background (Onboarding/Auth)
val BackgroundCard = Color(0xFFF2F2F7)        // Light gray card background

// Text Colors
val TextPrimary = Color(0xFF1C1C1E)           // iOS Label color
val TextSecondary = Color(0xFF8E8E93)         // iOS Secondary Label color
val TextInverse = Color(0xFFFFFFFF)           // White text on dark backgrounds
val TextOnPrimary = Color(0xFFFFFFFF)         // White text on primary colors

// Status Colors
val SuccessGreen = Color(0xFF34C759)          // iOS System Green
val ErrorRed = Color(0xFFFF3B30)             // iOS System Red
val WarningOrange = Color(0xFFFF9500)        // iOS System Orange

// Border Colors
val BorderLight = Color(0xFFE5E5EA)           // iOS Separator color
val BorderInactive = Color(0xFFBEBEC0)       // Inactive/disabled borders

// Shadow Colors
val ShadowColor = Color(0xFF000000)           // Black shadows

// Transparent Variants
val BlueTransparent10 = Color(0x1A007AFF)     // 10% opacity blue
val BlueTransparent30 = Color(0x4D007AFF)     // 30% opacity blue
val WhiteTransparent95 = Color(0xF2FFFFFF)    // 95% opacity white

// Connection Status Colors (for ConversationViewModel)
val StatusConnected = SuccessGreen            // Green when connected
val StatusConnecting = WarningOrange          // Orange when connecting
val StatusDisconnected = TextSecondary        // Gray when disconnected
val StatusFailed = ErrorRed                   // Red when failed
