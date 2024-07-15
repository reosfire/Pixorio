package ru.reosfire.pixorio.brushes.library

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntOffset
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import ru.reosfire.pixorio.EditorContext
import ru.reosfire.pixorio.brushes.AbstractBrush
import ru.reosfire.pixorio.brushes.EmptyPreviewTransaction
import ru.reosfire.pixorio.brushes.PaintingTransaction
import ru.reosfire.pixorio.brushes.PreviewTransaction
import ru.reosfire.pixorio.extensions.compose.toInt

@OptIn(ExperimentalComposeUiApi::class)
class Pencil(color: Color) : AbstractBrush() {

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

    private val currentTransaction = PencilTransaction(paint)
    private val currentPointTransaction = PencilCurrentPointTransaction(Offset.Zero, paint)

    private var lastPress = Offset.Zero
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

    private fun AwaitPointerEventScope.onPress(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) return

        currentTransaction.addPoint(click)

        pressed = true
        lastPress = click

        emitPreviewChange(currentTransaction)
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        if (pressed) {
            val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) return

            currentTransaction.addPoint(click)

            lastPress = click

            emitPreviewChange(currentTransaction)
        } else {
            val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) {
                emitPreviewChange(EmptyPreviewTransaction)
                return
            }

            currentPointTransaction.point = click
            emitPreviewChange(currentPointTransaction)
        }
    }

    private fun AwaitPointerEventScope.onRelease(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        emitTransaction(currentTransaction)
        currentTransaction.clear()
        pressed = false
    }

    class PencilCurrentPointTransaction(
        var point: Offset,
        private val paint: NativePaint,
    ) : PreviewTransaction {

        override fun preview(bitmap: Bitmap, canvas: NativeCanvas) {
            canvas.drawPoint(point.x, point.y, paint)
        }
    }

    class PencilTransaction(
        private val paint: NativePaint,
        private val points: MutableList<IntOffset> = mutableListOf(),
    ) : PaintingTransaction {

        private var savedState: Image? = null

        override fun preview(bitmap: Bitmap, canvas: NativeCanvas) {
            renderTo(bitmap, canvas)
        }

        override fun apply(bitmap: Bitmap, canvas: NativeCanvas) {
            savedState = Image.makeFromBitmap(bitmap)

            renderTo(bitmap, canvas)
        }

        override fun revert(bitmap: Bitmap, canvas: NativeCanvas) {
            if (savedState == null) error("cannot revert transaction which is not applied")
            savedState!!.readPixels(bitmap)
        }

        fun addPoint(point: Offset) {
            val point = point.toInt()
            if (points.isEmpty() || points.last() != point) points.add(point)
        }

        fun clear() {
            points.clear()
        }

        private fun renderTo(bitmap: Bitmap, canvas: NativeCanvas) {
            if (points.isEmpty()) return
            if (points.size == 1) {
                val point = points.first()
                canvas.drawPoint(point.x.toFloat(), point.y.toFloat(), paint)
            }
            for (i in 1..<points.size) {
                val prevPoint = points[i - 1]
                val currentPoint = points[i]
                canvas.drawLine(
                    prevPoint.x.toFloat(),
                    prevPoint.y.toFloat(),
                    currentPoint.x.toFloat(),
                    currentPoint.y.toFloat(),
                    paint,
                )
            }
        }
    }
}
