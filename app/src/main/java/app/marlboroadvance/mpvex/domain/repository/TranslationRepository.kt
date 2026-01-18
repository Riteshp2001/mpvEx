package app.marlboroadvance.mpvex.domain.repository

import app.marlboroadvance.mpvex.domain.translation.DownloadState
import app.marlboroadvance.mpvex.domain.translation.Language
import app.marlboroadvance.mpvex.domain.translation.TranslationService
import app.marlboroadvance.mpvex.preferences.TranslationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TranslationRepository(
  private val translationService: TranslationService,
  private val translationPreferences: TranslationPreferences,
) {

  private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

  suspend fun translate(text: String): Result<String> {
    if (!translationPreferences.translationEnabled.get()) return Result.success(text)
    
    val target = translationPreferences.targetLanguage.get()

    
    // Check cache first (Composite key of target-text)
    // We should clear cache on language change or include language in key.
    val cacheKey = "auto-$target-$text"
    memoryCache[cacheKey]?.let { return Result.success(it) }
    
    val result = translationService.translate(text, null, target)
    result.onSuccess { memoryCache[cacheKey] = it }
    
    return result
  }
  
  fun clearCache() {
    memoryCache.clear()
  }

  suspend fun getAvailableLanguages(): List<Language> = translationService.getAvailableLanguages()

  suspend fun downloadModel(languageCode: String): Flow<DownloadState> = 
    translationService.downloadModel(languageCode)

  suspend fun deleteModel(languageCode: String): Result<Unit> = 
    translationService.deleteModel(languageCode)

  suspend fun isModelDownloaded(languageCode: String): Boolean = 
    translationService.isModelDownloaded(languageCode)
    
  fun isTranslationEnabled(): Boolean = translationPreferences.translationEnabled.get()
  
  fun getTargetLanguage(): String = translationPreferences.targetLanguage.get()
}
