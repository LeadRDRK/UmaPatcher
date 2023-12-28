package com.leadrdrk.umapatcher.git

import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.leadrdrk.umapatcher.core.NotificationId
import com.leadrdrk.umapatcher.utils.safeNotify
import org.eclipse.jgit.lib.ProgressMonitor

private val NOTIFICATION_ID = NotificationId.GitSyncProgress.ordinal
private const val UPDATE_INTERVAL = 1000

class NotificationProgressMonitor(
    private val context: Context,
    private val builder: NotificationCompat.Builder
): ProgressMonitor {
    private var totalWorks = 0
    private var completedWorks = 0
    private var title = ""
    private var lastUpdate = SystemClock.elapsedRealtime()

    override fun start(totalTasks: Int) {}

    override fun beginTask(title: String, totalWork: Int) {
        this.totalWorks = totalWork
        this.completedWorks = 0
        this.title = title

        updateProgress()
    }

    override fun update(completed: Int) {
        completedWorks += completed
        updateProgress()
    }

    override fun endTask() {
        completedWorks = totalWorks
        updateProgress()
    }

    private fun updateProgress() {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastUpdate < UPDATE_INTERVAL) return

        if (totalWorks == ProgressMonitor.UNKNOWN) {
            builder.setProgress(0, 0, true)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(title))
        }
        else {
            builder.setProgress(totalWorks, completedWorks, false)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("$title ($completedWorks/$totalWorks)"))
        }
        context.safeNotify(NOTIFICATION_ID, builder.build())
        lastUpdate = currentTime
    }

    override fun isCancelled() = false
}