package com.example.minilauncher.util

fun formatDuration(millis: Long): String {
    val hours = millis / 3_600_000
    val minutes = (millis % 3_600_000) / 60_000
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        millis > 0 -> "<1m"
        else -> "0m"
    }
}
