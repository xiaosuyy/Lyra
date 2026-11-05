package com.lyra.player.ui.screen.tab.settings.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.lyra.player.R
import com.lyra.player.ui.LocalMiniPlayerHeight
import com.lyra.player.ui.effect.glass.AdvancedGlassNavigationHandoff
import com.lyra.player.ui.effect.glass.AdvancedGlassScene
import com.lyra.player.ui.effect.glass.LocalAdvancedGlassController
import com.lyra.player.ui.effect.glass.isolatedAdvancedGlassHorizontalTransition
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.melox.theme.MeloxType
import com.lyra.player.ui.util.currentWindowWidthDp
import com.lyra.player.ui.util.shouldAllowCollapsingTopAppBar

private val MiuixHighlightShape = RoundedCornerShape(18.dp)
private val MiuixSettingsContentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
private val MiuixPageRowPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
private val MiuixSettingsTabletMaxWidth = 920.dp

@Composable
private fun settingsCardSurface(): Color = MaterialTheme.colorScheme.surface

private fun shouldAnimateSettingsDetailTransition(
    initialPage: SettingsPage,
    targetPage: SettingsPage
): Boolean {
    return targetPage.backTargetPage() == initialPage ||
        initialPage.backTargetPage() == targetPage
}

private fun SettingsPage.participatesInSplitDetailTransition(): Boolean {
    return backTargetPage() != null || SettingsPage.entries.any { page ->
        page.backTargetPage() == this
    }
}

internal fun settingsPageRowTestTag(page: SettingsPage): String = "settings-page-row-${page.name}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixSettingsHomeScaffold(
    listState: LazyListState,
    topAppBarState: TopAppBarState,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        canScroll = {
            shouldAllowCollapsingTopAppBar(
                canScrollForward = listState.canScrollForward,
                canScrollBackward = listState.canScrollBackward,
                collapsedFraction = topAppBarState.collapsedFraction
            )
        }
    )
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val isTabletLayout = currentWindowWidthDp() >= 720.dp
    val horizontalPadding = if (isTabletLayout) 28.dp else 20.dp

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            val appBarScrim = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier.drawBehind {
                    val fraction = topAppBarState.collapsedFraction
                    if (fraction > 0f) {
                        drawRect(appBarScrim.copy(alpha = 0.94f * fraction))
                    }
                }
            ) {
                LargeTopAppBar(
                    title = { title() },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = MiuixSettingsTabletMaxWidth)
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 10.dp,
                    bottom = 18.dp + miniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.settingsHighlightTarget(
    targetId: String,
    highlightTargetId: String?,
    highlightPulse: Int,
    onHighlightFinished: (() -> Unit)? = null
): Modifier {
    val highlighted = targetId == highlightTargetId && highlightPulse > 0
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var highlightActive by remember(targetId) { mutableStateOf(false) }

    LaunchedEffect(highlighted, highlightPulse) {
        if (!highlighted) {
            highlightActive = false
            return@LaunchedEffect
        }
        repeat(2) {
            bringIntoViewRequester.bringIntoView()
            withFrameNanos { }
        }
        delay(80)
        repeat(4) { index ->
            highlightActive = index % 2 == 0
            delay(180)
        }
        highlightActive = false
        onHighlightFinished?.invoke()
    }

    val highlightColor by animateColorAsState(
        targetValue = if (highlightActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            Color.Transparent
        },
        label = "settings_item_highlight"
    )

    return this
        .bringIntoViewRequester(bringIntoViewRequester)
        .clip(MiuixHighlightShape)
        .background(highlightColor)
}

@Composable
internal fun MiuixSettingsSectionIntro(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MeloxType.caption,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f)
        )
        Text(
            text = description,
            style = MeloxType.caption,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.36f)
        )
    }
}

@Composable
internal fun MiuixSettingsPageGroupCard(
    pages: List<SettingsPage>,
    onPageClick: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
    selectedPage: SettingsPage? = null
) {
    if (pages.isEmpty()) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(settingsCardSurface(), MeloxShapes.largeCard)
    ) {
        pages.forEachIndexed { index, page ->
            MiuixSettingsPageRow(
                page = page,
                selected = selectedPage == page,
                showTopSeparator = index != 0,
                onClick = { onPageClick(page) }
            )
        }
    }
}

