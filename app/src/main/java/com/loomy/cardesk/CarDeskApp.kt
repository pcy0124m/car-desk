package com.loomy.cardesk

import android.app.Application
import com.loomy.cardesk.util.CrashHandler

/**
 * 应用入口：最早时机安装崩溃黑匣子。
 * 这样即使在 Activity 创建前的极早期崩溃，也能留下堆栈。
 */
class CarDeskApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        CrashHandler.boot(this, "Application.onCreate")
    }
}
