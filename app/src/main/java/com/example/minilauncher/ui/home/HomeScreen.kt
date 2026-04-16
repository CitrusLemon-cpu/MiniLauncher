package com.example.minilauncher.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    usageRepository: UsageRepository,
    modifier: Modifier = Modifier
) {
    var hasUsagePermission by remember(usageRepository) {
        mutableStateOf(usageRepository.hasUsagePermission())
    }
    val hasSeenPrompt by preferencesManager.hasSeenLauncherPrompt.collectAsStateWithLifecycle(initialValue = true)
    var showLauncherPrompt by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(hasSeenPrompt) {
        if (!hasSeenPrompt) {
            showLauncherPrompt = true
        }
    }

    LifecycleResumeEffect(usageRepository) {
        hasUsagePermission = usageRepository.hasUsagePermission()
        onPauseOrDispose { }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        ClockDisplay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.Center)
        ) {
            if (hasUsagePermission) {
                UsageChart(
                    appRepository = appRepository,
                    preferencesManager = preferencesManager,
                    usageRepository = usageRepository,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                UsagePermissionPrompt(
                    usageRepository = usageRepository,
                    modifier = Modifier.fillMaxSize(),
                    onPermissionChanged = { hasUsagePermission = it }
                )
            }
        }

        PinnedAppsRow(
            appRepository = appRepository,
            preferencesManager = preferencesManager,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }

    if (showLauncherPrompt) {
        AlertDialog(
            onDismissRequest = {
                showLauncherPrompt = false
                coroutineScope.launch { preferencesManager.setHasSeenLauncherPrompt(true) }
            },
            title = { Text("Set as Default Launcher?") },
            text = { Text("Would you like to make MiniLauncher your default home screen?") },
            confirmButton = {
                TextButton(onClick = {
                    showLauncherPrompt = false
                    coroutineScope.launch { preferencesManager.setHasSeenLauncherPrompt(true) }
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLauncherPrompt = false
                    coroutineScope.launch { preferencesManager.setHasSeenLauncherPrompt(true) }
                }) {
                    Text("Not Now")
                }
            }
        )
    }
}
