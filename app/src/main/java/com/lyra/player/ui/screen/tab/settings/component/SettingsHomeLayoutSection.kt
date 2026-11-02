package com.lyra.player.ui.screen.tab.settings.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyra.player.R
import com.lyra.player.core.customsource.CustomSourceManager
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.source.MusicSourceCatalog
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.core.source.SourceManager
import com.lyra.player.core.source.SourceManagerState
import com.lyra.player.data.settings.HomeSectionId
import com.lyra.player.data.platform.youtube.YouTubeFeatureGate
import com.lyra.player.ui.screen.tab.settings.page.MiuixSettingsSectionIntro
import com.lyra.player.ui.viewmodel.tab.home.HomeSourceCatalog
import com.lyra.player.ui.viewmodel.tab.home.HomeSourceOption

@Composable
internal fun SettingsHomeLayoutSection(enabledSections: Set<HomeSectionId>) {
    val context = LocalContext.current
    val repository = AppContainer.homeLayoutRepository
    val config by repository.config.collectAsStateWithLifecycle()
    val visibleOrder = config.order.filter { it in enabledSections }
    val visibleSourceTargets = HomeSourceTargets.filter { it in enabledSections }

    // 首页来源选项动态生成：已启用/就绪的音源 ∩ 首页能力（有 Provider）
    val youtubeEnabled by AppContainer.settingsRepo.youtubeEnabledFlow
        .collectAsStateWithLifecycle(initialValue = YouTubeFeatureGate.isEnabled())
    val declaredCustomSourceIds by CustomSourceManager.declaredSourceIds
        .collectAsStateWithLifecycle(initialValue = emptySet())
    LaunchedEffect(Unit) {
        CustomSourceManager.ensureLoaded(context.applicationContext)
    }
    val sourceOptions: List<HomeSourceOption> = remember(youtubeEnabled, declaredCustomSourceIds) {
        val available = SourceManager.sources(
            SourceManagerState(
                youtubeEnabled = youtubeEnabled,
                declaredCustomSourceIds = declaredCustomSourceIds
            )
        ).filter { it.isReady }.mapTo(HashSet()) { it.id }
        HomeSourceCatalog.options(available)
    }

    MiuixSettingsSectionIntro(
        title = stringResource(R.string.settings_home_layout_order_title),
        description = stringResource(R.string.settings_home_layout_order_desc)
    )
    visibleOrder.forEachIndexed { index, section ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = index > 0,
                onClick = { repository.moveSection(section, -1) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                enabled = index < config.order.lastIndex,
                onClick = { repository.moveSection(section, +1) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = stringResource(section.titleResId),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    MiuixSettingsSectionIntro(
        title = stringResource(R.string.settings_home_layout_source_title),
        description = stringResource(R.string.settings_home_layout_source_desc)
    )
    visibleSourceTargets.forEach { section ->
        HomeSourcePickerRow(
            section = section,
            currentSourceId = config.sources[section] ?: MusicSourceIds.NETEASE,
            options = sourceOptions,
            onSelect = { repository.setSource(section, it) }
        )
    }
}

private val HomeSourceTargets = listOf(
    HomeSectionId.TRENDING,
    HomeSectionId.RADAR,
    HomeSectionId.RECOMMENDED
)

@Composable
private fun HomeSourcePickerRow(
    section: HomeSectionId,
    currentSourceId: String,
    options: List<HomeSourceOption>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabelResId = options.firstOrNull { it.sourceId == currentSourceId }?.labelResId
        ?: MusicSourceCatalog.nameResIdFor(currentSourceId)
        ?: 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(section.titleResId),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = if (currentLabelResId != 0) stringResource(currentLabelResId) else currentSourceId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (option.labelResId != 0) {
                                    stringResource(option.labelResId)
                                } else {
                                    option.sourceId
                                }
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(option.sourceId)
                        }
                    )
                }
            }
        }
    }
}
