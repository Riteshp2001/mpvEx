package app.marlboroadvance.mpvex.utils.ai

/**
 * Enum representing supported AI providers for filename cleanup.
 */
enum class AiProvider(val id: String, val displayName: String) {
  GEMINI("gemini", "Google Gemini"),
  OPENAI("openai", "OpenAI / Compatible"),
  CUSTOM("custom", "Custom Endpoint");

  companion object {
    fun fromId(id: String): AiProvider = entries.find { it.id == id } ?: GEMINI
  }
}
