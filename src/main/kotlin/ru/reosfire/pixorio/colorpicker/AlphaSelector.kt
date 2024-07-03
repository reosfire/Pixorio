package ru.reosfire.pixorio.colorpicker

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.WindowPosition.PlatformDefault.x
import org.jetbrains.skia.Bitmap
import ru.reosfire.pixorio.BitmapCanvas

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun AlphaSelector(
    alpha: Float,
    onHueChanged: (Float) -> Unit,
    cellSize: Int = 5,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }

    fun updateAlpha(y: Float) {
        onHueChanged(y.coerceIn(0f, 1f))
    }

    Layout(
        modifier = modifier
            .onPointerEvent(PointerEventType.Press) {
                updateAlpha(it.changes.first().position.y / size.height)
                pressed = true
            }.onPointerEvent(PointerEventType.Move) {
                if (pressed) {
                    updateAlpha(it.changes.first().position.y / size.height)
                }
            }.onPointerEvent(PointerEventType.Release) {
                pressed = false
            }.drawWithCache {
                val background = createCheckeredBackground(IntSize((size.width / cellSize).toInt(), (size.height / cellSize).toInt())).asComposeImageBitmap()

                val barBackgroundColor = Color.Black.copy(alpha = 0.8f)
                val barColor = Color.hsv(0f, 0f, 0.5f)

                val barLeftEnd = Offset(0f, alpha * size.height)
                val barRightEnd = Offset(size.width, alpha * size.height)

                onDrawBehind {
                    drawImage(
                        background,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.None,
                    )

                    drawLine(barBackgroundColor, barLeftEnd, barRightEnd, 5f)
                    drawLine(barColor, barLeftEnd, barRightEnd, 3f)
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun createCheckeredBackground(
    size: IntSize
): Bitmap {
    val canvas = BitmapCanvas(size)

    for (y in 0 until size.height) {
        val yPercentage = y / size.height.toFloat()
        for (x in 0 until size.width) {
            val baseColor = if ((x + y) % 2 == 0) Color.Black else Color.White
            val shadowColor = Color(1f,1f,1f, yPercentage)
            canvas.setColor(x, y, shadowColor.compositeOver(baseColor))
        }
    }

    return canvas.createBitmap()
}
