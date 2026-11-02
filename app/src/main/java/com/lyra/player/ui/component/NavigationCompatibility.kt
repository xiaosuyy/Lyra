package com.lyra.player.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import com.lyra.player.navigation.Destinations
import com.lyra.player.ui.component.navigation.DEFAULT_BOTTOM_BAR_SELECTION_ALPHA

@Composable
fun LyraBottomBar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier,
    selectAlpha: Float = DEFAULT_BOTTOM_BAR_SELECTION_ALPHA
) {
    com.lyra.player.ui.component.navigation.LyraBottomBar(
        items = items,
        currentDestination = currentDestination,
        onItemSelected = onItemSelected,
        modifier = modifier,
        selectAlpha = selectAlpha
    )
}
