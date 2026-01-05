package app.marlboroadvance.mpvex.ui.player

import androidx.annotation.StringRes
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.Preference

enum class PlayerOrientation(
  @StringRes val titleRes: Int,
) {
  Free(R.string.pref_player_orientation_free),
  Video(R.string.pref_player_orientation_video),
  Portrait(R.string.pref_player_orientation_portrait),
  ReversePortrait(R.string.pref_player_orientation_reverse_portrait),
  SensorPortrait(R.string.pref_player_orientation_sensor_portrait),
  Landscape(R.string.pref_player_orientation_landscape),
  ReverseLandscape(R.string.pref_player_orientation_reverse_landscape),
  SensorLandscape(R.string.pref_player_orientation_sensor_landscape),
}

enum class VideoAspect(
  @StringRes val titleRes: Int,
) {
  Crop(R.string.player_aspect_crop),
  Fit(R.string.player_aspect_fit),
  Stretch(R.string.player_aspect_stretch),
}

enum class SingleActionGesture(
  @StringRes val titleRes: Int,
) {
  None(R.string.pref_gesture_double_tap_none),
  Seek(R.string.pref_gesture_double_tap_seek),
  PlayPause(R.string.pref_gesture_double_tap_play),
  Custom(R.string.pref_gesture_double_tap_custom),
}

enum class CustomKeyCodes(
  val keyCode: String,
) {
  DoubleTapLeft("MBTN_LEFT_DBL"),
  DoubleTapCenter("MBTN_MID_DBL"),
  DoubleTapRight("MBTN_RIGHT_DBL"),
  MediaPrevious("PREV"),
  MediaPlay("PLAYPAUSE"),
  MediaNext("NEXT"),
}

enum class Decoder(
  val title: String,
  val value: String,
) {
  AutoCopy("Auto", "auto-copy"),
  Auto("Auto", "auto"),
  SW("SW", "no"),
  HW("HW", "mediacodec-copy"),
  HWPlus("HW+", "mediacodec"),
  ;

  companion object {
    fun getDecoderFromValue(value: String): Decoder = Decoder.entries.first { it.value == value }
  }
}

enum class Debanding(
  @StringRes val titleRes: Int,
) {
  None(R.string.player_sheets_deband_none),
  CPU(R.string.player_sheets_deband_cpu),
  GPU(R.string.player_sheets_deband_gpu),
}

enum class Sheets {
  None,
  PlaybackSpeed,
  SubtitleTracks,
  SubtitleSearch,
  AudioTracks,
  Chapters,
  Decoders,
  More,
  VideoZoom,
  AspectRatios,
  FrameNavigation,
  MoreVideos,
}

enum class Panels {
  None,
  SubtitleSettings,
  SubtitleDelay,
  AudioDelay,
  VideoFilters,
  Playlist, // Added for Left Side Drawer
}

sealed class PlayerUpdates {
  data object None : PlayerUpdates()

  data object MultipleSpeed : PlayerUpdates()

  data class DynamicSpeedControl(
    val speed: Float,
    val showFullOverlay: Boolean = true,
  ) : PlayerUpdates()

  data object AspectRatio : PlayerUpdates()

  data object VideoZoom : PlayerUpdates()

  data class ShowText(
    val value: String,
  ) : PlayerUpdates()

  data class RepeatMode(
    val mode: app.marlboroadvance.mpvex.ui.player.RepeatMode,
  ) : PlayerUpdates()

  data class Shuffle(
    val enabled: Boolean,
  ) : PlayerUpdates()

  data class FrameInfo(
    val currentFrame: Int,
    val totalFrames: Int,
  ) : PlayerUpdates()
}

