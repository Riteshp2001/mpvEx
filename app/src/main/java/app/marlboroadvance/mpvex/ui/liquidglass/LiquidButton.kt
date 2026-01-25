package app.marlboroadvance.mpvex.ui.liquidglass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
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
    shape: Shape = RoundedCornerShape(24.dp), // More rounded for pill shape
    backdrop: Backdrop? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()
    
    // Glass visual container
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(if (isPressed) 12.dp.toPx() else 8.dp.toPx())
                if (isPressed) {
                    lens(6.dp.toPx(), 6.dp.toPx(), chromaticAberration = true)
                } else {
                    lens(2.dp.toPx(), 2.dp.toPx())
                }
            },
            highlight = {
                Highlight.Default.copy(alpha = if (isPressed) 0.8f else 0.4f)
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
                val baseColor = if (isDark) Color.White else Color.Black
                val alpha = if (isPressed) 0.15f else 0.05f
                drawRect(baseColor.copy(alpha = alpha))
            }
        )
    } else {
        // Fallback logic remains similar but simpler
        Modifier
    }

    Box(
        modifier = modifier
            .size(64.dp, 44.dp) // Slightly larger pill shape
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
