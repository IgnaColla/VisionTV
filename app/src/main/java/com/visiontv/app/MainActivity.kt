// app/src/main/java/com/visiontv/app/MainActivity.kt
package com.visiontv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.visiontv.app.ui.VisionTVApp

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VisionTVApp()
        }
    }
}