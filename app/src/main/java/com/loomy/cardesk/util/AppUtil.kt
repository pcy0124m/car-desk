package com.loomy.cardesk.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri

/** 已安装应用信息 */
data class AppInfo(
    val pkg: String,
    val name: String,
    val icon: Drawable?,
    val launchIntent: Intent?
)

/** 应用查询 / 启动 / 导航工具 */
object AppUtil {

    /** 列出所有可启动的应用（默认排除自己，避免桌面套娃） */
    fun installedApps(context: Context, excludeSelf: Boolean = true): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveList.mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            if (excludeSelf && pkg == context.packageName) return@mapNotNull null
            AppInfo(
                pkg = pkg,
                name = ri.loadLabel(pm).toString(),
                icon = ri.loadIcon(pm),
                launchIntent = pm.getLaunchIntentForPackage(pkg)
            )
        }.distinctBy { it.pkg }.sortedBy { it.name }
    }

    /** 启动一个应用，成功返回 true */
    fun launchApp(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun isInstalled(context: Context, pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun appName(context: Context, pkg: String?): String? {
        if (pkg.isNullOrBlank()) return null
        return try {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun appIcon(context: Context, pkg: String?): Drawable? {
        if (pkg.isNullOrBlank()) return null
        return try {
            context.packageManager.getApplicationIcon(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 用 geo: URI 唤起地图应用导航到目标（地址或坐标）。
     * 高德/百度/腾讯地图都会处理这个 Intent，选择权交给系统。
     */
    fun navigateTo(context: Context, query: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 没有应用处理 geo:，静默失败（车机上概率低）
        }
    }
}
