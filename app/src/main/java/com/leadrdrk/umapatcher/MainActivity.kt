package com.leadrdrk.umapatcher

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.leadrdrk.umapatcher.core.UpdateChecker
import com.leadrdrk.umapatcher.ui.component.SimpleOkCancelDialog
import com.leadrdrk.umapatcher.ui.screen.BottomBarDestination
import com.leadrdrk.umapatcher.ui.screen.NavGraphs
import com.leadrdrk.umapatcher.ui.screen.destinations.PatchingScreenDestination
import com.leadrdrk.umapatcher.ui.theme.UmaPatcherTheme
import com.leadrdrk.umapatcher.utils.deleteRecursive
import com.leadrdrk.umapatcher.utils.workDir
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.topjohnwu.superuser.Shell

private val rootInitialized = mutableStateOf(false)

class MainActivity : ComponentActivity() {
    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appInit()

        setContent {
            UmaPatcherTheme {
                MainContent()
            }
        }

        UpdateChecker.init(this)
    }

    private fun appInit() {
        requestPermissions()

        // Init work directory
        workDir.mkdir()
        deleteRecursive(workDir, deleteRoot = false)

        // Request root permissions
        Shell.getShell { rootInitialized.value = true }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(
                RequestPermission()
            ) {}
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun useKeepScreenOn(callback: () -> Unit) {
        runOnUiThread {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        callback()
        runOnUiThread {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun MainContent() {
    val navController = rememberNavController()
    val navHostEngine = rememberAnimatedNavHostEngine(
        rootDefaultAnimations = navAnimations
    )

    val bottomBarState = rememberSaveable { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    when (navBackStackEntry?.destination?.route) {
        PatchingScreenDestination.route -> bottomBarState.value = false
        else -> bottomBarState.value = true
    }

    var openUpdateDialog by remember { mutableStateOf(false) }
    var tagName by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    UpdateChecker.callback = { name ->
        tagName = name
        openUpdateDialog = true
    }

    Scaffold(
        bottomBar = { BottomBar(navController, bottomBarState) }
    ) { innerPadding ->
        if (rootInitialized.value) {
            DestinationsNavHost(
                modifier = Modifier.padding(innerPadding),
                navGraph = NavGraphs.root,
                navController = navController,
                engine = navHostEngine
            )
        }
        if (openUpdateDialog) {
            SimpleOkCancelDialog(
                title = stringResource(R.string.update_available),
                onClose = { ok ->
                    openUpdateDialog = false
                    if (ok) uriHandler.openUri(UpdateChecker.getReleaseUrl(tagName))
                }
            ) {
                Text(
                    stringResource(R.string.update_available_desc).format(tagName)
                )
            }
        }
    }
}

private val navAnimations = RootNavGraphDefaultAnimations(
    enterTransition = {
        scaleIn(
            animationSpec = tween(200),
            initialScale = 0.9f
        ) + fadeIn(
            animationSpec = tween(200)
        )
    },
    exitTransition = {
        scaleOut(
            animationSpec = tween(200),
            targetScale = 0.9f
        ) + fadeOut(
            animationSpec = tween(200)
        )
    },
    popExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
            animationSpec = tween(200)
        )
    }
)

@Composable
private fun BottomBar(navController: NavHostController, bottomBarState: MutableState<Boolean>) {
    AnimatedVisibility(
        visible = bottomBarState.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NavigationBar(tonalElevation = 8.dp) {
            BottomBarDestination.entries.forEach { destination ->
                val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                NavigationBarItem(
                    selected = isCurrentDestOnBackStack,
                    onClick = {
                        if (isCurrentDestOnBackStack) {
                            navController.popBackStack(destination.direction, false)
                        }

                        navController.navigate(destination.direction.route) {
                            popUpTo(NavGraphs.root.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        if (isCurrentDestOnBackStack) {
                            Icon(destination.iconSelected, stringResource(destination.label))
                        } else {
                            Icon(destination.iconNotSelected, stringResource(destination.label))
                        }
                    },
                    label = { Text(stringResource(destination.label)) },
                    alwaysShowLabel = false
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainContent()
}