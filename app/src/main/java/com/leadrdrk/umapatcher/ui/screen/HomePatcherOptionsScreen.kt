package com.leadrdrk.umapatcher.ui.screen

import androidx.compose.runtime.Composable
import com.leadrdrk.umapatcher.patcher.HomePatcher
import com.leadrdrk.umapatcher.ui.component.BaseStoryPatcherOptionsScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
@Destination
fun HomePatcherOptionsScreen(navigator: DestinationsNavigator) {
    BaseStoryPatcherOptionsScreen(
        navigator = navigator,
        patcher = { skipMachineTl, nThreads, forcePatch, makeBackup, cps, fps ->
            HomePatcher(
                skipMachineTl,
                nThreads,
                forcePatch,
                makeBackup,
                restoreMode = false,
                cps,
                fps
            )
        }
    )
}