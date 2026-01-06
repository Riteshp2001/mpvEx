package app.marlboroadvance.mpvex.ui.browser

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.fab.MediaActionFab
import app.marlboroadvance.mpvex.ui.browser.fab.PlaylistActionFab
import app.marlboroadvance.mpvex.ui.browser.folderlist.FolderListScreen
import app.marlboroadvance.mpvex.ui.browser.networkstreaming.NetworkStreamingScreen
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistScreen
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.PlayLinkSheet
import app.marlboroadvance.mpvex.ui.compose.LocalLazyGridState
import app.marlboroadvance.mpvex.ui.compose.LocalLazyListState
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object MainScreen : Screen {
  // Use a companion object to store state more persistently
  private var persistentSelectedTab: Int = 0
  
  // Shared state that can be updated by FileSystemBrowserScreen
  @Volatile
  private var isInSelectionModeShared: Boolean = false  // Controls FAB visibility
  
  @Volatile
  private var shouldHideNavigationBar: Boolean = false  // Controls navigation bar visibility
  
  @Volatile
  private var sharedVideoSelectionManager: Any? = null
  
  // Check if the selection contains only videos and update navigation bar visibility accordingly
  @Volatile
  private var onlyVideosSelected: Boolean = false
  
  /**
   * Update selection state and navigation bar visibility
   * This method should be called whenever selection changes
   */
  fun updateSelectionState(
    isInSelectionMode: Boolean,
    isOnlyVideosSelected: Boolean,
    selectionManager: Any?
  ) {
    this.isInSelectionModeShared = isInSelectionMode
    this.onlyVideosSelected = isOnlyVideosSelected
    this.sharedVideoSelectionManager = selectionManager
    
    // Only hide navigation bar when videos are selected AND in selection mode
    // This fixes the issue where bottom bar disappears when only videos are selected
    this.shouldHideNavigationBar = isInSelectionMode && isOnlyVideosSelected
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    var selectedTab by remember {
      mutableIntStateOf(persistentSelectedTab)
    }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Create separate list and grid states for each tab to track scroll position
    val foldersListState = rememberLazyListState()
    val foldersGridState = rememberLazyGridState()
    val recentListState = rememberLazyListState()
    val recentGridState = rememberLazyGridState()
    val playlistListState = rememberLazyListState()
    val playlistGridState = rememberLazyGridState()
    val networkListState = rememberLazyListState()
    val networkGridState = rememberLazyGridState()

    
    // Current active grid state based on selected tab
    // Since selectedTab now refers to index in *enabled* list, we need to map back to logical ID
    // However, for simplicity in FAB and content switching, we'll keep selectedTab as the index in the *displayed* list
    // and map it to the logical "screen ID" where:
    // 0 = Folders, 1 = Recent, 2 = Playlist, 3 = Network

    
    val playlistRepository = koinInject<PlaylistRepository>()
    
    // FAB state
    var fabMenuExpanded by remember { mutableStateOf(false) }
    val showLinkDialog = remember { mutableStateOf(false) }
    
    // Playlist FAB state
    val showCreatePlaylistDialog = remember { mutableStateOf(false) }
    val showM3UPlaylistDialog = remember { mutableStateOf(false) }
    
    // File picker
    val filePicker = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
      uri?.let {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            it,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
        }
        MediaUtils.playFile(it.toString(), context, "open_file")
      }
    }
    
    // Shared state (across the app)
    val isInSelectionMode = remember { mutableStateOf(isInSelectionModeShared) }
    val hideNavigationBar = remember { mutableStateOf(shouldHideNavigationBar) }
    val videoSelectionManager = remember { mutableStateOf<SelectionManager<*, *>?>(sharedVideoSelectionManager as? SelectionManager<*, *>) }
    
    // Check for state changes to ensure UI updates
    LaunchedEffect(Unit) {
      while (true) {
        // Update FAB visibility state
        if (isInSelectionMode.value != isInSelectionModeShared) {
          isInSelectionMode.value = isInSelectionModeShared
          android.util.Log.d("MainScreen", "Selection mode changed to: $isInSelectionModeShared")
        }
        
        // Update navigation bar visibility state - now considers if only videos are selected
        if (hideNavigationBar.value != shouldHideNavigationBar) {
          hideNavigationBar.value = shouldHideNavigationBar
          android.util.Log.d("MainScreen", "Navigation bar visibility changed to: ${!shouldHideNavigationBar}, onlyVideosSelected: $onlyVideosSelected")
        }
        
        // Update selection manager
        val currentManager = sharedVideoSelectionManager as? SelectionManager<*, *>
        if (videoSelectionManager.value != currentManager) {
          videoSelectionManager.value = currentManager
        }
        
        // Minimal delay for polling
        delay(16) // Roughly matches a frame at 60fps for responsive updates
      }
    }
    
    // Update persistent state whenever tab changes
    LaunchedEffect(selectedTab) {
      android.util.Log.d("MainScreen", "selectedTab changed to: $selectedTab (was ${persistentSelectedTab})")
      persistentSelectedTab = selectedTab
    }

    val browserPreferences = koinInject<BrowserPreferences>()
    val showRecentTab by browserPreferences.showRecentTab.collectAsState()
    val showPlaylistTab by browserPreferences.showPlaylistTab.collectAsState()
    val showNetworkTab by browserPreferences.showNetworkTab.collectAsState()

    // Define navigation items with their logical IDs
    data class NavItem(val title: String, val logicalId: Int, val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector, val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector)

    val allItems = listOf(
        NavItem("Folders", 0, Icons.Filled.Folder, Icons.Outlined.Folder),
        NavItem("Recent", 1, Icons.Filled.History, Icons.Outlined.History),
        NavItem("Playlist", 2, Icons.AutoMirrored.Filled.PlaylistPlay, Icons.AutoMirrored.Outlined.PlaylistPlay),
        NavItem("Network", 3, Icons.Filled.Wifi, Icons.Outlined.Wifi)
    )

    // Filter enabled items
    val enabledItems = remember(showRecentTab, showPlaylistTab, showNetworkTab) {
        allItems.filter { item ->
            when (item.logicalId) {
                0 -> true // Folders is always shown
                1 -> showRecentTab
                2 -> showPlaylistTab
                3 -> showNetworkTab
                else -> true
            }
        }
    }

    // Ensure selectedTab is within bounds of enabledItems
    LaunchedEffect(enabledItems.size) {
        if (selectedTab >= enabledItems.size) {
            selectedTab = 0
        }
    }

    // Get the logical ID of the currently selected tab
    val currentLogicalId = if (selectedTab < enabledItems.size) enabledItems[selectedTab].logicalId else 0

    // Update list/grid state helpers based on current logical ID
    // (Note: we moved these here because they depend on currentLogicalId)
    val currentListState = when (currentLogicalId) {
        0 -> foldersListState
        1 -> recentListState
        2 -> playlistListState
        3 -> networkListState
        else -> foldersListState
    }
    
    val currentGridState = when (currentLogicalId) {
        0 -> foldersGridState
        1 -> recentGridState
        2 -> playlistGridState
        3 -> networkGridState
        else -> foldersGridState
    }

    // Use Scaffold only for bottom bar, let nested screens handle their own top bars and padding
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        // Use AnimatedVisibility to smoothly animate the navigation bar
        // Only hide navigation bar when specifically needed (video selection operations)
        // Also hide if only 1 tab is enabled (user preferences)
        val showBottomNav = enabledItems.size > 1 && !hideNavigationBar.value
        
        AnimatedVisibility(
          visible = showBottomNav,
          enter = slideInVertically(initialOffsetY = { it }), // Start from below the screen
          exit = slideOutVertically(targetOffsetY = { it }) // Slide down off screen
        ) {
          NavigationBar {
              enabledItems.forEachIndexed { index, item ->
                  NavigationBarItem(
                      icon = {
                          Icon(
                              if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                              contentDescription = item.title,
                          )
                      },
                      label = { Text(item.title) },
                      selected = selectedTab == index,
                      onClick = { selectedTab = index },
                  )
              }
          }
        }
      },
      floatingActionButton = {
        // Only show FAB when not in selection mode
        // Use currentLogicalId to determine content
        if (!isInSelectionMode.value && currentLogicalId != 3) {
          AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            // Show different FAB content based on selected tab (logical ID)
            when (currentLogicalId) {
              // Folders tab (0)
              0 -> {
                MediaActionFab(
                  listState = currentListState,
                  gridState = currentGridState, // Add grid state
                  hasRecentlyPlayed = true,
                  enableRecentlyPlayed = true,
                  onOpenFile = { 
                    // Launch file picker to select a file
                    filePicker.launch(arrayOf("*/*"))
                  },
                  onPlayRecentlyPlayed = {
                    coroutineScope.launch {
                      val lastPlayedEntity = app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps.getLastPlayedEntity()
                      if (lastPlayedEntity != null) {
                        // Just play the single video
                        MediaUtils.playFile(
                          lastPlayedEntity.filePath,
                          context,
                          "recently_played_button",
                        )
                      }
                    }
                  },
                  onPlayLink = { showLinkDialog.value = true },
                  expanded = fabMenuExpanded,
                  onExpandedChange = { fabMenuExpanded = it },
                  modifier = Modifier,
                )
              }
              
              // Recent tab (1)
              1 -> {
                MediaActionFab(
                  listState = currentListState,
                  gridState = currentGridState, // Add grid state
                  hasRecentlyPlayed = true,
                  enableRecentlyPlayed = true,
                  onOpenFile = { 
                    // Launch file picker to select a file
                    filePicker.launch(arrayOf("*/*"))
                  },
                  onPlayRecentlyPlayed = {
                    coroutineScope.launch {
                      val lastPlayedEntity = app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps.getLastPlayedEntity()
                      if (lastPlayedEntity != null) {
                        // Just play the single video
                        MediaUtils.playFile(
                          lastPlayedEntity.filePath,
                          context,
                          "recently_played_button",
                        )
                      }
                    }
                  },
                  onPlayLink = { showLinkDialog.value = true },
                  expanded = fabMenuExpanded,
                  onExpandedChange = { fabMenuExpanded = it },
                  modifier = Modifier,
                )
              }
              
              // Playlist tab (2)
              2 -> {
                PlaylistActionFab(
                  listState = currentListState,
                  gridState = currentGridState, // Add grid state
                  onCreatePlaylist = { showCreatePlaylistDialog.value = true },
                  onAddM3UPlaylist = { showM3UPlaylistDialog.value = true },
                  expanded = fabMenuExpanded,
                  onExpandedChange = { fabMenuExpanded = it },
                  modifier = Modifier,
                )
              }
              
              // Network tab (3) - No FAB for this tab
              else -> { /* No FAB for Network tab */ }
            }
          }
        }
      },
    ) { paddingValues ->
      // Each screen handles its own bottom padding for the navigation bar
      Box(modifier = Modifier.fillMaxSize()) {
        when (currentLogicalId) {
          0 -> {
            CompositionLocalProvider(
              LocalLazyListState provides foldersListState,
              LocalLazyGridState provides foldersGridState
            ) {
              FolderListScreen.Content()
            }
          }
          1 -> {
            CompositionLocalProvider(
              LocalLazyListState provides recentListState,
              LocalLazyGridState provides recentGridState
            ) {
              RecentlyPlayedScreen.Content()
            }
          }
          2 -> {
            CompositionLocalProvider(
              LocalLazyListState provides playlistListState,
              LocalLazyGridState provides playlistGridState
            ) {
              PlaylistScreen.Content()
            }
          }
          3 -> {
            CompositionLocalProvider(
              LocalLazyListState provides networkListState,
              LocalLazyGridState provides networkGridState
            ) {
              NetworkStreamingScreen.Content()
            }
          }
        }
        
        // Dialog for play link functionality
        PlayLinkSheet(
          isOpen = showLinkDialog.value,
          onDismiss = { showLinkDialog.value = false },
          onPlayLink = { url -> 
            MediaUtils.playFile(url, context, "play_link") 
          },
        )
        
        // Dialog for playlist creation
        if (showCreatePlaylistDialog.value) {
          CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog.value = false },
            onConfirm = { name ->
              coroutineScope.launch {
                try {
                  playlistRepository.createPlaylist(name)
                  android.widget.Toast.makeText(
                    context,
                    "Playlist created successfully",
                    android.widget.Toast.LENGTH_SHORT
                  ).show()
                } catch (e: Exception) {
                  android.widget.Toast.makeText(
                    context,
                    "Failed to create playlist: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                  ).show()
                }
                showCreatePlaylistDialog.value = false
              }
            }
          )
        }
        
        // Dialog for M3U playlist creation
        if (showM3UPlaylistDialog.value) {
          AddM3UPlaylistDialog(
            onDismiss = { showM3UPlaylistDialog.value = false },
            onConfirm = { url ->
              coroutineScope.launch {
                val result = playlistRepository.createM3UPlaylist(url)
                result.onSuccess {
                  android.widget.Toast.makeText(
                    context,
                    "M3U Playlist added successfully",
                    android.widget.Toast.LENGTH_SHORT
                  ).show()
                }.onFailure { error ->
                  android.widget.Toast.makeText(
                    context,
                    "Failed to add M3U playlist: ${error.message}",
                    android.widget.Toast.LENGTH_LONG
                  ).show()
                }
                showM3UPlaylistDialog.value = false
              }
            },
            onPickLocalFile = { uri ->
              coroutineScope.launch {
                val result = playlistRepository.createM3UPlaylistFromFile(context, uri)
                result.onSuccess {
                  android.widget.Toast.makeText(
                    context,
                    "M3U Playlist added successfully",
                    android.widget.Toast.LENGTH_SHORT
                  ).show()
                }.onFailure { error ->
                  android.widget.Toast.makeText(
                    context,
                    "Failed to add M3U playlist: ${error.message}",
                    android.widget.Toast.LENGTH_LONG
                  ).show()
                }
                showM3UPlaylistDialog.value = false
              }
            }
          )
        }
      }
    }
  }

  // Create Playlist Dialog
  @Composable
  private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
  ) {
    var playlistName by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Create Playlist") },
      text = {
        androidx.compose.material3.OutlinedTextField(
          value = playlistName,
          onValueChange = { playlistName = it },
          label = { Text("Playlist Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        androidx.compose.material3.TextButton(
          onClick = {
            if (playlistName.isNotBlank()) {
              onConfirm(playlistName)
            }
          },
          enabled = playlistName.isNotBlank(),
        ) {
          Text("Create")
        }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      },
    )
  }

  // M3U Playlist Dialog
  @Composable
  private fun AddM3UPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onPickLocalFile: (android.net.Uri) -> Unit,
  ) {
    var playlistUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
      uri?.let {
        isLoading = true
        onPickLocalFile(it)
      }
    }

    androidx.compose.material3.AlertDialog(
      onDismissRequest = if (isLoading) {
        {}
      } else {
        onDismiss
      },
      title = { Text("Add M3U Playlist") },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Enter the URL of an M3U playlist file, or choose a local file",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = playlistUrl,
            onValueChange = { playlistUrl = it },
            label = { Text("Playlist URL") },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
          
          // Divider with "OR" text
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
              text = "OR",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
          }
          
          // Local file picker button
          OutlinedButton(
            onClick = {
              filePickerLauncher.launch("*/*")
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Filled.FolderOpen,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Local M3U File")
          }
          
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
          }
        }
      },
      confirmButton = {
        androidx.compose.material3.TextButton(
          onClick = {
            if (playlistUrl.isNotBlank()) {
              isLoading = true
              onConfirm(playlistUrl.trim())
            }
          },
          enabled = playlistUrl.isNotBlank() && !isLoading,
        ) {
          Text("Add from URL")
        }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(
          onClick = onDismiss,
          enabled = !isLoading
        ) {
          Text("Cancel")
        }
      },
    )
  }
}
