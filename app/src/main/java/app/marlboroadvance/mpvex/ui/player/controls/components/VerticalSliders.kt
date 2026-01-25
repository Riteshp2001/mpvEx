package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.theme.spacing
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.rememberLayerBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.drawBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.effects.blur
import app.marlboroadvance.mpvex.ui.liquidglass.effects.lens
import app.marlboroadvance.mpvex.ui.liquidglass.effects.vibrancy
import app.marlboroadvance.mpvex.ui.liquidglass.highlight.Highlight
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.Shadow
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.InnerShadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawBehind
import kotlin.math.roundToInt

fun percentage(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(
  value: Int,
  range: ClosedRange<Int>,
): Float = ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

@Composable
fun VerticalSlider(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  overflowValue: Float? = null,
  overflowRange: ClosedFloatingPointRange<Float>? = null,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()
  
  val coercedValue = value.coerceIn(range)
  val targetFraction = percentage(coercedValue, range)
  val targetHeight by animateFloatAsState(targetFraction, label = "vsliderheight")
  
  val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
  val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
  val thumbColor = MaterialTheme.colorScheme.primary

  if (enableLiquidGlass) {
      val backdrop = rememberLayerBackdrop()
      Box(
          modifier = modifier
              .height(120.dp)
              .aspectRatio(0.2f)
              .drawBackdrop(
                  backdrop = backdrop,
                  shape = { RoundedCornerShape(16.dp) },
                  effects = {
                      vibrancy()
                      blur(8.dp.toPx())
                      lens(4.dp.toPx(), 4.dp.toPx())
                  },
                  highlight = { Highlight.Default.copy(alpha = 0.2f) },
                  shadow = { Shadow(radius = 4.dp, alpha = 0.1f) },
                  onDrawSurface = {
                      // Subtle white tip for glass feel
                      drawRect(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f))
                  }
              )
              .clip(RoundedCornerShape(16.dp))
              .drawBehind {
                  // Track Background
                  drawRect(trackColor)
                  // Progress Fill
                  drawRect(
                      color = thumbColor,
                      topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * (1f - targetHeight)),
                      size = androidx.compose.ui.geometry.Size(size.width, size.height * targetHeight)
                  )
              },
          contentAlignment = Alignment.BottomCenter
      ) {
          if (overflowRange != null && overflowValue != null) {
              val overflowHeight by animateFloatAsState(
                  percentage(overflowValue, overflowRange),
                  label = "vslideroverflowheight",
              )
              Box(
                  Modifier
                      .fillMaxWidth()
                      .fillMaxHeight(overflowHeight)
                      .background(MaterialTheme.colorScheme.errorContainer),
              )
          }
      }
  } else {
      Box(
        modifier =
          modifier
            .height(120.dp)
            .aspectRatio(0.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
              shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.BottomCenter,
      ) {
        Box(
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(targetHeight)
            .background(MaterialTheme.colorScheme.tertiary),
        )
        if (overflowRange != null && overflowValue != null) {
          val overflowHeight by animateFloatAsState(
            percentage(overflowValue, overflowRange),
            label = "vslideroverflowheight",
          )
          Box(
            Modifier
              .fillMaxWidth()
              .fillMaxHeight(overflowHeight)
              .background(MaterialTheme.colorScheme.errorContainer),
          )
        }
      }
  }
}

@Composable
fun VerticalSlider(
  value: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier,
  overflowValue: Int? = null,
  overflowRange: ClosedRange<Int>? = null,
) {
  VerticalSlider(
      value = value.toFloat(),
      range = range.start.toFloat()..range.endInclusive.toFloat(),
      modifier = modifier,
      overflowValue = overflowValue?.toFloat(),
      overflowRange = overflowRange?.let { it.start.toFloat()..it.endInclusive.toFloat() }
  )
}

@Composable
fun BrightnessSlider(
  brightness: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()
  val coercedBrightness = brightness.coerceIn(range)
  
  val containerModifier = if (enableLiquidGlass) {
      val backdrop = rememberLayerBackdrop()
      Modifier.drawBackdrop(
          backdrop = backdrop,
          shape = { RoundedCornerShape(20.dp) },
          effects = {
              vibrancy()
              blur(12.dp.toPx())
          },
          highlight = { Highlight.Default.copy(alpha = 0.3f) },
          shadow = { Shadow(radius = 8.dp, alpha = 0.15f) }
      )
  } else {
      Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
  }

  Surface(
    modifier = modifier.then(containerModifier),
    shape = RoundedCornerShape(20.dp),
    color = if (enableLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Text(
        (coercedBrightness * 100).toInt().toString(),
        style = MaterialTheme.typography.bodySmall,
      )
      VerticalSlider(
        coercedBrightness,
        range,
      )
      Icon(
        when (percentage(coercedBrightness, range)) {
          in 0f..0.3f -> Icons.Default.BrightnessLow
          in 0.3f..0.6f -> Icons.Default.BrightnessMedium
          in 0.6f..1f -> Icons.Default.BrightnessHigh
          else -> Icons.Default.BrightnessMedium
        },
        contentDescription = null,
      )
    }
  }
}

@Composable
fun VolumeSlider(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
  displayAsPercentage: Boolean = false,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()
  val percentage = (percentage(volume, range) * 100).roundToInt()
  
  val containerModifier = if (enableLiquidGlass) {
      val backdrop = rememberLayerBackdrop()
      Modifier.drawBackdrop(
          backdrop = backdrop,
          shape = { RoundedCornerShape(20.dp) },
          effects = {
              vibrancy()
              blur(12.dp.toPx())
          },
          highlight = { Highlight.Default.copy(alpha = 0.3f) },
          shadow = { Shadow(radius = 8.dp, alpha = 0.15f) }
      )
  } else {
      Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
  }

  Surface(
    modifier = modifier.then(containerModifier),
    shape = RoundedCornerShape(20.dp),
    color = if (enableLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      val boostVolume = mpvVolume - 100
      Text(
        getVolumeSliderText(volume, mpvVolume, boostVolume, percentage, displayAsPercentage),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
      )
      VerticalSlider(
        if (displayAsPercentage) percentage else volume,
        if (displayAsPercentage) 0..100 else range,
        overflowValue = boostVolume,
        overflowRange = boostRange,
      )
      Icon(
        when (percentage) {
          0 -> Icons.AutoMirrored.Default.VolumeOff
          in 0..30 -> Icons.AutoMirrored.Default.VolumeMute
          in 30..60 -> Icons.AutoMirrored.Default.VolumeDown
          in 60..100 -> Icons.AutoMirrored.Default.VolumeUp
          else -> Icons.AutoMirrored.Default.VolumeOff
        },
        contentDescription = null,
      )
    }
  }
}

val getVolumeSliderText: @Composable (Int, Int, Int, Int, Boolean) -> String =
  { volume, mpvVolume, boostVolume, percentage, displayAsPercentage ->
    when {
      mpvVolume == 100 ->
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }

      mpvVolume > 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      mpvVolume < 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      else -> {
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }
      }
    }
  }
