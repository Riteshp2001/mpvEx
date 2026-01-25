package app.marlboroadvance.mpvex.ui.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import app.marlboroadvance.mpvex.ui.liquidglass.Backdrop
import app.marlboroadvance.mpvex.ui.liquidglass.effects.blur
import app.marlboroadvance.mpvex.ui.liquidglass.effects.lens
import app.marlboroadvance.mpvex.ui.liquidglass.effects.vibrancy
import app.marlboroadvance.mpvex.ui.liquidglass.highlight.Highlight
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.InnerShadow
import app.marlboroadvance.mpvex.ui.liquidglass.shadow.Shadow

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    backdrop: Backdrop? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isLightTheme = !isSystemInDarkTheme()
    val density = LocalDensity.current
    
    // Dynamic Glass Colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(if (isPressed) 12.dp.toPx() else 8.dp.toPx())
                if (isPressed) {
                    lens(6.dp.toPx(), 6.dp.toPx()) // Reverted refraction
                } else {
                    lens(2.dp.toPx(), 2.dp.toPx())
                }
            },
            highlight = {
                Highlight.Ambient.copy(alpha = if (isPressed) 0.8f else 0.4f)
            },
            innerShadow = {
                InnerShadow(
                    radius = if (isPressed) 6.dp else 2.dp,
                    alpha = if (isPressed) 0.6f else 0.2f
                )
            },
            shadow = {
                Shadow(
                    radius = if (isPressed) 4.dp else 2.dp,
                    alpha = if (isPressed) 0.3f else 0.1f,
                    offset = DpOffset(0.dp, if (isPressed) 2.dp else 1.dp)
                )
            },
            onDrawSurface = {
                // Subtle white tip (reverted from "total white")
                val alpha = if (isPressed) 0.15f else 0.1f
                drawRect(Color.White.copy(alpha = alpha))
            }
        )
    } else {
        Modifier.background(surfaceColor.copy(alpha = 0.1f), shape)
    }

    Box(
        modifier = modifier
            .size(64.dp, 44.dp)
            .then(glassModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
