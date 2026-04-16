package com.example.minilauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.minilauncher.data.AppRepository
import com.example.minilauncher.data.PreferencesManager
import com.example.minilauncher.data.UsageRepository
import com.example.minilauncher.ui.home.UsageBarItem
import com.example.minilauncher.ui.home.UsageBarRow
import com.example.minilauncher.ui.home.UsagePermissionPrompt
import com.example.minilauncher.ui.home.packageToUsageBarItem
import com.example.minilauncher.util.formatDuration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class UsageHistoryDetailState(
    val selectedDate: LocalDate,
    val dayEntries: List<UsageBarItem>,
    val dayTotal: Long,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val weekEntries: List<UsageBarItem>,
    val weekTotal: Long
)

@Composable
fun UsageHistoryCalendar(
    appRepository: AppRepository,
    usageRepository: UsageRepository,
    preferencesManager: PreferencesManager,
    hiddenUsageApps: Set<String>,
    weekStartDay: Int,
    showIcons: Boolean,
    showZeroMinuteApps: Boolean,
    modifier: Modifier = Modifier
) {
    var hasUsagePermission by remember(usageRepository) {
        mutableStateOf(usageRepository.hasUsagePermission())
    }
    var refreshToken by remember { mutableIntStateOf(0) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var monthUsage by remember { mutableStateOf<Map<LocalDate, Long>>(emptyMap()) }
    var detailState by remember {
        mutableStateOf(
            UsageHistoryDetailState(
                selectedDate = selectedDate,
                dayEntries = emptyList(),
                dayTotal = 0L,
                weekStart = selectedDate,
                weekEnd = selectedDate,
                weekEntries = emptyList(),
                weekTotal = 0L
            )
        )
    }
    var isMonthLoading by remember { mutableStateOf(true) }
    var isDetailLoading by remember { mutableStateOf(true) }
    var pendingHideUsageItem by remember { mutableStateOf<UsageBarItem?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(usageRepository, hiddenUsageApps, weekStartDay) {
        hasUsagePermission = usageRepository.hasUsagePermission()
        refreshToken++
        onPauseOrDispose { }
    }

    if (!hasUsagePermission) {
        UsagePermissionPrompt(
            usageRepository = usageRepository,
            modifier = modifier.height(260.dp),
            onPermissionChanged = { granted ->
                hasUsagePermission = granted
                if (granted) {
                    refreshToken++
                }
            }
        )
        return
    }

    LaunchedEffect(displayedMonth, hiddenUsageApps, refreshToken) {
        isMonthLoading = true
        monthUsage = withContext(Dispatchers.IO) {
            buildMonthUsageMap(
                usageRepository = usageRepository,
                displayedMonth = displayedMonth,
                hiddenUsageApps = hiddenUsageApps
            )
        }
        isMonthLoading = false
    }

    LaunchedEffect(selectedDate, hiddenUsageApps, weekStartDay, refreshToken) {
        isDetailLoading = true
        detailState = withContext(Dispatchers.IO) {
            buildUsageHistoryDetailState(
                appRepository = appRepository,
                usageRepository = usageRepository,
                selectedDate = selectedDate,
                hiddenUsageApps = hiddenUsageApps,
                weekStartDay = weekStartDay
            )
        }
        isDetailLoading = false
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CalendarHeader(
                displayedMonth = displayedMonth,
                onPreviousMonth = {
                    val newMonth = displayedMonth.minusMonths(1)
                    displayedMonth = newMonth
                    selectedDate = selectedDateInMonth(
                        currentDate = selectedDate,
                        targetMonth = newMonth
                    )
                },
                onNextMonth = {
                    val newMonth = displayedMonth.plusMonths(1)
                    displayedMonth = newMonth
                    selectedDate = selectedDateInMonth(
                        currentDate = selectedDate,
                        targetMonth = newMonth
                    )
                }
            )

            CalendarWeekHeaders(weekStartDay = weekStartDay)

            if (isMonthLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading usage history…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                CalendarMonthGrid(
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    weekStartDay = weekStartDay,
                    monthUsage = monthUsage,
                    onDateSelected = { selectedDate = it }
                )
            }

            if (isDetailLoading) {
                Text(
                    text = "Loading day breakdown…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DayBreakdownSection(
                    detailState = detailState,
                    showIcons = showIcons,
                    showZeroMinuteApps = showZeroMinuteApps,
                    onLongPressItem = { pendingHideUsageItem = it }
                )
                WeekSummarySection(
                    detailState = detailState,
                    showIcons = showIcons,
                    showZeroMinuteApps = showZeroMinuteApps,
                    onLongPressItem = { pendingHideUsageItem = it }
                )
            }
        }
    }

    pendingHideUsageItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingHideUsageItem = null },
            title = { Text("Hide Usage") },
            text = { Text("Hide ${item.label} from the usage breakdown?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setHiddenUsageApp(item.packageName, true)
                        }
                        pendingHideUsageItem = null
                    }
                ) {
                    Text("Hide")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHideUsageItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CalendarHeader(
    displayedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonthNavButton(label = "‹", onClick = onPreviousMonth)
        Text(
            text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        MonthNavButton(label = "›", onClick = onNextMonth)
    }
}

@Composable
private fun MonthNavButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CalendarWeekHeaders(weekStartDay: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        orderedWeekDays(weekStartDay).forEach { day ->
            Text(
                text = dayLabel(day).take(3),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    weekStartDay: Int,
    monthUsage: Map<LocalDate, Long>,
    onDateSelected: (LocalDate) -> Unit
) {
    val cells = remember(displayedMonth, weekStartDay, monthUsage) {
        buildCalendarCells(displayedMonth = displayedMonth, weekStartDay = weekStartDay)
    }
    val maxUsage = monthUsage.values.maxOrNull() ?: 0L

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(288.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled = false
    ) {
        items(cells.size, key = { index -> cells[index]?.toString() ?: "empty-$index" }) { index ->
            val date = cells[index]
            if (date == null) {
                Box(modifier = Modifier.size(36.dp))
            } else {
                val dayUsage = monthUsage[date] ?: 0L
                val isSelected = date == selectedDate
                val heatColor = when {
                    dayUsage <= 0L -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    maxUsage <= 0L -> MaterialTheme.colorScheme.surfaceVariant
                    else -> {
                        val ratio = (dayUsage.toFloat() / maxUsage.toFloat()).coerceIn(0f, 1f)
                        lerp(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.tertiary,
                            0.18f + (ratio * 0.82f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(heatColor)
                        .let { baseModifier ->
                            if (isSelected) {
                                baseModifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                baseModifier
                            }
                        }
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBreakdownSection(
    detailState: UsageHistoryDetailState,
    showIcons: Boolean,
    showZeroMinuteApps: Boolean,
    onLongPressItem: (UsageBarItem) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    var isExpanded by remember(detailState.selectedDate) { mutableStateOf(false) }
    val filteredEntries = remember(detailState.dayEntries, showZeroMinuteApps) {
        if (showZeroMinuteApps) {
            detailState.dayEntries
        } else {
            detailState.dayEntries.filter { it.duration >= 60_000L }
        }
    }
    val visibleEntries = if (isExpanded) filteredEntries else filteredEntries.take(10)
    val hasMore = filteredEntries.size > 10 && !isExpanded

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = detailState.selectedDate.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Total: ${formatDuration(detailState.dayTotal)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (filteredEntries.isEmpty()) {
            Text(
                text = "No usage recorded for this day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxDuration = filteredEntries.maxOfOrNull { it.duration } ?: 0L
            visibleEntries.forEach { item ->
                UsageBarRow(
                    item = item,
                    fraction = normalizedDuration(item.duration, maxDuration),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                    showIcon = showIcons,
                    modifier = Modifier.fillMaxWidth(),
                    onLongPress = { onLongPressItem(item) }
                )
            }

            if (hasMore) {
                TextButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Show ${filteredEntries.size - 10} more apps")
                }
            }
        }
    }
}

@Composable
private fun WeekSummarySection(
    detailState: UsageHistoryDetailState,
    showIcons: Boolean,
    showZeroMinuteApps: Boolean,
    onLongPressItem: (UsageBarItem) -> Unit
) {
    val rangeFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val filteredEntries = remember(detailState.weekEntries, showZeroMinuteApps) {
        if (showZeroMinuteApps) {
            detailState.weekEntries
        } else {
            detailState.weekEntries.filter { it.duration >= 60_000L }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "This Week",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${detailState.weekStart.format(rangeFormatter)} - ${detailState.weekEnd.format(rangeFormatter)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Total: ${formatDuration(detailState.weekTotal)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (filteredEntries.isEmpty()) {
            Text(
                text = "No usage recorded for this week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxDuration = filteredEntries.maxOfOrNull { it.duration } ?: 0L
            filteredEntries.take(5).forEach { item ->
                UsageBarRow(
                    item = item,
                    fraction = normalizedDuration(item.duration, maxDuration),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    showIcon = showIcons,
                    modifier = Modifier.fillMaxWidth(),
                    onLongPress = { onLongPressItem(item) }
                )
            }
        }
    }
}

private fun buildMonthUsageMap(
    usageRepository: UsageRepository,
    displayedMonth: YearMonth,
    hiddenUsageApps: Set<String>
): Map<LocalDate, Long> {
    return (1..displayedMonth.lengthOfMonth()).associate { dayOfMonth ->
        val date = displayedMonth.atDay(dayOfMonth)
        val total = usageRepository.getUsageForDay(date)
            .filterKeys { packageName -> packageName !in hiddenUsageApps }
            .values
            .sum()
        date to total
    }
}

private fun buildUsageHistoryDetailState(
    appRepository: AppRepository,
    usageRepository: UsageRepository,
    selectedDate: LocalDate,
    hiddenUsageApps: Set<String>,
    weekStartDay: Int
): UsageHistoryDetailState {
    val dayUsage = usageRepository.getUsageForDay(selectedDate)
        .filterKeys { packageName -> packageName !in hiddenUsageApps }
        .toList()
        .sortedByDescending { (_, duration) -> duration }
    val weekStart = startOfWeek(selectedDate, weekStartDay)
    val weekEnd = weekStart.plusDays(6)
    val weekUsage = usageRepository.getUsageForRange(weekStart, weekEnd)
        .filterKeys { packageName -> packageName !in hiddenUsageApps }
        .toList()
        .sortedByDescending { (_, duration) -> duration }

    return UsageHistoryDetailState(
        selectedDate = selectedDate,
        dayEntries = dayUsage.map { (packageName, duration) ->
            packageToUsageBarItem(appRepository, packageName, duration)
        },
        dayTotal = dayUsage.sumOf { (_, duration) -> duration },
        weekStart = weekStart,
        weekEnd = weekEnd,
        weekEntries = weekUsage.map { (packageName, duration) ->
            packageToUsageBarItem(appRepository, packageName, duration)
        },
        weekTotal = weekUsage.sumOf { (_, duration) -> duration }
    )
}

private fun buildCalendarCells(
    displayedMonth: YearMonth,
    weekStartDay: Int
): List<LocalDate?> {
    val firstDay = displayedMonth.atDay(1)
    val leadingEmptyDays = orderedWeekDays(weekStartDay).indexOf(firstDay.toCalendarDay())
        .coerceAtLeast(0)
    val dates = List(leadingEmptyDays) { null } + (1..displayedMonth.lengthOfMonth()).map(displayedMonth::atDay)
    val trailingEmptyDays = (7 - (dates.size % 7)).takeIf { it < 7 } ?: 0
    return dates + List(trailingEmptyDays) { null }
}

private fun orderedWeekDays(weekStartDay: Int): List<Int> {
    val days = listOf(
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY
    )
    val startIndex = days.indexOf(weekStartDay).takeIf { it >= 0 } ?: 0
    return (days.indices).map { index -> days[(startIndex + index) % days.size] }
}

private fun LocalDate.toCalendarDay(): Int = when (dayOfWeek.value) {
    1 -> Calendar.MONDAY
    2 -> Calendar.TUESDAY
    3 -> Calendar.WEDNESDAY
    4 -> Calendar.THURSDAY
    5 -> Calendar.FRIDAY
    6 -> Calendar.SATURDAY
    else -> Calendar.SUNDAY
}

private fun startOfWeek(date: LocalDate, weekStartDay: Int): LocalDate {
    val offset = orderedWeekDays(weekStartDay).indexOf(date.toCalendarDay()).coerceAtLeast(0)
    return date.minusDays(offset.toLong())
}

private fun selectedDateInMonth(
    currentDate: LocalDate,
    targetMonth: YearMonth
): LocalDate {
    return targetMonth.atDay(minOf(currentDate.dayOfMonth, targetMonth.lengthOfMonth()))
}

private fun normalizedDuration(duration: Long, maxDuration: Long): Float {
    if (duration <= 0L || maxDuration <= 0L) {
        return 0f
    }
    return (duration.toFloat() / maxDuration.toFloat()).coerceIn(0.08f, 1f)
}
