package com.leadrdrk.umapatcher.ui.screen

import androidx.compose.runtime.Composable
import com.leadrdrk.umapatcher.patcher.LyricsPatcher
import com.leadrdrk.umapatcher.ui.component.DefaultAssetsPatcherOptionsScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
@Destination
fun LyricsPatcherOptionsScreen(navigator: DestinationsNavigator) {
    DefaultAssetsPatcherOptionsScreen(
        navigator = navigator,
        patcher = { skipMachineTl, nThreads, forcePatch, makeBackup ->
            LyricsPatcher(
                skipMachineTl,
                nThreads,
                forcePatch,
                makeBackup,
            )
        }
    )
}