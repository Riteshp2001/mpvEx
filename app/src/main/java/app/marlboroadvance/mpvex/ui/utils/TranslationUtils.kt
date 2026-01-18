package app.marlboroadvance.mpvex.ui.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.domain.repository.TranslationRepository
import app.marlboroadvance.mpvex.preferences.TranslationPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun rememberTranslatedString(@StringRes id: Int, vararg args: Any): String {
  val context = LocalContext.current
  // Resolve the original string immediately so we have a valid fallback/initial value
  val originalString = if (args.isEmpty()) {
     stringResource(id)
  } else {
     stringResource(id, *args)
  }

  return rememberTranslatedString(originalString)
}

@Composable
fun rememberTranslatedString(text: String): String {
  if (text.isBlank()) return text

  val repository = koinInject<TranslationRepository>()
  val translationPreferences = koinInject<TranslationPreferences>()
  
  // Observe preferences to trigger re-translation if settings change
  val enabled by translationPreferences.translationEnabled.collectAsState()
  val targetLang by translationPreferences.targetLanguage.collectAsState()

  // Current displayed text
  var displayedText by remember(text) { mutableStateOf(text) }

  // Effect to handle translation
  LaunchedEffect(text, enabled, targetLang) {
    if (!enabled) {
      displayedText = text
      return@LaunchedEffect
    }

    // Perform translation
    val result = withContext(Dispatchers.IO) {
      repository.translate(text)
    }
    
    result.onSuccess { translated ->
      displayedText = translated
    }
  }

  return displayedText
}
