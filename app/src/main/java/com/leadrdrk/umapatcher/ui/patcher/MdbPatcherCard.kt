package com.leadrdrk.umapatcher.ui.patcher

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.MdbPatcher
import com.leadrdrk.umapatcher.ui.component.LastCommitTimeEffect
import com.leadrdrk.umapatcher.ui.screen.destinations.MdbPatcherOptionsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun MdbPatcherCard(navigator: DestinationsNavigator, showConfirmRestoreDialog: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    val translationLastUpdated = remember { mutableStateOf("N/A") }
    val isRestoreAvailable = remember { MdbPatcher.isRestoreAvailable(context) }
    val isPatched = remember { MdbPatcher.isPatched(context) }
    val isPatchedStr = remember {
        when (isPatched) {
            true -> "Yes"
            false -> "No"
            null -> "N/A"
        }
    }
    val lastModifiedStr = remember { MdbPatcher.getLastModifiedStr(context) ?: "N/A" }

    LastCommitTimeEffect(translationLastUpdated, MdbPatcher.MDB_TRANSLATIONS_PATH)

    PatcherCard(
        label = stringResource(R.string.mdb_patcher_label),
        icon = { Icon(painterResource(R.drawable.ic_database), null) },
        buttons = {
            AssistChip(
                onClick = { navigator.navigate(MdbPatcherOptionsScreenDestination) },
                label = {
                    Text(stringResource(R.string.patch))
                },
                leadingIcon = { Icon(Icons.Outlined.Build, stringResource(R.string.patch)) }
            )
            AssistChip(
                onClick = {
                    showConfirmRestoreDialog {
                        PatcherLauncher.launch(context, navigator, MdbPatcher(restoreMode = true))
                    }
                },
                label = { Text(stringResource(R.string.restore)) },
                leadingIcon = { Icon(Icons.Outlined.Refresh, stringResource(R.string.restore)) },
                enabled = isRestoreAvailable
            )
        }
    ) {
        Text(
            text = stringResource(R.string.patched_prefix) + isPatchedStr,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.last_modified_prefix) + lastModifiedStr,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.translation_last_updated_prefix) + translationLastUpdated.value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}