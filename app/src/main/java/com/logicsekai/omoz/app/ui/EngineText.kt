// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui

import android.annotation.SuppressLint
import android.content.Context
import com.logicsekai.omoz.engine.EngineInfo

/**
 * Localized engine name/description. Looks up `engine_<id>_name` / `engine_<id>_desc` string
 * resources and falls back to the English text in [EngineInfo], so a community sound works
 * even before anyone translates it.
 */
@SuppressLint("DiscouragedApi")
fun engineDisplayName(context: Context, info: EngineInfo): String =
    lookup(context, "engine_${info.id}_name") ?: info.name

@SuppressLint("DiscouragedApi")
fun engineDescription(context: Context, info: EngineInfo): String =
    lookup(context, "engine_${info.id}_desc") ?: info.description

@SuppressLint("DiscouragedApi")
private fun lookup(context: Context, key: String): String? {
    val id = context.resources.getIdentifier(key, "string", context.packageName)
    return if (id != 0) context.getString(id) else null
}
