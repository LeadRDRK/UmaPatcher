package com.leadrdrk.umapatcher.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.AppPatcher
import com.leadrdrk.umapatcher.ui.component.PatcherOptionsScreenBase
import com.leadrdrk.umapatcher.ui.component.RadioGroupOption
import com.leadrdrk.umapatcher.ui.patcher.PatcherLauncher
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun AppPatcherOptionsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val installMethod = remember { mutableIntStateOf(0) }
    val apkSource = remember { mutableIntStateOf(0) }

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data
        if (uri == null) {
            apkSource.intValue = 0
            return@rememberLauncherForActivityResult
        }
        fileUri = uri
    }

    LaunchedEffect(apkSource.intValue) {
        if (apkSource.intValue == 1) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                .apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
            launcher.launch(intent)
        }
    }

    PatcherOptionsScreenBase(
        navigator = navigator,
        onConfirm = {
            PatcherLauncher.launch(
                context,
                navigator,
                AppPatcher(
                    fileUri = if (apkSource.intValue == 1) fileUri else null,
                    mountInstall = installMethod.intValue == 0
                )
            )
        }
    ) {
        RadioGroupOption(
            title = stringResource(R.string.apk_file),
            desc = stringResource(R.string.apk_file_desc),
            choices = arrayOf(
                stringResource(R.string.get_apk_file_from_app),
                stringResource(R.string.select_and_patch_a_file),
            ),
            state = apkSource
        )
        RadioGroupOption(
            title = stringResource(R.string.install_method),
            desc = stringResource(R.string.install_method_desc),
            choices = arrayOf(
                stringResource(R.string.direct_install),
                stringResource(R.string.save_patched_apk_file),
            ),
            state = installMethod
        )
    }
}