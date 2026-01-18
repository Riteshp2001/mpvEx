package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

class TranslationPreferences(
  preferenceStore: PreferenceStore,
) {
  val translationEnabled = preferenceStore.getBoolean("translation_enabled", false)
  val targetLanguage = preferenceStore.getString("translation_target_language", "es") // Default Spanish?
}
