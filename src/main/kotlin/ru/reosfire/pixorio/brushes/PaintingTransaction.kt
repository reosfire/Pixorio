package ru.reosfire.pixorio.brushes

import ru.reosfire.pixorio.EditableImage

interface PaintingTransaction: PreviewTransaction {
    fun apply(editableImage: EditableImage)
    fun revert(editableImage: EditableImage)
}

object EmptyPaintingTransaction : PaintingTransaction {
    override fun apply(editableImage: EditableImage) = Unit
    override fun revert(editableImage: EditableImage) = Unit
    override fun preview(editableImage: EditableImage) = Unit
}
