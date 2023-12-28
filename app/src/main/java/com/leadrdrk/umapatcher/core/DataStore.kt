package com.leadrdrk.umapatcher.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")

object PrefKey {
    val GIT_CLONE_COMPLETED = booleanPreferencesKey("git_clone_completed")
    val GIT_REMOTE = stringPreferencesKey("git_remote")
    val GIT_BRANCH = stringPreferencesKey("git_branch")
    val SYNC_ON_STARTUP = booleanPreferencesKey("sync_on_startup")
    val CHECK_FOR_UPDATES = booleanPreferencesKey("check_for_updates")
    val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
}

val defaultValues = mapOf(
    Pair(PrefKey.GIT_CLONE_COMPLETED, false),
    Pair(PrefKey.GIT_REMOTE, "https://github.com/LeadRDRK/umamusu-translate.git"),
    Pair(PrefKey.GIT_BRANCH, "master"),
    Pair(PrefKey.SYNC_ON_STARTUP, true),
    Pair(PrefKey.CHECK_FOR_UPDATES, true),
    Pair(PrefKey.LAST_UPDATE_CHECK, 0L)
)

suspend fun Context.getPrefValue(key: Preferences.Key<*>): Any {
    return withContext(Dispatchers.IO) {
        val value = dataStore.data
            .map {
                it[key]
            }
        return@withContext value.firstOrNull() ?: defaultValues[key]!!
    }
}