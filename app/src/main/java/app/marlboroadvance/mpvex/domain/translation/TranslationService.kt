package app.marlboroadvance.mpvex.domain.translation

import kotlinx.coroutines.flow.Flow

interface TranslationService {
  suspend fun translate(text: String, source: String? = null, target: String): Result<String>
  
  suspend fun getAvailableLanguages(): List<Language>
  
  suspend fun downloadModel(languageCode: String): Flow<DownloadState>
  
  suspend fun deleteModel(languageCode: String): Result<Unit>
  
  suspend fun isModelDownloaded(languageCode: String): Boolean
}

data class Language(
  val code: String,
  val name: String,
)

sealed class DownloadState {
  data object NotDownloaded : DownloadState()
  data object Downloading : DownloadState()
  data object Downloaded : DownloadState()
  data class Error(val message: String) : DownloadState()
}