@Composable
private fun MiuixSettingsPageRow(
    page: SettingsPage,
    selected: Boolean,
    showTopSeparator: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val separator = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(settingsPageRowTestTag(page))
            .semantics {
                this.selected = selected
            }
            .drawBehind {
                if (showTopSeparator) {
                    drawLine(
                        color = separator,
                        start = Offset(48.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(MiuixPageRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = stringResource(page.titleRes),
                modifier = Modifier.size(22.dp),
                tint = accent
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(page.titleRes),
                style = MeloxType.body,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(page.descriptionRes),
                style = MeloxType.subheadline,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = accent.copy(alpha = 0.88f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixSettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    listState: LazyListState,
    topAppBarState: TopAppBarState,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        canScroll = {
            shouldAllowCollapsingTopAppBar(
                canScrollForward = listState.canScrollForward,
                canScrollBackward = listState.canScrollBackward,
                collapsedFraction = topAppBarState.collapsedFraction
            )
        }
    )
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val isTabletLayout = currentWindowWidthDp() >= 720.dp
    val horizontalPadding = if (isTabletLayout) 28.dp else 20.dp

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            val appBarScrim = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier.drawBehind {
                    val fraction = topAppBarState.collapsedFraction
                    if (fraction > 0f) {
                        drawRect(appBarScrim.copy(alpha = 0.94f * fraction))
                    }
                }
            ) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MeloxType.largeTitle,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = MiuixSettingsTabletMaxWidth)
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 10.dp,
                    bottom = 18.dp + miniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixSettingsResponsiveDetailScaffold(
    title: String,
    onBack: () -> Unit,
    listState: LazyListState,
    topAppBarState: TopAppBarState,
    splitLayout: Boolean,
    modifier: Modifier = Modifier,
    showSplitDetailBackButton: Boolean = false,
    selectedPage: SettingsPage? = null,
    homeListState: LazyListState,
    homeTopAppBarState: TopAppBarState,
    homeTitle: @Composable () -> Unit,
    homeContent: LazyListScope.() -> Unit,
    detailContent: (LazyListScope.(SettingsPage) -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    if (!splitLayout) {
        MiuixSettingsDetailScaffold(
            title = title,
            onBack = onBack,
            listState = listState,
            topAppBarState = topAppBarState,
            modifier = modifier,
            content = content
        )
        return
    }
    val isolateAdvancedGlassTransitions = LocalAdvancedGlassController.current.isEnabled

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
        ) {
            MiuixSettingsHomeScaffold(
                listState = homeListState,
                topAppBarState = homeTopAppBarState,
                title = homeTitle,
                content = homeContent
            )
        }
        Box(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
        ) {
            when {
                selectedPage == null -> {
                    MiuixSettingsDetailScaffold(
                        title = title,
                        onBack = onBack,
                        listState = listState,
                        topAppBarState = topAppBarState,
                        showBackButton = false,
                        content = content
                    )
                }

                !selectedPage.participatesInSplitDetailTransition() -> {
                    MiuixSettingsDetailScaffold(
                        title = title,
                        onBack = onBack,
                        listState = listState,
                        topAppBarState = topAppBarState,
                        showBackButton = showSplitDetailBackButton
                    ) {
                        if (detailContent == null) {
                            content()
                        } else {
                            detailContent(selectedPage)
                        }
                    }
                }

                else -> {
                    AnimatedContent(
                        targetState = selectedPage,
                        modifier = Modifier.fillMaxSize(),
                        label = "settings_split_detail_switch",
                        transitionSpec = {
                            if (shouldAnimateSettingsDetailTransition(initialState, targetState)) {
                                isolatedAdvancedGlassHorizontalTransition(
                                    forward = targetState.backTargetPage() == initialState
                                ).using(SizeTransform(clip = true))
                            } else {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        }
                    ) { page ->
                        AdvancedGlassNavigationHandoff(
                            enabled = isolateAdvancedGlassTransitions && transition.isRunning
                        ) {
                            AdvancedGlassScene(
                                active = isolateAdvancedGlassTransitions || page == selectedPage
                            ) {
                                MiuixSettingsDetailScaffold(
                                    title = stringResource(page.titleRes),
                                    onBack = onBack,
                                    listState = listState,
                                    topAppBarState = topAppBarState,
                                    showBackButton = showSplitDetailBackButton
                                ) {
                                    if (detailContent == null) {
                                        content()
                                    } else {
                                        detailContent(page)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MiuixSettingsHeader(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(settingsCardSurface(), MeloxShapes.largeCard)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MeloxType.headline,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MeloxType.subheadline,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiuixSettingsSectionCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var highlightActive by remember { mutableStateOf(false) }

    LaunchedEffect(highlighted, highlightPulse) {
        if (!highlighted || highlightPulse <= 0) {
            highlightActive = false
            return@LaunchedEffect
        }
        repeat(4) { index ->
            highlightActive = index % 2 == 0
            delay(180)
        }
        highlightActive = false
        onHighlightFinished?.invoke()
    }

    val highlightColor by animateColorAsState(
        targetValue = if (highlightActive) {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
        } else {
            Color.Transparent
        },
        label = "settings_section_highlight"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(settingsCardSurface(), MeloxShapes.largeCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(highlightColor)
                .padding(MiuixSettingsContentPadding)
        ) {
            content()
        }
    }
}

internal fun LazyListScope.miuixSettingsSectionCardItem(
    key: Any,
    highlighted: Boolean = false,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    item(key = key) {
        MiuixSettingsSectionCard(
            modifier = Modifier.animateItem(),
            highlighted = highlighted,
            highlightPulse = highlightPulse,
            onHighlightFinished = onHighlightFinished,
            content = content
        )
    }
}
