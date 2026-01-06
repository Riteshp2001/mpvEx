package app.marlboroadvance.mpvex.utils.media

import java.io.File
import java.util.Locale

object SubtitleUtils {
  // Common subtitle extensions
  val SUBTITLE_EXTENSIONS = listOf("srt", "ass", "ssa", "vtt", "sub")

  fun isSubtitleFile(fileName: String): Boolean {
    val lower = fileName.lowercase(Locale.getDefault())
    return SUBTITLE_EXTENSIONS.any { lower.endsWith(".$it") }
  }

  /**
   * Check if subtitle name matches video using robust strategies
   */
  fun matchesVideo(videoName: String, subtitleName: String): Boolean {
    val baseName = videoName.substringBeforeLast('.')
    val baseNameLower = baseName.lowercase(Locale.getDefault())
    val subBaseLower = subtitleName.substringBeforeLast('.').lowercase(Locale.getDefault())
    
    // Exact match
    if (subBaseLower == baseNameLower) return true
    
    // Subtitle starts with video name (handles Movie.en.srt, Movie.english.srt)
    if (subBaseLower.startsWith(baseNameLower)) return true
    
    // Handle downloaded movie patterns: split by common separators
    val videoWords = baseNameLower.split(Regex("[.\\s_-]+"))
    val subWords = subBaseLower.split(Regex("[.\\s_-]+"))
    
    // For longer names, check if first N words match (handles "Movie Name 2024" variations)
    // At least 2 words must match, and we check up to the first 3 words
    val matchWords = minOf(3, videoWords.size, subWords.size)
    if (matchWords >= 2 && videoWords.take(matchWords) == subWords.take(matchWords)) {
      return true
    }
    
    return false
  }

  /**
   * Find best matching external subtitle for a video file
   * Returns the extension (SRT, ASS, etc) or null
   */
  fun findExternalSubtitleFormat(videoFile: File): String? {
    val parentDir = videoFile.parentFile ?: return null
    
    // Check common subdirectories first (Subs, Subtitles, etc)
    val subDirs = listOf("Subs", "subs", "Subtitles", "subtitles", "Sub", "sub")
    val subDirMatch = subDirs.firstNotNullOfOrNull { dirName ->
       val subDir = File(parentDir, dirName)
       if (subDir.exists() && subDir.isDirectory) {
         subDir.listFiles()?.firstOrNull {
           it.isFile && isSubtitleFile(it.name)
         }
       } else {
         null
       }
    }
    
    if (subDirMatch != null) return subDirMatch.extension.uppercase(Locale.getDefault())

    // Check current directory
    val localMatch = parentDir.listFiles()?.firstOrNull { 
      it.isFile && isSubtitleFile(it.name) 
    }
    
    return localMatch?.extension?.uppercase(Locale.getDefault())
  }
}
