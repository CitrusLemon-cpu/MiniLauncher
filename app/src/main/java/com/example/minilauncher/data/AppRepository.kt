package com.example.minilauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import com.example.minilauncher.model.AppInfo
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository private constructor(
    private val context: Context
) {
    private val packageManager = context.packageManager
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshApps()
        }
    }

    init {
        registerPackageChangeReceiver()
        refreshApps()
    }

    fun refreshApps() {
        _apps.value = queryInstalledApps()
    }

    fun getAppInfo(packageName: String): AppInfo? {
        _apps.value.firstOrNull { it.packageName == packageName }?.let { return it }
        return runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            AppInfo(
                packageName = packageName,
                label = packageManager.getApplicationLabel(applicationInfo).toString(),
                icon = packageManager.getApplicationIcon(packageName)
            )
        }.getOrNull()
    }

    fun launchApp(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun queryInstalledApps(): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }

        return resolveInfos
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                if (packageName == context.packageName) {
                    return@mapNotNull null
                }
                AppInfo(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(packageManager)?.toString()?.ifBlank { packageName } ?: packageName,
                    icon = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun registerPackageChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(packageChangeReceiver, filter)
        }
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
