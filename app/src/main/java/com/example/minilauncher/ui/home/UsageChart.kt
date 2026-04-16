package com.example.minilauncher.ui.home

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository
import com.example.minilauncher.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class UsageBarItem(
    val packageName: String,
    val label: String,
    val duration: Long,
    val icon: Drawable?
)

private data class UsageChartState(
    val primaryItems: List<UsageBarItem>,
    val otherItems: List<UsageBarItem>,
    val totalUsage: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageChart(
    appRepository: AppRepository,
    preferencesManager: PreferencesManager,
    usageRepository: UsageRepository,
    modifier: Modifier = Modifier
) {
    val hiddenUsageApps by preferencesManager.hiddenUsageApps.collectAsStateWithLifecycle(initialValue = emptySet())
    var refreshToken by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var chartState by remember { mutableStateOf(UsageChartState(emptyList(), emptyList(), 0L)) }
    var showOtherSheet by remember { mutableStateOf(false) }
    val barColors = rememberUsageBarColors()

    LifecycleResumeEffect(hiddenUsageApps, usageRepository) {
        refreshToken++
        onPauseOrDispose { }
    }

    LaunchedEffect(hiddenUsageApps, refreshToken) {
        isLoading = true
        chartState = withContext(Dispatchers.IO) {
            buildUsageChartState(
                appRepository = appRepository,
                usageRepository = usageRepository,
                hiddenUsageApps = hiddenUsageApps
            )
        }
        isLoading = false
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            when {
                isLoading -> UsageChartLoadingState(modifier = Modifier.weight(1f, fill = false))
                chartState.totalUsage == 0L -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No screen time recorded yet today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    val allRows = remember(chartState.primaryItems, chartState.otherItems) {
                        buildList {
                            addAll(chartState.primaryItems)
                            if (chartState.otherItems.isNotEmpty()) {
                                add(
                                    UsageBarItem(
                                        packageName = "other",
                                        label = "Other",
                                        duration = chartState.otherItems.sumOf { it.duration },
                                        icon = null
                                    )
                                )
                            }
                        }
                    }
                    val maxDuration = allRows.maxOfOrNull { it.duration } ?: 0L

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allRows.forEachIndexed { index, item ->
                            val isOtherRow = item.packageName == "other"
                            UsageBarRow(
                                item = item,
                                fraction = normalizedDuration(item.duration, maxDuration),
                                color = if (isOtherRow) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                } else {
                                    barColors[index % barColors.size]
                                },
                                showIcon = !isOtherRow,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = if (isOtherRow && chartState.otherItems.isNotEmpty()) {
                                    { showOtherSheet = true }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Total: ${formatDuration(chartState.totalUsage)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showOtherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOtherSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Other apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Remaining usage for today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = chartState.otherItems,
                        key = { item -> item.packageName }
                    ) { item ->
                        UsageBarRow(
                            item = item,
                            fraction = normalizedDuration(
                                duration = item.duration,
                                maxDuration = chartState.otherItems.maxOfOrNull { other -> other.duration } ?: 0L
                            ),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
internal fun UsageBarRow(
    item: UsageBarItem,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.32f))
            .let { baseModifier ->
                if (onClick != null) {
                    baseModifier.clickable(onClick = onClick)
                } else {
                    baseModifier
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                UsageAppIcon(
                    drawable = item.icon,
                    label = item.label,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1.1f)
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = formatDuration(item.duration),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 52.dp)
        )
    }
}

@Composable
private fun UsageChartLoadingState(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "usage-chart-loading")
    val alpha by shimmer.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "usage-chart-loading-alpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun UsageAppIcon(
    drawable: Drawable?,
    label: String,
    modifier: Modifier = Modifier
) {
    if (drawable == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
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
        modifier = modifier.clip(RoundedCornerShape(10.dp))
    )
}

@Composable
private fun rememberUsageBarColors(): List<Color> {
    val colorScheme = MaterialTheme.colorScheme
    return remember(colorScheme) {
        listOf(
            colorScheme.tertiary.copy(alpha = 0.95f),
            colorScheme.tertiary.copy(alpha = 0.8f),
            colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
            colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            colorScheme.surfaceVariant.copy(alpha = 0.95f)
        )
    }
}

private fun normalizedDuration(duration: Long, maxDuration: Long): Float {
    if (duration <= 0L || maxDuration <= 0L) {
        return 0f
    }
    return (duration.toFloat() / maxDuration.toFloat()).coerceIn(0.08f, 1f)
}

private fun buildUsageChartState(
    appRepository: AppRepository,
    usageRepository: UsageRepository,
    hiddenUsageApps: Set<String>
): UsageChartState {
    val usageEntries = usageRepository.getTodayUsage()
        .filterKeys { packageName -> packageName !in hiddenUsageApps }
        .toList()
        .sortedByDescending { (_, duration) -> duration }

    val topItems = usageEntries.take(5).map { (packageName, duration) ->
        packageToUsageBarItem(appRepository, packageName, duration)
    }
    val otherItems = usageEntries.drop(5).map { (packageName, duration) ->
        packageToUsageBarItem(appRepository, packageName, duration)
    }

    return UsageChartState(
        primaryItems = topItems,
        otherItems = otherItems,
        totalUsage = usageEntries.sumOf { (_, duration) -> duration }
    )
}

internal fun packageToUsageBarItem(
    appRepository: AppRepository,
    packageName: String,
    duration: Long
): UsageBarItem {
    val appInfo = appRepository.getAppInfo(packageName)
    return UsageBarItem(
        packageName = packageName,
        label = appInfo?.label ?: packageName,
        duration = duration,
        icon = appInfo?.icon
    )
}
