package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import app.marlboroadvance.mpvex.ui.player.controls.LocalPlayerButtonsClickEvent
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.preferences.SeekbarStyle
import dev.vivvvek.seeker.Segment

import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import app.marlboroadvance.mpvex.ui.liquidglass.Backdrop
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.layerBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.rememberBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.rememberCombinedBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.rememberLayerBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.drawBackdrop
import app.marlboroadvance.mpvex.ui.liquidglass.effects.blur
import app.marlboroadvance.mpvex.ui.liquidglass.effects.lens
import app.marlboroadvance.mpvex.ui.liquidglass.highlight.Highlight
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.InnerShadow
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.Shadow
import app.marlboroadvance.mpvex.ui.liquidglass.util.DampedDragAnimation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@Composable
fun SeekbarWithTimers(
  position: Float,
  duration: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit,
  timersInverted: Pair<Boolean, Boolean>,
  positionTimerOnClick: () -> Unit,
  durationTimerOnCLick: () -> Unit,
  chapters: ImmutableList<Segment>,
  paused: Boolean,
  readAheadValue: Float = position,
  seekbarStyle: SeekbarStyle = SeekbarStyle.Wavy,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  var isUserInteracting by remember { mutableStateOf(false) }
  var userPosition by remember { mutableFloatStateOf(position) }

  // Animated position for smooth transitions
  val animatedPosition = remember { Animatable(position) }
  val scope = rememberCoroutineScope()

  // Only animate position updates when user is not interacting
  LaunchedEffect(position, isUserInteracting) {
    if (!isUserInteracting && position != animatedPosition.value) {
      scope.launch {
        animatedPosition.animateTo(
          targetValue = position,
          animationSpec =
            tween(
              durationMillis = 200,
              easing = LinearEasing,
            ),
        )
      }
    }
  }

  Row(
    modifier = modifier.height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
  ) {
    VideoTimer(
      value = if (isUserInteracting) userPosition else position,
      timersInverted.first,
      onClick = {
        clickEvent()
        positionTimerOnClick()
      },
      modifier = Modifier.width(92.dp),
    )

    // Seekbar
    Box(
      modifier =
        Modifier
          .weight(1f)
          .height(48.dp),
      contentAlignment = Alignment.Center,
    ) {
      val advancedPreferences = koinInject<AdvancedPreferences>()
      val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()
      val effectiveStyle = if (seekbarStyle == SeekbarStyle.Liquid && !enableLiquidGlass) {
        SeekbarStyle.Standard
      } else {
        seekbarStyle
      }

      when (effectiveStyle) {
        SeekbarStyle.Standard -> {
          StandardSeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            readAheadValue = readAheadValue,
            chapters = chapters,
            onSeek = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onSeekFinished = {
              scope.launch { animatedPosition.snapTo(userPosition) }
              isUserInteracting = false
              onValueChangeFinished()
            },
          )
        }
        SeekbarStyle.Wavy -> {
          SquigglySeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            readAheadValue = readAheadValue,
            chapters = chapters,
            isPaused = paused,
            isScrubbing = isUserInteracting,
            useWavySeekbar = true,
            seekbarStyle = SeekbarStyle.Wavy,
            onSeek = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onSeekFinished = {
              scope.launch { animatedPosition.snapTo(userPosition) }
              isUserInteracting = false
              onValueChangeFinished()
            },
          )
        }
        SeekbarStyle.Circular -> {
           SquigglySeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            readAheadValue = readAheadValue,
            chapters = chapters,
            isPaused = paused,
            isScrubbing = isUserInteracting,
            useWavySeekbar = true,
            seekbarStyle = SeekbarStyle.Circular,
            onSeek = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onSeekFinished = {
              scope.launch { animatedPosition.snapTo(userPosition) }
              isUserInteracting = false
              onValueChangeFinished()
            },
          )
        }
        SeekbarStyle.Simple -> {
             SquigglySeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            readAheadValue = readAheadValue,
            chapters = chapters,
            isPaused = paused,
            isScrubbing = isUserInteracting,
            useWavySeekbar = false,
            seekbarStyle = SeekbarStyle.Simple, 
            onSeek = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onSeekFinished = {
              scope.launch { animatedPosition.snapTo(userPosition) }
              isUserInteracting = false
              onValueChangeFinished()
            },
          )
        }
        SeekbarStyle.Thick -> {
          StandardSeekbar(
            position = if (isUserInteracting) userPosition else animatedPosition.value,
            duration = duration,
            readAheadValue = readAheadValue,
            chapters = chapters,
            seekbarStyle = SeekbarStyle.Thick,
            onSeek = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onSeekFinished = {
              scope.launch { animatedPosition.snapTo(userPosition) }
              isUserInteracting = false
              onValueChangeFinished()
            },
          )
        }
        SeekbarStyle.Liquid -> {
          LiquidSlider(
            value = if (isUserInteracting) userPosition else animatedPosition.value,
            onValueChange = { newPosition ->
              if (!isUserInteracting) isUserInteracting = true
              userPosition = newPosition
              onValueChange(newPosition)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..duration.coerceAtLeast(0.1f),
            readAheadValue = readAheadValue,
            chapters = chapters.map { it.start },
            backdrop = app.marlboroadvance.mpvex.ui.liquidglass.backdrops.emptyBackdrop(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          )
        }
      }
    }

    VideoTimer(
      value = if (timersInverted.second) position - duration else duration,
      isInverted = timersInverted.second,
      onClick = {
        clickEvent()
        durationTimerOnCLick()
      },
      modifier = Modifier.width(92.dp),
    )
  }
}

