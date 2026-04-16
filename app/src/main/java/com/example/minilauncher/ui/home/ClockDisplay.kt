package com.example.minilauncher.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ClockDisplay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    val is24HourFormat = DateFormat.is24HourFormat(context)

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            val nowMillis = System.currentTimeMillis()
            val delayMillis = (60_000L - (nowMillis % 60_000L)).coerceAtLeast(1L)
            delay(delayMillis)
        }
    }

    val timeFormatter = remember(is24HourFormat, locale) {
        DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "h:mm a", locale)
    }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = currentDateTime.format(timeFormatter),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = currentDateTime.format(dateFormatter),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
