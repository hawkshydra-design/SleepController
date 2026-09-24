package com.sleepcontroller.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.sleepcontroller.data.db.dao.BlockedAppDao
import com.sleepcontroller.data.db.entity.BlockedApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean,
    val isPermanentlyWhitelisted: Boolean,
    val category: String
)

@Singleton
class AppBlockRepository @Inject constructor(
    private val blockedAppDao: BlockedAppDao,
    private val context: Context
) {
    companion object {
        // These packages are NEVER blocked
        val PERMANENTLY_WHITELISTED = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.phone",
            "com.android.contacts",
            "com.google.android.contacts",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.settings",
            "com.android.emergency",
            "com.google.android.gms",           // Google Play Services
            "com.sleepcontroller"               // This app itself
        )

        val SOCIAL_MEDIA_PACKAGES = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.snapchat.android",
            "com.zhiliaoapp.musically",  // TikTok
            "com.reddit.frontpage",
            "com.linkedin.android",
            "org.telegram.messenger",
            "com.whatsapp"
        )

        val GAME_CATEGORIES = setOf(
            "game"
        )
    }

    fun getBlockedApps(): Flow<List<BlockedApp>> = blockedAppDao.getBlockedApps()

    fun getAllApps(): Flow<List<BlockedApp>> = blockedAppDao.getAllApps()

    suspend fun getBlockedPackageNames(): List<String> = blockedAppDao.getBlockedPackageNames()

    suspend fun isAppBlocked(packageName: String): Boolean = blockedAppDao.isAppBlocked(packageName)

    suspend fun setAppBlocked(packageName: String, blocked: Boolean) =
        blockedAppDao.setBlocked(packageName, blocked)

    suspend fun blockByCategory(category: String) = blockedAppDao.blockByCategory(category)

    suspend fun blockAll() = blockedAppDao.blockAll()

    suspend fun unblockAll() = blockedAppDao.unblockAll()

    suspend fun syncInstalledApps() {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)

        val apps = resolvedInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(pm).toString()
            val isPermanentlyWhitelisted = packageName in PERMANENTLY_WHITELISTED
            val category = categorizeApp(packageName)

            BlockedApp(
                packageName = packageName,
                appName = appName,
                isBlocked = !isPermanentlyWhitelisted, // Block by default except whitelisted
                isPermanentlyWhitelisted = isPermanentlyWhitelisted,
                category = category
            )
        }

        blockedAppDao.insertAll(apps)
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)

        return resolvedInfos.map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val isPermanentlyWhitelisted = packageName in PERMANENTLY_WHITELISTED
            InstalledAppInfo(
                packageName = packageName,
                appName = resolveInfo.loadLabel(pm).toString(),
                icon = resolveInfo.loadIcon(pm),
                isBlocked = false,
                isPermanentlyWhitelisted = isPermanentlyWhitelisted,
                category = categorizeApp(packageName)
            )
        }.sortedBy { it.appName }
    }

    private fun categorizeApp(packageName: String): String {
        return when {
            packageName in SOCIAL_MEDIA_PACKAGES -> "social"
            packageName.contains("game", ignoreCase = true) -> "games"
            packageName.contains("video") || packageName.contains("music") ||
                packageName.contains("youtube") || packageName.contains("netflix") ||
                packageName.contains("spotify") -> "media"
            else -> "other"
        }
    }
}
