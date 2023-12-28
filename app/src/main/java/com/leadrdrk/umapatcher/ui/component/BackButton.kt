package com.leadrdrk.umapatcher.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.leadrdrk.umapatcher.utils.safeNavigate
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun BackButton(navigator: DestinationsNavigator, enabled: Boolean = true) {
    val lifecycleOwner = LocalLifecycleOwner.current
    IconButton(
        onClick = {
            safeNavigate(lifecycleOwner) {
                navigator.popBackStack()
            }
        },
        enabled = enabled
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = null)
    }
}