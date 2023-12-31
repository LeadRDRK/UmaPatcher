package com.leadrdrk.umapatcher.ui.patcher

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.RacePatcher
import com.leadrdrk.umapatcher.ui.screen.destinations.RacePatcherOptionsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun RacePatcherCard(navigator: DestinationsNavigator, showConfirmRestoreDialog: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    AssetsPatcherCardBase(
        navigator,
        showConfirmRestoreDialog,
        label = stringResource(R.string.race_patcher_label),
        icon = { Icon(painterResource(R.drawable.ic_sports_score), null) },
        translationsPath = RacePatcher.RACE_TRANSLATIONS_PATH,
        optionsScreenDest = RacePatcherOptionsScreenDestination,
        restorePatcher = { RacePatcher(restoreMode = true) },
        isRestoreAvailable = remember { RacePatcher.isRestoreAvailable(context) }
    )
}