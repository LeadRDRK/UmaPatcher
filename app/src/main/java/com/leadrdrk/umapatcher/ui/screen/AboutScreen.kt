package com.leadrdrk.umapatcher.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leadrdrk.umapatcher.BuildConfig
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.UpdateChecker
import com.leadrdrk.umapatcher.ui.component.TopBar
import com.leadrdrk.umapatcher.ui.screen.destinations.OpenSourceLicensesScreenDestination
import com.leadrdrk.umapatcher.utils.safeNavigate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun AboutScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopBar(stringResource(R.string.about))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            val lifecycleOwner = LocalLifecycleOwner.current
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_about_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(96.dp, 96.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "v" + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Divider()
                Spacer(Modifier.height(4.dp))
                TextButtonWithIcon(
                    text = stringResource(R.string.check_for_updates),
                    icon = { Icon(Icons.Outlined.Refresh, null) }
                ) {
                    UpdateChecker.run(context)
                }
                TextButtonWithIcon(
                    text = stringResource(R.string.open_source_licenses),
                    icon = { Icon(Icons.Outlined.Info, null) }
                ) {
                    safeNavigate(lifecycleOwner) {
                        navigator.navigate(OpenSourceLicensesScreenDestination)
                    }
                }
                val uriHandler = LocalUriHandler.current
                TextButtonWithIcon(
                    text = stringResource(R.string.view_source_code),
                    icon = { Icon(painterResource(R.drawable.ic_github), null) }
                ) {
                    uriHandler.openUri("https://github.com/LeadRDRK/UmaPatcher")
                }
                TextButtonWithIcon(
                    text = stringResource(R.string.support_me),
                    icon = { Icon(painterResource(R.drawable.ic_kofi), null) }
                ) {
                    uriHandler.openUri("https://ko-fi.com/leadrdrk")
                }
            }
        }
    }
}

@Composable
fun TextButtonWithIcon(text: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth()
        ) {
            icon()
            Spacer(Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}