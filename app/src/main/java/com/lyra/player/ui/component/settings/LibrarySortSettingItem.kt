package com.lyra.player.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyra.player.R
import com.lyra.player.data.library.LibrarySortMode
import com.lyra.player.data.library.LibrarySortPreferences
import com.lyra.player.ui.component.overlay.DensityScaledAlertDialog as AlertDialog

private val LibrarySortSettingItemShape = RoundedCornerShape(18.dp)
private val LibrarySortOptionShape = RoundedCornerShape(16.dp)

@Composable
fun LibrarySortSettingItem(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember(context) { LibrarySortPreferences.getInstance(context) }
    val sortMode by preferences.sortMode.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier
            .clip(LibrarySortSettingItemShape)
            .clickable { showDialog = true },
        headlineContent = { Text(stringResource(R.string.settings_library_sort)) },
        supportingContent = { Text(librarySortModeLabel(sortMode)) },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Sort,
                contentDescription = null
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_library_sort_dialog_title)) },
            text = {
                Column {
                    LibrarySortMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(LibrarySortOptionShape)
                                .clickable {
                                    showDialog = false
                                    preferences.setSortMode(mode)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortMode == mode,
                                onClick = {
                                    showDialog = false
                                    preferences.setSortMode(mode)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = librarySortModeLabel(mode),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun librarySortModeLabel(mode: LibrarySortMode): String = when (mode) {
    LibrarySortMode.ADDED_TIME -> stringResource(R.string.settings_library_sort_added)
    LibrarySortMode.ALPHABETICAL -> stringResource(R.string.settings_library_sort_alphabetical)
}
