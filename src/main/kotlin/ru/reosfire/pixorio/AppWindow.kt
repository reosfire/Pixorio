package ru.reosfire.pixorio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skiko.toBufferedImage
import ru.reosfire.pixorio.colorpalette.ColorsPalette
import ru.reosfire.pixorio.colorpicker.ColorPicker
import ru.reosfire.pixorio.colorpicker.rememberColorPickerState
import ru.reosfire.pixorio.extensions.compose.hsvHue
import ru.reosfire.pixorio.extensions.compose.toInt
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

@Composable
fun ApplicationScope.AppWindow(
    bitmapSize: IntSize,
) {
    val coroutineScope = rememberCoroutineScope()

    val bitmap = remember { Bitmap().apply { allocN32Pixels(bitmapSize.width, bitmapSize.height) } }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        println(event.type.toString() + " " + event.key.toString())
        return false
    }

    var saveLocation by remember { mutableStateOf<File?>(null) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Pixorio",
        onKeyEvent = ::handleKeyEvent,
        state = rememberWindowState(WindowPlacement.Maximized),
    ) {
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item(
                    text = "Save",
                    onClick = {
                        coroutineScope.launch {
                            val lastSaveLocation = saveLocation
                            if (lastSaveLocation == null) {
                                val fileChosen = FileKit.pickFile(
                                    type = PickerType.Image,
                                    title = "Pixorio",
                                )?.file ?: return@launch

                                saveLocation = fileChosen
                                save(bitmap, fileChosen)
                            } else {
                                save(bitmap, lastSaveLocation)
                            }
                        }
                    },
                    shortcut = KeyShortcut(Key.S, ctrl = true)
                )
            }
        }
        App(bitmap)
    }
}

suspend fun save(bitmap: Bitmap, file: File) {
    val bufferedImage = bitmap.toBufferedImage()

    withContext(Dispatchers.IO) {
        ImageIO.write(bufferedImage, "png", file)
    }
}

@Composable
private fun App(bitmap: Bitmap) {
    MaterialTheme {
        PixelsPainter(bitmap)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PixelsPainter(
    bitmap: Bitmap,
) {
    val checkersBitmap = remember { createCheckeredBackground(IntSize(bitmap.width * 2, bitmap.height * 2)).asComposeImageBitmap() }
    val nativeCanvas = remember { NativeCanvas(bitmap) }

    val composeBitmap = remember { bitmap.asComposeImageBitmap() }

    var scalingFactor by remember { mutableStateOf(10f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    var pressed by remember { mutableStateOf(false) }
    var lastPress by remember { mutableStateOf(Offset(0f, 0f)) }

    val colorPickerState = rememberColorPickerState(Color.White)
    val currentColor by colorPickerState.colorState

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

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        Modifier
        .fillMaxSize()
    ) {
        Column(Modifier.background(Color.Gray.copy(alpha = 0.7f)).align(Alignment.Top).zIndex(2f)) {
            ColorPicker(
                state = colorPickerState,
                modifier = Modifier.width((255 + 40).dp).height((255).dp)
            )

            ColorsPalette(
                usedColors.sortedBy { it.hsvHue },
                onColorSelect = { colorPickerState.setColor(it) },
                modifier = Modifier.width((255 + 40).dp).height((255).dp),
            )
        }

        Canvas(
            Modifier
                .weight(1f)
                .align(Alignment.Top)
                .fillMaxSize()
                .pointerInput(Unit) {

                }
                .onPointerEvent(PointerEventType.Scroll) {
                    val dScale = it.changes.first().scrollDelta.y * scalingFactor * 0.1f
                    if (scalingFactor + dScale !in 0.2f..40f) return@onPointerEvent
                    val dSize = Offset(bitmap.width * dScale, bitmap.height * dScale)

                    val scrollPointInImageCoordinates = (it.changes.first().position - offset)
                    val relativeScrollPointCoords = Offset(scrollPointInImageCoordinates.x / (bitmap.width * scalingFactor), scrollPointInImageCoordinates.y / (bitmap.height * scalingFactor))

                    scalingFactor += dScale

                    offset -= Offset(dSize.x * relativeScrollPointCoords.x, dSize.y * relativeScrollPointCoords.y)
                    framesRendered++
                    focusRequester.requestFocus()
                }.onPointerEvent(PointerEventType.Press) {
                    if (it.button == PointerButton.Tertiary) {
                        val click = ((it.changes.first().position - offset) / scalingFactor).toInt()
                        if (click.x < 0 || click.y < 0 || click.x >= bitmap.width || click.y >= bitmap.height) return@onPointerEvent
                        colorPickerState.setColor(Color(bitmap.getColor(click.x, click.y)))
                    } else {
                        val click = ((it.changes.first().position - offset) / scalingFactor)
                        if (click.x < 0 || click.y < 0 || click.x >= bitmap.width || click.y >= bitmap.height) return@onPointerEvent
                        nativeCanvas.drawPoint(click.x, click.y, paint)

                        usedColors.add(currentColor)
                        pressed = true
                        lastPress = click
                        framesRendered++
                    }
                    focusRequester.requestFocus()
                }.onPointerEvent(PointerEventType.Move) {
                    if (pressed) {
                        val click = ((it.changes.first().position - offset) / scalingFactor)
                        if (click.x < 0 || click.y < 0 || click.x >= bitmap.width || click.y >= bitmap.height) return@onPointerEvent

                        nativeCanvas.drawLine(click.x, click.y, lastPress.x, lastPress.y, paint)

                        lastPress = click
                        framesRendered++
                        focusRequester.requestFocus()
                    }
                }.onPointerEvent(PointerEventType.Release) {
                    pressed = false
                    framesRendered++
                }
                .onKeyEvent { event ->
                    if (event.key == Key.Z && event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                        println("undo stub")

                        return@onKeyEvent true
                    }

                    false
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
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
