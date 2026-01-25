package app.marlboroadvance.mpvex.ui.liquidglass.effects

import android.graphics.RenderEffect
import android.os.Build

import app.marlboroadvance.mpvex.ui.liquidglass.BackdropEffectScope

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createChainEffect(effect, currentEffect)
        } else {
            effect
        }
}


