package com.lyra.player.ui.screen.account

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.data.profile.UserProfileStore
import com.lyra.player.data.sync.webdav.WebDavStorage
import com.lyra.player.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.melox.theme.MeloxSystemColors
import com.lyra.player.ui.melox.theme.MeloxType
import com.lyra.player.ui.viewmodel.WebDavSyncViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * 参考播放器的「我的」页：未登录时是 WebDAV 登录表单，
 * 登录后展示头像、昵称、听歌历史与云端歌单。
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCloudPlaylists: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webDavVm: WebDavSyncViewModel = viewModel()
    val state by webDavVm.uiState.collectAsStateWithLifecycle()
    val storage = remember(context) { WebDavStorage(context) }
    val profileStore = remember(context) { UserProfileStore.getInstance(context) }

    LaunchedEffect(webDavVm, context) {
        webDavVm.initialize(context)
    }

    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var basePath by remember { mutableStateOf("") }
    var formInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(state.isConfigured, state.serverUrl, state.username) {
        if (!formInitialized) {
            serverUrl = storage.getServerUrl().orEmpty()
            username = storage.getUsername().orEmpty()
            password = storage.getPassword().orEmpty()
            basePath = storage.getBasePath()
            formInitialized = true
        }
    }

    var wasConfigured by remember { mutableStateOf<Boolean?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }
    val profile by profileStore.profileFlow.collectAsStateWithLifecycle()
    val fallbackName = stringResource(R.string.account_name_label)
    val displayName = profile.displayName.ifBlank {
        state.username.ifBlank { fallbackName }
    }

    // 登录成功 → 自动同步一次。
    LaunchedEffect(state.isConfigured) {
        val configured = state.isConfigured
        if (wasConfigured == false && configured) {
            profileStore.rememberActiveAccount(state.username)
            if (profileStore.profileFor(state.username).displayName.isBlank()) {
                profileStore.setName(state.username, state.username)
            }
            webDavVm.performSync(context)
        }
        if (configured && profileStore.profileFlow.value.username != state.username) {
            profileStore.rememberActiveAccount(state.username)
        }
        wasConfigured = configured
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
                        val file = File(dir, "avatar-${state.username.ifBlank { "local" }}.img")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        file.absolutePath
                    }.getOrNull()
                }
                if (savedPath != null) {
                    profileStore.setAvatarUri(state.username, savedPath)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountCircleButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (state.isConfigured) {
                    AccountCircleButton(onClick = { showLogoutConfirm = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                            contentDescription = stringResource(R.string.account_logout),
                            modifier = Modifier.size(21.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.account_title),
                style = MeloxType.largeTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.account_title_sub),
                style = MeloxType.subheadline,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(22.dp))

            if (!state.isConfigured) {
                LoginForm(
                    serverUrl = serverUrl,
                    onServerUrlChange = { serverUrl = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    basePath = basePath,
                    onBasePathChange = { basePath = it },
                    isValidating = state.isValidating,
                    errorMessage = state.errorMessage,
                    onLogin = {
                        webDavVm.validateAndSaveConfiguration(
                            context = context,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            basePath = basePath,
                        )
                    },
                )
            } else {
                ProfileHeader(
                    avatarUri = profile.avatarUri,
                    displayName = displayName,
                    accountName = state.username,
                    onPickAvatar = { avatarPicker.launch("image/*") },
                    onEditName = {
                        draftName = displayName
                        showNameDialog = true
                    },
                )

                Spacer(Modifier.height(22.dp))
                AccountMenuCard(
                    onOpenHistory = onOpenHistory,
                    onOpenCloudPlaylists = onOpenCloudPlaylists,
                )

                Spacer(Modifier.height(18.dp))
                SyncStatusCard(
                    isSyncing = state.isSyncing,
                    lastSyncTime = state.lastSyncTime,
                    successMessage = state.successMessage,
                    errorMessage = state.errorMessage,
                    onSyncNow = { webDavVm.performSync(context) },
                )
            }
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.account_logout_confirm_title)) },
            text = { Text(stringResource(R.string.account_logout_confirm_message)) },
            confirmButton = {
                Text(
                    text = stringResource(R.string.account_logout),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable {
                            showLogoutConfirm = false
                            profileStore.clearActiveAccount()
                            webDavVm.clearConfiguration(context)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(R.string.action_cancel),
                    modifier = Modifier
                        .clickable { showLogoutConfirm = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.account_edit_name)) },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.account_name_label)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            },
            confirmButton = {
                Text(
                    text = stringResource(R.string.action_confirm),
                    modifier = Modifier
                        .clickable {
                            profileStore.setName(state.username, draftName.trim())
                            showNameDialog = false
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(R.string.action_cancel),
                    modifier = Modifier
                        .clickable { showNameDialog = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
        )
    }
}

@Composable
private fun AccountCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                MeloxShapes.circle
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 头像圆钮，首页右上角与账号页共用。 */
@Composable
fun AccountAvatar(
    avatarUri: String?,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUri.isNullOrBlank()) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_default_avatar_placeholder),
                contentDescription = stringResource(R.string.account_default_avatar_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            AsyncImage(
                model = File(avatarUri),
                contentDescription = stringResource(R.string.account_default_avatar_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    avatarUri: String?,
    displayName: String,
    accountName: String,
    onPickAvatar: () -> Unit,
    onEditName: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AccountAvatar(
            avatarUri = avatarUri,
            size = 82.dp,
            modifier = Modifier.clickable(onClick = onPickAvatar),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MeloxType.title2,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onEditName),
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = accountName,
                    style = MeloxType.caption,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.account_avatar_hint),
                style = MeloxType.caption,
                color = MeloxSystemColors.Blue.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun AccountMenuCard(
    onOpenHistory: () -> Unit,
    onOpenCloudPlaylists: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                MeloxShapes.card
            ),
    ) {
        AccountMenuRow(
            icon = Icons.Outlined.History,
            label = stringResource(R.string.account_history),
            onClick = onOpenHistory,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 52.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        )
        AccountMenuRow(
            icon = Icons.Outlined.Cloud,
            label = stringResource(R.string.account_cloud_playlists),
            onClick = onOpenCloudPlaylists,
        )
    }
}

