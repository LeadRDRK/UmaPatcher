package com.leadrdrk.umapatcher.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.BaseStoryPatcher
import com.leadrdrk.umapatcher.patcher.Patcher
import com.leadrdrk.umapatcher.ui.patcher.PatcherLauncher
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun AssetsPatcherOptionsScreenBase(
    navigator: DestinationsNavigator,
    onConfirm: () -> Unit,
    skipMachineTl: MutableState<Boolean>,
    nThreads: MutableIntState,
    forcePatch: MutableState<Boolean>,
    makeBackup: MutableState<Boolean>,
    options: @Composable () -> Unit
) {
    PatcherOptionsScreenBase(
        navigator = navigator,
        onConfirm = onConfirm
    ) {
        BooleanOption(
            title = stringResource(R.string.skip_machine_tl),
            desc = stringResource(R.string.skip_machine_tl_desc),
            state = skipMachineTl
        )
        IntOption(
            title = stringResource(R.string.number_of_threads),
            state = nThreads
        )
        BooleanOption(
            title = stringResource(R.string.force_patch),
            desc = stringResource(R.string.force_patch_desc),
            state = forcePatch
        )
        BooleanOption(
            title = stringResource(R.string.make_backup),
            desc = stringResource(R.string.make_backup_desc),
            state = makeBackup
        )
        options()
    }
}

@Composable
fun DefaultAssetsPatcherOptionsScreen(
    navigator: DestinationsNavigator,
    patcher: (Boolean, Int, Boolean, Boolean) -> Patcher
) {
    val context = LocalContext.current
    val skipMachineTl = remember { mutableStateOf(false) }
    val nThreads = remember { mutableIntStateOf(Runtime.getRuntime().availableProcessors()) }
    val forcePatch = remember { mutableStateOf(false) }
    val makeBackup = remember { mutableStateOf(false) }

    AssetsPatcherOptionsScreenBase(
        navigator = navigator,
        onConfirm = {
            PatcherLauncher.launch(
                context,
                navigator,
                patcher(
                    skipMachineTl.value,
                    nThreads.intValue,
                    forcePatch.value,
                    makeBackup.value
                )
            )
        },
        skipMachineTl = skipMachineTl,
        nThreads = nThreads,
        forcePatch = forcePatch,
        makeBackup = makeBackup
    ) {}
}

@Composable
fun BaseStoryPatcherOptionsScreen(
    navigator: DestinationsNavigator,
    patcher: (Boolean, Int, Boolean, Boolean, Int, Int) -> BaseStoryPatcher
) {
    val context = LocalContext.current
    val skipMachineTl = remember { mutableStateOf(false) }
    val nThreads = remember { mutableIntStateOf(Runtime.getRuntime().availableProcessors()) }
    val forcePatch = remember { mutableStateOf(false) }
    val makeBackup = remember { mutableStateOf(false) }
    val cps = remember { mutableIntStateOf(28) }
    val fps = remember { mutableIntStateOf(30) }

    AssetsPatcherOptionsScreenBase(
        navigator = navigator,
        onConfirm = {
            PatcherLauncher.launch(
                context,
                navigator,
                patcher(
                    skipMachineTl.value,
                    nThreads.intValue,
                    forcePatch.value,
                    makeBackup.value,
                    cps.intValue,
                    fps.intValue
                )
            )
        },
        skipMachineTl = skipMachineTl,
        nThreads = nThreads,
        forcePatch = forcePatch,
        makeBackup = makeBackup
    ) {
        IntOption(
            title = stringResource(R.string.characters_per_second),
            state = cps
        )
        IntOption(
            title = stringResource(R.string.frame_rate),
            state = fps
        )
    }
}