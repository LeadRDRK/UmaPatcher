package com.leadrdrk.umapatcher.ui.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.git.GitRepo
import com.leadrdrk.umapatcher.ui.component.BackButton
import com.leadrdrk.umapatcher.ui.component.TopBar
import com.leadrdrk.umapatcher.ui.patcher.PatcherLauncher
import com.leadrdrk.umapatcher.utils.safeNavigate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val MAX_LOG_LINES = 200

@Destination
@Composable
fun PatchingScreen(navigator: DestinationsNavigator) {
    val workingStr = stringResource(R.string.working)
    val completedStr = stringResource(R.string.completed)

    val log = remember { mutableStateListOf<String>() }
    var currentTask by remember { mutableStateOf(workingStr) }
    var progress by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Patcher callbacks
    fun onLog(line: String) {
        log.add(line)
        if (log.size > MAX_LOG_LINES)
            log.removeRange(0, log.size - MAX_LOG_LINES)
    }
    fun onProgress(p: Float) { progress = p }
    fun onTask(task: String) {
        currentTask = task
        log.add("-- $task")
    }

    val coroutineScope = rememberCoroutineScope()
    var sfFile by remember { mutableStateOf<File?>(null) }
    var sfCallback by remember { mutableStateOf({}) }
    val sfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data
        if (uri == null) {
            sfCallback()
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri).use { output ->
                if (output == null) return@use
                sfFile!!.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            sfCallback()
            sfFile = null
        }
    }

    fun onSaveFile(filename: String, file: File, callback: () -> Unit = {}) {
        sfFile = file
        sfCallback = callback
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_TITLE, filename)
            }
        sfLauncher.launch(intent)
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(log.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler {
        if (completed && sfFile == null) {
            safeNavigate(lifecycleOwner) {
                navigator.popBackStack()
            }
        }
    }

    // Wait for sync if it's syncing when the screen is launched
    // waitingForSync will only be changed once when syncing finishes the first time
    val syncing by remember { GitRepo.syncing }
    var waitingForSync by remember { mutableStateOf(GitRepo.syncing.value) }

    val syncFailedDesc = stringResource(R.string.sync_failed_desc)
    LaunchedEffect(syncing) {
        if (!syncing && waitingForSync) {
            // Check for failed clone
            if (GitRepo.ready.value) {
                waitingForSync = false
            } else {
                log.add(syncFailedDesc)
                completed = true
            }
        }
    }

    val initialSyncInfo = stringResource(R.string.initial_sync_info)
    val waitingForSyncStr = stringResource(R.string.waiting_for_sync)
    val patchSuccessMsg = stringResource(R.string.patch_success_msg)
    val patchFailedMsg = stringResource(R.string.patch_failed_msg)

    LaunchedEffect(waitingForSync) {
        if (PatcherLauncher.patching) return@LaunchedEffect
        if (waitingForSync) {
            // Show notice if this is the first sync/clone
            if (!GitRepo.ready.value) {
                log.add(initialSyncInfo)
            }
            onTask(waitingForSyncStr)
            return@LaunchedEffect
        }
        // Run the patcher
        val patcher = PatcherLauncher.patcher!!
        patcher.setCallbacks(::onLog, ::onProgress, ::onTask, ::onSaveFile)
        PatcherLauncher.runPatcher(context) { success ->
            completed = true
            log.add(if (success) patchSuccessMsg else patchFailedMsg)
        }
    }

    LaunchedEffect(completed) {
        if (completed) {
            currentTask = completedStr
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (completed) completedStr else workingStr,
                navigationIcon = { BackButton(navigator, enabled = completed && sfFile == null) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            TextField(
                value = log.joinToString("\n"),
                onValueChange = {},
                readOnly = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = TextUnit(1.4f, TextUnitType.Em)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Unspecified,
                    unfocusedIndicatorColor = Color.Unspecified
                )
            )
            Column(
                modifier = Modifier
                    .padding(all = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = currentTask,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}