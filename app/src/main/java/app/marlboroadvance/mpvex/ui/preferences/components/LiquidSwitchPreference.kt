package app.marlboroadvance.mpvex.ui.preferences.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.liquidglass.Backdrop
import app.marlboroadvance.mpvex.ui.liquidglass.LiquidSwitch
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import org.koin.compose.koinInject
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue

@Composable
fun LiquidSwitchPreference(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    backdrop: Backdrop? = null
) {
    val advancedPreferences = koinInject<AdvancedPreferences>()
    val enableLiquidGlass by advancedPreferences.enableLiquidGlass.collectAsState()

    ListItem(
        headlineContent = title,
        supportingContent = summary,
        trailingContent = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp, 48.dp)) {
                if (enableLiquidGlass) {
                    LiquidSwitch(
                        checked = value,
                        onCheckedChange = { if (enabled) onValueChange(it) },
                        enabled = enabled,
                        backdrop = backdrop
                    )
                } else {
                    androidx.compose.material3.Switch(
                        checked = value,
                        onCheckedChange = { if (enabled) onValueChange(it) },
                        enabled = enabled
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent // Allow backdrop to be visible
        ),
        modifier = modifier
            .clickable(enabled = enabled) {
                onValueChange(!value)
            }
    )
}
