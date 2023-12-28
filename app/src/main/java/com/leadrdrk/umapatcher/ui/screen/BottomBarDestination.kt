package com.leadrdrk.umapatcher.ui.screen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.ui.screen.destinations.AboutScreenDestination
import com.leadrdrk.umapatcher.ui.screen.destinations.HomeScreenDestination
import com.leadrdrk.umapatcher.ui.screen.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

@Suppress("unused")
enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    @StringRes val label: Int,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector
) {
    Home(HomeScreenDestination, R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
    Settings(SettingsScreenDestination, R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    About(AboutScreenDestination, R.string.about, Icons.Filled.Info, Icons.Outlined.Info)
}