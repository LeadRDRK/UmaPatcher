package com.leadrdrk.umapatcher.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leadrdrk.umapatcher.MainActivity
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.GameChecker
import com.leadrdrk.umapatcher.git.GitRepo
import com.leadrdrk.umapatcher.ui.component.SimpleOkCancelDialog
import com.leadrdrk.umapatcher.ui.component.TopBar
import com.leadrdrk.umapatcher.ui.patcher.HomePatcherCard
import com.leadrdrk.umapatcher.ui.patcher.LyricsPatcherCard
import com.leadrdrk.umapatcher.ui.patcher.MdbPatcherCard
import com.leadrdrk.umapatcher.ui.patcher.PreviewPatcherCard
import com.leadrdrk.umapatcher.ui.patcher.RacePatcherCard
import com.leadrdrk.umapatcher.ui.patcher.StoryPatcherCard
import com.leadrdrk.umapatcher.utils.getActivity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val iconRotateAnim = remember { Animatable(0f) }
    val syncing by remember { GitRepo.syncing }
    var openDialog by remember { mutableStateOf(false) }
    var restoreCallback by remember { mutableStateOf({}) }
    
    fun showConfirmRestoreDialog(callback: () -> Unit) {
        restoreCallback = callback
        openDialog = true
    }

    LaunchedEffect(syncing) {
        if (syncing) {
            iconRotateAnim.animateTo(
                targetValue = -360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing)
                )
            )
        }
        else {
            iconRotateAnim.stop()
            iconRotateAnim.snapTo(0f)
        }
    }

    val context = LocalContext.current
    val activity = context.getActivity() as MainActivity
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.app_name),
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { activity.startRepoSync() },
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_sync),
                            contentDescription = stringResource(R.string.sync_git_repo),
                            modifier = Modifier.rotate(iconRotateAnim.value)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InstallStatusCard()
            MdbPatcherCard(navigator, ::showConfirmRestoreDialog)
            StoryPatcherCard(navigator, ::showConfirmRestoreDialog)
            RacePatcherCard(navigator, ::showConfirmRestoreDialog)
            HomePatcherCard(navigator, ::showConfirmRestoreDialog)
            PreviewPatcherCard(navigator, ::showConfirmRestoreDialog)
            LyricsPatcherCard(navigator, ::showConfirmRestoreDialog)
            Spacer(Modifier.height(8.dp))
            
            if (openDialog) {
                SimpleOkCancelDialog(
                    title = stringResource(R.string.confirm),
                    onClose = { ok ->
                        openDialog = false
                        if (ok) restoreCallback()
                    }
                ) {
                    Text(stringResource(R.string.confirm_restore_desc))
                }
            }
        }
    }
}

@Composable
fun InstallStatusCard() {
    val pm = LocalContext.current.packageManager
    val packageInfo = GameChecker.getPackageInfo(pm)
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            if (packageInfo != null) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        })
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (packageInfo != null) {
                val title = stringResource(R.string.game_installed)
                Icon(Icons.Outlined.Info, title)
                Column(Modifier.padding(start = 20.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.package_name_prefix) + packageInfo.packageName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.version_name_prefix) + packageInfo.versionName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else {
                val title = stringResource(R.string.game_not_installed)
                Icon(Icons.Outlined.Warning, title)
                Column(Modifier.padding(start = 20.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.game_not_installed_info),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}