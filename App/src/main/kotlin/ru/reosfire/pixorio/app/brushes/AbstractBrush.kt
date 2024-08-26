package ru.reosfire.pixorio.app.brushes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ru.reosfire.pixorio.app.EditorContext

abstract class AbstractBrush {
    private val scope = CoroutineScope(Dispatchers.Default)

    val applyTransactionsFlow = MutableSharedFlow<PaintingTransaction>()
    val previewTransactionsFlow = MutableSharedFlow<PreviewTransaction>()

    abstract suspend fun PointerInputScope.inputEventsHandler(editorContext: EditorContext)
    abstract fun setColor(color: Color)

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

abstract class ColorIndependentBrush : AbstractBrush() {
    override fun setColor(color: Color) = Unit
}
