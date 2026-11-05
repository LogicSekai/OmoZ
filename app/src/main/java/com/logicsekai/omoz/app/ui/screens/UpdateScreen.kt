// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicsekai.omoz.app.BuildConfig
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.system.SystemSettings
import com.logicsekai.omoz.app.ui.components.OmoCard
import com.logicsekai.omoz.app.ui.components.OmoSwitch
import com.logicsekai.omoz.app.ui.components.PrimaryButton
import com.logicsekai.omoz.app.ui.components.SecondaryButton
import com.logicsekai.omoz.app.ui.components.TopBar
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.OrangeDeep
import com.logicsekai.omoz.app.ui.theme.Peach
import com.logicsekai.omoz.app.ui.theme.TrackGray
import com.logicsekai.omoz.app.update.UpdateManager
import com.logicsekai.omoz.app.update.UpdateManager.State
import java.util.Locale

/** App update: shows the installed version, checks GitHub Releases, downloads and installs. */
@Composable
fun UpdateScreen(manager: UpdateManager, onBack: () -> Unit) {
    val state by manager.state.collectAsStateWithLifecycle()
    val autoCheck by manager.autoCheck.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var needsPermission by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { manager.check() }

    fun openPage(url: String) {
        SystemSettings.openUrl(context, url.ifBlank { manager.releasesUrl })
    }

    Column(Modifier.fillMaxSize().background(Peach).statusBarsPadding()) {
        TopBar(stringResource(R.string.update_title), onBack, stringResource(R.string.back))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OmoCard(Modifier.fillMaxWidth(), radius = 28.dp, color = Orange) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(OrangeDeep),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_system_update), null, tint = CardWhite, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.update_installed), style = MaterialTheme.typography.labelMedium, color = CardWhite.copy(alpha = 0.85f))
                        Text(
                            "OmoZ ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleLarge,
                            color = CardWhite,
                        )
                    }
                }
            }

            if (!manager.isEnabled) {
                OmoCard(Modifier.fillMaxWidth()) {
                    Title(stringResource(R.string.update_disabled_title))
                    Caption(stringResource(R.string.update_disabled_body))
                }
            } else {
                StatusCard(
                    state = state,
                    canSelfInstall = manager.canSelfInstall,
                    lastCheckMillis = manager.lastCheckMillis,
                    needsPermission = needsPermission,
                    onCheck = { manager.check(force = true) },
                    onDownload = manager::download,
                    onCancel = manager::cancelDownload,
                    onInstall = {
                        needsPermission = manager.install(context) == UpdateManager.InstallResult.NEEDS_PERMISSION
                    },
                    onOpenPage = ::openPage,
                )

                OmoCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.update_auto_title), style = MaterialTheme.typography.titleSmall, color = Ink)
                            Text(stringResource(R.string.update_auto_caption), style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        }
                        Spacer(Modifier.width(12.dp))
                        OmoSwitch(autoCheck, manager::setAutoCheck)
                    }
                }

                Text(
                    stringResource(R.string.update_safety_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusCard(
    state: State,
    canSelfInstall: Boolean,
    lastCheckMillis: Long,
    needsPermission: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val refresh = rememberVectorPainter(Icons.Rounded.Refresh)
    val download = painterResource(R.drawable.ic_download)
    val openIcon = painterResource(R.drawable.ic_open_in_new)
    OmoCard(Modifier.fillMaxWidth()) {
        when (state) {
            State.Idle, State.Checking -> {
                Title(stringResource(R.string.update_checking))
                Spacer(Modifier.height(12.dp))
                Progress(null)
            }

            State.UpToDate -> {
                Title(stringResource(R.string.update_up_to_date))
                if (lastCheckMillis > 0) Caption(stringResource(R.string.update_last_checked, relativeTime(lastCheckMillis)))
                Spacer(Modifier.height(14.dp))
                SecondaryButton(stringResource(R.string.update_check_again), onCheck, icon = refresh)
            }

            is State.Available -> {
                val release = state.release
                Title(stringResource(R.string.update_available, release.version))
                if (release.apkSize > 0) Caption(stringResource(R.string.update_size, megabytes(release.apkSize)))
                ReleaseNotes(release.notes)
                Spacer(Modifier.height(14.dp))
                if (canSelfInstall && release.apkUrl != null) {
                    PrimaryButton(stringResource(R.string.update_download), onDownload, icon = download)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(stringResource(R.string.update_open_page), { onOpenPage(release.pageUrl) }, icon = openIcon)
                } else {
                    PrimaryButton(stringResource(R.string.update_open_page), { onOpenPage(release.pageUrl) }, icon = openIcon)
                }
            }

            is State.Downloading -> {
                Title(stringResource(R.string.update_downloading, state.release.version))
                Spacer(Modifier.height(12.dp))
                Progress(state.progress)
                state.progress?.let {
                    Spacer(Modifier.height(6.dp))
                    Caption("${(it * 100).toInt()}%")
                }
                Spacer(Modifier.height(14.dp))
                SecondaryButton(stringResource(R.string.update_cancel), onCancel)
            }

            is State.ReadyToInstall -> {
                Title(stringResource(R.string.update_ready, state.release.version))
                Caption(stringResource(R.string.update_ready_body))
                if (needsPermission) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.update_needs_permission), style = MaterialTheme.typography.bodySmall, color = Orange)
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.update_install), onInstall, icon = rememberVectorPainter(Icons.Rounded.CheckCircle))
            }

            is State.Failed -> {
                Title(stringResource(R.string.update_failed_title))
                Caption(
                    stringResource(
                        when (state.problem) {
                            UpdateManager.Problem.NETWORK -> R.string.update_failed_network
                            UpdateManager.Problem.CHECKSUM -> R.string.update_failed_checksum
                            UpdateManager.Problem.INVALID_APK -> R.string.update_failed_invalid
                            UpdateManager.Problem.STORAGE -> R.string.update_failed_storage
                        },
                    ),
                )
                Spacer(Modifier.height(14.dp))
                val release = state.release
                val retryDownload = release != null && canSelfInstall && release.apkUrl != null &&
                    state.problem == UpdateManager.Problem.NETWORK
                PrimaryButton(stringResource(R.string.update_retry), if (retryDownload) onDownload else onCheck, icon = refresh)
                Spacer(Modifier.height(10.dp))
                SecondaryButton(stringResource(R.string.update_open_page), { onOpenPage(release?.pageUrl.orEmpty()) }, icon = openIcon)
            }
        }
    }
}

@Composable
private fun Progress(value: Float?) {
    val modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
    if (value == null) {
        LinearProgressIndicator(modifier, color = Orange, trackColor = TrackGray)
    } else {
        LinearProgressIndicator(progress = { value }, modifier = modifier, color = Orange, trackColor = TrackGray)
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Ink)
}

@Composable
private fun Caption(text: String) {
    Spacer(Modifier.height(4.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = InkMuted)
}

@Composable
private fun ReleaseNotes(notes: String) {
    if (notes.isBlank()) return
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.update_whats_new), style = MaterialTheme.typography.labelLarge, color = Ink)
    Spacer(Modifier.height(4.dp))
    Text(
        notes.replace("\r", "").take(MAX_NOTES),
        style = MaterialTheme.typography.bodySmall,
        color = InkMuted,
        maxLines = 16,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun relativeTime(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

private fun megabytes(bytes: Long): String = String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576f)

private const val MAX_NOTES = 2_000
