package ru.reosfire.pixorio.colorpalette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import ru.reosfire.pixorio.BitmapCanvas
import ru.reosfire.pixorio.extensions.compose.contrastColor
import ru.reosfire.pixorio.extensions.compose.toInt

@Composable
fun ColorsPalette(
    colors: List<Color>,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.FixedSize(size = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(10.dp)
    ) {
        items(colors.size) { index ->
            val color = colors[index]
            ColorItem(
                color = color,
                onClick = { onColorSelect(color) },
            )
        }
    }
}

private val innerRectOffset = Offset(1f, 1f)
private val colorItemCornerRadius = CornerRadius(4f)

@Composable
private fun ColorItem(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = color.contrastColor

    Spacer(
        modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .drawWithCache {
                val innerRectSize = Size(size.width - 2, size.height - 2)

                val bg = createCheckeredBackground(IntSize((size.width / 5).toInt(), (size.height / 5).toInt())).asComposeImageBitmap()

                onDrawBehind {
                    drawImage(
                        bg,
                        dstSize = size.toInt(),
                        filterQuality = FilterQuality.None,
                    )
                    drawRoundRect(
                        color = color,
                        cornerRadius = colorItemCornerRadius,
                        topLeft = innerRectOffset,
                        size = innerRectSize,
                    )
                }
            }
    )
}

private fun createCheckeredBackground(
    size: IntSize
): Bitmap {
    val canvas = BitmapCanvas(size)

    val colorA = Color.hsv(0f, 0f, 0.7f)
    val colorB = Color.hsv(0f, 0f, 0.3f)

    for (y in 0 until size.height) {
        for (x in 0 until size.width) {
            val baseColor = if ((x + y) % 2 == 0) colorA else colorB
            canvas.setColor(x, y, baseColor)
        }
    }

    return canvas.createBitmap()
}