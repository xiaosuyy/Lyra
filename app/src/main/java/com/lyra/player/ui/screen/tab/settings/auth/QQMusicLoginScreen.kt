package com.lyra.player.ui.screen.tab.settings.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.activity.auth.QQMusicWebLoginActivity
import com.lyra.player.core.api.qqmusic.QQMusicAccountProfile
import com.lyra.player.ui.viewmodel.auth.QQMusicLoginViewModel

/** QQ 音乐账号登录页，登录后展示昵称头像并可退出。 */
@Composable
internal fun QQMusicLoginScreen(
    vm: QQMusicLoginViewModel,
    onBack: () -> Unit
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val cookieJson = result.data
                ?.getStringExtra(QQMusicWebLoginActivity.RESULT_COOKIE)
                .orEmpty()
            vm.loginWithCookies(cookieJson)
        }
    }

    BackHandler(onBack = onBack)
    LaunchedEffect(uiState.loginCompleted) {
        if (uiState.loginCompleted) {
            vm.consumeLoginCompleted()
            onBack()
        }
    }

    val foreground = MaterialTheme.colorScheme.onBackground
    val secondary = foreground.copy(alpha = 0.58f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            Spacer(Modifier.height(18.dp))
            if (!uiState.isLoggedIn) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_qq_music),
                        contentDescription = null,
                        modifier = Modifier.size(58.dp)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.qq_login_screen_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = foreground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.qq_login_screen_desc),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = secondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(30.dp))
            } else {
                Spacer(Modifier.height(60.dp))
            }

            if (uiState.isLoggedIn) {
                QQMusicLoggedInContent(
                    profile = uiState.profile,
                    loadingProfile = uiState.loadingProfile,
                    onLogout = vm::logout,
                    onBack = onBack
                )
            } else {
                Button(
                    onClick = {
                        vm.clearError()
                        webLoginLauncher.launch(
                            Intent(context, QQMusicWebLoginActivity::class.java)
                        )
                    },
                    enabled = !uiState.loggingIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (uiState.loggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.qq_login_web_button),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.qq_login_web_hint),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = secondary,
                    textAlign = TextAlign.Center
                )
                uiState.error?.let { message ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (!uiState.isLoggedIn) {
            Text(
                text = stringResource(R.string.qq_login_privacy_note),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = foreground.copy(alpha = 0.42f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QQMusicLoggedInContent(
    profile: QQMusicAccountProfile?,
    loadingProfile: Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val foreground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val avatarUrl = profile?.avatarUrl
            if (avatarUrl.isNullOrBlank()) {
                Image(
                    painter = painterResource(R.drawable.ic_qq_music),
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = profile?.nickname
                ?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.qq_login_entry_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
            textAlign = TextAlign.Center
        )
        if (loadingProfile && profile == null) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.action_done),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = stringResource(R.string.qq_login_logout),
                fontSize = 15.sp
            )
        }
    }
}
