package ru.reosfire.pixorio.app.brushes.library

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import org.jetbrains.skia.Image
import ru.reosfire.pixorio.app.EditableImage
import ru.reosfire.pixorio.app.EditorContext
import ru.reosfire.pixorio.app.brushes.AbstractBrush
import ru.reosfire.pixorio.app.brushes.EmptyPreviewTransaction
import ru.reosfire.pixorio.app.brushes.PaintingTransaction
import ru.reosfire.pixorio.app.extensions.compose.contains
import ru.reosfire.pixorio.app.extensions.compose.toInt

@OptIn(ExperimentalComposeUiApi::class)
class StraightLine(color: Color) : AbstractBrush() {

    private val paint = Paint().apply {
        this.color = color
        strokeWidth = 1f
        isAntiAlias = false
        filterQuality = FilterQuality.None
        shader = null
        blendMode = BlendMode.Src
        strokeCap = StrokeCap.Square
        strokeJoin = StrokeJoin.Miter
        alpha = color.alpha
        style = PaintingStyle.Stroke
        colorFilter = null
    }.asFrameworkPaint()

    private var currentTransaction = StraightLineTransaction(paint)
    private var pressed = false

    override suspend fun PointerInputScope.inputEventsHandler(editorContext: EditorContext) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Press -> onPress(event, editorContext)
                    PointerEventType.Move -> onMove(event, editorContext)
                    PointerEventType.Release -> onRelease(event, editorContext)
                }
            }
        }
    }

    override fun setColor(color: Color) {
        paint.color = color.toArgb()
    }

    private fun AwaitPointerEventScope.onPress(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        val click = with(editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click !in editorContext.editableImage.size) return

        currentTransaction.startPoint = click
        currentTransaction.endPoint = click
        pressed = true

        emitPreviewChange(currentTransaction)
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        if (pressed) {
            val click = with(editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click !in editorContext.editableImage.size) return

            currentTransaction.endPoint = click
            emitPreviewChange(currentTransaction)
        } else {
            val click = with(editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click !in editorContext.editableImage.size) {
                emitPreviewChange(EmptyPreviewTransaction)
                return
            }

            currentTransaction.startPoint = click
            currentTransaction.endPoint = click
            emitPreviewChange(currentTransaction)
        }
    }

    private fun AwaitPointerEventScope.onRelease(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        emitTransaction(currentTransaction)
        currentTransaction = StraightLineTransaction(paint)
        pressed = false
    }

    class StraightLineTransaction(
        private val paint: NativePaint,
        var startPoint: Offset = Offset.Zero,
        var endPoint: Offset = Offset.Zero
    ) : PaintingTransaction {

        private var savedState: Image? = null

        override fun preview(editableImage: EditableImage) {
            renderTo(editableImage)
        }

        override fun apply(editableImage: EditableImage) {
            savedState?.close()
            savedState = editableImage.makeSnapshot()

            renderTo(editableImage)
        }

        override fun revert(editableImage: EditableImage) {
            val state = savedState ?: error("Saved state is null")
            editableImage.loadFrom(state)
        }

        private fun renderTo(editableImage: EditableImage) {
            // Bresenham's line algorithm
            val x1 = startPoint.x.toInt()
            val y1 = startPoint.y.toInt()
            val x2 = endPoint.x.toInt()
            val y2 = endPoint.y.toInt()

            var x = x1
            var y = y1
            val dx = kotlin.math.abs(x2 - x1)
            val dy = -kotlin.math.abs(y2 - y1)
            val sx = if (x1 < x2) 1 else -1
            val sy = if (y1 < y2) 1 else -1
            var err = dx + dy
            
            while (true) {
                editableImage.drawPoint(x, y, paint)
                if (x == x2 && y == y2) break
                
                val e2 = 2 * err
                if (e2 >= dy) {
                    if (x == x2) break
                    err += dy
                    x += sx
                }
                if (e2 <= dx) {
                    if (y == y2) break
                    err += dx
                    y += sy
                }
            }
        }
    }
} 