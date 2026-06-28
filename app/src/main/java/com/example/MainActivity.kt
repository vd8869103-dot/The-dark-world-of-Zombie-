package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameViewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val gameViewModel: GameViewModel = viewModel()
                val currentScreen by gameViewModel.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07040B) // Deep horror dark violet background
                ) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenNavigationCrossfade"
                    ) { screen ->
                        when (screen) {
                            "main_menu" -> MainMenuScreen(viewModel = gameViewModel)
                            "settings" -> SettingsScreen(viewModel = gameViewModel)
                            "profile" -> ProfileScreen(viewModel = gameViewModel)
                            "shop" -> ShopScreen(viewModel = gameViewModel)
                            "play" -> PlayScreen(viewModel = gameViewModel)
                            else -> MainMenuScreen(viewModel = gameViewModel)
                        }
                    }
                }
            }
        }
    }
}
