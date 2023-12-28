package com.leadrdrk.umapatcher.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.PrefKey
import com.leadrdrk.umapatcher.core.dataStore
import com.leadrdrk.umapatcher.core.getPrefValue
import com.leadrdrk.umapatcher.ui.component.BooleanOption
import com.leadrdrk.umapatcher.ui.component.StringOption
import com.leadrdrk.umapatcher.ui.component.TopBar
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val checkForUpdates = remember { mutableStateOf(false) }
    val syncOnStartup = remember { mutableStateOf(false) }
    val gitRemote = remember { mutableStateOf("") }
    val gitBranch = remember { mutableStateOf("") }

    var configRead by remember { mutableStateOf(false) }

    @Composable
    fun PrefUpdateEffect(key: Any?, transform: suspend (MutablePreferences) -> Unit) {
        LaunchedEffect(key) {
            if (!configRead) return@LaunchedEffect
            context.dataStore.edit(transform)
        }
    }

    PrefUpdateEffect(checkForUpdates.value) {
        it[PrefKey.CHECK_FOR_UPDATES] = checkForUpdates.value
    }

    PrefUpdateEffect(syncOnStartup.value) {
        it[PrefKey.SYNC_ON_STARTUP] = syncOnStartup.value
    }

    PrefUpdateEffect(gitRemote.value) {
        it[PrefKey.GIT_REMOTE] = gitRemote.value
    }

    PrefUpdateEffect(gitBranch.value) {
        it[PrefKey.GIT_BRANCH] = gitBranch.value
    }

    LaunchedEffect(true) {
        checkForUpdates.value = context.getPrefValue(PrefKey.CHECK_FOR_UPDATES) as Boolean
        syncOnStartup.value = context.getPrefValue(PrefKey.SYNC_ON_STARTUP) as Boolean
        gitRemote.value = context.getPrefValue(PrefKey.GIT_REMOTE) as String
        gitBranch.value = context.getPrefValue(PrefKey.GIT_BRANCH) as String
        configRead = true
    }

    Scaffold(
        topBar = {
            TopBar(stringResource(R.string.settings))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (!configRead) return@Column

            BooleanOption(
                title = stringResource(R.string.check_for_updates),
                desc = stringResource(R.string.check_for_updates_desc),
                state = checkForUpdates
            )
            BooleanOption(
                title = stringResource(R.string.sync_on_startup),
                desc = stringResource(R.string.sync_on_startup_desc),
                state = syncOnStartup
            )
            StringOption(
                title = stringResource(R.string.git_remote),
                state = gitRemote
            )
            StringOption(
                title = stringResource(R.string.git_branch),
                state = gitBranch
            )
        }
    }
}