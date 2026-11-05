// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.logicsekai.omoz.app.BuildConfig
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.system.SystemSettings
import com.logicsekai.omoz.app.ui.components.AccentLinkRow
import com.logicsekai.omoz.app.ui.components.OmoCard
import com.logicsekai.omoz.app.ui.components.SecondaryButton
import com.logicsekai.omoz.app.ui.components.TopBar
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.OrangeDeep
import com.logicsekai.omoz.app.ui.theme.Peach

/**
 * Donations and ways to contribute. Links come from BuildConfig; a blank URL hides its row
 * (Google Play builds hide the donation links, which Play's payments policy does not allow).
 */
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = SystemSettings.openUrl(context, url)
    val source = BuildConfig.SOURCE_URL
    val community = BuildConfig.COMMUNITY_URL

    Column(Modifier.fillMaxSize().background(Peach).statusBarsPadding()) {
        TopBar(stringResource(R.string.support_title), onBack, stringResource(R.string.back))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OmoCard(Modifier.fillMaxWidth(), radius = 28.dp, color = Orange) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrangeDeep)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = CardWhite, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.support_hero_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = CardWhite,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.support_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CardWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val donations = listOf(
                Triple(R.string.support_sponsor, R.string.support_sponsor_caption, BuildConfig.SPONSOR_URL),
                Triple(R.string.support_kofi, R.string.support_kofi_caption, BuildConfig.KOFI_URL),
                Triple(R.string.support_trakteer, R.string.support_trakteer_caption, BuildConfig.TRAKTEER_URL),
            ).filter { it.third.isNotBlank() }
            if (donations.isNotEmpty()) {
                Section(stringResource(R.string.support_section_donate))
                donations.forEach { (title, caption, url) ->
                    AccentLinkRow(stringResource(title), stringResource(caption), { open(url) })
                }
            }

            if (source.isNotBlank()) {
                Section(stringResource(R.string.support_section_contribute))
                AccentLinkRow(stringResource(R.string.support_code), stringResource(R.string.support_code_caption), { open(source) })
                AccentLinkRow(stringResource(R.string.support_issue), stringResource(R.string.support_issue_caption), { open("$source/issues/new/choose") })
                AccentLinkRow(stringResource(R.string.support_guide), stringResource(R.string.support_guide_caption), { open("$source/blob/main/CONTRIBUTING.md") })
            }
            if (community.isNotBlank()) {
                AccentLinkRow(stringResource(R.string.support_community), community.removePrefix("https://"), { open(community) })
            }

            Spacer(Modifier.height(4.dp))
            val shareText = stringResource(R.string.support_share_text, community.ifBlank { source })
            SecondaryButton(
                stringResource(R.string.support_share),
                { share(context, shareText) },
                icon = rememberVectorPainter(Icons.Rounded.Share),
            )
            Text(
                stringResource(R.string.support_thanks),
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 6.dp))
}

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    try {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No app can share text; nothing to do.
    }
}
