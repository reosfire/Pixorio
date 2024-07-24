package ru.reosfire.pixorio.extensions.skiko

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.BufferUtil
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.Raster
import java.nio.ByteBuffer

fun Surface.toBitmap(): Bitmap {
    val result = Bitmap().apply { allocPixels(imageInfo) }

    readPixels(result, 0, 0)

    return result
}

private class DirectDataBuffer(val backing: ByteBuffer) : DataBuffer(TYPE_BYTE, backing.limit()) {
    override fun getElem(bank: Int, index: Int): Int {
        return backing[index].toInt()
    }

    override fun setElem(bank: Int, index: Int, value: Int) {
        throw UnsupportedOperationException("no write access")
    }
}

// This code is rewritten Bitmap.toBufferedImage extension function
fun Surface.toBufferedImage(): BufferedImage {
    val pixmap = Pixmap().also { peekPixels(it) }
    val pixelsNativePointer = pixmap.addr

    val pixelsBuffer = BufferUtil.getByteBufferFromPointer(pixelsNativePointer, imageInfo.minRowBytes * height)

    val order = when (imageInfo.colorType) {
        ColorType.RGB_888X -> intArrayOf(0, 1, 2, 3)
        ColorType.BGRA_8888 -> intArrayOf(2, 1, 0, 3)
        else -> throw UnsupportedOperationException("unsupported color type ${imageInfo.colorType}")
    }
    val raster = Raster.createInterleavedRaster(
        DirectDataBuffer(pixelsBuffer),
        this.width,
        this.height,
        this.width * 4,
        4,
        order,
        null
    )
    val colorModel = ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_sRGB),
        true,
        false,
        Transparency.TRANSLUCENT,
        DataBuffer.TYPE_BYTE
    )
    return BufferedImage(colorModel, raster, false, null)
}
