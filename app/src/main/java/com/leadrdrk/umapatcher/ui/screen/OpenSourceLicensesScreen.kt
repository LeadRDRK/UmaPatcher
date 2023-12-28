package com.leadrdrk.umapatcher.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.ui.component.BackButton
import com.leadrdrk.umapatcher.ui.component.TopBar
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun OpenSourceLicensesScreen(navigator: DestinationsNavigator) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.open_source_licenses),
                navigationIcon = { BackButton(navigator) }
            )
        }
    ) { contentPadding ->
        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = contentPadding,
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                badgeBackgroundColor = MaterialTheme.colorScheme.primary,
                badgeContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}