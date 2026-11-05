package com.lyra.player.ui.screen.tab.settings.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.activity.auth.NeteaseWebLoginActivity
import com.lyra.player.ui.viewmodel.auth.NeteaseAccountProfile
import com.lyra.player.ui.viewmodel.auth.NeteaseLoginUiState
import com.lyra.player.ui.viewmodel.auth.NeteaseLoginViewModel

/** 设置首页的网易云账号入口。 */
@Composable
internal fun NeteaseAccountEntryCard(
    uiState: NeteaseLoginUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = uiState.profile
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
            NeteaseAccountAvatar(
                avatarUrl = profile?.avatarUrl,
                size = 44.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        uiState.isLoggedIn && !profile?.nickname.isNullOrBlank() -> profile.nickname
                        uiState.isLoggedIn -> stringResource(R.string.netease_login_logged_in)
                        else -> stringResource(R.string.netease_login_entry_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = if (uiState.isLoggedIn) {
                        stringResource(R.string.netease_login_logged_in)
                    } else {
                        stringResource(R.string.netease_login_entry_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
                    maxLines = 1
                )
            }
        }
    }
}

/** 网易云手机号验证码登录页。 */
@Composable
internal fun NeteaseLoginScreen(
    vm: NeteaseLoginViewModel,
    onBack: () -> Unit
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val cookieJson = result.data
                ?.getStringExtra(NeteaseWebLoginActivity.RESULT_COOKIE)
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
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_netease_cloud_music),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.netease_login_screen_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = foreground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.netease_login_screen_desc),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = secondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))

            if (uiState.isLoggedIn) {
                NeteaseLoggedInContent(
                    profile = uiState.profile,
                    onLogout = vm::logout,
                    onBack = onBack
                )
            } else {
                NeteaseLoginForm(
                    uiState = uiState,
                    onPhoneChange = vm::onPhoneChange,
                    onCaptchaChange = vm::onCaptchaChange,
                    onSendCaptcha = vm::sendCaptcha,
                    onLogin = vm::login,
                    onWebLogin = {
                        webLoginLauncher.launch(
                            Intent(context, NeteaseWebLoginActivity::class.java)
                        )
                    }
                )
            }
        }

        NeteasePrivacyNote()
    }
}

@Composable
private fun NeteaseLoginForm(
    uiState: NeteaseLoginUiState,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onSendCaptcha: () -> Unit,
    onLogin: () -> Unit,
    onWebLogin: () -> Unit
) {
    val foreground = MaterialTheme.colorScheme.onBackground
    OutlinedTextField(
        value = uiState.phone,
        onValueChange = onPhoneChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !uiState.codeSent && !uiState.loggingIn,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.netease_login_phone_hint)) },
        leadingIcon = { Text("+86", color = foreground.copy(alpha = 0.72f)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = foreground.copy(alpha = 0.18f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )

    if (uiState.codeSent) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.captcha,
            onValueChange = onCaptchaChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.loggingIn,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.login_sms_code)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = foreground.copy(alpha = 0.18f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        TextButton(
            onClick = onSendCaptcha,
            enabled = uiState.countdownSec == 0 && !uiState.sending
        ) {
            Text(
                if (uiState.sending) {
                    stringResource(R.string.login_sending)
                } else if (uiState.countdownSec > 0) {
                    stringResource(R.string.settings_resend_code_countdown, uiState.countdownSec)
                } else {
                    stringResource(R.string.login_send_code)
                }
            )
        }
    }

    uiState.error?.let { error ->
        Text(
            text = error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(18.dp))
    Button(
        onClick = { if (uiState.codeSent) onLogin() else onSendCaptcha() },
        enabled = !uiState.loggingIn && !uiState.sending &&
            (if (uiState.codeSent) uiState.captcha.isNotBlank() else uiState.phone.isNotBlank()),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (uiState.sending || uiState.loggingIn) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = when {
                uiState.loggingIn -> stringResource(R.string.login_logging_in)
                uiState.sending -> stringResource(R.string.login_sending)
                uiState.codeSent -> stringResource(R.string.netease_login_verify)
                else -> stringResource(R.string.netease_login_continue)
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onWebLogin,
        enabled = !uiState.loggingIn && !uiState.sending,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = stringResource(R.string.netease_login_web_login),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.netease_login_web_hint),
        modifier = Modifier.fillMaxWidth(),
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun NeteaseLoggedInContent(
    profile: NeteaseAccountProfile?,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    NeteaseAccountAvatar(
        avatarUrl = profile?.avatarUrl,
        size = 96.dp
    )
    Spacer(Modifier.height(18.dp))
    Text(
        text = profile?.nickname?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.netease_login_logged_in),
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.netease_login_logged_in),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f)
    )
    Spacer(Modifier.height(26.dp))
    OutlinedButton(
        onClick = {
            onLogout()
            onBack()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(stringResource(R.string.netease_login_logout))
    }
}

@Composable
private fun NeteasePrivacyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.netease_login_privacy_note),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
        )
    }
}

@Composable
private fun NeteaseAccountAvatar(
    avatarUrl: String?,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNullOrBlank()) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_default_avatar_placeholder),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
