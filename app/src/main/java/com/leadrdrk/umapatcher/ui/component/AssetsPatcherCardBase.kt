package com.leadrdrk.umapatcher.ui.component

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
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.Patcher
import com.leadrdrk.umapatcher.ui.patcher.PatcherCard
import com.leadrdrk.umapatcher.ui.patcher.PatcherLauncher
import com.leadrdrk.umapatcher.ui.screen.destinations.DirectionDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun AssetsPatcherCardBase(
    navigator: DestinationsNavigator,
    showConfirmRestoreDialog: (() -> Unit) -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    translationsPath: String,
    optionsScreenDest: DirectionDestination,
    restorePatcher: () -> Patcher,
    isRestoreAvailable: Boolean
) {
    val context = LocalContext.current
    val translationLastUpdated = remember { mutableStateOf("N/A") }
    LastCommitTimeEffect(translationLastUpdated, translationsPath)

    PatcherCard(
        label = label,
        icon = icon,
        buttons = {
            AssistChip(
                onClick = { navigator.navigate(optionsScreenDest) },
                label = { Text(stringResource(R.string.patch)) },
                leadingIcon = { Icon(Icons.Outlined.Build, stringResource(R.string.patch)) }
            )
            AssistChip(
                onClick = {
                    showConfirmRestoreDialog {
                        PatcherLauncher.launch(context, navigator, restorePatcher())
                    }
                },
                label = { Text(stringResource(R.string.restore)) },
                leadingIcon = { Icon(Icons.Outlined.Refresh, stringResource(R.string.restore)) },
                enabled = isRestoreAvailable
            )
        }
    ) {
        Text(
            text = stringResource(R.string.translation_last_updated_prefix) + translationLastUpdated.value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}