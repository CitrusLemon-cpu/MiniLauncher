package com.example.minilauncher.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    appRepository: AppRepository,
    usageRepository: UsageRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val showIcons by preferencesManager.showIcons.collectAsStateWithLifecycle(initialValue = true)
    val weekStartDay by preferencesManager.weekStartDay.collectAsStateWithLifecycle(initialValue = Calendar.SUNDAY)
    val hiddenApps by preferencesManager.hiddenApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val hiddenUsageApps by preferencesManager.hiddenUsageApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val passwordHash by preferencesManager.passwordHash.collectAsStateWithLifecycle(initialValue = "")
    val apps by appRepository.apps.collectAsStateWithLifecycle()
    var secretSettingsVisible by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val staleHiddenPackages = remember(apps, hiddenApps) {
        hiddenApps.filterTo(mutableSetOf()) { packageName ->
            apps.none { it.packageName == packageName } && appRepository.getAppInfo(packageName) == null
        }
    }

    LaunchedEffect(staleHiddenPackages) {
        staleHiddenPackages.forEach { packageName ->
            preferencesManager.setHiddenApp(packageName, false)
        }
    }

    LaunchedEffect(secretSettingsVisible) {
        if (secretSettingsVisible) {
            delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable(onClick = onNavigateBack)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsToggleRow(
                title = "Show App Icons",
                checked = showIcons,
                onCheckedChange = { enabled ->
                    coroutineScope.launch {
                        preferencesManager.setShowIcons(enabled)
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            WeekStartPicker(
                selectedDay = weekStartDay,
                coroutineScope = coroutineScope,
                preferencesManager = preferencesManager
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Text(
                text = "Usage History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            UsageHistoryCalendar(
                appRepository = appRepository,
                usageRepository = usageRepository,
                hiddenUsageApps = hiddenUsageApps,
                weekStartDay = weekStartDay,
                showIcons = showIcons,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Button(
                onClick = {
                    when {
                        secretSettingsVisible -> secretSettingsVisible = false
                        passwordHash.isBlank() -> secretSettingsVisible = true
                        else -> showPasswordDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (secretSettingsVisible) "Hide Secret Settings" else "Secret Settings")
            }

            AnimatedVisibility(
                visible = secretSettingsVisible,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                SecretSettingsSection(
                    appRepository = appRepository,
                    preferencesManager = preferencesManager,
                    onSetPasswordClick = { showSetPasswordDialog = true },
                    onChangePasswordClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showPasswordDialog) {
        PasswordEntryDialog(
            storedHash = passwordHash,
            onDismiss = { showPasswordDialog = false },
            onSuccess = {
                showPasswordDialog = false
                secretSettingsVisible = true
            }
        )
    }

    if (showSetPasswordDialog) {
        SetPasswordDialog(
            onDismiss = { showSetPasswordDialog = false },
            onSetPassword = { hash ->
                coroutineScope.launch {
                    preferencesManager.setPasswordHash(hash)
                    showSetPasswordDialog = false
                }
            }
        )
    }

    if (showChangePasswordDialog && passwordHash.isNotBlank()) {
        ChangePasswordDialog(
            storedHash = passwordHash,
            onDismiss = { showChangePasswordDialog = false },
            onChangePassword = { hash ->
                coroutineScope.launch {
                    preferencesManager.setPasswordHash(hash)
                    showChangePasswordDialog = false
                }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun WeekStartPicker(
    selectedDay: Int,
    coroutineScope: CoroutineScope,
    preferencesManager: PreferencesManager
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Week Starts On",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dayLabel(selectedDay),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = dayLabel(selectedDay),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                weekDayOptions().forEach { day ->
                    DropdownMenuItem(
                        text = { Text(text = dayLabel(day)) },
                        onClick = {
                            expanded = false
                            coroutineScope.launch {
                                preferencesManager.setWeekStartDay(day)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun weekDayOptions(): List<Int> = listOf(
    Calendar.SUNDAY,
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY
)

internal fun dayLabel(day: Int): String = when (day) {
    Calendar.SUNDAY -> "Sunday"
    Calendar.MONDAY -> "Monday"
    Calendar.TUESDAY -> "Tuesday"
    Calendar.WEDNESDAY -> "Wednesday"
    Calendar.THURSDAY -> "Thursday"
    Calendar.FRIDAY -> "Friday"
    Calendar.SATURDAY -> "Saturday"
    else -> "Sunday"
}
