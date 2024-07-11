package ru.reosfire.pixorio.brushes

import androidx.compose.ui.input.pointer.PointerInputScope
import ru.reosfire.pixorio.EditorContext

abstract class AbstractBrush {
    private lateinit var transactionListener: (PaintingTransaction) -> Unit
    private lateinit var previewChangeListener: (PreviewTransaction) -> Unit

    abstract suspend fun PointerInputScope.inputEventsHandler(editorContext: EditorContext)

    protected fun emitTransaction(transaction: PaintingTransaction) {
        transactionListener(transaction)
    }

    protected fun emitPreviewChange(transaction: PreviewTransaction) {
        previewChangeListener(transaction)
    }

    fun setTransactionListener(listener: (PaintingTransaction) -> Unit) {
        transactionListener = listener
    }

    fun setPreviewChangeListener(listener: (PreviewTransaction) -> Unit) {
        previewChangeListener = listener
    }
}