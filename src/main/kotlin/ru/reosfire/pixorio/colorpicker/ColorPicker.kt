package ru.reosfire.pixorio.colorpicker

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.jetbrains.skia.Bitmap
import ru.reosfire.pixorio.BitmapCanvas
import ru.reosfire.pixorio.contrast
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun ColorPicker(
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }

    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var saturationValue by remember { mutableStateOf(Offset.Zero) }

    var color by remember { mutableStateOf(Color.White) }

    fun updateColor() {
        color = Color.hsv(
            hue = hue,
            saturation = saturationValue.x,
            value = 1 - saturationValue.y,
            alpha
        )
        onColorChanged(color)
    }

    fun updateSaturationValue(position: Offset, size: IntSize) {
        saturationValue = Offset(
            (position.x / size.width).coerceIn(0f, 1f),
            (position.y / size.height).coerceIn(0f, 1f),
        )

        updateColor()
    }

    fun updatePosition(position: Offset, size: IntSize) {
        pointerPosition = Offset(position.x.coerceIn(0f, size.width.toFloat()), position.y.coerceIn(0f, size.height.toFloat()))
        updateSaturationValue(position, size)
    }

    var pressed by remember { mutableStateOf(false) }

    Layout(
        contents = listOf(
            {
                Layout(modifier = Modifier
                    .onPointerEvent(PointerEventType.Press) {
                        updatePosition(it.changes.first().position, size)

                        pressed = true
                    }.onPointerEvent(PointerEventType.Move) {
                        if (pressed) {
                            updatePosition(it.changes.first().position, size)
                        }
                    }.onPointerEvent(PointerEventType.Release) {
                        pressed = false
                    }.drawWithCache {
                        val bitmap = colorPickerBitmap(IntSize(size.width.toInt(), size.height.toInt()), hue)

                        onDrawBehind {
                            drawImage(bitmap.asComposeImageBitmap())
                            drawCircle(Color.White.copy(alpha = 0.5f), 5f, pointerPosition)
                        }
                    }
                ) {_, constraints ->
                    val minMaxSide = min(constraints.maxWidth, constraints.maxHeight)
                    layout(minMaxSide, minMaxSide) { }
                }
            }, {
                Row {
                    AlphaSelector(
                        alpha = alpha,
                        onHueChanged = {
                            alpha = it
                            updateColor()
                        },
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    HueSelector(
                        hue = hue,
                        onHueChanged = {
                            hue = it
                            updateColor()
                        },
                        modifier = Modifier.width(20.dp)
                    )
                }
            }, {
                BasicTextField(
                    value = color.toHexString(),
                    onValueChange = {  },
                    textStyle = TextStyle.Default.copy(color = color.contrast),
                    modifier = Modifier.background(color).fillMaxWidth()
                )
            }
        ),
        modifier = modifier,
        measurePolicy = { (svSelectorMeasurables, hueSelectorMeasurables, textMeasurables), constraints ->
            val svSelector = svSelectorMeasurables.requireOneElement()
            val hueSelector = hueSelectorMeasurables.requireOneElement()
            val text = textMeasurables.requireOneElement()

            val textPlaceable = text.measure(constraints.copy(minWidth = 0, minHeight = 0))

            val availableHeight = constraints.maxHeight - textPlaceable.height

            val hueSelectorPlaceable = hueSelector.measure(
                constraints.copy(
                    maxWidth = constraints.maxWidth,
                    minWidth = 0,
                    maxHeight = availableHeight,
                    minHeight = 0,
                ),
            )

            val svSelectorPlaceable = svSelector.measure(
                constraints.copy(
                    maxWidth = constraints.maxWidth - hueSelectorPlaceable.width,
                    minWidth = 0,
                    maxHeight = availableHeight,
                    minHeight = 0,
                )
            )

            layout(constraints.maxWidth, constraints.maxHeight) {
                svSelectorPlaceable.place(0, 0)
                hueSelectorPlaceable.place(constraints.maxWidth - hueSelectorPlaceable.width, 0)
                textPlaceable.place(0, max(svSelectorPlaceable.height, hueSelectorPlaceable.height))
            }
        }
    )
}

private fun colorPickerBitmap(size: IntSize, hue: Float): Bitmap {
    val canvas = BitmapCanvas(size)

    val floatSize = size.toSize()

    for (y in 0..<size.height) {
        for (x in 0..<size.width) {
            val currentColor = Color.hsv(hue, x / floatSize.width, 1f - (y / floatSize.height))

            canvas.setColor(x, y, currentColor)
        }
    }

    return canvas.createBitmap()
}

private fun Color.toHexString(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    val a = (alpha * 255).toInt()
    return "${r.toHex()}${g.toHex()}${b.toHex()}${a.toHex()}"
}

private fun Int.toHex(): String {
    return toString(16).padStart(2, '0')
}

private fun <T> List<T>.requireOneElement(): T {
    require(size == 1)
    return this[0]
}
