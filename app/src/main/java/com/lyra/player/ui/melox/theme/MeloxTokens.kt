package com.lyra.player.ui.melox.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.shapes.Capsule

/** iOS system palette used by the reference design. */
object MeloxSystemColors {
    val Blue = Color(0xFF0A84FF)
    val Red = Color(0xFFFF3B30)
    val Green = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val HomeAccent = Color(0xFFFF3147)
    val SecondaryFill = Color(0x26787880)
    val TertiaryFill = Color(0x1F767680)
    val Separator = Color(0x4A3C3C43)
}

object MeloxShapes {
    val capsule: Shape = Capsule()
    val compact: Shape = ContinuousRoundedRectangle(16.dp)
    val card: Shape = ContinuousRoundedRectangle(22.dp)
    val largeCard: Shape = ContinuousRoundedRectangle(28.dp)
    val sheet: Shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    val circle: Shape = CircleShape
}

/** iOS-derived type scale. Family is inherited from the app theme. */
object MeloxType {
    val largeTitle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp)
    val title2 = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
    val headline = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp)
    val subheadline = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp)
    val caption = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
}

internal val MeloxMaterialTypography = Typography().let { base ->
    Typography(
        displayLarge = MeloxType.largeTitle,
        displayMedium = MeloxType.title2,
        displaySmall = MeloxType.headline,
        headlineLarge = MeloxType.largeTitle,
        headlineMedium = MeloxType.title2,
        headlineSmall = MeloxType.headline,
        titleLarge = MeloxType.title2,
        titleMedium = MeloxType.headline,
        titleSmall = MeloxType.subheadline,
        bodyLarge = MeloxType.body,
        bodyMedium = MeloxType.subheadline,
        bodySmall = MeloxType.caption,
        labelLarge = MeloxType.headline,
        labelMedium = MeloxType.subheadline,
        labelSmall = MeloxType.caption,
    )
}

object MeloxSpacing {
    val gutterPhone = 20.dp
    val gutterTablet = 28.dp
    val gutterDesktop = 36.dp
    val screenTop = 18.dp
    val sectionGap = 22.dp
    val contentBottomClearance = 156.dp
    val bottomBarExpanded = 57.dp
    val bottomBarCollapsed = 46.dp
    val miniPlayerHeight = 48.dp
    val settingsRow = 56.dp
    val songRow = 66.dp
    val navBar = 56.dp
}
