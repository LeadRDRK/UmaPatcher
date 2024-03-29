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
    val CHECK_FOR_UPDATES = booleanPreferencesKey("check_for_updates")
    val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
    val APP_LIBS_VERSION = stringPreferencesKey("app_libs_version")
}

val defaultValues = mapOf(
    Pair(PrefKey.CHECK_FOR_UPDATES, true),
    Pair(PrefKey.LAST_UPDATE_CHECK, 0L),
    Pair(PrefKey.APP_LIBS_VERSION, null)
)

suspend fun Context.getPrefValue(key: Preferences.Key<*>): Any? {
    return withContext(Dispatchers.IO) {
        val value = dataStore.data
            .map {
                it[key]
            }
        return@withContext value.firstOrNull() ?: defaultValues[key]
    }
}