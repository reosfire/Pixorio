package ru.reosfire.pixorio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.zIndex
import org.jetbrains.skia.Bitmap
import ru.reosfire.pixorio.colorpalette.ColorsPalette
import ru.reosfire.pixorio.colorpicker.ColorPicker
import ru.reosfire.pixorio.extensions.compose.hsvHue
import ru.reosfire.pixorio.extensions.compose.toInt
import kotlin.math.roundToInt

@Composable
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
    val bitmap = remember { Bitmap().apply { allocN32Pixels(size.width, size.height) } }
    val nativeCanvas = remember { NativeCanvas(bitmap) }

    val composeBitmap = remember { bitmap.asComposeImageBitmap() }

    var scalingFactor by remember { mutableStateOf(10f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    var pressed by remember { mutableStateOf(false) }
    var lastPress by remember { mutableStateOf(Offset(0f, 0f)) }

    var currentColor by remember { mutableStateOf(Color.White) }

    var framesRendered by remember { mutableStateOf(0) }

    val paint = remember(currentColor) {
        Paint().apply {
            color = currentColor
            strokeWidth = 1f
            isAntiAlias = false
            filterQuality = FilterQuality.None
            strokeCap = StrokeCap.Square
            strokeJoin = StrokeJoin.Bevel
            shader = null
            blendMode = BlendMode.Src
        }.asFrameworkPaint()
    }

    val usedColors = remember { mutableSetOf<Color>() }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.background(Color.Gray.copy(alpha = 0.7f)).align(Alignment.Top).zIndex(2f)) {
            ColorPicker(
                onColorChanged = {
                    currentColor = it
                },
                modifier = Modifier.width((255 + 40).dp).height((255).dp)
            )

            ColorsPalette(
                usedColors.sortedBy { it.hsvHue },
                onColorSelect = { currentColor = it },
                modifier = Modifier.width((255 + 40).dp).height((255).dp),
            )
        }

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
                        currentColor = Color(bitmap.getColor(click.x, click.y))
                    } else {
                        val click = ((it.changes.first().position - offset) / scalingFactor)
                        if (click.x < 0 || click.y < 0 || click.x >= size.width || click.y >= size.height) return@onPointerEvent
                        nativeCanvas.drawPoint(click.x, click.y, paint)
                        usedColors.add(currentColor)
                        pressed = true
                        lastPress = click
                        framesRendered++
                    }
                }.onPointerEvent(PointerEventType.Move) {
                    if (pressed) {
                        val click = ((it.changes.first().position - offset) / scalingFactor)
                        if (click.x < 0 || click.y < 0 || click.x >= size.width || click.y >= size.height) return@onPointerEvent

                        nativeCanvas.drawLine(click.x, click.y, lastPress.x, lastPress.y, paint)

                        lastPress = click
                        framesRendered++
                    }
                }.onPointerEvent(PointerEventType.Release) {
                    pressed = false
                    framesRendered++
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                framesRendered // TODO this is still very wierd solution. Probably the best solution will be to create my own observable wrapper for bitmap/canvas. (Just like mutable state)
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
            val value = if ((x + y) % 2 == 0) 0.7f else 0.8f
            canvas.setColor(x, y, Color.hsv(0f, 0f, value))
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
