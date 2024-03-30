package com.leadrdrk.umapatcher.patcher

import android.content.Context
import android.util.Log
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.GameChecker
import com.leadrdrk.umapatcher.utils.copyTo
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

abstract class Patcher(
    private var onLog: (String) -> Unit = {},
    private var onProgress: (Float) -> Unit = {},
    private var onTask: (String) -> Unit = {},
    private var onSaveFile: (String, File, (Boolean) -> Unit) -> Unit = { _: String, _: File, _: (Boolean) -> Unit -> }
) {
    var progress: Float = 0f
        set(value) {
            field = value
            onProgress(value)
        }

    var task: String = ""
        set(value) {
            field = value
            onTask(value)
        }

    fun setCallbacks(
        onLog: (String) -> Unit = {},
        onProgress: (Float) -> Unit = {},
        onTask: (String) -> Unit = {},
        onSaveFile: (String, File, (Boolean) -> Unit) -> Unit
    ) {
        this.onLog = onLog
        this.onProgress = onProgress
        this.onTask = onTask
        this.onSaveFile = onSaveFile
    }

    abstract fun run(context: Context): Boolean

    fun safeRun(context: Context): Boolean {
        return try {
            run(context)
        } catch (ex: Exception) {
            logException(ex)
            false
        }
    }

    protected fun log(line: String) {
        onLog(line)
    }

    protected fun logAll(lines: Iterable<String>) {
        lines.forEach(onLog)
    }

    protected fun logException(ex: Exception) {
        val sw = StringWriter()
        PrintWriter(sw).use {
            ex.printStackTrace(it)
        }
        log(sw.toString())
        Log.e("UmaPatcher", "logException", ex)
    }

    protected fun saveFile(filename: String, file: File, callback: (Boolean) -> Unit = {}) {
        onSaveFile(filename, file, callback)
    }

    protected fun copyFileProgress(srcFile: File, dstFile: File) {
        srcFile.inputStream().use { input ->
            dstFile.outputStream().use { output ->
                val length = srcFile.length().toFloat()
                input.copyTo(output) { current ->
                    progress = current / length
                }
            }
        }
    }

    protected fun isDirectInstallAllowed(context: Context): Boolean {
        if (!GameChecker.isPackageInstalled(context.packageManager)) {
            log(context.getString(R.string.direct_install_unavailable))
            return false
        }
        if (Shell.isAppGrantedRoot() != true) {
            log(context.getString(R.string.root_required))
            return false
        }
        return true
    }
}