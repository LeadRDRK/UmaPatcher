package com.leadrdrk.umapatcher.utils

import androidx.documentfile.provider.DocumentFile

fun DocumentFile.deleteRecursive(): Boolean {
    if (isDirectory) {
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