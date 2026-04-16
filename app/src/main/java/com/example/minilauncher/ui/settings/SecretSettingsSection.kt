package com.example.minilauncher.ui.settings

import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.model.AppInfo
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SecretSettingsSection(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    onSetPasswordClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val apps by appRepository.apps.collectAsStateWithLifecycle()
    val hiddenApps by preferencesManager.hiddenApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val hiddenUsageApps by preferencesManager.hiddenUsageApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val preventDeletion by preferencesManager.preventDeletion.collectAsStateWithLifecycle(initialValue = false)
    val passwordHash by preferencesManager.passwordHash.collectAsStateWithLifecycle(initialValue = "")
    var pendingUnhideApp by remember { mutableStateOf<AppInfo?>(null) }
    var pendingUnhideUsageApp by remember { mutableStateOf<AppInfo?>(null) }
    var showRemovePasswordDialog by remember { mutableStateOf(false) }
    var showNavigationHintDialog by remember { mutableStateOf(false) }

    val hiddenAppItems = remember(apps, hiddenApps) {
        hiddenApps
            .mapNotNull { packageName ->
                apps.firstOrNull { it.packageName == packageName } ?: appRepository.getAppInfo(packageName)
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
    val staleHiddenPackages = remember(apps, hiddenApps) {
        hiddenApps.filterTo(mutableSetOf()) { packageName ->
            apps.none { it.packageName == packageName } && appRepository.getAppInfo(packageName) == null
        }
    }
    val hiddenUsageAppItems = remember(apps, hiddenUsageApps) {
        hiddenUsageApps
            .mapNotNull { packageName ->
                apps.firstOrNull { it.packageName == packageName } ?: appRepository.getAppInfo(packageName)
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    LaunchedEffect(staleHiddenPackages) {
        staleHiddenPackages.forEach { packageName ->
            preferencesManager.setHiddenApp(packageName, false)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HiddenAppsSection(
            hiddenApps = hiddenAppItems,
            onLongPress = { pendingUnhideApp = it }
        )

        HiddenUsageAppsSection(
            hiddenUsageApps = hiddenUsageAppItems,
            onLongPress = { pendingUnhideUsageApp = it }
        )

        SecretToggleRow(
            title = "Prevent App Deletion",
            subtitle = "Long press will only show Hide option",
            checked = preventDeletion,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    preferencesManager.setPreventDeletion(enabled)
                }
            }
        )

        Button(
            onClick = {
                showNavigationHintDialog = true
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Set as Default Launcher")
        }

        if (passwordHash.isBlank()) {
            Button(
                onClick = onSetPasswordClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Set a Password")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onChangePasswordClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Change Password")
                }
                Button(
                    onClick = { showRemovePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Remove Password")
                }
            }
        }
    }

    pendingUnhideApp?.let { appInfo ->
        AlertDialog(
            onDismissRequest = { pendingUnhideApp = null },
            title = { Text(text = "Unhide ${appInfo.label}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setHiddenApp(appInfo.packageName, false)
                            pendingUnhideApp = null
                        }
                    }
                ) {
                    Text(text = "Unhide")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnhideApp = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    pendingUnhideUsageApp?.let { appInfo ->
        AlertDialog(
            onDismissRequest = { pendingUnhideUsageApp = null },
            title = { Text(text = "Show ${appInfo.label} Usage?") },
            text = { Text(text = "This will show ${appInfo.label} in the usage breakdown again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setHiddenUsageApp(appInfo.packageName, false)
                            pendingUnhideUsageApp = null
                        }
                    }
                ) {
                    Text(text = "Show")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnhideUsageApp = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showRemovePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePasswordDialog = false },
            title = { Text(text = "Remove Password") },
            text = { Text(text = "Remove password protection from Secret Settings?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setPasswordHash("")
                            showRemovePasswordDialog = false
                        }
                    }
                ) {
                    Text(text = "Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePasswordDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showNavigationHintDialog) {
        AlertDialog(
            onDismissRequest = { showNavigationHintDialog = false },
            title = { Text(text = "Switch to Button Navigation") },
            text = {
                Text(
                    text = "For the best experience with MiniLauncher, switch to button navigation. Go to Settings → System → Gestures → System Navigation → 3-button navigation."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNavigationHintDialog = false
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                ) {
                    Text(text = "Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNavigationHintDialog = false }) {
                    Text(text = "Dismiss")
                }
            }
        )
    }
}

@Composable
private fun HiddenAppsSection(
    hiddenApps: List<AppInfo>,
    onLongPress: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hidden Apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            HiddenAppsCountBadge(count = hiddenApps.size)
        }

        if (hiddenApps.isEmpty()) {
            Text(
                text = "No hidden apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                hiddenApps.forEach { appInfo ->
                    HiddenAppRow(
                        appInfo = appInfo,
                        onLongPress = { onLongPress(appInfo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenUsageAppsSection(
    hiddenUsageApps: List<AppInfo>,
    onLongPress: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hidden Usage Apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            HiddenAppsCountBadge(count = hiddenUsageApps.size)
        }

        if (hiddenUsageApps.isEmpty()) {
            Text(
                text = "No hidden usage apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                hiddenUsageApps.forEach { appInfo ->
                    HiddenAppRow(
                        appInfo = appInfo,
                        onLongPress = { onLongPress(appInfo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAppsCountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiddenAppRow(
    appInfo: AppInfo,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecretAppIcon(drawable = appInfo.icon, label = appInfo.label)
        Text(
            text = appInfo.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SecretToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, end = 16.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SecretAppIcon(drawable: Drawable?, label: String) {
    if (drawable == null) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
    )
}
