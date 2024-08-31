package ru.reosfire.pixorio.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap
import ru.reosfire.pixorio.app.extensions.compose.aInt
import ru.reosfire.pixorio.app.extensions.compose.bInt
import ru.reosfire.pixorio.app.extensions.compose.gInt
import ru.reosfire.pixorio.app.extensions.compose.rInt

class BitmapCanvas(
    private val size: IntSize,
    private val bitsPerPixel: Int = 4,
): AutoCloseable {
    private val bitmap = Bitmap().apply { allocN32Pixels(size.width, size.height) }
    private val pixels = ByteArray(size.height * size.width * bitsPerPixel)

    fun setColor(at: IntOffset, color: Color) = setColor(at.x, at.y, color)

    fun setColor(x: Int, y: Int, color: Color) {
        val baseIndex = (x + y * size.width) * bitsPerPixel
        pixels[baseIndex] = color.bInt.toByte()
        pixels[baseIndex + 1] = color.gInt.toByte()
        pixels[baseIndex + 2] = color.rInt.toByte()
        pixels[baseIndex + 3] = color.aInt.toByte()
    }

    fun createBitmap(): Bitmap {
        bitmap.installPixels(pixels)
        return bitmap
    }

    override fun close() {
        bitmap.close() // TODO: reuse bitmaps as much as possible. And close them when they no more needed
    }
}
