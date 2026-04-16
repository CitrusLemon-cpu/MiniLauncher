package com.example.minilauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

class UsageRepository private constructor(private val context: Context) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val ownPackageName = context.packageName

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsage(): Map<String, Long> = getUsageForDay(LocalDate.now())

    fun getUsageForDay(date: LocalDate): Map<String, Long> {
        val zoneId = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = minOf(
            date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            System.currentTimeMillis()
        )
        return queryUsage(startMillis = startMillis, endMillis = endMillis)
    }

    fun getUsageForRange(start: LocalDate, end: LocalDate): Map<String, Long> {
        if (end.isBefore(start)) {
            return emptyMap()
        }

        val aggregated = mutableMapOf<String, Long>()
        var current = start
        while (!current.isAfter(end)) {
            getUsageForDay(current).forEach { (packageName, duration) ->
                aggregated[packageName] = (aggregated[packageName] ?: 0L) + duration
            }
            current = current.plusDays(1)
        }
        return aggregated.filterValues { duration -> duration > 0L }
    }

    private fun queryUsage(startMillis: Long, endMillis: Long): Map<String, Long> {
        if (!hasUsagePermission() || endMillis <= startMillis) {
            return emptyMap()
        }

        return runCatching {
            usageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMillis, endMillis)
                .orEmpty()
                .asSequence()
                .filter { stat -> stat.packageName != ownPackageName }
                .groupBy { stat -> stat.packageName }
                .mapValues { (_, usageStats) ->
                    usageStats.sumOf { stat -> stat.totalTimeInForeground }
                }
                .filterValues { duration -> duration > 0L }
        }.getOrDefault(emptyMap())
    }

    companion object {
        @Volatile
        private var instance: UsageRepository? = null

        fun getInstance(context: Context): UsageRepository {
            return instance ?: synchronized(this) {
                instance ?: UsageRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
