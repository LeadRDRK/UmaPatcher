package com.leadrdrk.umapatcher.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.leadrdrk.umapatcher.R
import java.io.File

val Context.workDir: File
    get() = cacheDir.resolve("work")

val Context.repoDir: File
    get() = filesDir.resolve("repo")

fun Context.isNotificationAllowed(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    return true
}

@SuppressLint("MissingPermission")
fun Context.safeNotify(notificationId: Int, notification: Notification) {
    if (!isNotificationAllowed()) return
    NotificationManagerCompat.from(this).notify(notificationId, notification)
}

fun Context.createNotificationChannel(channelId: String, channelName: String, importance: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}

fun Context.createNotificationBuilder(
    channelId: String,
    contentTitle: String,
    contentText: String,
    priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    autoCancel: Boolean = false
): NotificationCompat.Builder {
    return NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_umapatcher_filled)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setPriority(priority)
        .setAutoCancel(autoCancel)
}

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun Context.getUid(): Int {
    return packageManager.getPackageUid(packageName, 0)
}

fun Context.showToast(text: String, duration: Int) {
    getActivity()?.runOnUiThread {
        Toast.makeText(this, text, duration).show()
    }
}