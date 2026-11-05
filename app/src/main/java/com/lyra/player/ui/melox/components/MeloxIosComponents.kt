package com.lyra.player.ui.melox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.melox.theme.MeloxType

@Composable
fun MeloxIosTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleStyle: TextStyle = MeloxType.largeTitle,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
    ) {
        if (navigation != null) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    navigation()
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
            }
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        subtitle?.let {
            Text(
                text = it,
                style = MeloxType.subheadline,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
            )
        }
    }
}

@Composable
fun MeloxIosGroupedList(
    modifier: Modifier = Modifier,
    surfaceColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.045f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor, MeloxShapes.largeCard),
        content = content,
    )
}

@Composable
fun MeloxIosListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    chevronTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    onClick: (() -> Unit)? = null,
    showTopSeparator: Boolean = true,
) {
    val separator = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (subtitle == null) 56.dp else 68.dp)
            .drawBehind {
                if (showTopSeparator) {
                    drawLine(
                        color = separator,
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            it()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MeloxType.body,
                color = MaterialTheme.colorScheme.onBackground,
            )
            subtitle?.let {
                Text(
                    it,
                    style = MeloxType.subheadline,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        detail?.let {
            Text(
                it,
                style = MeloxType.subheadline,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
            )
        }
        trailing?.invoke()
        if (onClick != null && trailing == null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = chevronTint,
            )
        }
    }
}

@Composable
fun MeloxIosListIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}
