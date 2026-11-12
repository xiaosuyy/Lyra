package com.lyra.player.ui.screen.playlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyra.player.R
import com.lyra.player.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import com.lyra.player.ui.haptic.HapticIconButton
import com.lyra.player.ui.haptic.HapticTextButton
import com.lyra.player.ui.screen.tab.settings.auth.NeteaseAccountAvatar
import com.lyra.player.ui.melox.theme.MeloxType

/** 顶栏组合遮罩：外面一个大胶囊，里面每个图标再各自一个圆形遮罩。 */
@Composable
internal fun PlaylistModernTopBarPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PlaylistGlassMaskColor.copy(alpha = 0.5f))
            .border(0.75.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
internal fun PlaylistModernPillIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    HapticIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(38.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PlaylistGlassMaskColor)
                .border(0.75.dp, Color.Black.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides PlaylistGlassMaskInk) {
                content()
            }
        }
    }
}

/** 已登录且可参与导入 / 同步的账号。 */
internal data class FavoritesChannelAccount(
    val channelId: String,
    val channelLabel: String,
    val displayName: String,
    val avatarUrl: String?
)

internal enum class FavoritesChannelPickerMode { IMPORT, SYNC }

@Composable
internal fun FavoritesChannelPickerDialog(
    title: String,
    accounts: List<FavoritesChannelAccount>,
    selectedChannels: Set<String>,
    loading: Boolean,
    confirmEnabled: Boolean,
    onToggleChannel: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    loading -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.local_playlist_channel_loading_account),
                            style = MeloxType.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    accounts.isEmpty() -> Text(
                        text = stringResource(R.string.local_playlist_channel_no_account),
                        style = MeloxType.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { account ->
                            FavoritesChannelAccountRow(
                                account = account,
                                selected = account.channelId in selectedChannels,
                                onClick = { onToggleChannel(account.channelId) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            HapticTextButton(
                onClick = onConfirm,
                enabled = confirmEnabled
            ) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            HapticTextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun FavoritesChannelAccountRow(
    account: FavoritesChannelAccount,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        },
        animationSpec = tween(durationMillis = 150),
        label = "favorites-channel-row"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeteaseAccountAvatar(avatarUrl = account.avatarUrl, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = account.channelLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { onClick() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