@Composable
private fun AccountMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MeloxSystemColors.Blue,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MeloxType.body,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun SyncStatusCard(
    isSyncing: Boolean,
    lastSyncTime: Long,
    successMessage: String?,
    errorMessage: String?,
    onSyncNow: () -> Unit,
) {
    val lastSyncText = remember(lastSyncTime) {
        if (lastSyncTime <= 0L) null
        else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(lastSyncTime))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                MeloxShapes.card
            )
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.account_last_sync),
            style = MeloxType.caption,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Text(
            text = lastSyncText ?: stringResource(R.string.account_never_synced),
            style = MeloxType.subheadline,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val message = errorMessage ?: successMessage
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MeloxType.caption,
                color = if (errorMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(MeloxShapes.capsule)
                .background(MeloxSystemColors.Blue.copy(alpha = if (isSyncing) 0.5f else 1f))
                .clickable(enabled = !isSyncing, onClick = onSyncNow)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(
                        if (isSyncing) R.string.account_syncing else R.string.account_sync_now
                    ),
                    style = MeloxType.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    basePath: String,
    onBasePathChange: (String) -> Unit,
    isValidating: Boolean,
    errorMessage: String?,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                MeloxShapes.largeCard
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.account_login_desc),
            style = MeloxType.caption,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        AccountTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = stringResource(R.string.webdav_server_url_label),
            placeholder = stringResource(R.string.webdav_server_url_placeholder),
            keyboardType = KeyboardType.Uri,
        )
        AccountTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.webdav_username_label),
            placeholder = stringResource(R.string.webdav_username_placeholder),
        )
        AccountTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.webdav_password_label),
            password = true,
        )
        AccountTextField(
            value = basePath,
            onValueChange = onBasePathChange,
            label = stringResource(R.string.webdav_base_path_label),
            placeholder = stringResource(R.string.webdav_base_path_placeholder),
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MeloxType.caption,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MeloxShapes.capsule)
                .background(MeloxSystemColors.Blue.copy(alpha = if (isValidating) 0.5f else 1f))
                .clickable(enabled = !isValidating, onClick = onLogin)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.account_login_title),
                    style = MeloxType.headline,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun AccountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else {
            { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        singleLine = true,
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else keyboardType,
            imeAction = ImeAction.Next,
        ),
        shape = MeloxShapes.compact,
        textStyle = MeloxType.body,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
            unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
