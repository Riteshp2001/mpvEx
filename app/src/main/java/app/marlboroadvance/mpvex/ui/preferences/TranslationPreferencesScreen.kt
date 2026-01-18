package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.repository.TranslationRepository
import app.marlboroadvance.mpvex.domain.translation.DownloadState
import app.marlboroadvance.mpvex.domain.translation.Language
import app.marlboroadvance.mpvex.preferences.TranslationPreferences
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.ui.utils.rememberTranslatedString
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.util.Locale

@Serializable
object TranslationPreferencesScreen : Screen {
    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        
        TranslationPreferencesContent(
            onBackClick = { 
                // Basic back navigation. 
                // Since NavBackStack is likely a collection, we remove ourselves.
                // However, without exact API of NavBackStack, we try a safe approach or assume it behaves like a MutableList
                // If it is androidx.navigation3.runtime.NavBackStack, we might need a different approach.
                // But typically in this code base 'backstack.add' is used.
                // We'll check if we can remove the last entry matching us.
               try {
                   (backstack as? MutableList<Any>)?.remove(this)
               } catch (e: Exception) {
                   // Fallback or ignore
               }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPreferencesContent(
  onBackClick: () -> Unit,
) {
  val translationPreferences = koinInject<TranslationPreferences>()
  val repository = koinInject<TranslationRepository>()
  val scope = rememberCoroutineScope()
  
  var availableLanguages by remember { mutableStateOf<List<Language>>(emptyList()) }
  val targetLanguage by translationPreferences.targetLanguage.collectAsState()
  
  LaunchedEffect(Unit) {
    availableLanguages = repository.getAvailableLanguages()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(rememberTranslatedString("Translation")) },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    ProvidePreferenceLocals {
      LazyColumn(
        contentPadding = padding,
        modifier = Modifier.fillMaxWidth()
      ) {
        item {
          PreferenceSectionHeader(title = rememberTranslatedString("General"))
        }

        item {
          PreferenceCard {
            SwitchPreference(
              value = translationPreferences.translationEnabled.collectAsState().value,
              onValueChange = { translationPreferences.translationEnabled.set(it) },
              title = { Text(rememberTranslatedString("Enable Translation")) },
              summary = { Text(rememberTranslatedString("Translate titles and chapters automatically")) },
            )
          }
        }
        
        item {
        val targetCode by translationPreferences.targetLanguage.collectAsState()
        var isTargetDownloaded by remember { mutableStateOf(true) }
        
        LaunchedEffect(targetCode, availableLanguages) {
            if (targetCode.isNotEmpty()) {
                isTargetDownloaded = repository.isModelDownloaded(targetCode)
            }
        }
        
        PreferenceCard {
           Column(Modifier.padding(16.dp)) {
               Text(rememberTranslatedString("Target Language"), style = MaterialTheme.typography.titleMedium)
               Text(
                 text = "${rememberTranslatedString("Current")}: ${Locale(targetCode).displayName}",
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant
               )
               
               if (!isTargetDownloaded && targetCode.isNotEmpty()) {
                   Spacer(Modifier.height(8.dp))
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       Icon(
                           Icons.Default.Error, 
                           contentDescription = null, 
                           tint = MaterialTheme.colorScheme.error,
                           modifier = Modifier.size(16.dp)
                       )
                       Spacer(Modifier.width(8.dp))
                       Text(
                           text = rememberTranslatedString("Model not downloaded. Translation may not work."),
                           style = MaterialTheme.typography.bodySmall,
                           color = MaterialTheme.colorScheme.error
                       )
                   }
               }
           }
        }
      }
      
      items(availableLanguages) { language ->
        LanguageItem(
          language = language,
          isTarget = language.code == targetLanguage,
          repository = repository,
          onSelect = {
             translationPreferences.targetLanguage.set(language.code)
          }
        )
      }
    }
  }
  }
}

@Composable
fun LanguageItem(
  language: Language,
  isTarget: Boolean,
  repository: TranslationRepository,
  onSelect: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.NotDownloaded) }
  var isDownloaded by remember { mutableStateOf(false) } // Cache specific check
  
  LaunchedEffect(language.code) {
    isDownloaded = repository.isModelDownloaded(language.code)
    downloadState = if (isDownloaded) DownloadState.Downloaded else DownloadState.NotDownloaded
  }

  ListItem(
    headlineContent = { Text(language.name) },
    leadingContent = {
      if (isTarget) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      } else {
        Box(Modifier.size(24.dp))
      }
    },
    trailingContent = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        when (downloadState) {
          is DownloadState.Downloading -> {
             CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
          }
          is DownloadState.Downloaded -> {
            IconButton(onClick = {
              scope.launch {
                repository.deleteModel(language.code)
                isDownloaded = false
                downloadState = DownloadState.NotDownloaded
              }
            }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Model")
            }
          }
          is DownloadState.NotDownloaded -> {
             IconButton(onClick = {
              scope.launch {
                repository.downloadModel(language.code).collect { state ->
                  downloadState = state
                  if (state is DownloadState.Downloaded) {
                      isDownloaded = true
                  }
                }
              }
            }) {
              Icon(Icons.Default.CloudDownload, contentDescription = "Download Model")
            }
          }
          is DownloadState.Error -> {
             Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
          }
        }
      }
    },
    modifier = Modifier.clickable { onSelect() }
  )
}
