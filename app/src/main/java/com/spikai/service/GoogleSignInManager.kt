package com.spikai.service

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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
    
    // Firebase and Credential Manager
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)
    
    companion object {
        private const val TAG = "GoogleSignInManager"
        @Volatile
        private var INSTANCE: GoogleSignInManager? = null
        
        fun getInstance(context: Context): GoogleSignInManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleSignInManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        Log.d(TAG, "🏗️ GoogleSignInManager initializing...")
        
        // Check if user is already signed in
        auth.currentUser?.let { user ->
            Log.d(TAG, "🔄 Found existing signed-in user:")
            Log.d(TAG, "   👤 Name: ${user.displayName}")
            Log.d(TAG, "   📧 Email: ${user.email}")
            Log.d(TAG, "   🆔 UID: ${user.uid}")
            _isSignedIn.value = true
            _currentUser.value = user
            
            // This is likely where your "Usuario autenticado exitosamente" message comes from
            Log.d(TAG, "🎊 Usuario autenticado exitosamente - continuando progreso")
        } ?: run {
            Log.d(TAG, "❌ No existing user found")
        }
        
        // Listen for auth state changes
        auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            Log.d(TAG, "🔔 Auth state changed:")
            if (user != null) {
                Log.d(TAG, "   ✅ User signed in: ${user.displayName}")
                _isSignedIn.value = true
                _currentUser.value = user
            } else {
                Log.d(TAG, "   ❌ User signed out")
                _isSignedIn.value = false
                _currentUser.value = null
            }
        }
        
        Log.d(TAG, "✅ GoogleSignInManager initialized")
    }
    
    fun signInWithGoogle(activity: Activity, completion: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.d(TAG, "🔗 Starting Google Sign-In flow")
                Log.d(TAG, "📱 Activity: ${activity::class.simpleName}")
                
                // Check if user is already signed in
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    Log.d(TAG, "⚠️ User is already signed in: ${currentUser.displayName}")
                    Log.d(TAG, "   📧 Email: ${currentUser.email}")
                    Log.d(TAG, "   🆔 UID: ${currentUser.uid}")
                    Log.d(TAG, "   ⏰ Proceeding with new sign-in anyway...")
                }
                
                // Instantiate a Google sign-in request
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(context.getString(com.spikai.R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false) // Show all accounts, not just previously authorized
                    .build()
                
                Log.d(TAG, "🔧 Created GoogleIdOption with client ID")
                
                // Create the Credential Manager request
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                Log.d(TAG, "📝 Created credential request")
                
                // Get the credential
                Log.d(TAG, "⏳ Requesting credentials from CredentialManager...")
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity,
                )
                
                Log.d(TAG, "✅ Received credential response")
                handleSignIn(result, completion)
                
            } catch (e: GetCredentialException) {
                Log.w(TAG, "❌ Google Sign-In failed with GetCredentialException", e)
                Log.e(TAG, "Error type: ${e::class.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                handleSignInError(e, completion)
            } catch (e: Exception) {
                Log.w(TAG, "❌ Unexpected error during sign-in", e)
                Log.e(TAG, "Error type: ${e::class.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                handleSignInError(e, completion)
            }
        }
    }
    
    private suspend fun handleSignIn(result: GetCredentialResponse, completion: (Boolean) -> Unit) {
        try {
            val credential = result.credential
            Log.d(TAG, "🔍 Processing credential of type: ${credential.type}")
            
            // Check if credential is of type Google ID
            if (credential is androidx.credentials.CustomCredential && 
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                
                Log.d(TAG, "✅ Credential is Google ID Token type")
                
                // Create Google ID Token
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Log.d(TAG, "🎫 Created GoogleIdTokenCredential")
                Log.d(TAG, "👤 ID: ${googleIdTokenCredential.id}")
                Log.d(TAG, "📧 Display Name: ${googleIdTokenCredential.displayName}")
                
                // Sign in to Firebase using the token
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken, completion)
            } else {
                Log.w(TAG, "❌ Credential is not of type Google ID!")
                Log.w(TAG, "Received credential type: ${credential.type}")
                _errorMessage.value = "Invalid credential type"
                _isLoading.value = false
                completion(false)
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "❌ Received an invalid google id token response", e)
            handleSignInError(e, completion)
        } catch (e: Exception) {
            Log.w(TAG, "❌ Error handling sign-in", e)
            handleSignInError(e, completion)
        }
    }
    
    private suspend fun firebaseAuthWithGoogle(idToken: String, completion: (Boolean) -> Unit) {
        try {
            Log.d(TAG, "🔥 Starting Firebase authentication with Google token")
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Log.d(TAG, "🎟️ Created Firebase credential")
            
            val authResult = auth.signInWithCredential(credential).await()
            Log.d(TAG, "✅ Firebase signInWithCredential completed")
            
            val user = authResult.user
            if (user == null) {
                Log.w(TAG, "❌ signInWithCredential:failure - no user returned")
                _errorMessage.value = "Authentication failed"
                _isLoading.value = false
                completion(false)
                return
            }
            
            Log.d(TAG, "🎉 signInWithCredential:success")
            Log.d(TAG, "👤 User ID: ${user.uid}")
            Log.d(TAG, "📧 Email: ${user.email}")
            Log.d(TAG, "🏷️ Display Name: ${user.displayName}")
            Log.d(TAG, "📸 Photo URL: ${user.photoUrl}")
            
            // Save to Firestore
            Log.d(TAG, "💾 Saving user data to Firestore...")
            val success = saveUserToFirestore(user)
            if (success) {
                _isSignedIn.value = true
                _currentUser.value = user
                _errorMessage.value = null
                
                Log.d(TAG, "🎊 Usuario autenticado exitosamente - continuando progreso")
                completion(true)
            } else {
                Log.e(TAG, "❌ Failed to save user data to Firestore")
                _errorMessage.value = "Failed to save user data"
                completion(false)
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "❌ signInWithCredential:failure", e)
            Log.e(TAG, "Exception type: ${e::class.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            _errorMessage.value = "Authentication failed: ${e.localizedMessage}"
            completion(false)
        } finally {
            _isLoading.value = false
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚪 Starting sign out process...")
                
                auth.signOut()
                Log.d(TAG, "✅ Firebase Auth sign out completed")
                
                _isSignedIn.value = false
                _currentUser.value = null
                _errorMessage.value = null
                
                Log.d(TAG, "🎯 User signed out successfully")
                
            } catch (e: Exception) {
                Log.w(TAG, "❌ Sign out failed", e)
                _errorMessage.value = "Sign out failed: ${e.localizedMessage}"
            }
        }
    }
    
    // For testing purposes - clears authentication state
    fun clearAuthForTesting() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 Clearing auth for testing...")
                auth.signOut()
                _isSignedIn.value = false
                _currentUser.value = null
                _errorMessage.value = null
                Log.d(TAG, "🧹 Auth cleared for testing")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear auth for testing", e)
            }
        }
    }
    
    private suspend fun saveUserToFirestore(user: FirebaseUser): Boolean {
        return try {
            Log.d(TAG, "💾 Attempting to save user data to Firestore...")
            Log.d(TAG, "🏢 Database instance: spik")
            
            val db = FirebaseFirestore.getInstance("spik")
            val userData = mapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "name" to (user.displayName ?: ""),
                "photoURL" to (user.photoUrl?.toString() ?: ""),
                "lastLogin" to Timestamp.now(),
                "createdAt" to Timestamp(user.metadata?.creationTimestamp?.let { Date(it) } ?: Date())
            )
            
            Log.d(TAG, "📄 User data prepared: $userData")
            
            db.collection("users")
                .document(user.uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Log.d(TAG, "✅ User data saved successfully to Firestore")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving user data to Firestore", e)
            Log.e(TAG, "Exception type: ${e::class.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            false
        }
    }
    
    private fun handleSignInError(error: Exception, completion: (Boolean) -> Unit) {
        Log.e(TAG, "Sign in failed", error)
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