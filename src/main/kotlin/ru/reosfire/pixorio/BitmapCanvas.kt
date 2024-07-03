package ru.reosfire.pixorio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap

class BitmapCanvas(
    private val size: IntSize,
    private val bitsPerPixel: Int = 4,
) {
    private val bitmap = Bitmap().apply { allocN32Pixels(size.width, size.height) }
    val pixels = ByteArray(size.height * size.width * bitsPerPixel)

    fun setColor(at: IntOffset, color: Color) = setColor(at.x, at.y, color)

    fun setColor(x: Int, y: Int, color: Color) {
        val baseIndex = (x + y * size.width) * bitsPerPixel
        pixels[baseIndex] = (color.blue * 255).toInt().toByte()
        pixels[baseIndex + 1] = (color.green * 255).toInt().toByte()
        pixels[baseIndex + 2] = (color.red * 255).toInt().toByte()
        pixels[baseIndex + 3] = (color.alpha * 255).toInt().toByte()
    }

    fun createBitmap(): Bitmap {
        bitmap.installPixels(pixels)
        return bitmap
    }
}