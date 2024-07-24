package ru.reosfire.pixorio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Rect
import ru.reosfire.pixorio.brushes.AbstractBrush
import ru.reosfire.pixorio.brushes.PaintingTransaction
import ru.reosfire.pixorio.brushes.library.Fill
import ru.reosfire.pixorio.brushes.library.Pencil
import ru.reosfire.pixorio.extensions.compose.contains
import ru.reosfire.pixorio.extensions.compose.hsvHue
import ru.reosfire.pixorio.extensions.compose.times
import ru.reosfire.pixorio.extensions.compose.toInt
import ru.reosfire.pixorio.shaders.CheckeredShaderBrush
import ru.reosfire.pixorio.ui.components.brushes.palette.BrushUiData
import ru.reosfire.pixorio.ui.components.brushes.palette.BrushesPalette
import ru.reosfire.pixorio.ui.components.colorpalette.ColorsPalette
import ru.reosfire.pixorio.ui.components.colorpicker.ColorPicker
import ru.reosfire.pixorio.ui.components.colorpicker.rememberColorPickerState
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

@Composable
fun ApplicationScope.AppWindow(
    bitmapSize: IntSize,
    onCloseRequest: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val editableImage = remember { BasicEditableImage(bitmapSize) }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        println(event.type.toString() + " " + event.key.toString())
        return false
    }

    var saveLocation by remember { mutableStateOf<File?>(null) }

    Window(
        onCloseRequest = onCloseRequest,
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
                                save(editableImage, fileChosen)
                            } else {
                                save(editableImage, lastSaveLocation)
                            }
                        }
                    },
                    shortcut = KeyShortcut(Key.S, ctrl = true)
                )
            }
        }
        App(editableImage)
    }
}

suspend fun save(editableImage: EditableImage, file: File) {
    val bufferedImage = editableImage.toBufferedImage()

    withContext(Dispatchers.IO) {
        ImageIO.write(bufferedImage, "png", file)
    }
}

@Composable
private fun App(editableImage: EditableImage) {
    MaterialTheme {
        PixelsPainter(editableImage)
    }
}

