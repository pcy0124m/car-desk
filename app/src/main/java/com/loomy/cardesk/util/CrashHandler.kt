package com.loomy.cardesk.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃捕获器 + 启动日志（双保险）：
 *
 * 1. crash.log：未捕获异常堆栈，双写到 内部私有目录 和 外部存储
 *    - /data/data/com.loomy.cardesk/files/crash.log   （需 root，MT管理器可读）
 *    - /sdcard/Android/data/com.loomy.cardesk/files/crash.log
 * 2. boot.log：每次启动逐步记录执行到哪一步，即使不崩溃也能定位卡点
 *    - 同样双写
 */
object CrashHandler {

    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeLog("crash.log") { w ->
                w.appendLine("==== CRASH ${now()} ====")
                w.appendLine("Thread: ${thread.name}")
                w.appendLine(Log.getStackTraceString(throwable))
                w.appendLine("")
            }
            default?.uncaughtException(thread, throwable)
        }
    }

    /** 启动/步骤标记：每次启动覆盖写入，记录执行进度 */
    fun boot(context: Context, step: String) {
        val ctx = appContext ?: context.applicationContext
        writeLog("boot.log") { w ->
            w.appendLine("${now()} [${step}]")
        }
    }

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

    private fun writeLog(fileName: String, block: (FileWriter) -> Unit) {
        val ctx = appContext ?: return
        // 内部目录（一定可写，需 root 查看）
        try {
            val dir = ctx.filesDir
            dir.mkdirs()
            block(FileWriter(File(dir, fileName), true).buffered())
        } catch (e: Exception) {
        }
        // 外部目录（车机上 MT管理器直接可看）
        try {
            val dir = ctx.getExternalFilesDir(null)
            if (dir != null) {
                dir.mkdirs()
                block(FileWriter(File(dir, fileName), true).buffered())
            }
        } catch (e: Exception) {
        }
    }
}
