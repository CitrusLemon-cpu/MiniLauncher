package com.example.minilauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager

@Composable
fun HomeScreen(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
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
            // TODO: UsageChart goes here
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
