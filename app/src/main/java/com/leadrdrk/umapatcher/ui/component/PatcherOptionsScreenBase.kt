package com.leadrdrk.umapatcher.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.utils.safeNavigate
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun PatcherOptionsScreenBase(
    navigator: DestinationsNavigator,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.options),
                navigationIcon = { BackButton(navigator) },
                actions = {
                    TextButton(
                        onClick = {
                            safeNavigate(lifecycleOwner) {
                                navigator.popBackStack()
                                onConfirm()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            content()
        }
    }
}