package com.example.minilauncher.ui.home

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.model.AppInfo

@Composable
fun PinnedAppsRow(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val apps by appRepository.apps.collectAsStateWithLifecycle()
    val pinnedPackages by preferencesManager.pinnedApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val pinnedApps = pinnedPackages.mapNotNull { packageName ->
        apps.firstOrNull { it.packageName == packageName } ?: appRepository.getAppInfo(packageName)
    }
    val firstRowApps = pinnedApps.take(5)
    val secondRowApps = pinnedApps.drop(5).take(5)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (pinnedApps.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) {
                    EmptyPinnedSlot()
                }
            }
        } else {
            PinnedAppsGridRow(
                pinnedApps = firstRowApps,
                onLaunchApp = appRepository::launchApp
            )
            if (secondRowApps.isNotEmpty()) {
                PinnedAppsGridRow(
                    pinnedApps = secondRowApps,
                    onLaunchApp = appRepository::launchApp
                )
            }
        }
    }
}

@Composable
private fun PinnedAppsGridRow(
    pinnedApps: List<AppInfo>,
    onLaunchApp: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        repeat(5) { index ->
            val appInfo = pinnedApps.getOrNull(index)
            if (appInfo == null) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLaunchApp(appInfo.packageName) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIcon(drawable = appInfo.icon, label = appInfo.label)
                    Text(
                        text = appInfo.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(52.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPinnedSlot() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    )
}

@Composable
private fun AppIcon(drawable: Drawable?, label: String) {
    if (drawable == null) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label.take(1), style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    AndroidView(
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            imageView.setImageDrawable(drawable)
            imageView.contentDescription = label
        },
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}
