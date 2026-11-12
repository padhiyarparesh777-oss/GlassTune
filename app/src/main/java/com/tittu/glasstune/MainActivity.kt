package com.tittu.glasstune

import android.os.Bundle
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
        setContent {
            val vm: MusicViewModel = viewModel()
            GlassTuneApp(vm)
        }
    }
}
