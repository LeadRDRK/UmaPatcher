package com.leadrdrk.umapatcher.git

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.NotificationId
import com.leadrdrk.umapatcher.core.PrefKey
import com.leadrdrk.umapatcher.core.dataStore
import com.leadrdrk.umapatcher.core.getPrefValue
import com.leadrdrk.umapatcher.utils.createNotificationBuilder
import com.leadrdrk.umapatcher.utils.createNotificationChannel
import com.leadrdrk.umapatcher.utils.deleteRecursive
import com.leadrdrk.umapatcher.utils.repoDir
import com.leadrdrk.umapatcher.utils.safeNotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.WindowCacheConfig
import org.eclipse.jgit.treewalk.filter.AndTreeFilter
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.TreeFilter
import java.io.File

private const val CHANNEL_ID = "GIT_SYNC"
private val NOTIFICATION_ID = NotificationId.GitSyncProgress.ordinal

object GitRepo {
    private var git_: Git? = null
    private lateinit var repoDir: File
    private var lastCommitTimeCache = mutableMapOf<String, Int?>()

    val syncing = mutableStateOf(false)
    val ready = mutableStateOf(false)
    val lastSynced = mutableLongStateOf(0L)

    suspend fun init(context: Context) {
        repoDir = context.repoDir

        // no rocket science will make jgit faster than cgit, but this will do for now
        val config = WindowCacheConfig()
        config.packedGitLimit = 128L * WindowCacheConfig.MB
        config.install()

        withContext(Dispatchers.IO) {
            val cloneCompleted = context.getPrefValue(PrefKey.GIT_CLONE_COMPLETED) as Boolean
            if (cloneCompleted) {
                git_ = Git.open(repoDir)
                ready.value = true
            }
        }

        context.createNotificationChannel(CHANNEL_ID, "Git sync", NotificationManager.IMPORTANCE_LOW)
    }

    fun sync(context: Context, scope: CoroutineScope) {
        if (syncing.value) return
        syncing.value = true

        // We launch the coroutine from here and set the syncing value earlier to make sure
        // that the syncing value is set to true when the function returns.
        scope.launch {
            // lazy catch-all wrapper
            try {
                rawSync(context)
            }
            catch (e: Exception) {
                Log.e("UmaPatcher", "JGit exception", e)
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(NotificationId.GitSyncProgress.ordinal)
                val builder = context.createNotificationBuilder(
                    CHANNEL_ID,
                    context.getString(R.string.sync_failed),
                    context.getString(R.string.sync_failed_desc),
                    autoCancel = true
                )
                context.safeNotify(NotificationId.GitSyncFailed.ordinal, builder.build())
            }

            syncing.value = false
            lastSynced.longValue = System.currentTimeMillis()
        }
    }

    private suspend fun rawSync(context: Context) {
        val notificationBuilder = context.createNotificationBuilder(
            CHANNEL_ID,
            context.getString(R.string.syncing),
            context.getString(R.string.syncing_desc),
            NotificationCompat.PRIORITY_LOW
        )

        context.safeNotify(NOTIFICATION_ID, notificationBuilder.build())
        val progressMonitor = NotificationProgressMonitor(context, notificationBuilder)

        withContext(Dispatchers.IO) {
            val remote = context.getPrefValue(PrefKey.GIT_REMOTE) as String
            val branch = context.getPrefValue(PrefKey.GIT_BRANCH) as String
            val git = git_

            if (git != null) {
                // Check if this repo points to the current remote
                val url: String = git.repository.config.getString("remote", "origin", "url")
                if (url != remote) {
                    // Close repo and invalidate completed status
                    git.close()
                    setCloneCompleted(context, false)

                    // Clone new repo
                    cloneRepo(context, remote, branch, progressMonitor)
                    return@withContext
                }

                // Pull changes
                git.pull()
                    .setRemoteBranchName(branch)
                    .setProgressMonitor(progressMonitor)
                    .call()

                // Checkout if needed
                if (git.repository.branch == branch) return@withContext
                git.checkout()
                    .setName(branch)
                    .call()
            }
            else {
                cloneRepo(context, remote, branch, progressMonitor)
            }
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID)
        lastCommitTimeCache.clear()
    }

    private suspend fun cloneRepo(context: Context, remote: String, branch: String, progressMonitor: ProgressMonitor) {
        // Make sure that there isn't a git tree already
        deleteRecursive(repoDir)

        // Clone repo
        val branchRef = "refs/heads/$branch"
        git_ = Git.cloneRepository()
            .setURI(remote)
            .setDirectory(repoDir)
            .setBranch(branchRef)
            .setBranchesToClone(listOf(branchRef))
            .setProgressMonitor(progressMonitor)
            .call()

        ready.value = true
        setCloneCompleted(context, true)
    }

    fun getLastCommitTimeOfPath(path: String): Int? {
        val cached = lastCommitTimeCache[path]
        if (cached != null) return cached

        val git = git_!!
        val repo = git.repository
        val rw = RevWalk(repo)
        rw.markStart(rw.parseCommit(repo.resolve(Constants.HEAD)))
        rw.treeFilter = AndTreeFilter.create(
            PathFilter.create(path),
            TreeFilter.ANY_DIFF
        )

        val commitTime = rw.next()?.commitTime
        lastCommitTimeCache[path] = commitTime
        return commitTime
    }

    private suspend fun setCloneCompleted(context: Context, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PrefKey.GIT_CLONE_COMPLETED] = value
        }
    }
}