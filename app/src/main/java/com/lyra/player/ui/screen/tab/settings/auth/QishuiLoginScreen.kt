package com.lyra.player.ui.screen.tab.settings.auth

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.ui.screen.tab.settings.auth/QishuiLoginScreen
 * Created: 2026/11/8
 */

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lyra.player.R
import com.lyra.player.ui.viewmodel.auth.QishuiLoginUiState
import com.lyra.player.ui.viewmodel.auth.QishuiLoginViewModel

private val QishuiGreenStart = Color(0xFF39D98A)
private val QishuiGreenEnd = Color(0xFF0FA968)

@Composable
internal fun QishuiAccountEntryCard(
    uiState: QishuiLoginUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.055f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(QishuiGreenStart, QishuiGreenEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        uiState.isLoggedIn && uiState.nickname.isNotBlank() -> uiState.nickname
                        uiState.isLoggedIn -> stringResource(R.string.qishui_account_title)
                        else -> stringResource(R.string.qishui_account_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = if (uiState.isLoggedIn) {
                        stringResource(R.string.qishui_account_logged_in)
                    } else {
                        stringResource(R.string.qishui_account_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
internal fun QishuiLoginScreen(
    vm: QishuiLoginViewModel,
    onBack: () -> Unit
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val latestOnBack by rememberUpdatedState(onBack)

    BackHandler(onBack = onBack)
    LaunchedEffect(uiState.loginCompleted) {
        if (uiState.loginCompleted) {
            vm.consumeLoginCompleted()
            latestOnBack()
        }
    }
    DisposableEffect(Unit) {
        vm.startQrLogin()
        onDispose { vm.stopQrLogin() }
    }

    val background = MaterialTheme.colorScheme.background
    val foreground = MaterialTheme.colorScheme.onBackground
    val secondary = foreground.copy(alpha = 0.58f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(foreground.copy(alpha = 0.06f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = foreground
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(QishuiGreenStart, QishuiGreenEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.qishui_login_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = foreground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))

            if (uiState.isLoggedIn) {
                Text(
                    text = uiState.nickname.ifBlank {
                        stringResource(R.string.qishui_account_title)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.qishui_account_logged_in),
                    fontSize = 14.sp,
                    color = secondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = vm::logout) {
                    Text(
                        text = stringResource(R.string.qishui_logout),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                QrCard(uiState = uiState)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = uiState.error ?: uiState.statusMessage.ifBlank {
                        stringResource(R.string.qishui_qr_scan_hint)
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (uiState.error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        secondary
                    },
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        vm.stopQrLogin()
                        vm.startQrLogin()
                    },
                    enabled = !uiState.loading
                ) {
                    Text(
                        text = stringResource(R.string.qishui_qr_refresh),
                        color = if (uiState.loading) secondary else QishuiGreenEnd
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = stringResource(R.string.qishui_login_scan_note),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = foreground.copy(alpha = 0.42f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QrCard(uiState: QishuiLoginUiState) {
    val qr = remember(uiState.qrLink) { decodeQr(uiState.qrLink) }
    Box(
        modifier = Modifier
            .size(232.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (qr != null) {
            Image(
                bitmap = qr,
                contentDescription = stringResource(R.string.qishui_qr_scan_hint),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            )
        } else {
            CircularProgressIndicator(
                color = QishuiGreenEnd,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun decodeQr(link: String): ImageBitmap? {
    if (link.isBlank()) return null
    return runCatching {
        val matrix = QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, 720, 720)
        val rendered = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                rendered.setPixel(
                    x,
                    y,
                    if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        rendered.asImageBitmap()
    }.getOrNull()
}
