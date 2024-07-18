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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.BlendMode
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
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.*
import org.jetbrains.skiko.toBufferedImage
import ru.reosfire.pixorio.brushes.AbstractBrush
import ru.reosfire.pixorio.brushes.PaintingTransaction
import ru.reosfire.pixorio.brushes.library.Fill
import ru.reosfire.pixorio.brushes.library.Pencil
import ru.reosfire.pixorio.extensions.compose.hsvHue
import ru.reosfire.pixorio.extensions.compose.toInt
import ru.reosfire.pixorio.ui.components.brushes.palette.BrushUiData
import ru.reosfire.pixorio.ui.components.brushes.palette.BrushesPalette
import ru.reosfire.pixorio.ui.components.colorpalette.ColorsPalette
import ru.reosfire.pixorio.ui.components.colorpicker.ColorPicker
import ru.reosfire.pixorio.ui.components.colorpicker.rememberColorPickerState
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

@Composable
fun ApplicationScope.AppWindow(
    bitmapSize: IntSize,
    onCloseRequest: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val bitmap = remember { Bitmap().apply { allocPixels(ImageInfo.makeN32(bitmapSize.width, bitmapSize.height, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)) } }

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

class EditorContext(
    val bitmap: Bitmap,
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
    bitmap: Bitmap,
) {
    val nativeCanvas = remember { NativeCanvas(bitmap) }

    val previewBitmap = remember { Bitmap().apply { allocPixels(ImageInfo.makeN32(bitmap.width, bitmap.height, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)) } }
    val previewNativeCanvas = remember { NativeCanvas(previewBitmap) }

    val editorContext = remember { EditorContext(bitmap) }

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

    var currentImage by remember { mutableStateOf(Image.makeFromBitmap(bitmap)) }

    fun updateImage() {
        currentImage.close()
        currentImage = Image.makeFromBitmap(bitmap)
        currentImage.readPixels(previewBitmap)
    }

    val imageDrawerPaint = remember {
        Paint().apply {
            blendMode = BlendMode.SrcOver
        }.asFrameworkPaint()
    }

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
                            it.apply(bitmap, nativeCanvas)

                            updateImage()

                            transactionsQueue.add(it)
                            redoQueue.clear()

                            framesRendered++
                            if (currentColor !in usedColors) usedColors.add(currentColor)
                        }

                        setPreviewChangeListener {
                            currentImage.readPixels(previewBitmap)

                            it.preview(previewBitmap, previewNativeCanvas)
                            framesRendered++
                        }

                        inputEventsHandler(editorContext)
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    val dScale = it.changes.first().scrollDelta.y * editorContext.scalingFactor * 0.1f
                    if (editorContext.scalingFactor + dScale !in 0.2f..40f) return@onPointerEvent
                    val dSize = Offset(bitmap.width * dScale, bitmap.height * dScale)

                    val scrollPointInImageCoordinates = (it.changes.first().position - editorContext.offset)
                    val relativeScrollPointCoords = Offset(scrollPointInImageCoordinates.x / (bitmap.width * editorContext.scalingFactor), scrollPointInImageCoordinates.y / (bitmap.height * editorContext.scalingFactor))

                    editorContext.scalingFactor += dScale

                    editorContext.offset -= Offset(dSize.x * relativeScrollPointCoords.x, dSize.y * relativeScrollPointCoords.y)
                    framesRendered++
                    focusRequester.requestFocus()
                }.onPointerEvent(PointerEventType.Press) {
                    if (it.button == PointerButton.Tertiary) {
                        val click = ((it.changes.first().position - editorContext.offset) / editorContext.scalingFactor).toInt()
                        if (click.x < 0 || click.y < 0 || click.x >= bitmap.width || click.y >= bitmap.height) return@onPointerEvent
                        colorPickerState.setColor(Color(bitmap.getColor(click.x, click.y)))
                    }
                    focusRequester.requestFocus()
                }
                .onKeyEvent { event ->
                    if (event.key == Key.Z && event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                        if (transactionsQueue.isNotEmpty()) {
                            val lastTransaction = transactionsQueue.removeLast()

                            lastTransaction.revert(bitmap, nativeCanvas)
                            updateImage()

                            redoQueue.add(lastTransaction)

                            framesRendered++
                            return@onKeyEvent true
                        }
                    }
                    if (event.key == Key.Y && event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                        if (redoQueue.isNotEmpty()) {
                            val lastTransaction = redoQueue.removeLast()

                            lastTransaction.apply(bitmap, nativeCanvas)
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

                    val resultSize = Size(bitmap.width * editorContext.scalingFactor, bitmap.height * editorContext.scalingFactor)
                    val offset = editorContext.offset

                    val bgEffect = RuntimeEffect.makeForShader(CHECKERED_BG_SHADER)
                    val byteBuffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)

                    val shaderData = byteBuffer.createCheckeredBGShaderData(
                        squareSize = editorContext.scalingFactor / 2,
                        offset = offset
                    )

                    val shader = bgEffect.makeShader(shaderData, null, null)

                    val checkeredShaderBrush = ShaderBrush(shader)

                    onDrawBehind {
                        drawRect(
                            brush = checkeredShaderBrush,
                            topLeft = offset,
                            size = resultSize
                        )

                        Image.makeFromBitmap(previewBitmap).use { image ->
                            drawContext.canvas.nativeCanvas.drawImageRect(
                                image = image,
                                src = Rect.makeXYWH(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                                dst = Rect.makeXYWH(offset.x, offset.y, resultSize.width, resultSize.height),
                                samplingMode = FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE), // FilterQuality.None
                                paint = imageDrawerPaint,
                                strict = true,
                            )
                        }
                    }
                }
        )
    }
}

private fun ByteBuffer.createCheckeredBGShaderData(
    squareSize: Float,
    offset: Offset,
) = Data.makeFromBytes(
    this.clear()
        .putFloat(squareSize)
        .putFloat(offset.x)
        .putFloat(offset.y)
        .array()
)

@Language("GLSL")
private const val CHECKERED_BG_SHADER = """
uniform float squareSize;
uniform float offsetX;
uniform float offsetY;

vec4 main(vec2 pixel) {
    int x = int((pixel.x - offsetX) / squareSize);
    int y = int((pixel.y - offsetY) / squareSize);
    
    float sum = float(x + y);
    
    if (int(mod(sum, 2.0)) == 0)
        return vec4(0.7, 0.7, 0.7, 1.0);
    else
        return vec4(0.8, 0.8, 0.8, 1.0);
}
"""
