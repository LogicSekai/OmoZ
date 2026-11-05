// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.logicsekai.omoz.app.BuildConfig
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.ui.components.DrawerItem
import com.logicsekai.omoz.app.ui.components.DrawerScaffold
import com.logicsekai.omoz.app.ui.screens.AboutScreen
import com.logicsekai.omoz.app.ui.screens.BluetoothScreen
import com.logicsekai.omoz.app.ui.screens.HomeScreen
import com.logicsekai.omoz.app.ui.screens.SettingsScreen
import com.logicsekai.omoz.app.ui.screens.SoundsScreen
import com.logicsekai.omoz.app.ui.screens.SupportScreen
import com.logicsekai.omoz.app.ui.screens.UpdateScreen
import com.logicsekai.omoz.app.update.UpdateManagerOwner

object Routes {
    const val HOME = "home"
    const val SOUNDS = "sounds"
    const val BLUETOOTH = "bluetooth"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val SUPPORT = "support"
    const val UPDATE = "update"
}

private data class MenuEntry(val route: String, val label: Int, val icon: @Composable () -> Painter)

@Composable
fun OmoZApp(vm: MainViewModel, openRoute: String?, onRouteOpened: () -> Unit, onExit: () -> Unit) {
    val nav = rememberNavController()
    val updates = (LocalContext.current.applicationContext as UpdateManagerOwner).updateManager
    val updateState by updates.state.collectAsStateWithLifecycle()
    var drawerOpen by remember { mutableStateOf(false) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.HOME

    fun go(to: String) {
        drawerOpen = false
        if (to == route) return
        nav.navigate(to) {
            popUpTo(Routes.HOME) { saveState = false }
            launchSingleTop = true
        }
    }

    // A tapped "new version" notification opens the update screen.
    LaunchedEffect(openRoute) {
        if (openRoute != null) {
            go(openRoute)
            onRouteOpened()
        }
    }

    val entries = listOf(
        MenuEntry(Routes.HOME, R.string.nav_home) { rememberVectorPainter(Icons.Rounded.Home) },
        MenuEntry(Routes.SOUNDS, R.string.nav_sounds) { painterResource(R.drawable.ic_turbine) },
        MenuEntry(Routes.BLUETOOTH, R.string.nav_bluetooth) { painterResource(R.drawable.ic_bluetooth) },
        MenuEntry(Routes.SETTINGS, R.string.nav_settings) { rememberVectorPainter(Icons.Rounded.Settings) },
        MenuEntry(Routes.SUPPORT, R.string.nav_support) { painterResource(R.drawable.ic_heart) },
        MenuEntry(Routes.ABOUT, R.string.nav_about) { rememberVectorPainter(Icons.Rounded.Info) },
    )

    DrawerScaffold(
        open = drawerOpen,
        footer = "OmoZ ${BuildConfig.VERSION_NAME}",
        closeLabel = stringResource(R.string.close),
        onClose = { drawerOpen = false },
        menu = {
            entries.forEach { e ->
                DrawerItem(e.icon(), stringResource(e.label), e.route == route) { go(e.route) }
            }
            DrawerItem(painterResource(R.drawable.ic_power), stringResource(R.string.nav_exit), false, onExit)
        },
    ) {
        NavHost(nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onMenu = { drawerOpen = true },
                    onOpenSounds = { go(Routes.SOUNDS) },
                    onOpenBluetooth = { go(Routes.BLUETOOTH) },
                    onOpenAbout = { go(Routes.ABOUT) },
                    updateVersion = (updateState as? com.logicsekai.omoz.app.update.UpdateManager.State.Available)?.release?.version,
                    onOpenUpdate = { go(Routes.UPDATE) },
                )
            }
            composable(Routes.SOUNDS) { SoundsScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.BLUETOOTH) { BluetoothScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm, onBack = { nav.popBackStack() }, onOpenUpdate = { go(Routes.UPDATE) })
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSupport = { go(Routes.SUPPORT) },
                    onOpenUpdate = { go(Routes.UPDATE) },
                )
            }
            composable(Routes.SUPPORT) { SupportScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.UPDATE) { UpdateScreen(updates, onBack = { nav.popBackStack() }) }
        }
    }
}
