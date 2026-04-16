package com.example.minilauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository
import com.example.minilauncher.ui.drawer.AppDrawerScreen
import com.example.minilauncher.ui.home.HomeScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    usageRepository: UsageRepository,
    homeResetCount: Int,
    onNavigateToSettings: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (pagerState.currentPage == 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    LaunchedEffect(homeResetCount) {
        if (homeResetCount > 0 && pagerState.currentPage != 0) {
            pagerState.animateScrollToPage(0)
        }
    }

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
