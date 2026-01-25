package app.marlboroadvance.mpvex.ui.liquidglass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import app.marlboroadvance.mpvex.ui.liquidglass.backdrops.*
import app.marlboroadvance.mpvex.ui.liquidglass.Backdrop
import app.marlboroadvance.mpvex.ui.liquidglass.effects.blur
import app.marlboroadvance.mpvex.ui.liquidglass.effects.lens
import app.marlboroadvance.mpvex.ui.liquidglass.effects.vibrancy
import app.marlboroadvance.mpvex.ui.liquidglass.highlight.Highlight
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.Shadow
import androidx.compose.ui.graphics.lerp as ColorLerp
import androidx.compose.ui.util.lerp as FloatLerp

@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isLightTheme = !isSystemInDarkTheme()
    
    // Dimensions from reference implementation (LiquidToggle.kt)
    val trackWidth = 64.dp
    val trackHeight = 28.dp
    val thumbWidth = 40.dp
    val thumbHeight = 24.dp
    val padding = 2.dp
    val dragWidth = 20.dp
    
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(200),
        label = "PressProgress"
    )

    val fraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(300),
        label = "Fraction"
    )

    // Dynamic Colors from Theme
    val accentColor = MaterialTheme.colorScheme.primary
    val baseTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    // Shapes
    val shape = RoundedCornerShape(50)

    // Internal backdrop for the track
    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange?.invoke(!checked) }
            )
            .size(trackWidth, trackHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track
        Box(
            Modifier
                .size(trackWidth, trackHeight)
                .layerBackdrop(trackBackdrop)
                .clip(shape)
                .drawBehind {
                    drawRect(ColorLerp(baseTrackColor, accentColor, fraction))
                }
        )

        // Thumb
        Box(
            Modifier
                .graphicsLayer {
                    val p = padding.toPx()
                    val d = dragWidth.toPx()
                    translationX = FloatLerp(p, p + d, fraction)
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop ?: emptyBackdrop(),
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val scaleX = FloatLerp(2f / 3f, 0.75f, pressProgress)
                            val scaleY = FloatLerp(0f, 0.75f, pressProgress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx() * (1f - pressProgress))
                        lens(
                            5.dp.toPx() * pressProgress,
                            10.dp.toPx() * pressProgress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = pressProgress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    layerBlock = {
                        // Squash and stretch based on transition fraction
                        // The expansion effect: stretch in X, squash in Y during move
                        val stretch = (4f * fraction * (1f - fraction)) // Parabolic curve: 0 at start/end, max at middle
                        scaleX = 1f + stretch * 0.4f
                        scaleY = 1f - stretch * 0.2f
                    },
                    onDrawSurface = {
                        // Total white only in shrink state (progress 0)
                        // Fades out as it expands/presses
                        val progress = pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(thumbWidth, thumbHeight)
        )
    }
}
