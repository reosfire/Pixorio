package ru.reosfire.pixorio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.*
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import ru.reosfire.pixorio.app.brushes.PaintingTransaction
import ru.reosfire.pixorio.app.brushes.library.Fill
import ru.reosfire.pixorio.app.brushes.library.ImageBrush
import ru.reosfire.pixorio.app.brushes.library.Pencil
import ru.reosfire.pixorio.app.extensions.compose.*
import ru.reosfire.pixorio.app.filepicker.FilePickerDialog
import ru.reosfire.pixorio.app.shaders.CheckeredShaderBrush
import ru.reosfire.pixorio.app.ui.components.PastePalette
import ru.reosfire.pixorio.app.ui.components.brushes.palette.BrushUiData
import ru.reosfire.pixorio.app.ui.components.brushes.palette.BrushesPalette
import ru.reosfire.pixorio.app.ui.components.colorpalette.ColorsPalette
import ru.reosfire.pixorio.app.ui.components.colorpicker.ColorPicker
import ru.reosfire.pixorio.app.ui.components.colorpicker.rememberColorPickerState
import ru.reosfire.pixorio.designsystem.theme.MainTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

class FilePickerState {
    var shown by mutableStateOf(false)

    private var cachedSaveLocation by mutableStateOf<File?>(null)
    private var lastContinuation: CancellableContinuation<File>? = null

    suspend fun getSaveFile(): File {
        return cachedSaveLocation ?: getSaveFileUncached().also { cachedSaveLocation = it }
    }

    fun resume(file: File) {
        lastContinuation?.resume(file)
        shown = false
        lastContinuation = null
    }

    private suspend fun getSaveFileUncached() = suspendCancellableCoroutine { continuation ->
        cachedSaveLocation = null
        shown = true
        lastContinuation = continuation

        continuation.invokeOnCancellation { shown = false }
    }
}

@Composable
fun rememberFilePickerState(): FilePickerState {
    return remember { FilePickerState() }
}

@Composable
fun SaveFilePicker(state: FilePickerState) {
    if (state.shown) {
        FilePickerDialog(onCancelled = { state.shown = false }, onSelected = { state.resume(it) })
    }
}

@Composable
fun ApplicationScope.AppWindow(
    bitmapSize: IntSize,
    onCloseRequest: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val editableImage = remember { BasicEditableImage(bitmapSize) }

    val filePickerState = rememberFilePickerState()

    Window(
        onCloseRequest = onCloseRequest,
        title = APP_NAME,
        state = rememberWindowState(WindowPlacement.Maximized),
    ) {
        SaveFilePicker(filePickerState)

        MainTheme {
            MenuBar {
                Menu("File", mnemonic = 'F') {
                    Item(
                        text = "Save",
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                save(editableImage, filePickerState.getSaveFile())
                            }
                        },
                        shortcut = KeyShortcut(Key.S, ctrl = true)
                    )
                }
            }

            PixelsPainter(editableImage)
        }
    }
}

