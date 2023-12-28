package com.leadrdrk.umapatcher.patcher

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.GameChecker
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

abstract class Patcher(
    private var onLog: (String) -> Unit = {},
    private var onProgress: (Float) -> Unit = {},
    private var onTask: (String) -> Unit = {},
    private var onSaveFile: (String, File, () -> Unit) -> Unit = { _: String, _: File, _: () -> Unit -> }
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
        onSaveFile: (String, File, () -> Unit) -> Unit
    ) {
        this.onLog = onLog
        this.onProgress = onProgress
        this.onTask = onTask
        this.onSaveFile = onSaveFile
    }

    abstract fun run(context: Context): Boolean

    protected fun log(line: String) {
        onLog(line)
    }

    protected fun logAll(lines: Iterable<String>) {
        lines.forEach(onLog)
    }

    protected fun saveFile(filename: String, file: File, callback: () -> Unit = {}) {
        onSaveFile(filename, file, callback)
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
        if (!testDirectory(GameChecker.filesDir.resolve("dat").path)) {
            log(context.getString(R.string.game_data_not_found))
            return false
        }
        return true
    }

    protected fun openDatabase(path: String, flags: Int = SQLiteDatabase.OPEN_READWRITE): SQLiteDatabase? {
        return try {
            SQLiteDatabase.openDatabase(path, null, flags)
        }
        catch (e: SQLiteException) {
            val sw = StringWriter()
            PrintWriter(sw).use {
                e.printStackTrace(it)
            }
            log(sw.toString())
            null
        }
    }
}