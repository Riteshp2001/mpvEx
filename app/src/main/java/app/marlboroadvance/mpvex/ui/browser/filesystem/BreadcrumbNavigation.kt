package app.marlboroadvance.mpvex.ui.browser.filesystem

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.browser.PathComponent
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.rememberLayerBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.drawBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.effects.blur
import app.marlboroadvance.mpvex.ui.liquidglass.effects.lens
import app.marlboroadvance.mpvex.ui.liquidglass.effects.vibrancy
import app.marlboroadvance.mpvex.ui.liquidglass.highlight.Highlight
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.Shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import androidx.compose.runtime.getValue

@Composable
fun BreadcrumbNavigation(
  breadcrumbs: List<PathComponent>,
  onBreadcrumbClick: (PathComponent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()
  val scrollState = rememberScrollState()

  // Auto-scroll to end when breadcrumbs change
  LaunchedEffect(breadcrumbs) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  val surfaceColor = MaterialTheme.colorScheme.surface
  val containerModifier = if (enableLiquidGlass) {
      val glassBackdrop = rememberLayerBackdrop()
      Modifier.drawBackdrop(
          backdrop = glassBackdrop,
          shape = { RoundedCornerShape(12.dp) },
          effects = {
              vibrancy()
              blur(8.dp.toPx())
              lens(4.dp.toPx(), 4.dp.toPx())
          },
          highlight = { Highlight.Default.copy(alpha = 0.2f) },
          shadow = { Shadow(radius = 4.dp, alpha = 0.1f) },
          onDrawSurface = {
              drawRect(surfaceColor.copy(alpha = 0.15f))
          }
      )
  } else {
      Modifier
  }

  Row(
    modifier =
      modifier
        .then(containerModifier)
        .horizontalScroll(scrollState)
        .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    breadcrumbs.forEachIndexed { index, component ->
      if (index > 0) {
        Icon(
          imageVector = Icons.Filled.ChevronRight,
          contentDescription = "Separator",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      }

      TextButton(
        onClick = { onBreadcrumbClick(component) },
        modifier = Modifier.padding(horizontal = 2.dp),
      ) {
        Text(
          text = component.name,
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (index == breadcrumbs.lastIndex) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
