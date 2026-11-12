package com.tittu.glasstune

import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tittu.glasstune.ui.GlassTuneApp
import com.tittu.glasstune.ui.MusicViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Request the panel high-refresh mode when the device exposes 120 Hz.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.attributes = window.attributes.apply { preferredRefreshRate = 120f }
        }
        setContent {
            val vm: MusicViewModel = viewModel()
            GlassTuneApp(vm)
        }
    }
}
