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
import com.leadrdrk.umapatcher.patcher.MdbPatcher
import com.leadrdrk.umapatcher.ui.component.BooleanOption
import com.leadrdrk.umapatcher.ui.component.PatcherOptionsScreenBase
import com.leadrdrk.umapatcher.ui.component.RadioGroupOption
import com.leadrdrk.umapatcher.ui.patcher.PatcherLauncher
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun MdbPatcherOptionsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val makeBackup = remember { mutableStateOf(true) }
    val installMethod = remember { mutableIntStateOf(0) }
    val skillVariant = remember { mutableIntStateOf(0) }
    val isPatched = remember { MdbPatcher.isPatched(context) }

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data
        if (uri == null) {
            installMethod.intValue = 0
            return@rememberLauncherForActivityResult
        }
        fileUri = uri
    }

    LaunchedEffect(installMethod.intValue) {
        if (installMethod.intValue == 1) {
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
                MdbPatcher(
                    directInstall = installMethod.intValue == 0,
                    makeBackup = makeBackup.value && isPatched != true,
                    fileUri = fileUri,
                    overrides = if (skillVariant.intValue == 1)
                        mapOf("skill-desc" to "alt/skill-desc") else
                        mapOf()
                )
            )
        }
    ) {
        RadioGroupOption(
            title = stringResource(R.string.install_method),
            desc = stringResource(R.string.install_method_desc),
            choices = arrayOf(
                stringResource(R.string.direct_install),
                stringResource(R.string.select_and_patch_a_file),
            ),
            state = installMethod
        )
        BooleanOption(
            title = stringResource(R.string.make_backup),
            desc = stringResource(R.string.make_backup_desc),
            state = makeBackup,
            enabled = (installMethod.intValue == 0 && isPatched != true)
        )
        RadioGroupOption(
            title = stringResource(R.string.skill_descriptions),
            desc = stringResource(R.string.translation_variant_desc),
            choices = arrayOf(
                stringResource(R.string.flavour_text),
                stringResource(R.string.data_values),
            ),
            state = skillVariant
        )
    }
}