package com.spikai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.auth.FirebaseAuth
import com.spikai.ui.SpikAIApp
import com.spikai.ui.components.DataCorruptionErrorView
import com.spikai.ui.theme.SpikAITheme
import com.spikai.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    
    private val appViewModel: AppViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "🚀 MainActivity onCreate started")
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "🔥 Firebase Auth initialized")
        
        setContent {
            SpikAITheme {
                SpikAIApp()
                
                // Check current authentication state
                LaunchedEffect(Unit) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        Log.d(TAG, "✅ User is already signed in:")
                        Log.d(TAG, "   👤 Name: ${currentUser.displayName}")
                        Log.d(TAG, "   📧 Email: ${currentUser.email}")
                        Log.d(TAG, "   🆔 UID: ${currentUser.uid}")
                    } else {
                        Log.d(TAG, "❌ No user currently signed in")
                    }
                }
            }
        }
        
        Log.d(TAG, "🎨 SpikAI content set successfully")
    }
    
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "▶️ MainActivity onStart")
        
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "🔄 onStart: User is signed in as ${currentUser.displayName}")
        } else {
            Log.d(TAG, "🔄 onStart: No user signed in")
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "▶️ MainActivity onResume")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "⏸️ MainActivity onPause")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "⏹️ MainActivity onStop")
    }
}

@Preview(showBackground = true)
@Composable
fun SpikAIAppPreview() {
    SpikAITheme {
        SpikAIApp()
    }
}
