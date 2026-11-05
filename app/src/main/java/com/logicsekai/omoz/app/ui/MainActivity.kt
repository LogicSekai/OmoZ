// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import com.logicsekai.omoz.app.engine.StartReason
import com.logicsekai.omoz.app.system.Permissions
import com.logicsekai.omoz.app.ui.theme.OmoZTheme
import com.logicsekai.omoz.app.update.UpdateNotifier

class MainActivity : ComponentActivity() {

    private lateinit var vm: MainViewModel

    /** Screen to open on the next composition (e.g. from the update notification). */
    private val openRoute = mutableStateOf<String?>(null)

    private val firstRunPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm = ViewModelProvider(this)[MainViewModel::class.java]

        if (savedInstanceState == null && !Permissions.hasLocation(this)) {
            firstRunPermissions.launch(Permissions.foregroundSet())
        }
        handleIntent(intent)

        setContent {
            OmoZTheme {
                OmoZApp(
                    vm = vm,
                    openRoute = openRoute.value,
                    onRouteOpened = { openRoute.value = null },
                    onExit = { vm.stopEngine(); finishAndRemoveTask() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra(UpdateNotifier.EXTRA_OPEN) == UpdateNotifier.OPEN_UPDATE) {
            openRoute.value = Routes.UPDATE
            intent.removeExtra(UpdateNotifier.EXTRA_OPEN)
        }
        if (intent?.action == ACTION_START_ENGINE) {
            vm.startEngine(StartReason.BLUETOOTH)
            intent.action = null
        }
    }

    companion object {
        const val ACTION_START_ENGINE = "com.logicsekai.omoz.action.OPEN_AND_START"
    }
}
