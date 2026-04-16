package com.example.minilauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository

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
}
