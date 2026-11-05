// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app

import android.content.Context
import com.logicsekai.omoz.app.data.SettingsRepository

/**
 * Minimal service locator. Kept deliberately simple (no DI framework) so new contributors
 * can follow every dependency by reading one file.
 */
object AppGraph {
    @Volatile private var settingsRepo: SettingsRepository? = null

    fun settings(context: Context): SettingsRepository =
        settingsRepo ?: synchronized(this) {
            settingsRepo ?: SettingsRepository(context.applicationContext).also { settingsRepo = it }
        }
}
