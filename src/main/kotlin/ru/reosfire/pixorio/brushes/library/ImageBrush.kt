package ru.reosfire.pixorio.brushes.library

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import ru.reosfire.pixorio.EditableImage
import ru.reosfire.pixorio.EditorContext
import ru.reosfire.pixorio.brushes.ColorIndependentBrush
import ru.reosfire.pixorio.brushes.EmptyPreviewTransaction
import ru.reosfire.pixorio.brushes.PaintingTransaction
import ru.reosfire.pixorio.height
import ru.reosfire.pixorio.width

@OptIn(ExperimentalComposeUiApi::class)
class ImageBrush(val image: Image) : ColorIndependentBrush() {

    private var currentTransaction = ImageTransaction(image)

    private var pressed = false

    override suspend fun PointerInputScope.inputEventsHandler(editorContext: EditorContext) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Press -> onPress(event, editorContext)
                    PointerEventType.Move -> onMove(event, editorContext)
                    PointerEventType.Release -> onRelease(event, editorContext)
                    PointerEventType.Scroll -> onScroll(event, editorContext)
                }
            }
        }
    }

    private fun AwaitPointerEventScope.onPress(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
        if (click.x < 0 || click.y < 0 || click.x >= editorContext.editableImage.width || click.y >= editorContext.editableImage.height) return

        currentTransaction.point = click

        pressed = true

        emitPreviewChange(currentTransaction)
    }

    private fun AwaitPointerEventScope.onMove(event: PointerEvent, editorContext: EditorContext) {
        if (pressed) {
            val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click.x < 0 || click.y < 0 || click.x >= editorContext.editableImage.width || click.y >= editorContext.editableImage.height) return

            currentTransaction.point = click

            emitPreviewChange(currentTransaction)
        } else {
            val click = with (editorContext) { event.changes.first().position.toLocalCoordinates() }
            if (click.x < 0 || click.y < 0 || click.x >= editorContext.editableImage.width || click.y >= editorContext.editableImage.height) {
                emitPreviewChange(EmptyPreviewTransaction)
                return
            }

            currentTransaction.point = click
            emitPreviewChange(currentTransaction)
        }
    }

    private fun AwaitPointerEventScope.onRelease(event: PointerEvent, editorContext: EditorContext) {
        if (event.button != PointerButton.Primary) return

        emitTransaction(currentTransaction)
        currentTransaction = ImageTransaction(image)
        pressed = false
    }

    private fun AwaitPointerEventScope.onScroll(event: PointerEvent, editorContext: EditorContext) {
        currentTransaction.rotation += event.changes.first().scrollDelta.y.toInt()

        emitPreviewChange(currentTransaction)
        event.changes.first().consume()
    }

    class ImageTransaction(
        private val image: Image,
        var point: Offset = Offset.Zero,
        var rotation: Int = 0,
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
            editableImage.drawImageRect(
                image = image,
                src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                dst = Rect.makeXYWH(point.x.toInt().toFloat(), point.y.toInt().toFloat(), image.width.toFloat(), image.height.toFloat()),
            )
        }
    }
}
