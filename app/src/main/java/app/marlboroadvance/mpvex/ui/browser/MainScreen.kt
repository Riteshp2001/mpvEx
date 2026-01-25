package app.marlboroadvance.mpvex.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Language
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.folderlist.FolderListScreen
import app.marlboroadvance.mpvex.ui.browser.networkstreaming.NetworkStreamingScreen
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistScreen
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.compose.LocalLazyGridState
import app.marlboroadvance.mpvex.ui.compose.LocalLazyListState
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.rememberLayerBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.layerBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.LiquidBottomTabs
import app.marlboroadvance.mpvex.ui.liquidglass.LiquidBottomTab
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

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
  private var isBrowserBottomBarVisible: Boolean = false  // Tracks browser bottom bar visibility
  
  @Volatile
  private var sharedVideoSelectionManager: Any? = null
  
  // Check if the selection contains only videos and update navigation bar visibility accordingly
  @Volatile
  private var onlyVideosSelected: Boolean = false
  
  // Track when permission denied screen is showing to hide FAB
  @Volatile
  private var isPermissionDenied: Boolean = false
  
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
  
  /**
   * Update permission state to control FAB visibility
   */
  fun updatePermissionState(isDenied: Boolean) {
    this.isPermissionDenied = isDenied
  }

  /**
   * Get current permission denied state
   */
  fun getPermissionDeniedState(): Boolean = isPermissionDenied

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state
   */
  fun updateBottomBarVisibility(shouldShow: Boolean) {
    // Hide bottom navigation when floating bottom bar is visible
    this.shouldHideNavigationBar = !shouldShow
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    var selectedTab by remember {
      mutableIntStateOf(persistentSelectedTab)
    }

    val context = LocalContext.current
    val density = LocalDensity.current

    // Create shared LazyListState and LazyGridState for folder navigation
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val advancedPreferences = koinInject<AdvancedPreferences>()
    val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()
    val backdrop = rememberLayerBackdrop()

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

    Box(Modifier.fillMaxSize()) {
        Scaffold(
          modifier = Modifier
              .fillMaxSize()
              .let { if (enableLiquidGlass) it.layerBackdrop(backdrop) else it },
          bottomBar = {
            if (!enableLiquidGlass) {
                // Animated bottom navigation bar with slide animations
                AnimatedVisibility(
                  visible = !hideNavigationBar.value,
                  enter = slideInVertically(
                    animationSpec = tween(durationMillis = 300),
                    initialOffsetY = { fullHeight -> fullHeight }
                  ),
                  exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 300),
                    targetOffsetY = { fullHeight -> fullHeight }
                  )
                ) {
                  NavigationBar(
                    modifier = Modifier
                      .clip(
                        RoundedCornerShape(
                          topStart = 28.dp,
                          topEnd = 28.dp,
                          bottomStart = 0.dp,
                          bottomEnd = 0.dp
                        )
                      )
                  ) {
                    NavigationBarItem(
                      icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                      label = { Text("Home") },
                      selected = selectedTab == 0,
                      onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                      icon = { Icon(Icons.Filled.History, contentDescription = "Recents") },
                      label = { Text("Recents") },
                      selected = selectedTab == 1,
                      onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                      icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Playlists") },
                      label = { Text("Playlists") },
                      selected = selectedTab == 2,
                      onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                      icon = { Icon(Icons.Filled.Language, contentDescription = "Network") },
                      label = { Text("Network") },
                      selected = selectedTab == 3,
                      onClick = { selectedTab = 3 }
                    )
                  }
                }
            }
          }
        ) { paddingValues ->
          Box(modifier = Modifier.fillMaxSize()) {
            // Always use 80dp bottom padding regardless of navigation bar visibility
            val fabBottomPadding = 80.dp
    
            AnimatedContent(
              targetState = selectedTab,
              transitionSpec = {
                // Material 3 Expressive slide-in-fade animation (like Google Phone app)
                val slideDistance = with(density) { 48.dp.roundToPx() }
                val animationDuration = 400
                
                if (targetState > initialState) {
                  // Moving forward: slide in from right with fade
                  (slideInHorizontally(
                    animationSpec = tween(
                      durationMillis = animationDuration,
                      easing = FastOutSlowInEasing
                    ),
                    initialOffsetX = { slideDistance }
                  ) + fadeIn(
                    animationSpec = tween(
                      durationMillis = animationDuration,
                      easing = FastOutSlowInEasing
                    )
                  )) togetherWith (slideOutHorizontally(
                    animationSpec = tween(
                      durationMillis = animationDuration,
                      easing = FastOutSlowInEasing
                    ),
                    targetOffsetX = { -slideDistance }
                  ) + fadeOut(
                    animationSpec = tween(
                      durationMillis = animationDuration / 2,
                      easing = FastOutSlowInEasing
                    )
                  ))
                } else {
                  // Moving backward: slide in from left with fade
                  (slideInHorizontally(
                    animationSpec = tween(
                      durationMillis = animationDuration,
                      easing = FastOutSlowInEasing
                    ),
                    initialOffsetX = { -slideDistance }
                  ) + fadeIn(
                    animationSpec = tween(
                      durationMillis = animationDuration,
                      easing = FastOutSlowInEasing
                    )
                  )) togetherWith (slideOutHorizontally(
                    animationSpec = tween(
                      durationMillis = animationDuration,
                      easing = FastOutSlowInEasing
                    ),
                    targetOffsetX = { slideDistance }
                  ) + fadeOut(
                    animationSpec = tween(
                      durationMillis = animationDuration / 2,
                      easing = FastOutSlowInEasing
                    )
                  ))
                }
              },
              label = "tab_animation"
            ) { targetTab ->
              CompositionLocalProvider(
                LocalNavigationBarHeight provides fabBottomPadding,
                LocalLazyListState provides lazyListState,
                LocalLazyGridState provides lazyGridState
              ) {
                when (targetTab) {
                  0 -> FolderListScreen.Content()
                  1 -> RecentlyPlayedScreen.Content()
                  2 -> PlaylistScreen.Content()
                  3 -> NetworkStreamingScreen.Content()
                }
              }
            }
          }
        }
    
        if (enableLiquidGlass && !hideNavigationBar.value) {
             Box(
                 modifier = Modifier
                     .align(Alignment.BottomCenter)
                     .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
             ) {
                 val isDark = isSystemInDarkTheme()
                 val iconColor = if (isDark) Color.White else Color.Black
                 
                 LiquidBottomTabs(
                     selectedTabIndex = { selectedTab },
                     onTabSelected = { selectedTab = it },
                     backdrop = backdrop,
                     tabsCount = 4,
                     modifier = Modifier
                 ) {
                     LiquidBottomTab(onClick = { selectedTab = 0 }) {
                          Icon(Icons.Rounded.Home, "Home", tint = iconColor, modifier = Modifier.size(24.dp))
                          androidx.compose.material3.Text("Home", color = iconColor, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                     }
                     LiquidBottomTab(onClick = { selectedTab = 1 }) {
                          Icon(Icons.Rounded.History, "Recents", tint = iconColor, modifier = Modifier.size(24.dp))
                          androidx.compose.material3.Text("Recents", color = iconColor, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                     }
                     LiquidBottomTab(onClick = { selectedTab = 2 }) {
                         Icon(Icons.Rounded.PlaylistPlay, "Playlists", tint = iconColor, modifier = Modifier.size(24.dp))
                         androidx.compose.material3.Text("Playlists", color = iconColor, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                     }
                     LiquidBottomTab(onClick = { selectedTab = 3 }) {
                         Icon(Icons.Rounded.Language, "Network", tint = iconColor, modifier = Modifier.size(24.dp))
                         androidx.compose.material3.Text("Network", color = iconColor, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                     }
                 }
             }
        }
    }
  }
}

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }
