// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.logicsekai.omoz.app.BuildConfig
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.system.SystemSettings
import com.logicsekai.omoz.app.ui.components.AccentLinkRow
import com.logicsekai.omoz.app.ui.components.ArtStyle
import com.logicsekai.omoz.app.ui.components.EngineArt
import com.logicsekai.omoz.app.ui.components.OmoCard
import com.logicsekai.omoz.app.ui.components.TopBar
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.OrangeSoft
import com.logicsekai.omoz.app.ui.theme.Peach
import com.logicsekai.omoz.engine.EngineCatalog

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenSupport: () -> Unit, onOpenUpdate: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Peach).statusBarsPadding()) {
        TopBar(stringResource(R.string.nav_about), onBack, stringResource(R.string.back))
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OmoCard(Modifier.fillMaxWidth(), radius = 28.dp) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Orange)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    EngineArt(ArtStyle.TURBINE, CardWhite, OrangeSoft, Modifier.size(60.dp), spin = 8f)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME, EngineCatalog.all.size),
                    style = MaterialTheme.typography.bodySmall, color = InkMuted, modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
            AccentLinkRow(stringResource(R.string.nav_support), stringResource(R.string.about_support_sub), onOpenSupport)
            AccentLinkRow(stringResource(R.string.nav_update), stringResource(R.string.about_update_sub), onOpenUpdate)
            if (BuildConfig.SOURCE_URL.isNotBlank()) {
                AccentLinkRow(
                    stringResource(R.string.about_source),
                    BuildConfig.SOURCE_URL.removePrefix("https://"),
                    { SystemSettings.openUrl(context, BuildConfig.SOURCE_URL) },
                )
                AccentLinkRow(
                    stringResource(R.string.about_license_row),
                    stringResource(R.string.about_license_row_sub),
                    { SystemSettings.openUrl(context, BuildConfig.SOURCE_URL + "/blob/main/COMMERCIAL-LICENSE.md") },
                )
            }
            OmoCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_contribute_title), style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_contribute_body), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            OmoCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_safety_title), style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_safety_body), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            OmoCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_licenses_title), style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_licenses_body), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