@Composable
private fun SquigglySeekbar(
  position: Float,
  duration: Float,
  readAheadValue: Float,
  chapters: ImmutableList<Segment>,
  isPaused: Boolean,
  isScrubbing: Boolean,
  useWavySeekbar: Boolean,
  seekbarStyle: SeekbarStyle,
  onSeek: (Float) -> Unit,
  onSeekFinished: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

  // Manual Interaction State Tracking
  var isPressed by remember { mutableStateOf(false) }
  var isDragged by remember { mutableStateOf(false) }
  val isInteracting = isPressed || isDragged || isScrubbing 

  // Animation state
  var phaseOffset by remember { mutableFloatStateOf(0f) }
  var heightFraction by remember { mutableFloatStateOf(1f) }

  val scope = rememberCoroutineScope()

  // Wave parameters
  val waveLength = 80f
  val lineAmplitude = if (useWavySeekbar) 6f else 0f
  val phaseSpeed = 10f // px per second
  val transitionPeriods = 1.5f
  val minWaveEndpoint = 0f
  val matchedWaveEndpoint = 1f
  val transitionEnabled = true

  // Animate height fraction based on paused state and scrubbing state
  LaunchedEffect(isPaused, isScrubbing, useWavySeekbar) {
    if (!useWavySeekbar) {
      heightFraction = 0f
      return@LaunchedEffect
    }

    scope.launch {
      val shouldFlatten = isPaused || isScrubbing
      val targetHeight = if (shouldFlatten) 0f else 1f
      val duration = if (shouldFlatten) 550 else 800
      val startDelay = if (shouldFlatten) 0L else 60L

      kotlinx.coroutines.delay(startDelay)

      val animator = Animatable(heightFraction)
      animator.animateTo(
        targetValue = targetHeight,
        animationSpec =
          tween(
            durationMillis = duration,
            easing = LinearEasing,
          ),
      ) {
        heightFraction = value
      }
    }
  }

  // Animate wave movement only when not paused
  LaunchedEffect(isPaused, useWavySeekbar) {
    if (isPaused || !useWavySeekbar) return@LaunchedEffect

    var lastFrameTime = withFrameMillis { it }
    while (isActive) {
      withFrameMillis { frameTimeMillis ->
        val deltaTime = (frameTimeMillis - lastFrameTime) / 1000f
        phaseOffset += deltaTime * phaseSpeed
        phaseOffset %= waveLength
        lastFrameTime = frameTimeMillis
      }
    }
  }

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(48.dp)
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {
                isPressed = true
                tryAwaitRelease()
                isPressed = false
            },
            onTap = { offset ->
                val newPosition = (offset.x / size.width) * duration
                onSeek(newPosition.coerceIn(0f, duration))
                onSeekFinished()
            }
          )
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { isDragged = true },
            onDragEnd = { 
                isDragged = false
                onSeekFinished() 
            },
            onDragCancel = { 
                isDragged = false
                onSeekFinished() 
            },
          ) { change, _ ->
            change.consume()
            val newPosition = (change.position.x / size.width) * duration
            onSeek(newPosition.coerceIn(0f, duration))
          }
        },
  ) {
    val strokeWidth = 5.dp.toPx()
    val progress = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
    val readAheadProgress = if (duration > 0f) (readAheadValue / duration).coerceIn(0f, 1f) else 0f
    val totalWidth = size.width
    val totalProgressPx = totalWidth * progress
    val totalReadAheadPx = totalWidth * readAheadProgress
    val centerY = size.height / 2f

    // Calculate wave progress
    val waveProgressPx =
      if (!transitionEnabled || progress > matchedWaveEndpoint) {
        totalWidth * progress
      } else {
        val t = (progress / matchedWaveEndpoint).coerceIn(0f, 1f)
        totalWidth * (minWaveEndpoint + (matchedWaveEndpoint - minWaveEndpoint) * t)
      }

    // Helper function to compute amplitude
    fun computeAmplitude(
      x: Float,
      sign: Float,
    ): Float =
      if (transitionEnabled) {
        val length = transitionPeriods * waveLength
        val coeff = ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
        sign * heightFraction * lineAmplitude * coeff
      } else {
        sign * heightFraction * lineAmplitude
      }

    // Build wavy path for played portion
    val path = Path()
    val waveStart = -phaseOffset - waveLength / 2f
    val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx

    path.moveTo(waveStart, centerY)

    var currentX = waveStart
    var waveSign = 1f
    var currentAmp = computeAmplitude(currentX, waveSign)
    val dist = waveLength / 2f

    while (currentX < waveEnd) {
      waveSign = -waveSign
      val nextX = currentX + dist
      val midX = currentX + dist / 2f
      val nextAmp = computeAmplitude(nextX, waveSign)

      path.cubicTo(
        midX,
        centerY + currentAmp,
        midX,
        centerY + nextAmp,
        nextX,
        centerY + nextAmp,
      )

      currentAmp = nextAmp
      currentX = nextX
    }

    // Draw path up to progress position using clipping
    val clipTop = lineAmplitude + strokeWidth
    val gapHalf = 1.dp.toPx()

    fun drawPathWithGaps(
      startX: Float,
      endX: Float,
      color: Color,
    ) {
      if (endX <= startX) return
      if (duration <= 0f) {
        clipRect(
          left = startX,
          top = centerY - clipTop,
          right = endX,
          bottom = centerY + clipTop,
        ) {
          drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
        }
        return
      }
      val gaps =
        chapters
          .map { (it.start / duration).coerceIn(0f, 1f) * totalWidth }
          .filter { it in startX..endX }
          .sorted()
          .map { x -> (x - gapHalf).coerceAtLeast(startX) to (x + gapHalf).coerceAtMost(endX) }

      var segmentStart = startX
      for ((gapStart, gapEnd) in gaps) {
        if (gapStart > segmentStart) {
          clipRect(
            left = segmentStart,
            top = centerY - clipTop,
            right = gapStart,
            bottom = centerY + clipTop,
          ) {
            drawPath(
              path = path,
              color = color,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
          }
        }
        segmentStart = gapEnd
      }
      if (segmentStart < endX) {
        clipRect(
          left = segmentStart,
          top = centerY - clipTop,
          right = endX,
          bottom = centerY + clipTop,
        ) {
          drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
        }
      }
    }

    // Played segment
    drawPathWithGaps(0f, totalProgressPx, primaryColor)

    // Buffer segment
    if (totalReadAheadPx > totalProgressPx) {
      val bufferAlpha = 0.5f
      drawPathWithGaps(totalProgressPx, totalReadAheadPx, primaryColor.copy(alpha = bufferAlpha))
    }

    if (transitionEnabled) {
      val disabledAlpha = 77f / 255f
      val unplayedStart = maxOf(totalProgressPx, totalReadAheadPx)
      drawPathWithGaps(unplayedStart, totalWidth, primaryColor.copy(alpha = disabledAlpha))
    } else {
      val flatLineStart = maxOf(totalProgressPx, totalReadAheadPx)
      drawLine(
        color = surfaceVariant.copy(alpha = 0.4f),
        start = Offset(flatLineStart, centerY),
        end = Offset(totalWidth, centerY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
      )
    }

    // Draw round cap
    val startAmp = kotlin.math.cos(kotlin.math.abs(waveStart) / waveLength * (2f * kotlin.math.PI.toFloat()))
    drawCircle(
      color = primaryColor,
      radius = strokeWidth / 2f,
      center = Offset(0f, centerY + startAmp * lineAmplitude * heightFraction),
    )

// SquigglySeekbar (Circular Thumb)
    if (seekbarStyle == SeekbarStyle.Circular) {
         val thumbRadius = 10.dp.toPx()
         drawCircle(
            color = primaryColor,
            radius = thumbRadius,
            center = Offset(totalProgressPx, centerY)
         )
    } else {
        // Vertical Bar (Wavy/Simple Thumb)
        val barHalfHeight = (lineAmplitude + strokeWidth)
        val barWidth = 5.dp.toPx()

        if (barHalfHeight > 0.5f) {
            drawLine(
              color = primaryColor,
              start = Offset(totalProgressPx, centerY - barHalfHeight),
              end = Offset(totalProgressPx, centerY + barHalfHeight),
              strokeWidth = barWidth,
              cap = StrokeCap.Round,
            )
        }
    }
  }
}

@Composable
fun VideoTimer(
  value: Float,
  isInverted: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  Text(
    modifier =
      modifier
        .fillMaxHeight()
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(),
          onClick = onClick,
        )
        .wrapContentHeight(Alignment.CenterVertically),
    text = Utils.prettyTime(value.toInt(), isInverted),
    color = Color.White,
    textAlign = TextAlign.Center,
  )
}

