package ru.reosfire.pixorio.app.brushes

import ru.reosfire.pixorio.app.EditableImage

interface PaintingTransaction: PreviewTransaction {
    fun apply(editableImage: EditableImage)
    fun revert(editableImage: EditableImage)
}

object EmptyPaintingTransaction : PaintingTransaction {
    override fun apply(editableImage: EditableImage) = Unit
    override fun revert(editableImage: EditableImage) = Unit
    override fun preview(editableImage: EditableImage) = Unit
}
