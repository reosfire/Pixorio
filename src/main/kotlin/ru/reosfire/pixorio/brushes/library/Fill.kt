package ru.reosfire.pixorio.brushes.library

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntOffset
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import ru.reosfire.pixorio.EditorContext
import ru.reosfire.pixorio.brushes.AbstractBrush
import ru.reosfire.pixorio.brushes.EmptyPreviewTransaction
import ru.reosfire.pixorio.brushes.PaintingTransaction
import ru.reosfire.pixorio.extensions.compose.toInt

@OptIn(ExperimentalComposeUiApi::class)
class Fill(color: Color) : AbstractBrush() {

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
        if (event.button != PointerButton.Primary) return

        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) return

        emitTransaction(FillTransaction(click.toInt(), paint))
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click.x < 0 || click.y < 0 || click.x >= editorContext.bitmap.width || click.y >= editorContext.bitmap.height) {
            emitPreviewChange(EmptyPreviewTransaction)
            return
        }

        emitPreviewChange(FillTransaction(click.toInt(), paint))
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
            savedState!!.readPixels(bitmap)
        }

        private fun renderTo(bitmap: Bitmap, canvas: NativeCanvas) {
            val startColor = bitmap.getColor(startingPoint.x, startingPoint.y)
            val paintColor = paint.color.normalizeColor()

            fun travers(currentX: Int, currentY: Int) {
                val traversColor = bitmap.getColor(currentX, currentY)
                if (traversColor != startColor) return
                if (traversColor == paintColor) return

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

private fun Int.normalizeColor(): Int {
    val alpha = this shr 24
    return if (alpha == 0) 0 else this
}
