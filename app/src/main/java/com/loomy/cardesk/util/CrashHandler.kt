package com.loomy.cardesk.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃捕获器（黑匣子）：
 * 任何未捕获异常都会把完整堆栈写到
 *   /sdcard/Android/data/com.loomy.cardesk/files/crash.log
 * 车机上用 MT管理器 / 文件管理器即可打开查看，方便快速定位闪退原因。
 */
object CrashHandler {

    fun install(context: Context) {
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = context.getExternalFilesDir(null)
                if (dir != null) {
                    dir.mkdirs()
                    val logFile = File(dir, "crash.log")
                    FileWriter(logFile, true).use { w ->
                        w.appendLine("==== ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} ====")
                        w.appendLine("Thread: ${thread.name}")
                        w.appendLine(Log.getStackTraceString(throwable))
                        w.appendLine("")
                    }
                }
            } catch (e: Exception) {
                // 写日志失败也不影响原崩溃流程
            }
            default?.uncaughtException(thread, throwable)
        }
    }
}
