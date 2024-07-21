package com.leadrdrk.umapatcher.utils

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File

val Context.workDir: File
    get() = cacheDir.resolve("work")

val Context.repoDir: File
    get() = filesDir.resolve("repo")

val Context.ksFile: File
    get() = filesDir.resolve("keystore.bks")

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun Context.showToast(text: String, duration: Int) {
    getActivity()?.runOnUiThread {
        Toast.makeText(this, text, duration).show()
    }
}