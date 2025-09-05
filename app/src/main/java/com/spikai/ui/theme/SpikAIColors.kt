package com.spikai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * SpikAI Color System
 * Provides consistent access to all app colors with proper naming conventions
 * that match the original iOS app design system
 */
object SpikAIColors {
    
    // Brand Colors
    val PrimaryBlue: Color @Composable get() = PrimaryBlue
    val PrimaryPurple: Color @Composable get() = PrimaryPurple
    val GoogleBlue: Color @Composable get() = GoogleBlue
    
    // Background Colors
    val BackgroundPrimary: Color @Composable get() = BackgroundPrimary
    val BackgroundSecondary: Color @Composable get() = BackgroundSecondary
    val BackgroundTertiary: Color @Composable get() = BackgroundTertiary
    val BackgroundCard: Color @Composable get() = BackgroundCard
    
    // Text Colors
    val TextPrimary: Color @Composable get() = TextPrimary
    val TextSecondary: Color @Composable get() = TextSecondary
    val TextInverse: Color @Composable get() = TextInverse
    val TextOnPrimary: Color @Composable get() = TextOnPrimary
    
    // Status Colors
    val SuccessGreen: Color @Composable get() = SuccessGreen
    val ErrorRed: Color @Composable get() = ErrorRed
    val WarningOrange: Color @Composable get() = WarningOrange
    
    // Border Colors
    val BorderLight: Color @Composable get() = BorderLight
    val BorderInactive: Color @Composable get() = BorderInactive
    
    // Shadow Colors
    val ShadowColor: Color @Composable get() = ShadowColor
    
    // Transparent Variants
    val BlueTransparent10: Color @Composable get() = BlueTransparent10
    val BlueTransparent30: Color @Composable get() = BlueTransparent30
    val WhiteTransparent95: Color @Composable get() = WhiteTransparent95
    
    // Connection Status Colors
    val StatusConnected: Color @Composable get() = StatusConnected
    val StatusConnecting: Color @Composable get() = StatusConnecting
    val StatusDisconnected: Color @Composable get() = StatusDisconnected
    val StatusFailed: Color @Composable get() = StatusFailed
    
    /**
     * Helper functions for gradient creation
     */
    @Composable
    fun primaryGradientColors(): List<Color> = listOf(PrimaryBlue, PrimaryPurple)
    
    @Composable
    fun backgroundGradientColors(): List<Color> = listOf(
        BackgroundPrimary,
        BackgroundSecondary,
        Color(0xFF0f3460) // Third gradient stop for conversation backgrounds
    )
    
    @Composable
    fun cardGradientColors(): List<Color> = listOf(
        BackgroundCard,
        BackgroundTertiary
    )
}
