package com.example.minilauncher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.minilauncher.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLongPressMenu(
    appInfo: AppInfo,
    showDelete: Boolean,
    isPinned: Boolean,
    isUsageHidden: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onPinToggle: () -> Unit,
    onUsageToggle: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = appInfo.label,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (showDelete) {
                MenuAction(text = "Delete", onClick = onDelete)
            }
            MenuAction(text = "Hide", onClick = onHide)
            MenuAction(
                text = if (isPinned) "Remove from Home" else "Pin to Home",
                onClick = onPinToggle
            )
            MenuAction(
                text = if (isUsageHidden) "Show App Usage" else "Hide App Usage",
                onClick = onUsageToggle
            )
        }
    }
}

@Composable
private fun MenuAction(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
    )
}
