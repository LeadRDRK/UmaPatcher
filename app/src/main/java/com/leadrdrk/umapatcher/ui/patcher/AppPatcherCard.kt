package com.leadrdrk.umapatcher.ui.patcher

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.AppPatcher
import com.leadrdrk.umapatcher.ui.component.RadioGroupOption
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun AppPatcherCard(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val isMountInstalled = remember { AppPatcher.isMountInstalled(context) }
    var isApkMounted by remember { mutableStateOf(if (isMountInstalled) AppPatcher.isApkMounted(context) else false) }

    // Options
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

    PatcherCard(
        label = stringResource(R.string.app_patcher_label),
        icon = { Icon(painterResource(R.drawable.ic_apk_install), null) },
        buttons = {
            AssistChip(
                onClick = {
                    PatcherLauncher.launch(
                        navigator,
                        AppPatcher(
                            fileUri = if (apkSource.intValue == 1) fileUri else null,
                            mountInstall = installMethod.intValue == 1
                        )
                    )
                },
                label = { Text(stringResource(R.string.patch)) },
                leadingIcon = { Icon(Icons.Outlined.Build, stringResource(R.string.patch)) }
            )
            if (isMountInstalled) {
                AssistChip(
                    onClick = {
                        if (isApkMounted) {
                            if (AppPatcher.unmountApk(context)) isApkMounted = false
                        }
                        else {
                            if (AppPatcher.runMountScript(context)) isApkMounted = true
                        }
                    },
                    label = {
                        Text(stringResource(
                            if (isApkMounted) R.string.unmount
                            else R.string.mount
                        ))
                    },
                    leadingIcon = {
                        Icon(painterResource(
                            if (isApkMounted) R.drawable.ic_eject
                            else R.drawable.ic_mount
                        ), null)
                    }
                )
            }
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
                stringResource(R.string.save_patched_apk_file),
                stringResource(R.string.direct_install)
            ),
            state = installMethod
        )
    }
}