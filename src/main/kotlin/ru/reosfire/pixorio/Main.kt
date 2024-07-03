package ru.reosfire.pixorio

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.zIndex
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import ru.reosfire.pixorio.colorpicker.ColorPicker
import kotlin.math.roundToInt
import kotlin.math.sqrt


fun Offset.toInt() = IntOffset(x.toInt(), y.toInt())
val Offset.norm: Float
    get() = sqrt(x * x + y * y)
fun Offset.normalized() = Offset(x / norm, y / norm)
fun IntOffset.distance(other: IntOffset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt((dx * dx + dy * dy).toFloat())
}

// TODO rewrite to normal algorithm
fun lineBetween(start: IntOffset, end: IntOffset): List<IntOffset> {
    if (start == end) return listOf(start)
    val n = 1000
    val step = (end - start).toOffset().normalized() * (start.distance(end) / n)
    var currentPosition = start.toOffset()

    val result = mutableListOf<IntOffset>()
    repeat(n) {
        currentPosition += step

        result.add(currentPosition.round())
    }
    return result
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        PixelsPainter()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PixelsPainter() {
    val size = remember { IntSize(128, 128) }

    val checkersBitmap = remember { createCheckeredBackground(IntSize(size.width * 2, size.height * 2)).asComposeImageBitmap() }

    val pixels = remember {
        ByteArray(size.height * size.width * 4) {
            when {
                // Blue
                it % 4 == 0 -> 0
                //Green
                it % 4 == 1 -> 0
                // Red
                it % 4 == 2 -> 0
                //Alpha
                else -> 255
            }.toByte()
        }
    }

    val bitmap = remember {
        Bitmap().apply {
            allocN32Pixels(size.width, size.height)
            installPixels(pixels)
        }
    }

    val composeBitmap = remember { bitmap.asComposeImageBitmap() }

    var scalingFactor by remember { mutableStateOf(10f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    var pressed by remember { mutableStateOf(false) }
    var lastPress by remember { mutableStateOf(IntOffset(0, 0)) }

    var currentColor by remember { mutableStateOf(Color.White) }

    var framesRendered by remember { mutableStateOf(0) }

    Row(Modifier.fillMaxSize()) {
        ColorPicker(
            onColorChanged = {
                currentColor = it
            },
            modifier = Modifier.width((255 + 4 + 20).dp).height((255 + 10).dp).align(Alignment.Top).zIndex(2f)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top)
                .onPointerEvent(PointerEventType.Scroll) {
                    val dScale = it.changes.first().scrollDelta.y / 2f
                    if (scalingFactor + dScale !in 0.1f..30f) return@onPointerEvent
                    val dSize = Offset(size.width * dScale, size.height * dScale)

                    val scrollPointInImageCoordinates = (it.changes.first().position - offset)
                    val relativeScrollPointCoords = Offset(scrollPointInImageCoordinates.x / (size.width * scalingFactor), scrollPointInImageCoordinates.y / (size.width * scalingFactor))

                    scalingFactor += dScale

                    offset -= Offset(dSize.x * relativeScrollPointCoords.x, dSize.y * relativeScrollPointCoords.y)
                    framesRendered++
                }.onPointerEvent(PointerEventType.Press) {
                    if (it.button == PointerButton.Tertiary) {
                        val click = ((it.changes.first().position - offset) / scalingFactor).toInt()
                        if (click.x < 0 || click.y < 0 || click.x >= size.width || click.y >= size.height) return@onPointerEvent
                        val baseIndex = (click.x + click.y * size.width) * 4
                        currentColor = Color(
                            pixels[baseIndex + 2].toUByte().toFloat() / 255f,
                            pixels[baseIndex + 1].toUByte().toFloat() / 255f,
                            pixels[baseIndex].toUByte().toFloat() / 255f,
                            pixels[baseIndex + 3].toUByte().toFloat() / 255f,
                        )
                    } else {
                        val click = ((it.changes.first().position - offset) / scalingFactor).toInt()
                        if (click.x < 0 || click.y < 0 || click.x >= size.width || click.y >= size.height) return@onPointerEvent
                        val baseIndex = (click.x + click.y * size.width) * 4
                        pixels[baseIndex] = (currentColor.blue * 255).toInt().toByte()
                        pixels[baseIndex + 1] = (currentColor.green * 255).toInt().toByte()
                        pixels[baseIndex + 2] = (currentColor.red * 255).toInt().toByte()
                        pixels[baseIndex + 3] = (currentColor.alpha * 255).toInt().toByte()
                        bitmap.installPixels(pixels)
                        pressed = true
                        lastPress = click
                        framesRendered++
                    }
                }.onPointerEvent(PointerEventType.Move) {
                    if (pressed) {
                        val click = ((it.changes.first().position - offset) / scalingFactor).toInt()
                        if (click.x < 0 || click.y < 0 || click.x >= size.width || click.y >= size.height) return@onPointerEvent

                        for (point in lineBetween(click, lastPress)) {
                            val baseIndex = (point.x + point.y * size.width) * 4
                            pixels[baseIndex] = (currentColor.blue * 255).toInt().toByte()
                            pixels[baseIndex + 1] = (currentColor.green * 255).toInt().toByte()
                            pixels[baseIndex + 2] = (currentColor.red * 255).toInt().toByte()
                            pixels[baseIndex + 3] = (currentColor.alpha * 255).toInt().toByte()
                        }
                        bitmap.installPixels(pixels)

                        lastPress = click
                        framesRendered++
                    }
                }.onPointerEvent(PointerEventType.Release) {
                    pressed = false
                    framesRendered++
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                framesRendered // TODO this is still very wierd solution

                drawImage(
                    checkersBitmap,
                    dstSize = IntSize((bitmap.width * scalingFactor).roundToInt(), (bitmap.height * scalingFactor).roundToInt()),
                    dstOffset = offset.round(),
                    blendMode = BlendMode.Src,
                    filterQuality = FilterQuality.None,
                )
                drawImage(
                    composeBitmap,
                    dstSize = IntSize((bitmap.width * scalingFactor).roundToInt(), (bitmap.height * scalingFactor).roundToInt()),
                    dstOffset = offset.round(),
                    blendMode = BlendMode.SrcOver,
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }
}

private fun createCheckeredBackground(
    size: IntSize
): Bitmap {
    val canvas = BitmapCanvas(size)

    for (y in 0 until size.height) {
        for (x in 0 until size.width) {
            val baseColor = if ((x + y) % 2 == 0) Color.LightGray else Color.DarkGray
            canvas.setColor(x, y, baseColor)
        }
    }

    return canvas.createBitmap()
}

fun handleKeyEvent(event: KeyEvent): Boolean {
    println(event.type.toString() + " " + event.key.toString())
    return false
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Pixorio",
        onKeyEvent = ::handleKeyEvent,
    ) {
        App()
    }
}
