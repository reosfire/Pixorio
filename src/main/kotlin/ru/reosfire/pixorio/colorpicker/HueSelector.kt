package ru.reosfire.pixorio.colorpicker

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun HueSelector(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }

    fun updateHue(y: Float) {
        onHueChanged(y.coerceIn(0f, 360f))
    }

    val backgroundBrush = remember { hueBrush() }

    Layout(
        modifier = modifier
            .onPointerEvent(PointerEventType.Press) {
                updateHue(it.changes.first().position.y / size.height * 360f)
                pressed = true
            }.onPointerEvent(PointerEventType.Move) {
                if (pressed) {
                    updateHue(it.changes.first().position.y / size.height * 360f)
                }
            }.onPointerEvent(PointerEventType.Release) {
                pressed = false
            }.drawWithCache {
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
