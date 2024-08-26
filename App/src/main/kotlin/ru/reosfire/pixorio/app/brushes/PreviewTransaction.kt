package ru.reosfire.pixorio.app.brushes

import ru.reosfire.pixorio.app.EditableImage

interface PreviewTransaction {
    fun preview(editableImage: EditableImage)
}

object EmptyPreviewTransaction : PreviewTransaction {
    override fun preview(editableImage: EditableImage) = Unit
}
