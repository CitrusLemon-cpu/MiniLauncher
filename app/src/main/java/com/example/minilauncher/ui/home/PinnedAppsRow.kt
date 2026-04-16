package com.example.minilauncher.ui.home

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (pinnedApps.isEmpty()) {
            repeat(5) {
                EmptyPinnedSlot()
            }
        } else {
            pinnedApps.forEach { appInfo ->
                Column(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { appRepository.launchApp(appInfo.packageName) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIcon(drawable = appInfo.icon, label = appInfo.label)
                    Text(
                        text = appInfo.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
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
            .padding(horizontal = 6.dp)
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    )
}

@Composable
private fun AppIcon(drawable: Drawable?, label: String) {
    if (drawable == null) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
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
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
    )
}
