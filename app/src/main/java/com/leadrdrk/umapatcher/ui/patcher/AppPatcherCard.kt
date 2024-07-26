package com.leadrdrk.umapatcher.ui.patcher

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.patcher.AppPatcher
import com.leadrdrk.umapatcher.ui.component.RadioGroupOption
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun AppPatcherCard(navigator: DestinationsNavigator) {
    // Options
    val installMethod = rememberSaveable { mutableIntStateOf(0) }
    var fileUris by rememberSaveable { mutableStateOf<Array<Uri>>(arrayOf()) }
    val fileSelectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it.data ?: return@rememberLauncherForActivityResult

        val clipData = data.clipData
        if (clipData != null) {
            fileUris = Array(clipData.itemCount) { i ->
                clipData.getItemAt(i).uri
            }
            return@rememberLauncherForActivityResult
        }

        val uri = data.data
        if (uri != null) {
            fileUris = Array(1) { uri }
        }
    }

    PatcherCard(
        label = stringResource(R.string.app_patcher_label),
        icon = { Icon(painterResource(R.drawable.ic_apk_install), null) },
        buttons = {
            Button(
                enabled = (installMethod.intValue == 1 || fileUris.isNotEmpty()),
                onClick = {
                    PatcherLauncher.launch(
                        navigator,
                        AppPatcher(
                            fileUris = if (installMethod.intValue == 1) arrayOf()
                                else fileUris,
                            install = installMethod.intValue == 0,
                            directInstall = installMethod.intValue == 1,
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.patch))
            }
        }
    ) {
        RadioGroupOption(
            title = stringResource(R.string.install_method),
            desc = stringResource(R.string.install_method_desc),
            choices = arrayOf(
                stringResource(R.string.normal_install),
                stringResource(R.string.direct_install),
                stringResource(R.string.save_patched_file)
            ),
            state = installMethod
        )
        if (installMethod.intValue != 1) {
            Spacer(Modifier.height(16.dp))
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .clickable {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                type = "*/*"
                            }
                        fileSelectLauncher.launch(intent)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_file_open), null)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.tap_to_select_file),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.n_files_selected).format(fileUris.size),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.app_patcher_supported_files),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}