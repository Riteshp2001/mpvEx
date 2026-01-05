package app.marlboroadvance.mpvex.ui.player.controls.components.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.presentation.components.ExpandableCard
import app.marlboroadvance.mpvex.ui.player.FilterPreset
import app.marlboroadvance.mpvex.ui.player.controls.CARDS_MAX_WIDTH
import app.marlboroadvance.mpvex.ui.player.controls.panelCardsColors
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject

/**
 * Video settings card for applying predefined filter presets.
 * Each preset applies a combination of brightness, contrast, saturation, gamma, and hue
 * values optimized for different viewing scenarios.
 * 
 * @param currentPreset The currently selected filter preset (for per-video persistence)
 * @param onPresetChange Callback when the preset changes (for per-video persistence)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoSettingsFilterPresetsCard(
  currentPreset: FilterPreset = FilterPreset.NONE,
  onPresetChange: (FilterPreset) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val decoderPreferences = koinInject<DecoderPreferences>()
  var isExpanded by remember { mutableStateOf(true) }
  var selectedPreset by remember(currentPreset) { mutableStateOf(currentPreset) }

  ExpandableCard(
    isExpanded = isExpanded,
    onExpand = { isExpanded = !isExpanded },
    title = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
      ) {
        Icon(Icons.Default.AutoAwesome, null)
        Text(stringResource(R.string.player_sheets_filter_presets_title))
      }
    },
    colors = panelCardsColors(),
    modifier = modifier.widthIn(max = CARDS_MAX_WIDTH),
  ) {
    Column(
      modifier = Modifier.padding(MaterialTheme.spacing.small),
    ) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
      ) {
        FilterPreset.entries.forEach { preset ->
          FilterChip(
            selected = selectedPreset == preset,
            onClick = {
              selectedPreset = preset
              applyPreset(preset, decoderPreferences)
              onPresetChange(preset)
            },
            label = {
              Text(
                text = stringResource(preset.titleRes),
                style = MaterialTheme.typography.labelMedium,
              )
            },
            leadingIcon = if (selectedPreset == preset) {
              {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
              }
            } else null,
          )
        }
      }
      
      Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
      
      // Show current preset description
      if (selectedPreset != FilterPreset.NONE) {
        Text(
          text = getPresetDescription(selectedPreset),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/**
 * Applies the selected filter preset to the video.
 * Updates both MPV properties and persisted preferences.
 */
private fun applyPreset(preset: FilterPreset, decoderPreferences: DecoderPreferences) {
  // Apply brightness
  decoderPreferences.brightnessFilter.set(preset.brightness)
  MPVLib.setPropertyInt("brightness", preset.brightness)
  
  // Apply contrast
  decoderPreferences.contrastFilter.set(preset.contrast)
  MPVLib.setPropertyInt("contrast", preset.contrast)
  
  // Apply saturation
  decoderPreferences.saturationFilter.set(preset.saturation)
  MPVLib.setPropertyInt("saturation", preset.saturation)
  
  // Apply gamma
  decoderPreferences.gammaFilter.set(preset.gamma)
  MPVLib.setPropertyInt("gamma", preset.gamma)
  
  // Apply hue
  decoderPreferences.hueFilter.set(preset.hue)
  MPVLib.setPropertyInt("hue", preset.hue)
}

/**
 * Applies a filter preset by name (for restoring from saved state).
 */
fun applyPresetByName(presetName: String, decoderPreferences: DecoderPreferences): FilterPreset {
  val preset = FilterPreset.entries.find { it.name == presetName } ?: FilterPreset.NONE
  applyPreset(preset, decoderPreferences)
  return preset
}

/**
 * Returns a brief description of what each preset is optimized for.
 */
@Composable
private fun getPresetDescription(preset: FilterPreset): String {
  return when (preset) {
    FilterPreset.NONE -> ""
    FilterPreset.VIVID -> "Enhanced colors for vibrant, punchy visuals"
    FilterPreset.WARM_TONE -> "Golden warmth for cozy, warm scenes"
    FilterPreset.COOL_TONE -> "Blue-shifted for sci-fi and mystery"
    FilterPreset.SOFT_PASTEL -> "Gentle aesthetics with soft colors"
    FilterPreset.CINEMATIC -> "Film-like with rich blacks and warm highlights"
    FilterPreset.DRAMATIC -> "High contrast for action sequences"
    FilterPreset.NIGHT_MODE -> "Comfortable viewing in dark environments"
    FilterPreset.NOSTALGIC -> "Retro look reminiscent of classic content"
    FilterPreset.GHIBLI_STYLE -> "Natural, earthy tones with soft warmth"
    FilterPreset.NEON_POP -> "Hyper-saturated for vibrant scenes"
    FilterPreset.DEEP_BLACK -> "Enhanced blacks for OLED displays"
  }
}


