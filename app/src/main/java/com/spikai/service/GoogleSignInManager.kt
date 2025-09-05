package com.spikai.service

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class GoogleSignInManager private constructor(
    private val context: Context
) : ViewModel() {
    
    // Published properties equivalent
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // TODO: Implement ErrorHandlingService equivalent
    // private val errorHandler = ErrorHandlingService.shared
    
    // Firebase and Google Sign-In clients
    private val auth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient
    
    companion object {
        @Volatile
        private var INSTANCE: GoogleSignInManager? = null
        
        fun getInstance(context: Context): GoogleSignInManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleSignInManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.spikai.R.string.default_web_client_id)) // TODO: Add to strings.xml
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        // Check if user is already signed in
        auth.currentUser?.let { user ->
            _isSignedIn.value = true
            _currentUser.value = user
        }
        
        // Listen for auth state changes
        auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _isSignedIn.value = user != null
            _currentUser.value = user
        }
    }
    
    fun signInWithGoogle(activity: Activity, completion: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // TODO: Implement Firebase client ID configuration check similar to Swift version
                // val clientID = FirebaseApp.getInstance()?.options?.applicationId
                // if (clientID == null) {
                //     val error = SpikError.authenticationFailed
                //     errorHandler.showError(error)
                //     _errorMessage.value = error.localizedDescription
                //     completion(false)
                //     return@launch
                // }
                
                _isLoading.value = true
                _errorMessage.value = null
                
                // Start Google Sign-In intent
                val signInIntent = googleSignInClient.signInIntent
                // TODO: Handle activity result - this needs to be called from Activity with result launcher
                // For now, we'll structure it to be called from the UI layer
                
                println("🔗 [GoogleSignInManager] Starting Google Sign-In flow")
                completion(false) // TODO: Implement proper activity result handling
                
            } catch (e: Exception) {
                handleSignInError(e, completion)
            }
        }
    }
    
    // Call this from Activity's onActivityResult or modern result launcher
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            val idToken = account.idToken
            if (idToken == null) {
                // TODO: Use SpikError equivalent
                // val error = SpikError.authenticationFailed
                // errorHandler.showError(error)
                // _errorMessage.value = error.localizedDescription
                _errorMessage.value = "Failed to get ID token"
                return false
            }
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            
            val user = authResult.user
            if (user == null) {
                // TODO: Use SpikError equivalent
                // val error = SpikError.authenticationFailed
                // errorHandler.showError(error)
                // _errorMessage.value = error.localizedDescription
                _errorMessage.value = "Authentication failed"
                return false
            }
            
            // Log user info
            println("Signed in as ${user.displayName ?: "Unknown"}")
            
            // Save to Firestore
            val success = saveUserToFirestore(user)
            if (success) {
                _isSignedIn.value = true
                _currentUser.value = user
                _errorMessage.value = null
                
                // Upload any pending FCM token now that user is authenticated
                // TODO: Implement FCMTokenService equivalent
                // FCMTokenService.shared.uploadPendingTokenIfExists()
                
                true
            } else {
                // TODO: Use SpikError equivalent
                // val error = SpikError.syncFailed
                // errorHandler.showError(error)
                // _errorMessage.value = error.localizedDescription
                _errorMessage.value = "Failed to save user data"
                false
            }
            
        } catch (e: Exception) {
            // TODO: Implement SpikError and ErrorHandlingService
            // val spikError = errorHandler.handleError(e) ?: SpikError.authenticationFailed
            // errorHandler.showError(spikError)
            // _errorMessage.value = spikError.localizedDescription
            _errorMessage.value = "Authentication failed: ${e.localizedMessage}"
            false
        } finally {
            _isLoading.value = false
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                googleSignInClient.signOut().await()
                
                _isSignedIn.value = false
                _currentUser.value = null
                _errorMessage.value = null
                
                println("🚪 [GoogleSignInManager] User signed out successfully")
                
            } catch (e: Exception) {
                // TODO: Implement SpikError and ErrorHandlingService
                // val spikError = errorHandler.handleError(e)
                // errorHandler.showError(spikError)
                // _errorMessage.value = spikError.localizedDescription
                _errorMessage.value = "Sign out failed: ${e.localizedMessage}"
            }
        }
    }
    
    private suspend fun saveUserToFirestore(user: FirebaseUser): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance("spik")
            val userData = mapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "name" to (user.displayName ?: ""),
                "photoURL" to (user.photoUrl?.toString() ?: ""),
                "lastLogin" to Timestamp.now(),
                "createdAt" to Timestamp(user.metadata?.creationTimestamp?.let { Date(it) } ?: Date())
            )
            
            db.collection("users")
                .document(user.uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            println("✅ [GoogleSignInManager] User data saved successfully")
            true
            
        } catch (e: Exception) {
            println("❌ [GoogleSignInManager] Error saving user data: ${e.localizedMessage}")
            false
        }
    }
    
    private fun handleSignInError(error: Exception, completion: (Boolean) -> Unit) {
        // TODO: Implement SpikError and ErrorHandlingService
        // val spikError = errorHandler.handleError(error) ?: SpikError.authenticationFailed
        // errorHandler.showError(spikError)
        // _errorMessage.value = spikError.localizedDescription
        _errorMessage.value = "Sign in failed: ${error.localizedMessage}"
        _isLoading.value = false
        completion(false)
    }
    
    fun isSignedIn(): Boolean {
        return auth.currentUser != null && _isSignedIn.value
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup if needed
    }
}
