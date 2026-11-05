package com.lyra.player.ui.screen.tab.settings.miuix

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.lyra.player.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lyra.player.ui.haptic.performHapticFeedback
import com.lyra.player.ui.melox.theme.MeloxSystemColors
import com.lyra.player.ui.melox.theme.isMeloxDarkTheme

private val MiuixControlShape = RoundedCornerShape(16.dp)
private val MiuixDialogShape = RoundedCornerShape(28.dp)
private val MiuixInnerShape = RoundedCornerShape(12.dp)

@Composable
internal fun MiuixSettingsDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.widthIn(max = 340.dp),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = MiuixDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        properties = properties
    )
}

@Composable
internal fun MiuixSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    Button(
        onClick = {
            context.performHapticFeedback()
            onClick()
        },
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = MiuixControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
internal fun MiuixSettingsOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    Button(
        onClick = {
            context.performHapticFeedback()
            onClick()
        },
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = MiuixControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f),
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
            disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
internal fun MiuixSettingsTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable () -> Unit
) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            context.performHapticFeedback()
            onClick()
        },
        modifier = modifier.height(40.dp),
        enabled = enabled,
        shape = MiuixControlShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        text()
    }
}

@Composable
internal fun MiuixSettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = MiuixControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
            unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
            disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
internal fun MiuixSettingsInlineMessage(
    message: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val containerColor = if (isSuccess) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isSuccess) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val accentColor = if (isSuccess) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MiuixControlShape,
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun MiuixSettingsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val checkedTrack = if (isMeloxDarkTheme()) MeloxSystemColors.GreenDark else MeloxSystemColors.Green
    val uncheckedTrack = if (isMeloxDarkTheme()) {
        Color(0xFF787880).copy(alpha = 0.36f)
    } else {
        Color(0xFF787878).copy(alpha = 0.20f)
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = checkedTrack,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = uncheckedTrack,
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.70f),
            disabledCheckedTrackColor = checkedTrack.copy(alpha = 0.40f),
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.70f),
            disabledUncheckedTrackColor = uncheckedTrack.copy(alpha = 0.60f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixSettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f),
            activeTickColor = Color.White.copy(alpha = 0.55f),
            inactiveTickColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
        )
    )
}

@Composable
internal fun MiuixSettingsSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) {
        return
    }
    val context = LocalContext.current
    val safeSelectedIndex = selectedIndex.coerceIn(labels.indices)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(MiuixControlShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .height(48.dp)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / labels.size.toFloat()
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * safeSelectedIndex.toFloat(),
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            label = "miuix_segmented_tab_indicator"
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .fillMaxHeight()
                .fillMaxWidth(1f / labels.size)
                .clip(MiuixInnerShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f))
        )

        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == safeSelectedIndex
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "miuix_segmented_tab_content"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(MiuixInnerShape)
                        .clickable {
                            if (!selected) {
                                context.performHapticFeedback()
                                onSelectedIndexChange(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        Text(text = label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MiuixSettingsChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val supportingColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MiuixControlShape)
            .clickable(enabled = enabled) {
                context.performHapticFeedback()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, color = contentColor)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = supportingColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

@Composable
internal fun MiuixSettingsCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.size(40.dp),
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.colorScheme.outline,
            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            disabledCheckedColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledUncheckedColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
internal fun MiuixSettingsDialogContent(
    verticalSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

@Composable
internal fun MiuixSettingsDialogSpacer() {
    Spacer(Modifier.height(8.dp))
}
