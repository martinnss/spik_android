package com.spikai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.spikai.ui.SpikAIApp
import com.spikai.ui.theme.SpikAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpikAITheme {
                SpikAIApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpikAIAppPreview() {
    SpikAITheme {
        SpikAIApp()
    }
}
