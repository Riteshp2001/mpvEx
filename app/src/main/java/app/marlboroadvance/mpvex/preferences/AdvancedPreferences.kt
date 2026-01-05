package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

class AdvancedPreferences(
  preferenceStore: PreferenceStore,
) {
  val mpvConfStorageUri = preferenceStore.getString("mpv_conf_storage_location_uri")
  val mpvConf = preferenceStore.getString("mpv.conf")
  val inputConf = preferenceStore.getString("input.conf")

  val verboseLogging = preferenceStore.getBoolean("verbose_logging", BuildConfig.BUILD_TYPE != "release")

  val enabledStatisticsPage = preferenceStore.getInt("enabled_stats_page", 0)

  val enableRecentlyPlayed = preferenceStore.getBoolean("enable_recently_played", true)

  val enableLuaScripts = preferenceStore.getBoolean("enable_lua_scripts", false)
  val selectedLuaScripts = preferenceStore.getStringSet("selected_lua_scripts", emptySet())

  // AI Integration settings
  val aiEnabled = preferenceStore.getBoolean("ai_enabled", false)
  val aiProvider = preferenceStore.getString("ai_provider", "gemini") // "gemini", "openai", "custom"
  val geminiApiKey = preferenceStore.getString("gemini_api_key")
  val geminiModel = preferenceStore.getString("gemini_model", "gemini-2.0-flash")
  val openAiApiKey = preferenceStore.getString("openai_api_key")
  val openAiModel = preferenceStore.getString("openai_model", "gpt-4o-mini")
  val customAiEndpoint = preferenceStore.getString("custom_ai_endpoint")
  val customAiApiKey = preferenceStore.getString("custom_ai_api_key")
  val customAiModel = preferenceStore.getString("custom_ai_model")
  val autoCleanFilename = preferenceStore.getBoolean("auto_clean_filename", false)

}
