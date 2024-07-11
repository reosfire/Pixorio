package ru.reosfire.pixorio.brushes

import androidx.compose.ui.graphics.NativeCanvas
import org.jetbrains.skia.Bitmap

interface PreviewTransaction {
    fun preview(bitmap: Bitmap, canvas: NativeCanvas)
}