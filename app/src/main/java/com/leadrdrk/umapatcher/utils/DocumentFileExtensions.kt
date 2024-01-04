package com.leadrdrk.umapatcher.utils

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

fun DocumentFile.deleteRecursive(): Boolean {
    if (isDirectory) {
        Log.d("dir", uri.toString())
        listFiles().forEach {
            if (!it.deleteRecursive()) return false
        }
    }
    return delete()
}

fun DocumentFile.createDirectoryOverwrite(name: String): DocumentFile? {
    val dir = findFile(name) ?: return createDirectory(name)
    if (dir.isFile) {
        dir.delete()
        return createDirectory(name)
    }
    return dir
}

fun DocumentFile.createFileOverwrite(name: String): DocumentFile? {
    val file = findFile(name) ?: return createFile("*/*", name)
    if (file.isDirectory) {
        file.deleteRecursive()
        return createFile("*/*", name)
    }
    return file
}