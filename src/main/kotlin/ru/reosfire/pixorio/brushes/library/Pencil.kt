package ru.reosfire.pixorio.brushes.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntOffset
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import ru.reosfire.pixorio.EditorContext
import ru.reosfire.pixorio.brushes.AbstractBrush
import ru.reosfire.pixorio.brushes.PaintingTransaction
import ru.reosfire.pixorio.extensions.compose.toInt

class Pencil(private val paint: NativePaint) : AbstractBrush() {

    private var currentTransaction: PencilTransaction = PencilTransaction(
        mutableListOf(),
        paint
    )

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
        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) return

        currentTransaction.addPoint(click.toInt())

        pressed = true
        lastPress = click

        emitPreviewChange(currentTransaction)
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        if (pressed) {
            val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) return

            currentTransaction.addPoint(click.toInt())

            lastPress = click

            emitPreviewChange(currentTransaction)
        }
    }

    private fun AwaitPointerEventScope.onRelease(event: PointerEvent, editorContext: EditorContext) {
        emitTransaction(currentTransaction)
        currentTransaction = PencilTransaction(
            mutableListOf(),
            paint
        )
        pressed = false
    }

    class PencilTransaction(
        private val points: MutableList<IntOffset>,
        private val paint: NativePaint,
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
            canvas.clear(0)
            canvas.drawImage(savedState!!, 0f, 0f)
        }

        fun addPoint(point: IntOffset) {
            if (points.isEmpty() || points.last() != point) points.add(point)
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
