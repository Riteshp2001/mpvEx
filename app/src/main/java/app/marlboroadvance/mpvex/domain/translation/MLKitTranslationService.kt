package app.marlboroadvance.mpvex.domain.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.Locale

class MLKitTranslationService : TranslationService {
  
  private val modelManager = RemoteModelManager.getInstance()

  override suspend fun translate(text: String, source: String?, target: String): Result<String> {
    return try {
      val sourceLang = source ?: TranslateLanguage.ENGLISH // Fallback/Auto-detect logic could be improved
      val options = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLang)
        .setTargetLanguage(target)
        .build()
        
      val translator = Translation.getClient(options)
      
      // Ensure model is ready (though we expect it to be downloaded)
      // We can add logic here to download if missing, but requirements say optional download
      
      val result = translator.translate(text).await()
      translator.close()
      Result.success(result)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun getAvailableLanguages(): List<Language> {
    return TranslateLanguage.getAllLanguages().map { code ->
      Language(
        code = code,
        name = Locale(code).displayName
      )
    }.sortedBy { it.name }
  }

  override suspend fun downloadModel(languageCode: String): Flow<DownloadState> = callbackFlow {
    trySend(DownloadState.Downloading)
    
    val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(languageCode).build()
    val conditions = DownloadConditions.Builder()
      // .requireWifi() // Can be configurable
      .build()
      
    modelManager.download(model, conditions)
      .addOnSuccessListener {
        trySend(DownloadState.Downloaded)
        close()
      }
      .addOnFailureListener {
        trySend(DownloadState.Error(it.message ?: "Unknown error"))
        close()
      }
      
    awaitClose { /* Cleanup if needed */ }
  }

  override suspend fun deleteModel(languageCode: String): Result<Unit> {
    return try {
      val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(languageCode).build()
      modelManager.deleteDownloadedModel(model).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun isModelDownloaded(languageCode: String): Boolean {
    val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(languageCode).build()
    return modelManager.isModelDownloaded(model).await()
  }
}
