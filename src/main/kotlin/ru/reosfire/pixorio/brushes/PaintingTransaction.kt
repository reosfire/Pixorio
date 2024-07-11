package ru.reosfire.pixorio.brushes

import androidx.compose.ui.graphics.NativeCanvas
import org.jetbrains.skia.Bitmap

interface PaintingTransaction: PreviewTransaction {
    fun apply(bitmap: Bitmap, canvas: NativeCanvas)
    fun revert(bitmap: Bitmap, canvas: NativeCanvas)
}