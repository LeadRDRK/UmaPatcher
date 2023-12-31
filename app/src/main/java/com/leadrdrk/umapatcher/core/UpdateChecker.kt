package com.leadrdrk.umapatcher.core

import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import com.leadrdrk.umapatcher.BuildConfig
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UpdateChecker {
    private const val CHECK_TIMEOUT = 300000 // 5 minutes

    private val releases = GitHubReleases("LeadRDRK/UmaPatcher")
    private const val currentTag = "v${BuildConfig.VERSION_NAME}"
    private val scope = CoroutineScope(Dispatchers.IO)

    private var running = false
    var callback: (String) -> Unit = {}

    fun init(context: Context) {
        scope.launch {
            if (context.getPrefValue(PrefKey.CHECK_FOR_UPDATES) as Boolean) {
                val lastUpdateCheck = context.getPrefValue(PrefKey.LAST_UPDATE_CHECK) as Long
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateCheck < CHECK_TIMEOUT)
                    return@launch

                context.dataStore.edit { preferences ->
                    preferences[PrefKey.LAST_UPDATE_CHECK] = currentTime
                }

                try {
                    running = true
                    rawRun()
                }
                catch (_: Exception) {}
                running = false
            }
        }
    }

    fun run(context: Context? = null) {
        if (running) return
        running = true

        scope.launch {
            try {
                if (!rawRun()) {
                    context?.showToast(
                        context.getString(R.string.no_updates_available),
                        Toast.LENGTH_SHORT
                    )
                }
            }
            catch (_: Exception) {
                context?.showToast(
                    context.getString(R.string.failed_to_check_for_updates),
                    Toast.LENGTH_SHORT
                )
            }
            running = false
        }
    }

    private fun rawRun(): Boolean {
        val release = releases.fetchLatest()
        val tagName = release["tag_name"] as String

        return if (tagName != currentTag) {
            callback(tagName)
            true
        } else false
    }

    fun getReleaseUrl(tagName: String) = releases.getReleaseUrl(tagName)
}