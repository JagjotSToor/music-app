package com.zhehr.auralis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zhehr.auralis.ui.AuralisApp

class MainActivity : ComponentActivity() {
    private val container get() = (application as AuralisApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalContainer provides container) { AuralisApp() }
        }
    }

    override fun onStart() {
        super.onStart()
        container.player.connect()
        container.player.onUiVisibility(true)
    }

    override fun onStop() {
        container.player.onUiVisibility(false)
        super.onStop()
    }
}
