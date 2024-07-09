package ru.reosfire.pixorio.brushes

import androidx.compose.ui.graphics.NativeCanvas
import org.jetbrains.skia.Bitmap

interface PaintingTransaction {
    fun preview(bitmap: Bitmap, canvas: NativeCanvas)
    fun apply(bitmap: Bitmap, canvas: NativeCanvas)
    fun revert(bitmap: Bitmap, canvas: NativeCanvas)
}