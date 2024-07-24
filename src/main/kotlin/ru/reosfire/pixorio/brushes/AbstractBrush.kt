package ru.reosfire.pixorio.brushes

import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ru.reosfire.pixorio.EditorContext

abstract class AbstractBrush {
    private val scope = CoroutineScope(Dispatchers.Default)

    val applyTransactionsFlow = MutableSharedFlow<PaintingTransaction>()
    val previewTransactionsFlow = MutableSharedFlow<PreviewTransaction>()

    abstract suspend fun PointerInputScope.inputEventsHandler(editorContext: EditorContext)

    protected fun emitTransaction(transaction: PaintingTransaction) {
        scope.launch {
            applyTransactionsFlow.emit(transaction)
        }
    }

    protected fun emitPreviewChange(transaction: PreviewTransaction) {
        scope.launch {
            previewTransactionsFlow.emit(transaction)
        }
    }
}