package ru.reosfire.pixorio.brushes

import ru.reosfire.pixorio.EditableImage

interface PaintingTransaction: PreviewTransaction {
    fun apply(editableImage: EditableImage)
    fun revert(editableImage: EditableImage)
}