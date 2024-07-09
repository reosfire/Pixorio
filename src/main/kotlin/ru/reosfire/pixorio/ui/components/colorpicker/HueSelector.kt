package ru.reosfire.pixorio.ui.components.colorpicker

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import ru.reosfire.pixorio.draggable

@Composable
@Preview
fun HueSelector(
    hueState: MutableFloatState,
    modifier: Modifier = Modifier,
) {
    var hue by hueState

    fun updateHue(y: Float) {
        hue = y.coerceIn(0f, 360f)
    }

    val backgroundBrush = remember { hueBrush() }

    Layout(
        modifier = modifier
            .draggable {
                updateHue(it.position.y / size.height * 360f)
            }
            .drawWithCache {
                val barBackgroundColor = Color.Black.copy(alpha = 0.8f)
                val barColor = Color.hsv(hue, 1f, 1f)

                val barLeftEnd = Offset(0f, hue / 360f * size.height)
                val barRightEnd = Offset(size.width, hue / 360f * size.height)

                onDrawBehind {
                    drawRect(backgroundBrush)

                    drawLine(barBackgroundColor, barLeftEnd, barRightEnd, 5f)
                    drawLine(barColor, barLeftEnd, barRightEnd, 3f)
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun hueBrush(colorStopsCount: Int = 36): Brush {
    val factor = 360f / colorStopsCount
    val colorStops = ArrayList<Color>(colorStopsCount)
    for (i in 0..<colorStopsCount) {
        colorStops.add(Color.hsv(i * factor, 1f, 1f))
    }
    return Brush.verticalGradient(colorStops)
}
