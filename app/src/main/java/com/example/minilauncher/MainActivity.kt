package com.example.minilauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository
import com.example.minilauncher.ui.MainScreen
import com.example.minilauncher.ui.settings.SettingsScreen
import com.example.minilauncher.ui.theme.MiniLauncherTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : AppCompatActivity() {
    private val homeResetTrigger = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniLauncherTheme {
                MainNavigation(homeResetTrigger = homeResetTrigger)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        homeResetTrigger.value += 1
    }
}

@Composable
fun MainNavigation(homeResetTrigger: StateFlow<Int>) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val appRepository = AppRepository.getInstance(context)
    val preferencesManager = PreferencesManager.getInstance(context)
    val usageRepository = UsageRepository.getInstance(context)
    val resetCount by homeResetTrigger.collectAsStateWithLifecycle()

    LaunchedEffect(resetCount) {
        if (resetCount > 0) {
            navController.popBackStack("main", inclusive = false)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    appRepository = appRepository,
                    preferencesManager = preferencesManager,
                    usageRepository = usageRepository,
                    homeResetCount = resetCount,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    preferencesManager = preferencesManager,
                    appRepository = appRepository,
                    usageRepository = usageRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
