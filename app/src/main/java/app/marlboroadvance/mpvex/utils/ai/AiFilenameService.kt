package app.marlboroadvance.mpvex.utils.ai

import android.util.Log
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service for cleaning up movie / TV filenames using AI.
 * Supports Gemini, OpenAI, and OpenAI-compatible custom endpoints.
 */
class AiFilenameService(
  private val preferences: AdvancedPreferences,
) {

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  companion object {
    private const val TAG = "AiFilenameService"

    private const val GEMINI_API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/"
    private const val OPENAI_API_URL =
      "https://api.openai.com/v1/chat/completions"

    /**
     * CRITICAL PROMPT:
     * - Preserves exact words (plural/singular safe)
     * - Stops correctly at year / quality markers
     * - No rephrasing, no grammar fixing
     */
     private const val CLEANUP_PROMPT = """
You are an advanced media title cleaner.
Extract ONLY the specific Movie or TV Show title from the filename.

=== EXAMPLES (LEARN FROM THESE) ===
Input: "120.Bahadur.2025.1080p.WEB-DL.x264.mkv"
Output: "Bahadur"

Input: "The.Tigers.Apprentice.2024.1080p.AMZN.WEB-DL.DDP5.1.Atmos.H.264.mkv"
Output: "The Tigers Apprentice"

Input: "[Group] Series.Name.S01E05.Title.Of.Episode.1080p.mkv"
Output: "Series Name"

Input: "Gandalf Zloty - Demotywatory.pl.mp4"
Output: "Gandalf Zloty"

Input: "Movie.Name.2023.HDR.2160p.WEB-DL-Group.mkv"
Output: "Movie Name"

=== RULES ===
1. REMOVE starting numbers if they look like an index (e.g., "120.").
2. STOP immediately at the YEAR (19xx, 20xx). Remove the year and everything after it.
3. STOP immediately at TV markers (S01, E01, Season, Episode).
4. REMOVE content inside brackets [] or parentheses ().
5. REMOVE domain names (e.g. site.com, site.pl).
6. REPLACE dots/underscores with spaces.

Output ONLY the clean title string.

=== FILENAME TO CLEAN ===
"""
  }

  /**
   * Whether AI cleanup is enabled and configured.
   */
  fun isAvailable(): Boolean {
    if (!preferences.aiEnabled.get()) return false

    return when (AiProvider.fromId(preferences.aiProvider.get())) {
      AiProvider.GEMINI ->
        preferences.geminiApiKey.get().isNotBlank()

      AiProvider.OPENAI ->
        preferences.openAiApiKey.get().isNotBlank()

      AiProvider.CUSTOM ->
        preferences.customAiEndpoint.get().isNotBlank() &&
          preferences.customAiApiKey.get().isNotBlank()
    }
  }

  /**
   * Clean a messy filename using AI.
   * Falls back safely if AI response is empty.
   */
  suspend fun cleanFilename(rawFilename: String): Result<String> =
    withContext(Dispatchers.IO) {

      if (!isAvailable()) {
        return@withContext Result.failure(
          Exception("AI not configured")
        )
      }

      try {
        val provider =
          AiProvider.fromId(preferences.aiProvider.get())

        val aiResult = when (provider) {
          AiProvider.GEMINI ->
            callGemini(rawFilename)

          AiProvider.OPENAI ->
            callOpenAi(
              rawFilename,
              OPENAI_API_URL,
              preferences.openAiApiKey.get(),
              preferences.openAiModel.get()
            )

          AiProvider.CUSTOM ->
            callOpenAi(
              rawFilename,
              preferences.customAiEndpoint.get(),
              preferences.customAiApiKey.get(),
              preferences.customAiModel.get()
            )
        }

        val cleaned = aiResult
          .trim()
          .trim('"', '\'', '*')
          .replace(Regex("\\s{2,}"), " ")

        if (cleaned.isBlank()) {
          Result.failure(Exception("Empty AI response"))
        } else {
          Result.success(cleaned)
        }

      } catch (e: Exception) {
        Log.e(TAG, "Filename cleanup failed", e)
        Result.failure(e)
      }
    }

  // ---------------- GEMINI ----------------

  private fun callGemini(rawFilename: String): String {
    val apiKey = preferences.geminiApiKey.get()
    val model =
      preferences.geminiModel.get().ifBlank { "gemini-2.5-flash" }

    val url =
      "${GEMINI_API_URL}${model}:generateContent?key=$apiKey"

    val body = JSONObject().apply {
      put(
        "contents",
        JSONArray().apply {
          put(
            JSONObject().apply {
              put(
                "parts",
                JSONArray().apply {
                  put(
                    JSONObject().apply {
                      put("text", CLEANUP_PROMPT + rawFilename)
                    }
                  )
                }
              )
            }
          )
        }
      )
      put(
        "generationConfig",
        JSONObject().apply {
          put("temperature", 0.0)
          put("maxOutputTokens", 60)
        }
      )
    }

    val request = Request.Builder()
      .url(url)
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    client.newCall(request).execute().use { response ->
      val bodyStr = response.body?.string()
        ?: throw Exception("Empty Gemini response")

      if (!response.isSuccessful) {
        throw Exception("Gemini error ${response.code}: $bodyStr")
      }

      val json = JSONObject(bodyStr)
      return json
        .getJSONArray("candidates")
        .getJSONObject(0)
        .getJSONObject("content")
        .getJSONArray("parts")
        .getJSONObject(0)
        .getString("text")
    }
  }

  // ---------------- OPENAI / CUSTOM ----------------

  private fun callOpenAi(
    rawFilename: String,
    endpoint: String,
    apiKey: String,
    model: String,
  ): String {

    val body = JSONObject().apply {
      put("model", model.ifBlank { "gpt-4o-mini" })
      put(
        "messages",
        JSONArray().apply {
          put(
            JSONObject().apply {
              put("role", "system")
              put(
                "content",
                "Extract the exact media title from filenames. Never change word forms."
              )
            }
          )
          put(
            JSONObject().apply {
              put("role", "user")
              put("content", CLEANUP_PROMPT + rawFilename)
            }
          )
        }
      )
      put("temperature", 0.0)
      put("max_tokens", 60)
    }

    val request = Request.Builder()
      .url(endpoint)
      .post(body.toString().toRequestBody(jsonMediaType))
      .addHeader("Authorization", "Bearer $apiKey")
      .build()

    client.newCall(request).execute().use { response ->
      val bodyStr = response.body?.string()
        ?: throw Exception("Empty OpenAI response")

      if (!response.isSuccessful) {
        throw Exception("OpenAI error ${response.code}: $bodyStr")
      }

      val json = JSONObject(bodyStr)
      return json
        .getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .getString("content")
    }
  }
}
