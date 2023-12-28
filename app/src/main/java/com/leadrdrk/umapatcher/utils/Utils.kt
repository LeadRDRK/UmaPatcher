package com.leadrdrk.umapatcher.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun safeNavigate(lifecycleOwner: LifecycleOwner, callback: () -> Unit) {
    val currentState = lifecycleOwner.lifecycle.currentState
    if (currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        callback()
    }
}

fun deleteRecursive(fileOrDirectory: File, deleteRoot: Boolean = true) {
    if (fileOrDirectory.isDirectory)
        for (child in fileOrDirectory.listFiles()!!)
            deleteRecursive(child)

    if (deleteRoot) fileOrDirectory.delete()
}

fun unixTimestampToString(time: Int) = unixTimestampToString(time.toLong())
fun unixTimestampToString(time: Long): String {
    val d = Date(time * 1000L)
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(d)
}