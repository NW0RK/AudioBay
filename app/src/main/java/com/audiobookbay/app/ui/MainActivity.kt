package com.audiobookbay.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.audiobookbay.app.ui.navigation.AudioBookBayNavHost
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.audiobookbay.app.domain.repository.AppUpdateRepository
import com.audiobookbay.app.ui.screens.settings.AppUpdateAnnouncement

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var updates: AppUpdateRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioBookBayTheme {
                AudioBookBayNavHost()
                AppUpdateAnnouncement(updates)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { updates.check(automatic = true) }
    }
}
