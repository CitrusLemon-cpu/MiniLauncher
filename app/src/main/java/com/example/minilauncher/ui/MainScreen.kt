package com.example.minilauncher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository
import com.example.minilauncher.ui.drawer.AppDrawerScreen
import com.example.minilauncher.ui.home.HomeScreen

@Composable
fun MainScreen(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    usageRepository: UsageRepository,
    onNavigateToSettings: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                appRepository = appRepository,
                preferencesManager = preferencesManager,
                usageRepository = usageRepository,
                modifier = Modifier.fillMaxSize()
            )

            1 -> AppDrawerScreen(
                appRepository = appRepository,
                preferencesManager = preferencesManager,
                onNavigateToSettings = onNavigateToSettings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
