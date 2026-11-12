package com.lyra.player.ui.screen.tab.settings.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.viewmodel.auth.NeteaseLoginUiState
import com.lyra.player.ui.viewmodel.auth.QQMusicLoginUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val CardHeight = 76.dp

// 前置卡下缘正好压过后置卡头像的中线：76 - (14 + 44/2)
private val StackPeek = 40.dp

/**
 * 设置首页账号区的半堆叠卡片：网易云与 QQ 音乐两张入口卡叠放，
 * 上下滑动切换前置卡片，前置卡片点击进入对应登录页。
 */
@Composable
internal fun AccountStackedCards(
    neteaseState: NeteaseLoginUiState,
    qqState: QQMusicLoginUiState,
    frontIsQq: Boolean,
    onFrontChange: (Boolean) -> Unit,
    onOpenNetease: () -> Unit,
    onOpenQq: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(if (frontIsQq) 1f else 0f) }
    var animationJob by remember { mutableStateOf<Job?>(null) }

    fun animateToProgress(target: Float) {
        animationJob?.cancel()
        animationJob = scope.launch {
            animate(
                initialValue = progress,
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ -> progress = value }
        }
    }

    LaunchedEffect(frontIsQq) {
        val target = if (frontIsQq) 1f else 0f
        if (progress != target) animateToProgress(target)
    }

    val peekPx = with(LocalDensity.current) { StackPeek.toPx() }
    val dragState = rememberDraggableState { delta ->
        animationJob?.cancel()
        progress = (progress - delta / peekPx).coerceIn(0f, 1f)
    }
    val currentFrontIsQq by rememberUpdatedState(frontIsQq)
    val currentOnFrontChange by rememberUpdatedState(onFrontChange)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight + StackPeek)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        val commitToQq = when {
                            velocity < -900f -> true
                            velocity > 900f -> false
                            else -> progress >= 0.5f
                        }
                        if (commitToQq != currentFrontIsQq) {
                            currentOnFrontChange(commitToQq)
                        } else {
                            animateToProgress(if (commitToQq) 1f else 0f)
                        }
                    }
                )
        ) {
            val neteaseBehind = remember { derivedStateOf { progress >= 0.5f } }.value
            AccountEntryCard(
                title = neteaseState.profile?.nickname
                    ?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.netease_login_entry_title),
                subtitle = if (neteaseState.isLoggedIn) {
                    null
                } else {
                    stringResource(R.string.account_entry_tap_to_login)
                },
                leading = {
                    NeteaseAccountAvatar(
                        avatarUrl = neteaseState.profile?.avatarUrl,
                        size = 44.dp
                    )
                },
                onClick = {
                    if (neteaseBehind) onFrontChange(false) else onOpenNetease()
                },
                modifier = Modifier
                    .graphicsLayer {
                        val p = progress
                        translationY = peekPx * p
                        val scale = 1f - 0.04f * p
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.2f * p
                    }
                    .zIndex(if (neteaseBehind) 1f else 2f)
            )
            val qqBehind = !neteaseBehind
            AccountEntryCard(
                title = qqState.profile?.nickname
                    ?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.qq_login_entry_title),
                subtitle = if (qqState.isLoggedIn) {
                    null
                } else {
                    stringResource(R.string.account_entry_tap_to_login)
                },
                leading = { QQMusicEntryAvatar(avatarUrl = qqState.profile?.avatarUrl) },
                onClick = {
                    if (qqBehind) onFrontChange(true) else onOpenQq()
                },
                modifier = Modifier
                    .graphicsLayer {
                        val p = progress
                        translationY = peekPx * (1f - p)
                        val scale = 0.96f + 0.04f * p
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.8f + 0.2f * p
                    }
                    .zIndex(if (qqBehind) 1f else 2f)
            )
        }
    }
}

@Composable
private fun AccountEntryCard(
    title: String,
    subtitle: String?,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(MeloxShapes.largeCard)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MeloxShapes.largeCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun QQMusicEntryAvatar(avatarUrl: String?) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNullOrBlank()) {
            Image(
                painter = painterResource(R.drawable.ic_qq_music),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
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
