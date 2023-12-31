package com.leadrdrk.umapatcher.ui.patcher

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.PreviewPatcher
import com.leadrdrk.umapatcher.ui.screen.destinations.PreviewPatcherOptionsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun PreviewPatcherCard(navigator: DestinationsNavigator, showConfirmRestoreDialog: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    AssetsPatcherCardBase(
        navigator,
        showConfirmRestoreDialog,
        label = stringResource(R.string.preview_patcher_label),
        icon = { Icon(painterResource(R.drawable.ic_preview), null) },
        translationsPath = PreviewPatcher.PREVIEW_TRANSLATIONS_PATH,
        optionsScreenDest = PreviewPatcherOptionsScreenDestination,
        restorePatcher = { PreviewPatcher(restoreMode = true) },
        isRestoreAvailable = remember { PreviewPatcher.isRestoreAvailable(context) }
    )
}