package ru.reosfire.pixorio.brushes.library

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntOffset
import org.jetbrains.skia.Image
import ru.reosfire.pixorio.EditableImage
import ru.reosfire.pixorio.EditorContext
import ru.reosfire.pixorio.brushes.AbstractBrush
import ru.reosfire.pixorio.brushes.EmptyPreviewTransaction
import ru.reosfire.pixorio.brushes.PaintingTransaction
import ru.reosfire.pixorio.extensions.compose.contains
import ru.reosfire.pixorio.extensions.compose.toInt
import ru.reosfire.pixorio.height
import ru.reosfire.pixorio.width

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

    override fun setColor(color: Color) {
        paint.color = color.toArgb()
    }

    private fun AwaitPointerEventScope.onPress(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click !in editorContext.editableImage.size) return

        emitTransaction(FillTransaction(click.toInt(), paint))
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click !in editorContext.editableImage.size) {
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
            val startColor = editableImage.getColor(startingPoint.x, startingPoint.y)
            val paintColor = paint.color.normalizeColor()

            val traversQueue = ArrayDeque<IntOffset>()
            traversQueue.addLast(IntOffset(startingPoint.x, startingPoint.y))

            while (traversQueue.isNotEmpty()) {
                val (currentX, currentY) = traversQueue.removeFirst()
                val currentColor = editableImage.getColor(currentX, currentY)

                if (currentColor != startColor) continue
                if (currentColor == paintColor) continue

                editableImage.drawPoint(currentX, currentY, paint)

                if (currentX > 0) traversQueue.addLast(IntOffset(currentX - 1, currentY))
                if (currentX + 1 < editableImage.width) traversQueue.addLast(IntOffset(currentX + 1, currentY))
                if (currentY > 0) traversQueue.addLast(IntOffset(currentX, currentY - 1))
                if (currentY + 1 < editableImage.height) traversQueue.addLast(IntOffset(currentX, currentY + 1))
            }
        }
    }
}

private fun Int.normalizeColor(): Int {
    val alpha = this shr 24
    return if (alpha == 0) 0 else this
}
