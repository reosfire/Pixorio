package ru.reosfire.pixorio.brushes

import ru.reosfire.pixorio.EditableImage

interface PreviewTransaction {
    fun preview(editableImage: EditableImage)
}

object EmptyPreviewTransaction : PreviewTransaction {
    override fun preview(editableImage: EditableImage) = Unit
}
