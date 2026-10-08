package com.zhehr.auralis.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.theme.AuralisTheme
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.theme.ThemeMode
import com.zhehr.auralis.ui.components.MiniPlayer
import com.zhehr.auralis.ui.navigation.TopLevel
import com.zhehr.auralis.ui.screens.DetailScreen
import com.zhehr.auralis.ui.screens.HomeScreen
import com.zhehr.auralis.ui.screens.LibraryScreen
import com.zhehr.auralis.ui.screens.NowPlayingScreen
import com.zhehr.auralis.ui.screens.SearchScreen
import com.zhehr.auralis.ui.screens.SettingsScreen

@Composable
fun AuralisApp() {
    val container = LocalContainer.current
    val themeMode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val reduceMotion by container.settings.reduceMotion.collectAsStateWithLifecycle(initialValue = false)

    AuralisTheme(mode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            PermissionGate { Shell(reduceMotion) }
        }
    }
}

@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalContainer.current
    val audioPerm =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    fun hasAudio() = ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasAudio()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAudio()
        asked = true
    }

    // Incremental: cheap when nothing changed.
    LaunchedEffect(granted) { if (granted) container.scanner.scan() }

    if (granted) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AURALIS", style = MaterialTheme.typography.titleLarge, color = LocalAuralisPalette.current.accent)
            Text("by ZHEHR", style = MaterialTheme.typography.labelSmall, color = LocalAuralisPalette.current.mutedText)
            Spacer(Modifier.height(32.dp))
            Text("Let AURALIS find your music", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "AURALIS needs to read the audio files on this phone to build your library. " +
                    "Everything stays on your device and nothing is uploaded. " +
                    "On Android 13 and newer it will also ask to show playback controls in your notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val perms = buildList {
                    add(audioPerm)
                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                launcher.launch(perms.toTypedArray())
            }) { Text("Continue") }
            if (asked) {
                TextButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                    )
                }) { Text("Open app settings") }
            }
        }
    }
}

@Composable
private fun Shell(reduceMotion: Boolean) {
    val container = LocalContainer.current
    val nav = rememberNavController()
    val currentId by container.player.currentId.collectAsStateWithLifecycle()
    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = nowPlayingOpen) { nowPlayingOpen = false }
    val ms = if (reduceMotion) 0 else 320

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    if (currentId != null) MiniPlayer(onOpen = { nowPlayingOpen = true })
                    AuralisNavigationBar(nav)
                }
            },
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = TopLevel.Home.route,
                modifier = Modifier.padding(inner),
                enterTransition = { fadeIn(tween(if (reduceMotion) 0 else 200)) },
                exitTransition = { fadeOut(tween(if (reduceMotion) 0 else 150)) },
                popEnterTransition = { fadeIn(tween(if (reduceMotion) 0 else 200)) },
                popExitTransition = { fadeOut(tween(if (reduceMotion) 0 else 150)) },
            ) {
                composable(TopLevel.Home.route) { HomeScreen(nav) }
                composable(TopLevel.Library.route) { LibraryScreen(nav) }
                composable(TopLevel.Search.route) { SearchScreen() }
                composable(TopLevel.Settings.route) { SettingsScreen() }
                composable(
                    "detail/{kind}/{key}",
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("key") { type = NavType.StringType },
                    ),
                ) { entry ->
                    DetailScreen(
                        kind = entry.arguments?.getString("kind").orEmpty(),
                        key = entry.arguments?.getString("key").orEmpty(),
                        nav = nav,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = nowPlayingOpen && currentId != null,
            enter = slideInVertically(tween(ms)) { it } + fadeIn(tween(ms)),
            exit = slideOutVertically(tween(ms)) { it } + fadeOut(tween(ms)),
        ) {
            NowPlayingScreen(onClose = { nowPlayingOpen = false }, reduceMotion = reduceMotion)
        }
    }
}

@Composable
private fun AuralisNavigationBar(nav: NavHostController) {
    val palette = LocalAuralisPalette.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    NavigationBar(containerColor = palette.surface) {
        TopLevel.entries.forEach { dest ->
            val selected = route == dest.route || (dest == TopLevel.Library && route?.startsWith("detail") == true)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(dest.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(dest.icon), contentDescription = null) },
                label = { Text(stringResource(dest.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = palette.accent,
                    unselectedIconColor = palette.mutedText,
                    unselectedTextColor = palette.mutedText,
                    indicatorColor = palette.surfaceHigh,
                ),
            )
        }
    }
}
