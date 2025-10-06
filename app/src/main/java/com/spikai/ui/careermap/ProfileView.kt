package com.spikai.ui.careermap

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spikai.model.UserProfile
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    isSignedIn: Boolean = false,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showingSignOutAlert by remember { mutableStateOf(false) }
    var showingDeleteAccountAlert by remember { mutableStateOf(false) }
    var showingDeleteSuccessAlert by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        loadUserProfile { profile ->
            userProfile = profile
        }
    }
    
    // Full-screen modal with solid background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // Solid white background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            TopAppBar(
                title = { 
                    Text(
                        text = "Mi Perfil",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cerrar",
                            color = Color(0xFF007AFF) // PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF) // BackgroundPrimary
                ),
                modifier = Modifier.padding(top = 50.dp) // Safe area padding
            )
            
            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Avatar y información básica
                ProfileHeader(
                    userProfile = userProfile,
                    isSignedIn = isSignedIn
                )
                
                // Configuraciones
                SettingsSection(
                    isSignedIn = isSignedIn,
                    isDeletingAccount = isDeletingAccount,
                    onPrivacyPolicyClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.spik.cl/privacy"))
                        context.startActivity(intent)
                    },
                    onTermsClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.spik.cl/terms"))
                        context.startActivity(intent)
                    },
                    onSignOutClick = { showingSignOutAlert = true },
                    onDeleteAccountClick = { showingDeleteAccountAlert = true }
                )
            }
        }
    }
    
    // Alert dialogs positioned above the main content
    if (showingSignOutAlert) {
        AlertDialog(
            onDismissRequest = { showingSignOutAlert = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingSignOutAlert = false
                        signOut(context) {
                            // After successful sign out, trigger the callback
                            onSignOut()
                        }
                    }
                ) {
                    Text("Cerrar Sesión", color = Color(0xFFFF3B30)) // Destructive color
                }
            },
            dismissButton = {
                TextButton(onClick = { showingSignOutAlert = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showingDeleteAccountAlert) {
        AlertDialog(
            onDismissRequest = { showingDeleteAccountAlert = false },
            title = { Text("Eliminar Cuenta") },
            text = { Text("¿Estás seguro? Esta acción eliminará tu cuenta y datos en 30 días.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingDeleteAccountAlert = false
                        deleteAccount(
                            isDeletingAccount = isDeletingAccount,
                            onStateChange = { isDeletingAccount = it },
                            onSuccess = { showingDeleteSuccessAlert = true }
                        )
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFFF3B30)) // Destructive color
                }
            },
            dismissButton = {
                TextButton(onClick = { showingDeleteAccountAlert = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showingDeleteSuccessAlert) {
        AlertDialog(
            onDismissRequest = { showingDeleteSuccessAlert = false },
            title = { Text("Cuenta Programada para Eliminación") },
            text = { Text("Tu cuenta será eliminada en 30 días. Si inicias sesión antes de ese tiempo, la eliminación será cancelada automáticamente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingDeleteSuccessAlert = false
                        onDismiss()
                    }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    userProfile: UserProfile,
    isSignedIn: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // BackgroundSecondary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF).copy(alpha = 0.2f)), // PrimaryBlue with opacity
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF007AFF), // PrimaryBlue
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Información básica
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (userProfile.name.isEmpty()) "Usuario Spik" else userProfile.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E) // TextPrimary
                )
                
                Text(
                    text = userProfile.levelDisplayText,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93) // TextSecondary
                )
                
                // Show sign-in status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSignedIn) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = if (isSignedIn) Color(0xFF34C759) else Color(0xFF8E8E93), // SuccessGreen or TextSecondary
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Text(
                        text = if (isSignedIn) "Sesión iniciada" else "Sin sesión",
                        fontSize = 12.sp,
                        color = if (isSignedIn) Color(0xFF34C759) else Color(0xFF8E8E93) // SuccessGreen or TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    isSignedIn: Boolean,
    isDeletingAccount: Boolean,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E) // TextPrimary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF2F2F7) // secondarySystemBackground equivalent
            )
        ) {
            Column {
                SettingRow(
                    icon = Icons.Default.Security,
                    title = "Política de Privacidad",
                    color = Color(0xFF007AFF), // PrimaryBlue
                    showArrow = true,
                    onClick = onPrivacyPolicyClick
                )
                
                Divider(
                    modifier = Modifier.padding(start = 44.dp),
                    color = Color(0xFFE5E5EA) // BorderLight
                )
                
                SettingRow(
                    icon = Icons.Default.Description,
                    title = "Términos de Servicio",
                    color = Color(0xFF007AFF), // PrimaryBlue
                    showArrow = true,
                    onClick = onTermsClick
                )
                
                // Show sign out option only if user is signed in
                if (isSignedIn) {
                    Divider(
                        color = Color(0xFFE5E5EA) // BorderLight
                    )
                    
                    SettingRow(
                        icon = Icons.Default.Logout,
                        title = "Cerrar Sesión",
                        color = Color(0xFFFF3B30), // ErrorRed
                        showArrow = false,
                        onClick = onSignOutClick
                    )
                }
                
                // Account deletion button
                Divider(
                    modifier = Modifier.padding(start = 44.dp),
                    color = Color(0xFFE5E5EA) // BorderLight
                )
                 /*
                SettingRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Eliminar Cuenta",
                    color = Color(0xFFFF3B30), // ErrorRed
                    showArrow = false,
                    enabled = !isDeletingAccount,
                    onClick = onDeleteAccountClick
                )*/
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    color: Color,
    showArrow: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) color else Color(0xFF8E8E93),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = title,
            fontSize = 16.sp,
            color = if (enabled) Color(0xFF1C1C1E) else Color(0xFF8E8E93), // TextPrimary or TextSecondary
            modifier = Modifier.weight(1f)
        )

        if (showArrow) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = Color(0xFF8E8E93), // TextSecondary
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// MARK: - Helper Functions

private suspend fun loadUserProfile(onLoaded: (UserProfile) -> Unit) {
    // TODO: Load from SharedPreferences equivalent
    delay(100) // Simulate loading

    // Fallback for when no profile exists
    val profile = UserProfile().apply {
        englishLevel = com.spikai.model.EnglishLevel.PRINCIPIANTE
        name = "Usuario Spik"
    }

    println("✅ [ProfileView] User profile loaded: ${profile.englishLevel?.rawValue ?: "unknown"}")
    onLoaded(profile)
}

private fun signOut(context: android.content.Context, onComplete: () -> Unit) {
    println("🚪 [ProfileView] User signing out")
    
    // Sign out from Firebase
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    auth.signOut()
    println("✅ [ProfileView] Signed out from Firebase")
    
    // Clear user profile from SharedPreferences
    val prefs = context.getSharedPreferences("career_map_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().remove("userProfile").apply()
    println("✅ [ProfileView] Cleared user profile from SharedPreferences")
    
    // Clear career progress from SharedPreferences
    prefs.edit().remove("careerProgress").apply()
    println("✅ [ProfileView] Cleared career progress from SharedPreferences")
    
    // Clear onboarding status
    val spikPrefs = context.getSharedPreferences("spik_ai_prefs", android.content.Context.MODE_PRIVATE)
    spikPrefs.edit().remove("hasCompletedOnboarding").apply()
    println("✅ [ProfileView] Cleared onboarding status")
    
    // Clear local data service cache
    val localDataService = com.spikai.service.LocalDataService.getInstance(context)
    localDataService.clearAllCache()
    println("✅ [ProfileView] Cleared local data service cache")
    
    println("✅ [ProfileView] Sign out completed")
    onComplete()
}

private fun deleteAccount(
    isDeletingAccount: Boolean,
    onStateChange: (Boolean) -> Unit,
    onSuccess: () -> Unit
) {
    if (isDeletingAccount) return

    println("🗑️ [ProfileView] Starting account deletion process")
    onStateChange(true)

    // TODO: Implement AccountDeletionService equivalent
    // val success = AccountDeletionService.shared.requestAccountDeletion()

    // Simulate async operation
    // For now, assume success
    onStateChange(false)
    println("✅ [ProfileView] Account deletion request completed successfully")
    onSuccess()
}

@Preview(showBackground = true)
@Composable
private fun ProfileViewPreview() {
    ProfileView(onDismiss = { })
}