@Composable
fun StandardSeekbar(
    position: Float,
    duration: Float,
    readAheadValue: Float,
    chapters: ImmutableList<Segment>,
    isPaused: Boolean = false,
    isScrubbing: Boolean = false,
    useWavySeekbar: Boolean = false,
    seekbarStyle: SeekbarStyle = SeekbarStyle.Standard,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    
    val isThick = seekbarStyle == SeekbarStyle.Thick
    val trackHeightDp = if (isThick) 16.dp else 8.dp
    val thumbWidth = 6.dp
    val thumbHeight = if (isThick) 16.dp else 24.dp
    val thumbShape = if (isThick) RoundedCornerShape(thumbWidth / 2) else CircleShape

    Slider(
        value = position,
        onValueChange = onSeek,
        onValueChangeFinished = onSeekFinished,
        valueRange = 0f..duration.coerceAtLeast(0.1f),
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        track = { sliderState ->
            val disabledAlpha = 0.3f
            val bufferAlpha = 0.5f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeightDp),
            ) {
                val min = sliderState.valueRange.start
                val max = sliderState.valueRange.endInclusive
                val range = (max - min).takeIf { it > 0f } ?: 1f

                val playedFraction = ((sliderState.value - min) / range).coerceIn(0f, 1f)
                val readAheadFraction = ((readAheadValue - min) / range).coerceIn(0f, 1f)

                val playedPx = size.width * playedFraction
                val readAheadPx = size.width * readAheadFraction
                val trackHeight = size.height
                
                // Radius for the outer ends of the seekbar
                val outerRadius = trackHeight / 2f
                
                // MODIFIED: For Thick style, inner corners now match the outer rounding
                val innerRadius = if (isThick) outerRadius else 2.dp.toPx()
                
                val thumbTrackGapSize = 14.dp.toPx()
                val gapHalf = thumbTrackGapSize / 2f
                val chapterGapHalf = 1.dp.toPx()
                
                val thumbGapStart = (playedPx - gapHalf).coerceIn(0f, size.width)
                val thumbGapEnd = (playedPx + gapHalf).coerceIn(0f, size.width)
                
                val chapterGaps = chapters
                    .map { (it.start / duration).coerceIn(0f, 1f) * size.width }
                    .filter { it > 0f && it < size.width }
                    .map { x -> (x - chapterGapHalf) to (x + chapterGapHalf) }
                
                fun drawSegment(startX: Float, endX: Float, color: Color) {
                    if (endX - startX < 0.5f) return
                    
                    val path = Path()
                    val isOuterLeft = startX <= 0.5f
                    val isInnerLeft = kotlin.math.abs(startX - thumbGapEnd) < 0.5f
                    
                    val cornerRadiusLeft = when {
                        isOuterLeft -> CornerRadius(outerRadius)
                        isInnerLeft -> CornerRadius(innerRadius)
                        else -> CornerRadius.Zero
                    }

                    val isOuterRight = endX >= size.width - 0.5f
                    val isInnerRight = kotlin.math.abs(endX - thumbGapStart) < 0.5f

                    val cornerRadiusRight = when {
                        isOuterRight -> CornerRadius(outerRadius)
                        isInnerRight -> CornerRadius(innerRadius)
                        else -> CornerRadius.Zero
                    }
                    
                    path.addRoundRect(
                        RoundRect(
                            left = startX,
                            top = 0f,
                            right = endX,
                            bottom = trackHeight,
                            topLeftCornerRadius = cornerRadiusLeft,
                            bottomLeftCornerRadius = cornerRadiusLeft,
                            topRightCornerRadius = cornerRadiusRight,
                            bottomRightCornerRadius = cornerRadiusRight
                        )
                    )
                    drawPath(path, color)
                }
                
                fun drawRangeWithGaps(
                    rangeStart: Float, 
                    rangeEnd: Float, 
                    gaps: List<Pair<Float, Float>>, 
                    color: Color
                ) {
                    if (rangeEnd <= rangeStart) return
                    val relevantGaps = gaps
                        .filter { (gStart, gEnd) -> gEnd > rangeStart && gStart < rangeEnd }
                        .sortedBy { it.first }
                    
                    var currentPos = rangeStart
                    for ((gStart, gEnd) in relevantGaps) {
                        val segmentEnd = gStart.coerceAtMost(rangeEnd)
                        if (segmentEnd > currentPos) {
                            drawSegment(currentPos, segmentEnd, color)
                        }
                        currentPos = gEnd.coerceAtLeast(currentPos)
                    }
                    if (currentPos < rangeEnd) {
                        drawSegment(currentPos, rangeEnd, color)
                    }
                }
                
                // 1. Unplayed Background
                drawRangeWithGaps(thumbGapEnd, size.width, chapterGaps, primaryColor.copy(alpha = disabledAlpha))
                
                // 2. Buffer
                if (readAheadPx > thumbGapEnd) {
                    drawRangeWithGaps(thumbGapEnd, readAheadPx, chapterGaps, primaryColor.copy(alpha = bufferAlpha))
                }
                
                // 3. Played
                if (thumbGapStart > 0) {
                    drawRangeWithGaps(0f, thumbGapStart, chapterGaps, primaryColor)
                }
            }
        },
        thumb = {
            Box(
                modifier = Modifier
                    .width(thumbWidth)
                    .height(thumbHeight)
                    .background(primaryColor, thumbShape)
            )
        }
    )
}

