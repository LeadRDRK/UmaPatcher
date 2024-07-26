package com.leadrdrk.umapatcher.ui.screen

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.leadrdrk.umapatcher.ui.component.OptionBase
import com.leadrdrk.umapatcher.ui.component.TopBar
import com.leadrdrk.umapatcher.utils.ksFile
import com.leadrdrk.umapatcher.utils.showToast
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val checkForUpdates = remember { mutableStateOf(false) }
    val appLibsVersion = remember { mutableStateOf("") }
    var configRead by remember { mutableStateOf(false) }

    val exportKsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data ?: return@rememberLauncherForActivityResult
        val ksFile = context.ksFile
        if (!ksFile.exists()) return@rememberLauncherForActivityResult

        context.contentResolver.openOutputStream(uri).use { output ->
            if (output == null) {
                context.showToast(
                    context.getString(R.string.failed_to_save_file),
                    Toast.LENGTH_SHORT
                )
                return@use
            }
            ksFile.inputStream().use { input ->
                input.copyTo(output)
                context.showToast(
                    context.getString(R.string.file_saved),
                    Toast.LENGTH_SHORT
                )
            }
        }
    }

    val importKsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data ?: return@rememberLauncherForActivityResult

        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) {
                context.showToast(
                    context.getString(R.string.failed_to_import_keystore),
                    Toast.LENGTH_SHORT
                )
                return@use
            }
            context.ksFile.outputStream().use { output ->
                input.copyTo(output)
                context.showToast(
                    context.getString(R.string.keystore_imported),
                    Toast.LENGTH_SHORT
                )
            }
        }
    }

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

    PrefUpdateEffect(appLibsVersion.value) {
        it[PrefKey.APP_LIBS_VERSION] = appLibsVersion.value
    }

    LaunchedEffect(true) {
        checkForUpdates.value = context.getPrefValue(PrefKey.CHECK_FOR_UPDATES) as Boolean
        appLibsVersion.value = context.getPrefValue(PrefKey.APP_LIBS_VERSION) as String
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

            OptionBase(
                title = stringResource(R.string.export_signing_key),
                desc = stringResource(R.string.export_signing_key_desc),
                onClick = {
                    if (context.ksFile.exists()) {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_TITLE, "UmaPatcher.bks")
                            }
                        exportKsLauncher.launch(intent)
                    }
                    else {
                        context.showToast(
                            context.getString(R.string.no_keystore_to_export),
                            Toast.LENGTH_SHORT
                        )
                    }
                }
            ) {
            }

            OptionBase(
                title = stringResource(R.string.import_signing_key),
                desc = stringResource(R.string.import_signing_key_desc),
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                    importKsLauncher.launch(intent)
                }
            ) {
            }

            OptionBase(
                title = stringResource(R.string.force_redownload_mod),
                desc = stringResource(R.string.force_redownload_mod_desc),
                onClick = {
                    context.showToast(
                        context.getString(R.string.force_redownload_mod_notice),
                        Toast.LENGTH_SHORT
                    )
                    appLibsVersion.value = ""
                }
            ) {
            }
        }
    }
}