package com.example.minilauncher.ui.drawer

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.model.AppInfo
import com.example.minilauncher.ui.settings.PasswordEntryDialog
import kotlinx.coroutines.launch

@Composable
fun AppDrawerScreen(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apps by appRepository.apps.collectAsStateWithLifecycle()
    val hiddenApps by preferencesManager.hiddenApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val hiddenUsageApps by preferencesManager.hiddenUsageApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val pinnedApps by preferencesManager.pinnedApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val showIcons by preferencesManager.showIcons.collectAsStateWithLifecycle(initialValue = true)
    val preventDeletion by preferencesManager.preventDeletion.collectAsStateWithLifecycle(initialValue = false)
    val requirePasswordToHide by preferencesManager.requirePasswordToHide.collectAsStateWithLifecycle(initialValue = false)
    val passwordHash by preferencesManager.passwordHash.collectAsStateWithLifecycle(initialValue = "")
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var appPendingHide by remember { mutableStateOf<AppInfo?>(null) }
    var showHidePasswordDialog by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, hiddenApps, searchQuery) {
        apps.filter { appInfo ->
            appInfo.packageName !in hiddenApps &&
                appInfo.label.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        text = "Search",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Text(
                        text = "⌕",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.tertiary
                )
            )
            IconButton(onClick = onNavigateToSettings) {
                Text(text = "⚙", style = MaterialTheme.typography.headlineSmall)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = filteredApps,
                key = { appInfo -> appInfo.packageName }
            ) { appInfo ->
                AppListItem(
                    appInfo = appInfo,
                    showIcons = showIcons,
                    onClick = { appRepository.launchApp(appInfo.packageName) },
                    onLongPress = { selectedApp = appInfo }
                )
            }
        }
    }

    selectedApp?.let { appInfo ->
        AppLongPressMenu(
            appInfo = appInfo,
            showDelete = !preventDeletion,
            isPinned = appInfo.packageName in pinnedApps,
            isUsageHidden = appInfo.packageName in hiddenUsageApps,
            onDismiss = { selectedApp = null },
            onDelete = {
                appRepository.uninstallApp(appInfo.packageName)
                selectedApp = null
            },
            onHide = {
                appPendingHide = appInfo
                selectedApp = null
            },
            onPinToggle = {
                coroutineScope.launch {
                    if (appInfo.packageName in pinnedApps) {
                        preferencesManager.removePinnedApp(appInfo.packageName)
                    } else {
                        val added = preferencesManager.addPinnedApp(appInfo.packageName)
                        if (!added) {
                            Toast.makeText(context, "Maximum 10 pinned apps", Toast.LENGTH_SHORT).show()
                        }
                    }
                    selectedApp = null
                }
            },
            onUsageToggle = {
                coroutineScope.launch {
                    preferencesManager.setHiddenUsageApp(
                        appInfo.packageName,
                        appInfo.packageName !in hiddenUsageApps
                    )
                    selectedApp = null
                }
            }
        )
    }

    appPendingHide?.let { appInfo ->
        if (!showHidePasswordDialog) {
            if (requirePasswordToHide && passwordHash.isNotBlank()) {
                LaunchedEffect(appInfo) {
                    showHidePasswordDialog = true
                }
            } else {
                AlertDialog(
                    onDismissRequest = { appPendingHide = null },
                    title = { Text("Hide App") },
                    text = { Text("Are you sure you want to hide ${appInfo.label}?") },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                preferencesManager.setHiddenApp(appInfo.packageName, true)
                                appPendingHide = null
                            }
                        }) {
                            Text("Hide")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { appPendingHide = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    if (showHidePasswordDialog && appPendingHide != null) {
        PasswordEntryDialog(
            storedHash = passwordHash,
            onDismiss = {
                showHidePasswordDialog = false
                appPendingHide = null
            },
            onSuccess = {
                coroutineScope.launch {
                    appPendingHide?.let { appInfo ->
                        preferencesManager.setHiddenApp(appInfo.packageName, true)
                    }
                    showHidePasswordDialog = false
                    appPendingHide = null
                }
            }
        )
    }
}