suspend fun save(editableImage: EditableImage, file: File) {
    assert(!file.isFile) { "File must be just file not a directory or something else" }

    val bufferedImage = editableImage.toBufferedImage()

    val writerFormatNames = ImageIO.getWriterFormatNames()
    val extension = file.extension.takeUnless { it !in writerFormatNames } ?: "png" // empty extension is also checked here

    val extendedFile = File("${file.parent}${File.separator}${file.nameWithoutExtension}.$extension")
    withContext(Dispatchers.IO) {
        ImageIO.write(bufferedImage, extension, extendedFile)
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

private class TriggerState {
    private var state by mutableIntStateOf(0)

    fun pull() = state++
    fun subscribe() = state
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PixelsPainter(
    editableImage: EditableImage,
) {
    val previewEditableImage = remember { BasicEditableImage(editableImage.size).also { it.loadFrom(editableImage) } }

    val editorContext = remember { EditorContext(editableImage) }

    val redrawTrigger = remember { TriggerState() }

    val colorPickerState = rememberColorPickerState(Color.White)
    val currentColor by colorPickerState.colorState

    val brushesList = remember {
        listOf(
            BrushUiData(
                name = "Pencil",
                iconResource = "icons/brushes/pencil.png",
                brush = Pencil(currentColor)
            ),
            BrushUiData(
                name = "Fill",
                iconResource = "icons/brushes/bucket.png",
                brush = Fill(currentColor)
            ),
        )
    }

    var currentBrush by remember { mutableStateOf(brushesList.first().brush) }

    LaunchedEffect(currentColor, currentBrush) {
        currentBrush.setColor(currentColor)
    }

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

    val clips = remember { mutableStateListOf<ImageBrush>() }

    val checkeredShaderBrush = remember { CheckeredShaderBrush() }

    var currentCursorPosition by remember { mutableStateOf(IntOffset.Zero) }
    var clipStart by remember { mutableStateOf<IntOffset?>(null) }

    fun getClipRect() = clipStart?.let {
        val minX = min(it.x, currentCursorPosition.x)
        val maxX = max(it.x, currentCursorPosition.x)
        val minY = min(it.y, currentCursorPosition.y)
        val maxY = max(it.y, currentCursorPosition.y)

        IRect.makeLTRB(minX, minY, maxX + 1, maxY + 1)
    }

    val selectionPaint = remember { Paint().apply { color = Color.Blue.copy(alpha = 0.4f).toArgb() } }

    val borderColor = MaterialTheme.colors.onBackground

    LaunchedEffect(currentBrush) {
        currentBrush.applyTransactionsFlow.collect {
            it.apply(editableImage)

            updateImage()

            transactionsQueue.add(it)
            redoQueue.clear()

            redrawTrigger.pull()
            if (currentColor !in usedColors) usedColors.add(currentColor)
        }
    }

    LaunchedEffect(currentBrush) {
        currentBrush.previewTransactionsFlow.collect {
            updateImage()

            it.preview(previewEditableImage)
            redrawTrigger.pull()
        }
    }
    Box() {
        Row(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .onKeyEvent { event ->
                    if (event.key == Key.Z && event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                        if (transactionsQueue.isNotEmpty()) {
                            val lastTransaction = transactionsQueue.removeLast()

                            lastTransaction.revert(editableImage)
                            updateImage()

                            redoQueue.add(lastTransaction)

                            redrawTrigger.pull()
                            return@onKeyEvent true
                        }
                    }
                    if (event.key == Key.Y && event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                        if (redoQueue.isNotEmpty()) {
                            val lastTransaction = redoQueue.removeLast()

                            lastTransaction.apply(editableImage)
                            updateImage()

                            transactionsQueue.add(lastTransaction)

                            redrawTrigger.pull()
                            return@onKeyEvent true
                        }
                    }

                    if (event.key == Key.F && event.type == KeyEventType.KeyDown) {
                        if (clipStart == null && currentCursorPosition in editableImage.size) {
                            focusRequester.requestFocus()
                            clipStart = currentCursorPosition
                        }
                    }

                    if (event.key == Key.F && event.type == KeyEventType.KeyUp) {
                        if (currentCursorPosition in editableImage.size) {
                            getClipRect()?.let { clipRect ->
                                editableImage.makeSnapshot(clipRect)?.let { rectSnapshot ->
                                    clips.add(ImageBrush(rectSnapshot))
                                }
                            }
                        }

                        clipStart = null
                    }

                    false
                }
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .border(1.dp, Color.White)
                    .background(Color.Gray.copy(alpha = 0.7f))
                    .align(Alignment.Top)
                    .zIndex(2f)
            ) {
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
                    brushes = brushesList,
                    onBrushSelect = {
                        currentBrush = it.brush
                    },
                    selectedBrush = currentBrush,
                    modifier = Modifier.width((255 + 40).dp).height((255).dp).border(1.dp, Color.White),
                )

                PastePalette(
                    clips,
                    onSelect = { currentBrush = it },
                    selectedBrush = currentBrush,
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
                        redrawTrigger.pull()
                        focusRequester.requestFocus()
                    }.onPointerEvent(PointerEventType.Press) {
                        if (it.button == PointerButton.Tertiary) {
                            val click = with(editorContext) { it.changes.first().position.toLocalCoordinates() }.toInt()
                            if (click !in editableImage.size) return@onPointerEvent
                            colorPickerState.setColor(editableImage.getComposeColor(click.x, click.y))
                        }
                        focusRequester.requestFocus()
                    }.onPointerEvent(PointerEventType.Move) {
                        currentCursorPosition =
                            with(editorContext) { it.changes.first().position.toLocalCoordinates() }.toInt()
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    .drawWithCache {
                        val resultSize = editableImage.size * editorContext.scalingFactor
                        val offset = editorContext.offset

                        val dstRect = Rect.makeXYWH(offset.x, offset.y, resultSize.width, resultSize.height)

                        checkeredShaderBrush.setUniforms(
                            squareSize = max(2f, editorContext.scalingFactor / 2),
                            offset = offset,
                        )

                        onDrawBehind {
                            redrawTrigger.subscribe()

                            drawRect(
                                color = borderColor,
                                topLeft = Offset(offset.x - BORDER_SIZE, offset.y - BORDER_SIZE),
                                size = Size(resultSize.width + BORDER_SIZE * 2, resultSize.height + BORDER_SIZE * 2),
                            )

                            drawRect(
                                brush = checkeredShaderBrush,
                                topLeft = offset,
                                size = resultSize
                            )

                            previewEditableImage.render(this, dstRect)

                            getClipRect()?.toRect()?.let { clipRect ->
                                useNativeCanvas {
                                    translate(offset.x, offset.y)
                                    scale(editorContext.scalingFactor, editorContext.scalingFactor)
                                    drawRect(clipRect, selectionPaint)
                                }
                            }
                        }
                    }
            )
        }
        Text(
            text = "Cursor: $currentCursorPosition",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color.DarkGray.copy(alpha = 0.8f))
                .padding(10.dp),
            color = Color.White
        )
    }
}

private const val BORDER_SIZE = 1
