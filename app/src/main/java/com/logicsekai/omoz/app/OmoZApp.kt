// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app

import android.app.Application
import com.logicsekai.omoz.app.service.EngineNotification
import com.logicsekai.omoz.app.update.UpdateChecker
import com.logicsekai.omoz.app.update.UpdateManager
import com.logicsekai.omoz.app.update.UpdateManagerOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OmoZApp : Application(), UpdateManagerOwner {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** New versions from GitHub Releases (also reached by the background update worker). */
    override val updateManager: UpdateManager by lazy {
        UpdateManager(
            context = this,
            checker = UpdateChecker(BuildConfig.UPDATE_REPO, BuildConfig.VERSION_NAME),
            canSelfInstall = BuildConfig.SELF_UPDATE,
            scope = appScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        EngineNotification.createChannels(this)
        updateManager.start()
    }
}