enum class VideoFilters(
  @StringRes val titleRes: Int,
  val preference: (DecoderPreferences) -> Preference<Int>,
  val mpvProperty: String,
) {
  BRIGHTNESS(
    R.string.player_sheets_filters_brightness,
    { it.brightnessFilter },
    "brightness",
  ),
  SATURATION(
    R.string.player_sheets_filters_Saturation,
    { it.saturationFilter },
    "saturation",
  ),
  CONTRAST(
    R.string.player_sheets_filters_contrast,
    { it.contrastFilter },
    "contrast",
  ),
  GAMMA(
    R.string.player_sheets_filters_gamma,
    { it.gammaFilter },
    "gamma",
  ),
  HUE(
    R.string.player_sheets_filters_hue,
    { it.hueFilter },
    "hue",
  ),
}

/**
 * Predefined video filter presets with optimized color grading values.
 * Each preset enhances video content in different ways:
 * - Values are in range -100 to 100 for MPV compatibility
 * - Carefully tuned for various viewing scenarios and art styles
 */
enum class FilterPreset(
  @StringRes val titleRes: Int,
  val brightness: Int,
  val contrast: Int,
  val saturation: Int,
  val gamma: Int,
  val hue: Int,
) {
  /** No filter applied - original colors */
  NONE(R.string.filter_preset_none, 0, 0, 0, 0, 0),
  
  /** Enhanced vivid colors - boosts saturation and contrast for punchy visuals */
  VIVID(R.string.filter_preset_vivid, 2, 12, 18, 0, 0),
  
  /** Warm tone - adds golden warmth, great for slice-of-life and romance */
  WARM_TONE(R.string.filter_preset_warm_tone, 3, 5, 8, 5, 8),
  
  /** Cool tone - blue-shifted for sci-fi, mecha, and mystery */
  COOL_TONE(R.string.filter_preset_cool_tone, 0, 8, 5, 0, -12),
  
  /** Soft pastel - reduced saturation with lifted shadows for gentle aesthetics */
  SOFT_PASTEL(R.string.filter_preset_soft_pastel, 8, -8, -15, 8, 0),
  
  /** Cinematic - film-like with crushed blacks and warm highlights */
  CINEMATIC(R.string.filter_preset_cinematic, -3, 18, 5, -5, 3),
  
  /** Dramatic - high contrast for action and battle sequences */
  DRAMATIC(R.string.filter_preset_dramatic, -5, 25, 15, -8, 0),
  
  /** Night mode - reduced brightness and blue light for comfortable dark viewing */
  NIGHT_MODE(R.string.filter_preset_night_mode, -12, -5, -10, 10, 5),
  
  /** Nostalgic - faded retro look reminiscent of classic content */
  NOSTALGIC(R.string.filter_preset_nostalgic, 0, -5, -8, 12, 5),
  
  /** Ghibli style - natural, earthy tones with soft warmth */
  GHIBLI_STYLE(R.string.filter_preset_ghibli_style, 5, 3, 10, 5, 6),
  
  /** Neon pop - hyper-saturated for vibrant scenes */
  NEON_POP(R.string.filter_preset_neon_pop, 5, 15, 30, -3, 0),
  
  /** Deep black - enhanced blacks with high contrast for OLED displays */
  DEEP_BLACK(R.string.filter_preset_deep_black, -8, 20, 8, -10, 0),
}

enum class DebandSettings(
  @StringRes val titleRes: Int,
  val preference: (DecoderPreferences) -> Preference<Int>,
  val mpvProperty: String,
  val start: Int,
  val end: Int,
) {
  Iterations(
    R.string.player_sheets_deband_iterations,
    { it.debandIterations },
    "deband-iterations",
    0,
    16,
  ),
  Threshold(
    R.string.player_sheets_deband_threshold,
    { it.debandThreshold },
    "deband-threshold",
    0,
    200,
  ),
  Range(
    R.string.player_sheets_deband_range,
    { it.debandRange },
    "deband-range",
    1,
    64,
  ),
  Grain(
    R.string.player_sheets_deband_grain,
    { it.debandGrain },
    "deband-grain",
    0,
    200,
  ),
}