@Composable
fun SeekbarPreview(
  style: SeekbarStyle,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "seekbar_preview")
  val progress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "progress"
  )
  val duration = 100f
  val position = progress * duration

  // Dummy chapters for preview to visualize chapter separation
  val dummyChapters = persistentListOf(
    Segment(name = "Chapter 1", start = 0f),
    Segment(name = "Chapter 2", start = duration * 0.35f),
    Segment(name = "Chapter 3", start = duration * 0.65f),
  )
  
  Box(
    modifier = modifier
      .height(32.dp),
    contentAlignment = Alignment.Center
  ) {
    // Seekbar content
    val advancedPreferences = koinInject<AdvancedPreferences>()
    val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()
    val effectiveStyle = if (style == SeekbarStyle.Liquid && !enableLiquidGlass) {
      SeekbarStyle.Standard
    } else {
      style
    }

    when (effectiveStyle) {
      SeekbarStyle.Standard -> {
        StandardSeekbar(
          position = position,
          duration = duration,
          readAheadValue = position,
          chapters = dummyChapters,
          isPaused = false,
          isScrubbing = false,
          useWavySeekbar = true,
          seekbarStyle = SeekbarStyle.Standard,
          onSeek = {},
          onSeekFinished = {},
        )
      }
      SeekbarStyle.Wavy -> {
        SquigglySeekbar(
          position = position,
          duration = duration,
          readAheadValue = position,
          chapters = dummyChapters,
          isPaused = false,
          isScrubbing = false,
          useWavySeekbar = true,
          seekbarStyle = SeekbarStyle.Wavy,
          onSeek = {},
          onSeekFinished = {},
        )
      }
      SeekbarStyle.Circular -> {
        SquigglySeekbar(
          position = position,
          duration = duration,
          readAheadValue = position,
          chapters = dummyChapters,
          isPaused = false,
          isScrubbing = false,
          useWavySeekbar = true,
          seekbarStyle = SeekbarStyle.Circular,
          onSeek = {},
          onSeekFinished = {},
        )
      }
      SeekbarStyle.Simple -> {
           SquigglySeekbar(
          position = position,
          duration = duration,
          readAheadValue = position,
          chapters = dummyChapters,
          isPaused = false,
          isScrubbing = false,
          useWavySeekbar = false,
          seekbarStyle = SeekbarStyle.Simple,
          onSeek = {},
          onSeekFinished = {},
        )
      }
      SeekbarStyle.Thick -> {
        StandardSeekbar(
          position = position,
          duration = duration,
          readAheadValue = position,
          chapters = dummyChapters,
          isPaused = false,
          isScrubbing = false,
          useWavySeekbar = true,
          seekbarStyle = SeekbarStyle.Thick,
          onSeek = {},
          onSeekFinished = {},
        )
      }
      SeekbarStyle.Liquid -> {
        LiquidSlider(
          value = position,
          onValueChange = {},
          valueRange = 0f..duration,
          readAheadValue = position, // Simulate full buffer matching position for preview
          chapters = dummyChapters.map { it.start },
          backdrop = app.marlboroadvance.mpvex.ui.liquidglass.backdrops.emptyBackdrop(),
          modifier = Modifier.fillMaxWidth().height(32.dp)
        )
      }
    }
    
    // Invisible overlay that intercepts all touch events and triggers onClick
    if (onClick != null) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // No ripple on overlay itself
            onClick = onClick
          )
      )
    }
  }
}