class EditorContext(
    val editableImage: EditableImage,
    val scalingFactorState: MutableFloatState = mutableFloatStateOf(10f),
    val offsetState: MutableState<Offset> = mutableStateOf(Offset.Zero),
) {
    var scalingFactor by scalingFactorState
    var offset by offsetState

    fun Offset.toLocalCoordinates(): Offset {
        return (this - offset) / scalingFactor
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PixelsPainter(
    editableImage: EditableImage,
) {
    val previewEditableImage = remember { BasicEditableImage(editableImage.size).also { it.loadFrom(editableImage) } }

    val editorContext = remember { EditorContext(editableImage) }

    var framesRendered by remember { mutableStateOf(0) }

    val colorPickerState = rememberColorPickerState(Color.White)
    val currentColor by colorPickerState.colorState

    var brushFactory by remember { mutableStateOf<(Color) -> AbstractBrush>({ Pencil(it) }) }

    val currentBrush by remember(currentColor, brushFactory) { mutableStateOf(brushFactory(currentColor)) }

    val usedColors = remember { mutableStateListOf<Color>() }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val transactionsQueue = remember { ArrayDeque<PaintingTransaction>() }
    val redoQueue = remember { ArrayDeque<PaintingTransaction>() }

    fun updateImage() {
        previewEditableImage.loadFrom(editableImage)
    }

    val checkeredShaderBrush = remember { CheckeredShaderBrush() }

    Row(
        Modifier
            .fillMaxSize()
    ) {
        Column(Modifier.fillMaxHeight().border(1.dp, Color.White).background(Color.Gray.copy(alpha = 0.7f)).align(Alignment.Top).zIndex(2f)) {
            ColorPicker(
                state = colorPickerState,
                modifier = Modifier.width((255 + 40).dp).height((255).dp).border(1.dp, Color.White),
            )

            ColorsPalette(
                usedColors.sortedBy { it.hsvHue },
                onColorSelect = { colorPickerState.setColor(it) },
                modifier = Modifier.width((255 + 40).dp).height((255).dp).border(1.dp, Color.White),
            )

            BrushesPalette(
                brushes = listOf(
                    BrushUiData(
                        name = "Pencil",
                        iconResource = "icons/brushes/pencil.png",
                        factorize = ::Pencil
                    ),
                    BrushUiData(
                        name = "Fill",
                        iconResource = "icons/brushes/bucket.png",
                        factorize = ::Fill
                    )
                ),
                onBrushSelect = {
                    brushFactory = it.factorize
                },
                modifier = Modifier.width((255 + 40).dp).height((255).dp).border(1.dp, Color.White),
            )
        }

        Spacer(
            Modifier
                .weight(1f)
                .align(Alignment.Top)
                .fillMaxSize()
                .pointerInput(currentBrush) {
                    with(currentBrush) {
                        setTransactionListener {
                            it.apply(editableImage)

                            updateImage()

                            transactionsQueue.add(it)
                            redoQueue.clear()

                            framesRendered++
                            if (currentColor !in usedColors) usedColors.add(currentColor)
                        }

                        setPreviewChangeListener {
                            updateImage()

                            it.preview(previewEditableImage)
                            framesRendered++
                        }

                        inputEventsHandler(editorContext)
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    val dScale = it.changes.first().scrollDelta.y * editorContext.scalingFactor * 0.1f
                    if (editorContext.scalingFactor + dScale !in 0.2f..40f) return@onPointerEvent
                    val dSize = editableImage.size * dScale

                    val scrollPointInImageCoordinates = (it.changes.first().position - editorContext.offset)
                    val relativeScrollPointCoords = Offset(
                        scrollPointInImageCoordinates.x / (editableImage.width * editorContext.scalingFactor),
                        scrollPointInImageCoordinates.y / (editableImage.height * editorContext.scalingFactor)
                    )

                    editorContext.scalingFactor += dScale

                    editorContext.offset = Offset(
                        x = editorContext.offset.x - dSize.width * relativeScrollPointCoords.x,
                        y = editorContext.offset.y - dSize.height * relativeScrollPointCoords.y,
                    )
                    framesRendered++
                    focusRequester.requestFocus()
                }.onPointerEvent(PointerEventType.Press) {
                    if (it.button == PointerButton.Tertiary) {
                        val click = ((it.changes.first().position - editorContext.offset) / editorContext.scalingFactor).toInt()
                        if (click !in editableImage.size) return@onPointerEvent
                        colorPickerState.setColor(Color(editableImage.getColor(click.x, click.y)))
                    }
                    focusRequester.requestFocus()
                }
                .onKeyEvent { event ->
                    if (event.key == Key.Z && event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                        if (transactionsQueue.isNotEmpty()) {
                            val lastTransaction = transactionsQueue.removeLast()

                            lastTransaction.revert(editableImage)
                            updateImage()

                            redoQueue.add(lastTransaction)

                            framesRendered++
                            return@onKeyEvent true
                        }
                    }
                    if (event.key == Key.Y && event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                        if (redoQueue.isNotEmpty()) {
                            val lastTransaction = redoQueue.removeLast()

                            lastTransaction.apply(editableImage)
                            updateImage()

                            transactionsQueue.add(lastTransaction)

                            framesRendered++
                            return@onKeyEvent true
                        }
                    }

                    false
                }
                .focusRequester(focusRequester)
                .focusable()
                .drawWithCache {
                    framesRendered // TODO this is still very wierd solution. Probably the best solution will be to create my own observable wrapper for bitmap/canvas. (Just like mutable state)

                    val resultSize = editableImage.size * editorContext.scalingFactor
                    val offset = editorContext.offset

                    val dstRect = Rect.makeXYWH(offset.x, offset.y, resultSize.width, resultSize.height)

                    checkeredShaderBrush.setUniforms(
                        squareSize = max(2f, editorContext.scalingFactor / 2),
                        offset = offset
                    )

                    onDrawBehind {
                        drawRect(
                            brush = checkeredShaderBrush,
                            topLeft = offset,
                            size = resultSize
                        )

                        previewEditableImage.render(this, dstRect)
                    }
                }
        )
    }
}
