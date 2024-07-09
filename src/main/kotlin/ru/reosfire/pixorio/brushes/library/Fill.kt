package ru.reosfire.pixorio.brushes.library

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

class Fill(private val paint: NativePaint) : AbstractBrush() {

    override suspend fun PointerInputScope.inputEventsHandler(editorContext: EditorContext) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Press -> onPress(event, editorContext)
                    PointerEventType.Move -> onMove(event, editorContext)
                }
            }
        }
    }

    private fun AwaitPointerEventScope.onPress(event: PointerEvent, editorContext: EditorContext) {
        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) return

        emitTransaction(FillTransaction(click.toInt(), paint))
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) return

        // emitPreviewChange(FillTransaction(click.toInt(), paint))
    }

    class FillTransaction(
        private val startingPoint: IntOffset,
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

        private fun renderTo(bitmap: Bitmap, canvas: NativeCanvas) {
            val startColor = bitmap.getColor(startingPoint.x, startingPoint.y)

            fun travers(currentX: Int, currentY: Int) {
                val traversColor = bitmap.getColor(currentX, currentY)
                if (traversColor != startColor) return
                if (traversColor == paint.color) return

                canvas.drawPoint(currentX.toFloat(), currentY.toFloat(), paint)

                if (currentX > 0) travers(currentX - 1, currentY)
                if (currentX + 1 < bitmap.width) travers(currentX + 1, currentY)
                if (currentY > 0) travers(currentX, currentY - 1)
                if (currentY + 1 < bitmap.height) travers(currentX, currentY + 1)
            }

            travers(startingPoint.x, startingPoint.y)
        }
    }
}