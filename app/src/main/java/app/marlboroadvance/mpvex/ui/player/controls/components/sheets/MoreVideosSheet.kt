package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class for video items in the More Videos sheet
 */
data class VideoItem(
  val uri: Uri,
  val title: String,
  val index: Int,
)

/**
 * Sheet showing other videos in the same folder/playlist with thumbnails.
 * Inspired by YouTube's "More videos" feature and video thumbnail strips.
 */
@Composable
fun MoreVideosSheet(
  videos: List<VideoItem>,
  currentIndex: Int,
  onSelectVideo: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  
  // Scroll to current video when sheet opens
  LaunchedEffect(currentIndex) {
    if (videos.isNotEmpty() && currentIndex >= 0 && currentIndex < videos.size) {
      listState.animateScrollToItem(
        index = currentIndex,
        scrollOffset = -100 // Offset to center it a bit
      )
    }
  }

  PlayerSheet(onDismissRequest) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
    ) {
      // Header
      Text(
        text = "More Videos (${videos.size})",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .padding(bottom = 8.dp)
      )
      
      if (videos.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No other videos in this folder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        // Horizontal scrolling video thumbnails
        LazyRow(
          state = listState,
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          itemsIndexed(videos) { index, video ->
            VideoThumbnailCard(
              video = video,
              isCurrentVideo = index == currentIndex,
              onClick = { 
                if (index != currentIndex) {
                  onSelectVideo(index)
                  onDismissRequest()
                }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VideoThumbnailCard(
  video: VideoItem,
  isCurrentVideo: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var thumbnail by remember(video.uri) { mutableStateOf<Bitmap?>(null) }
  
  // Load thumbnail asynchronously
  LaunchedEffect(video.uri) {
    thumbnail = withContext(Dispatchers.IO) {
      try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, video.uri)
        val frame = retriever.getFrameAtTime(1_000_000) // 1 second in microseconds
        retriever.release()
        frame
      } catch (e: Exception) {
        null
      }
    }
  }
  
  Column(
    modifier = modifier
      .width(160.dp)
      .clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Thumbnail with overlay
    Box(
      modifier = Modifier
        .width(160.dp)
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .then(
          if (isCurrentVideo) {
            Modifier.border(
              width = 3.dp,
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(12.dp)
            )
          } else Modifier
        ),
      contentAlignment = Alignment.Center,
    ) {
      // Thumbnail image or placeholder
      if (thumbnail != null) {
        Image(
          bitmap = thumbnail!!.asImageBitmap(),
          contentDescription = video.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Icon(
          imageVector = Icons.Default.VideoFile,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(24.dp)
        )
      }
      
      // Gradient overlay at bottom
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
            )
          )
      )
      
      // Play indicator for current video
      if (isCurrentVideo) {
        Surface(
          modifier = Modifier
            .align(Alignment.Center)
            .padding(8.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "Playing",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
        }
      }
    }
    
    // Video title
    Text(
      text = video.title.substringBeforeLast(".").take(30),
      style = MaterialTheme.typography.bodySmall,
      color = if (isCurrentVideo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .padding(top = 6.dp)
        .fillMaxWidth()
    )
  }
}
