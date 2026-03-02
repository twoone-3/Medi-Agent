package com.project.medi_agent

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.project.medi_agent.ui.screens.AppContent
import com.project.medi_agent.ui.theme.MediAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("mediagent_prefs", Context.MODE_PRIVATE) }
            
            var seniorMode by remember { 
                mutableStateOf(prefs.getBoolean("senior_mode", false)) 
            }
            
            // 0: System, 1: Light, 2: Dark
            var themeMode by remember { 
                mutableIntStateOf(prefs.getInt("theme_mode", 0)) 
            }
            
            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "senior_mode" -> seniorMode = p.getBoolean(key, false)
                        "theme_mode" -> themeMode = p.getInt(key, 0)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MediAgentTheme(darkTheme = isDarkTheme, isSeniorMode = seniorMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}
