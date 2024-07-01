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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.zIndex
import org.jetbrains.skia.Bitmap
import kotlin.math.roundToInt


fun Offset.toInt() = IntOffset(x.toInt(), y.toInt())

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
    var updateTrigger by remember { mutableStateOf(true) }

    val size = remember { IntSize(32, 32) }

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

    var currentColor by remember { mutableStateOf(Color.White) }

    Row(Modifier.fillMaxSize()) {
        ColorPicker(
            onColorChanged = {
                currentColor = it
            },
            modifier = Modifier.align(Alignment.Top).zIndex(2f)
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
                        bitmap.installPixels(pixels)
                        pressed = true
                    }
                }.onPointerEvent(PointerEventType.Move) {
                    if (pressed) {
                        val click = ((it.changes.first().position - offset) / scalingFactor).toInt()
                        if (click.x < 0 || click.y < 0 || click.x >= size.width || click.y >= size.height) return@onPointerEvent
                        val baseIndex = (click.x + click.y * size.width) * 4
                        pixels[baseIndex] = (currentColor.blue * 255).toInt().toByte()
                        pixels[baseIndex + 1] = (currentColor.green * 255).toInt().toByte()
                        pixels[baseIndex + 2] = (currentColor.red * 255).toInt().toByte()
                        bitmap.installPixels(pixels)
                    }
                }.onPointerEvent(PointerEventType.Release) {
                    pressed = false
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                updateTrigger = !updateTrigger //TODO: this is the crappest solution possible. Because it triggers redraw every frame, which could be bypassed most of the time
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
