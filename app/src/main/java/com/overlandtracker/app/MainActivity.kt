package com.overlandtracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import com.overlandtracker.app.domain.navigation.NavigationService
import com.overlandtracker.app.ui.map.MapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    MapScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.startForegroundService(this, Intent(this, NavigationService::class.java))
    }

    override fun onStop() {
        stopService(Intent(this, NavigationService::class.java))
        super.onStop()
    }
}
