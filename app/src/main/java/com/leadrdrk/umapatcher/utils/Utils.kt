package com.leadrdrk.umapatcher.utils

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import net.lingala.zip4j.ZipFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
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

fun copyDirectory(
    context: Context,
    sourceLocation: File,
    targetLocation: DocumentFile,
    logParentPaths: String = "",
    logCallback: (String) -> Unit = {}
): Boolean {
    if (sourceLocation.isDirectory) {
        val children = sourceLocation.list() ?: return false
        children.forEach { child ->
            val childSourceLocation = File(sourceLocation, child)
            val childTargetLocation = if (childSourceLocation.isDirectory)
                targetLocation.createDirectoryOverwrite(child) else
                targetLocation.createFileOverwrite(child)
            if (childTargetLocation == null) return false

            val newParentPaths = if (childSourceLocation.isDirectory)
                "$logParentPaths/$child" else logParentPaths

            if (!copyDirectory(context, childSourceLocation, childTargetLocation, newParentPaths, logCallback))
                return false
        }
    } else {
        logCallback("$logParentPaths/${sourceLocation.name}")
        try {
            sourceLocation.inputStream().use { input ->
                context.contentResolver.openOutputStream(targetLocation.uri).use { output ->
                    if (output == null) return false
                    input.copyTo(output)
                }
            }
        }
        catch (ex: Exception) {
            Log.e("UmaPatcher", "copyDirectory", ex)
            return false
        }
    }
    return true
}

fun unixTimestampToString(time: Int) = unixTimestampToString(time.toLong())
fun unixTimestampToString(time: Long): String {
    val d = Date(time * 1000L)
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(d)
}

fun downloadFile(url: URL, outputFile: File) {
    val conn = url.openConnection() as HttpURLConnection
    conn.doInput = true
    conn.instanceFollowRedirects = true
    conn.connect()

    if (conn.responseCode != 200) throw IOException()

    conn.inputStream.use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

fun downloadFileAndDigestSHA1(url: URL, outputFile: File): ByteArray {
    val conn = url.openConnection() as HttpURLConnection
    conn.doInput = true
    conn.instanceFollowRedirects = true
    conn.connect()

    if (conn.responseCode != 200) throw IOException()

    val sha1 = MessageDigest.getInstance("SHA-1")
    conn.inputStream.use { input ->
        outputFile.outputStream().use { output ->
            val dataBuffer = ByteArray(1024)
            var bytesRead: Int
            while (input.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                output.write(dataBuffer, 0, bytesRead)
                sha1.update(dataBuffer, 0, bytesRead)
            }
        }
    }

    return sha1.digest()
}

private val gson = Gson()
fun fetchJson(url: URL, accept: String = "application/json"): HashMap<*, *> {
    val conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty("Accept", accept)
    conn.doInput = true
    conn.instanceFollowRedirects = true
    conn.connect()

    if (conn.responseCode != 200) throw IOException()

    val body = conn.inputStream.use { input ->
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)

        var length: Int
        while (input.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }
        result.toString("UTF-8")
    }

    return gson.fromJson(body, HashMap::class.java)
}

private val HEX_ARRAY = "0123456789abcdef".toCharArray()
fun bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
        val v = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = HEX_ARRAY[v ushr 4]
        hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
    }
    return String(hexChars)
}

fun ZipFile.hasDirectory(dir: String): Boolean {
    fileHeaders.forEach {
        if (it.fileName.startsWith(dir)) {
            return true
        }
    }
    return false
}

fun InputStream.copyTo(out: OutputStream, onProgress: (currentBytes: Int) -> Unit) {
    val buffer = ByteArray(524288) // 512KiB
    var lengthRead: Int
    var currentBytes = 0
    while (read(buffer).also { lengthRead = it } > 0) {
        out.write(buffer, 0, lengthRead)
        currentBytes += lengthRead
        onProgress(currentBytes)
    }
    out.flush()
}