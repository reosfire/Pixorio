package ru.reosfire.pixorio.ui.components.colorpicker

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import ru.reosfire.pixorio.BitmapCanvas
import ru.reosfire.pixorio.draggable

private val BAR_BG_COLOR = Color.Black.copy(alpha = 0.8f)
private val BAR_COLOR = Color.hsv(0f, 0f, 0.5f)

@Composable
@Preview
fun AlphaSelector(
    alphaState: MutableFloatState,
    cellSize: Int = 5,
    modifier: Modifier = Modifier,
) {
    var alpha by alphaState

    fun updateAlpha(y: Float) {
        alpha = y.coerceIn(0f, 1f)
    }

    val imageDrawerPaint = remember { NativePaint() }

    Layout(
        modifier = modifier
            .draggable {
                updateAlpha(it.position.y / size.height)
            }
            .drawWithCache {
                val background = createCheckeredBackground(IntSize((size.width / cellSize).toInt(), (size.height / cellSize).toInt()))
                val backgroundImage = Image.makeFromBitmap(background)

                val srcRect = Rect.makeXYWH(0f, 0f, background.width.toFloat(), background.height.toFloat())
                val dstRect = Rect.makeXYWH(0f, 0f, size.width, size.height)

                onDrawBehind {
                    drawContext.canvas.nativeCanvas.drawImageRect(
                        image = backgroundImage,
                        src = srcRect,
                        dst = dstRect,
                        samplingMode = SamplingMode.DEFAULT, // FilterQuality.None
                        paint = imageDrawerPaint,
                        strict = true,
                    )

                    val barLeftEnd = Offset(0f, alpha * size.height)
                    val barRightEnd = Offset(size.width, alpha * size.height)

                    drawLine(BAR_BG_COLOR, barLeftEnd, barRightEnd, 5f)
                    drawLine(BAR_COLOR, barLeftEnd, barRightEnd, 3f)
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
