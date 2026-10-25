package com.example.digitalspiritlevel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.digitalspiritlevel.level.LevelAction
import com.example.digitalspiritlevel.level.LevelScreen
import com.example.digitalspiritlevel.level.LevelViewModel
import com.example.digitalspiritlevel.ui.theme.DigitalSpiritLevelTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LevelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DigitalSpiritLevelTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LevelScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAction(LevelAction.Start)
    }

    override fun onPause() {
        viewModel.onAction(LevelAction.Stop)
        super.onPause()
    }
}
