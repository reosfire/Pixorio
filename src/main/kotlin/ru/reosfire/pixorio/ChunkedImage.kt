package ru.reosfire.pixorio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.*
import org.jetbrains.skiko.toBufferedImage
import java.awt.image.BufferedImage

interface EditableImage {
    val size: IntSize

    fun getColor(x: Number, y: Number): Int

    fun drawPoint(x: Number, y: Number, paint: Paint)
    fun drawLine(x0: Number, y0: Number, x1: Number, y1: Number, paint: NativePaint)

    fun render(drawScope: DrawScope, dstRect: Rect)

    fun loadFrom(other: EditableImage)
    fun loadFrom(snapshot: Image)

    fun makeSnapshot(): Image
    fun toBufferedImage(): BufferedImage
}

inline val EditableImage.height: Int
    get() = size.height

inline val EditableImage.width: Int
    get() = size.height

fun EditableImage.getComposeColor(x: Number, y: Number) = Color(getColor(x, y))

class BasicEditableImage(
    override val size: IntSize,
) : EditableImage {
    private val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32(size.width, size.height, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)) }
    private val canvas = NativeCanvas(bitmap)

    override fun getColor(x: Number, y: Number): Int {
        return bitmap.getColor(x.toInt(), y.toInt())
    }

    override fun drawPoint(x: Number, y: Number, paint: Paint) {
        canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
    }

    override fun drawLine(x0: Number, y0: Number, x1: Number, y1: Number, paint: NativePaint) {
        canvas.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), paint)
    }

    override fun render(drawScope: DrawScope, dstRect: Rect) {
        val targetCanvas = drawScope.drawContext.canvas.nativeCanvas

        Image.makeFromBitmap(bitmap).use { image ->
            targetCanvas.drawImageRect(
                image = image,
                dst = dstRect,
            )
        }
    }

    override fun loadFrom(other: EditableImage) {
        if (other !is BasicEditableImage) error("BasicEditableImage supports loading only from other BasicEditableImage")

        Image.makeFromBitmap(other.bitmap).use { image ->
            loadFrom(image)
        }
    }

    override fun loadFrom(snapshot: Image) {
        snapshot.readPixels(bitmap)
    }

    override fun makeSnapshot(): Image {
        return Image.makeFromBitmap(bitmap)
    }
    override fun toBufferedImage(): BufferedImage {
        return bitmap.toBufferedImage()
    }
}
