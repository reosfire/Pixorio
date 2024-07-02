package ru.reosfire.pixorio

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.jetbrains.skia.Bitmap

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun ColorPicker(
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturationValue by remember { mutableStateOf(Offset.Zero) }

    val bitmap = colorPickerBitmap(IntSize(256, 256), hue)

    var color by remember { mutableStateOf(Color.White) }

    fun updateColor(x: Float, y: Float) {
        color = Color.hsv(hue, x.coerceIn(0f, 255f) / 255f, 1 - y.coerceIn(0f, 255f) / 255f)
        onColorChanged(color)
    }

    var pressed by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .background(color)
        .width(255.dp + 20.dp)
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(255.dp)
                .height(255.dp)
                .onPointerEvent(PointerEventType.Press) {
                    updateColor(it.changes.first().position.x, it.changes.first().position.y)
                    saturationValue = it.changes.first().position
                    pressed = true
                }.onPointerEvent(PointerEventType.Move) {
                    if (pressed) {
                        updateColor(it.changes.first().position.x, it.changes.first().position.y)
                        saturationValue = it.changes.first().position
                    }
                }.onPointerEvent(PointerEventType.Release) {
                    pressed = false
                }
        ) {
            drawImage(bitmap.asComposeImageBitmap())
            drawCircle(Color.White.copy(alpha = 0.5f), 5f, saturationValue)
        }
        HueSelector(
            hue = hue,
            onHueChanged = {
                hue = it
            },
            modifier = Modifier.width(20.dp).height(360.dp).align(Alignment.TopEnd),
        )

        TextField(color.toHexString(), onValueChange = {  }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
private fun HueSelector(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val composeBitmap = remember { hueSelectorBitmap(IntSize(20, 360)) }

    var pressed by remember { mutableStateOf(false) }

    fun updateHue(y: Float) {
        onHueChanged(y.coerceIn(0f, 360f))
    }

    Layout(
        modifier = modifier
            .onPointerEvent(PointerEventType.Press) {
                updateHue(it.changes.first().position.y)
                pressed = true
            }.onPointerEvent(PointerEventType.Move) {
                if (pressed) {
                    updateHue(it.changes.first().position.y)
                }
            }.onPointerEvent(PointerEventType.Release) {
                pressed = false
            }.drawBehind {
                drawImage(
                    image = composeBitmap,
                    filterQuality = FilterQuality.None,
                )

                drawLine(Color.Black.copy(alpha = 0.8f), Offset(0f, hue), Offset(20f, hue), 5f)
                drawLine(Color.hsv(hue, 1f, 1f), Offset(0f, hue), Offset(20f, hue), 3f)
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun colorPickerBitmap(size: IntSize, hue: Float): Bitmap {
    val floatSize = size.toSize()

    val pixels = ByteArray(size.height * size.width * 4)

    for (y in 0..<size.height) {
        for (x in 0..<size.width) {
            val currentColor = Color.hsv(hue, x / floatSize.width, 1f - (y / floatSize.height))

            val baseIndex = (x + y * size.width) * 4
            pixels[baseIndex] = (currentColor.blue * 255).toInt().toByte() // blue
            pixels[baseIndex + 1] = (currentColor.green * 255).toInt().toByte() // green
            pixels[baseIndex + 2] = (currentColor.red * 255).toInt().toByte() // red
            pixels[baseIndex + 3] = (currentColor.alpha * 255).toInt().toByte() // alpha
        }
    }

    return Bitmap().apply {
        allocN32Pixels(size.width, size.height)
        installPixels(pixels)
    }
}

private fun hueSelectorBitmap(size: IntSize): ImageBitmap {
    val pixels = ByteArray(size.height * size.width * 4)

    for (y in 0..<size.height) {
        val hue = y.toFloat()
        for (x in 0..<size.width) {
            val currentColor = Color.hsv(hue, 1f, 1f)

            val baseIndex = (x + y * size.width) * 4
            pixels[baseIndex] = (currentColor.blue * 255).toInt().toByte() // blue
            pixels[baseIndex + 1] = (currentColor.green * 255).toInt().toByte() // green
            pixels[baseIndex + 2] = (currentColor.red * 255).toInt().toByte() // red
            pixels[baseIndex + 3] = (currentColor.alpha * 255).toInt().toByte() // alpha
        }
    }

    return Bitmap().apply {
        allocN32Pixels(size.width, size.height)
        installPixels(pixels)
    }.asComposeImageBitmap()
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
