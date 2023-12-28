package com.leadrdrk.umapatcher.core

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.leadrdrk.umapatcher.BuildConfig
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val REPO_PATH = "LeadRDRK/UmaPatcher"
    private const val CHECK_TIMEOUT = 300000 // 5 minutes

    private val apiUrl = URL("https://api.github.com/repos/${REPO_PATH}/releases/latest")
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
        val conn = apiUrl.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.doInput = true
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

        val gson = Gson()
        val release = gson.fromJson(body, HashMap::class.java)
        val tagName = release["tag_name"] as String

        return if (tagName != currentTag) {
            callback(tagName)
            true
        } else false
    }

    fun getReleaseUrl(tagName: String) =
        "https://github.com/${REPO_PATH}/releases/tag/${Uri.encode(tagName)}"
}