private val ContinuousCapsule = RoundedCornerShape(50)

@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float>,
    readAheadValue: Float = 0f,
    chapters: List<Float> = emptyList(),
    visibilityThreshold: Float = 0.001f,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val bufferColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value,
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    onValueChange(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange)
                        else (targetValue - delta).coerceIn(valueRange)
                    )
                }
            )
        }
        
        // Update animation when value changes externally
        LaunchedEffect(value) {
             if (dampedDragAnimation.targetValue != value) {
                 dampedDragAnimation.updateValue(value)
             }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            
            // Helper to draw segments with gaps
            fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegments(
                color: Color,
                width: Float,
                height: Float,
                totalTrackWidth: Float
            ) {
                 val gapSize = 2.dp.toPx()
                 val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
                 
                 if (chapters.isEmpty()) {
                     drawRect(color = color, size = androidx.compose.ui.geometry.Size(width, height))
                 } else {
                     val chapterPositions = chapters
                         .map { (it - valueRange.start) / range * totalTrackWidth }
                         .filter { it > 0f && it < totalTrackWidth }
                         .sorted()
                         
                     var currentX = 0f
                     chapterPositions.forEach { pos ->
                         val gapStart = pos - gapSize / 2
                         val gapEnd = pos + gapSize / 2
                         
                         // Draw up to gap start
                         if (gapStart > currentX) {
                             val segmentEnd = gapStart.coerceAtMost(width)
                             if (segmentEnd > currentX) {
                                 drawRect(
                                     color = color,
                                     topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                                     size = androidx.compose.ui.geometry.Size(segmentEnd - currentX, height)
                                 )
                             }
                         }
                         currentX = gapEnd
                     }
                     
                     // Draw remaining segment
                     if (currentX < width) {
                         drawRect(
                             color = color,
                             topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                             size = androidx.compose.ui.geometry.Size(width - currentX, height)
                         )
                     }
                 }
            }

            // Track with Chapter Gaps
            androidx.compose.foundation.Canvas(
                Modifier
                    .clip(ContinuousCapsule)
                    .height(6f.dp)
                    .fillMaxWidth()
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val targetValue =
                                (if (isLtr) valueRange.start + delta
                                else valueRange.endInclusive - delta)
                                    .coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                            onValueChangeFinished?.invoke()
                        }
                    }
            ) {
                drawSegments(trackColor, size.width, size.height, size.width)
            }

            // Buffer / Read-ahead Bar
            if (readAheadValue > valueRange.start) {
                val bufferProgress = ((readAheadValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                androidx.compose.foundation.Canvas(
                    Modifier
                        .clip(ContinuousCapsule)
                        .height(6f.dp)
                        .fillMaxWidth()
                ) {
                    drawSegments(bufferColor, size.width * bufferProgress, size.height, size.width)
                }
            }
            
            // Filled Progress Bar (Accent)
            androidx.compose.foundation.Canvas(
                Modifier
                    .clip(ContinuousCapsule)
                    .height(6f.dp)
                    .fillMaxWidth()
            ) {
                val filledWidth = (size.width * dampedDragAnimation.progress).coerceAtLeast(0f)
                drawSegments(accentColor, filledWidth, size.height, size.width)
            }
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        // Total white only in shrink state (progress 0)
                        // Fades out as it expands/presses
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}

@Composable
fun LiquidGlass(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    blurRadius: Dp = 10.dp,
    refractionAmount: Float = 0.5f,
    highlight: Highlight = Highlight.Default,
    shadow: Shadow? = Shadow.Default,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(blurRadius.toPx())
                    lens(
                        refractionHeight = blurRadius.toPx() / 2,
                        refractionAmount = blurRadius.toPx() * refractionAmount,
                        chromaticAberration = true
                    )
                },
                highlight = { highlight },
                shadow = { shadow },
                onDrawSurface = {
                     // Optional: add a slight tint if needed
                }
            )
    ) {
        content()
    }
}
