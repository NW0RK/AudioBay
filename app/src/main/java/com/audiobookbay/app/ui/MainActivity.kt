package com.audiobookbay.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.audiobookbay.app.ui.navigation.AudioBookBayNavHost
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioBookBayTheme {
                AudioBookBayNavHost()
            }
        }
    }
}
