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
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
class Circle(color: Color) : AbstractBrush() {

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

    private var currentTransaction = CircleTransaction(paint)
    private var pressed = false
    private var isCtrlPressed = false

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

        isCtrlPressed = event.keyboardModifiers.isCtrlPressed
        currentTransaction.startPoint = click
        currentTransaction.endPoint = click
        currentTransaction.isCircle = isCtrlPressed
        pressed = true

        emitPreviewChange(currentTransaction)
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        if (pressed) {
            val click = with(editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click !in editorContext.editableImage.size) return

            isCtrlPressed = event.keyboardModifiers.isCtrlPressed
            currentTransaction.endPoint = click
            currentTransaction.isCircle = isCtrlPressed
            emitPreviewChange(currentTransaction)
        } else {
            val click = with(editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click !in editorContext.editableImage.size) {
                emitPreviewChange(EmptyPreviewTransaction)
                return
            }

            currentTransaction.startPoint = click
            currentTransaction.endPoint = click
            currentTransaction.isCircle = isCtrlPressed
            emitPreviewChange(currentTransaction)
        }
    }

    private fun AwaitPointerEventScope.onRelease(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        emitTransaction(currentTransaction)
        currentTransaction = CircleTransaction(paint)
        pressed = false
    }

    class CircleTransaction(
        private val paint: NativePaint,
        var startPoint: Offset = Offset.Zero,
        var endPoint: Offset = Offset.Zero,
        var isCircle: Boolean = false
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
            val x1 = startPoint.x.toInt()
            val y1 = startPoint.y.toInt()
            val x2 = endPoint.x.toInt()
            val y2 = endPoint.y.toInt()

            if (isCircle) {
                // Draw perfect circle
                val radius = sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble()).toInt()
                drawCircle(editableImage, x1, y1, radius)
            } else {
                // Draw ellipse
                val rx = abs(x2 - x1)
                val ry = abs(y2 - y1)
                drawEllipse(editableImage, x1, y1, rx, ry)
            }
        }

        private fun drawCircle(editableImage: EditableImage, centerX: Int, centerY: Int, radius: Int) {
            var x = radius
            var y = 0
            var err = 0

            while (x >= y) {
                drawCirclePoints(editableImage, centerX, centerY, x, y)
                y++
                err += 2 * y + 1
                if (err > x) {
                    x--
                    err -= 2 * x + 1
                }
            }
        }

        private fun drawCirclePoints(editableImage: EditableImage, cx: Int, cy: Int, x: Int, y: Int) {
            // Draw 8 symmetric points
            editableImage.drawPoint(cx + x, cy + y, paint)
            editableImage.drawPoint(cx - x, cy + y, paint)
            editableImage.drawPoint(cx + x, cy - y, paint)
            editableImage.drawPoint(cx - x, cy - y, paint)
            editableImage.drawPoint(cx + y, cy + x, paint)
            editableImage.drawPoint(cx - y, cy + x, paint)
            editableImage.drawPoint(cx + y, cy - x, paint)
            editableImage.drawPoint(cx - y, cy - x, paint)
        }

        private fun drawEllipse(editableImage: EditableImage, centerX: Int, centerY: Int, radiusX: Int, radiusY: Int) {
            var x = -radiusX
            var y = 0
            var e2 = radiusY
            var dx = (1 + 2 * x) * e2 * e2
            var dy = x * x
            var err = dx + dy

            while (x <= 0) {
                editableImage.drawPoint(centerX - x, centerY + y, paint)
                editableImage.drawPoint(centerX + x, centerY + y, paint)
                editableImage.drawPoint(centerX + x, centerY - y, paint)
                editableImage.drawPoint(centerX - x, centerY - y, paint)

                e2 = 2 * err
                if (e2 >= dx) {
                    x++
                    dx += 2 * radiusY * radiusY
                    err += dx
                }
                if (e2 <= dy) {
                    y++
                    dy += 2 * radiusX * radiusX
                    err += dy
                }
            }
        }
    }
} 