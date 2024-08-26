package ru.reosfire.pixorio.app.ui.components.colorpalette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.reosfire.pixorio.app.shaders.CheckeredShaderBrush
import kotlin.math.min

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

@Composable
private fun ColorItem(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkeredShaderBrush = remember { CheckeredShaderBrush() }

    Spacer(
        modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .drawWithCache {
                val innerRectSize = Size(size.width - 2, size.height - 2)

                checkeredShaderBrush.setUniforms(
                    squareSize = min(size.height / CHECKS_COUNT, size.width / CHECKS_COUNT),
                    offset = Offset.Zero,
                )

                onDrawBehind {
                    drawRoundRect(
                        brush = checkeredShaderBrush,
                        cornerRadius = colorItemCornerRadius,
                        topLeft = Offset.Zero,
                        size = size
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

private const val CHECKS_COUNT = 3

private val innerRectOffset = Offset(1f, 1f)
private val colorItemCornerRadius = CornerRadius(4f)
