package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.auth.GoogleAuthHelper
import com.example.ui.DemetraApp
import com.example.ui.components.configureDemetraImageLoader
import com.example.ui.components.prewarmDemetraImages
import com.example.ui.components.prewarmMapsRenderer
import com.example.ui.theme.DemetraTheme
import com.example.viewmodel.DemetraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check and tuned Coil ImageLoader before UI creation
        configureAppCheck(this, intent)
        configureDemetraImageLoader(applicationContext)

        enableEdgeToEdge()
        setContent {
            DemetraTheme {
                LaunchedEffect(Unit) {
                    launch { prewarmDemetraImages(applicationContext) }
                    launch { GoogleAuthHelper.attemptAutoSignIn(this@MainActivity) }
                    delay(180L)
                    prewarmMapsRenderer(applicationContext)
                }
                val demetraViewModel: DemetraViewModel = viewModel()
                DemetraApp(viewModel = demetraViewModel)
            }
        }
    }
}
