package ru.reosfire.pixorio

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.*
import org.jetbrains.skia.Paint
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
    private val imageInfo = ImageInfo.makeN32(size.width, size.height, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)

    private val surface = Surface.makeRaster(imageInfo)
    private val canvas = surface.canvas

    private val data = Data.makeUninitialized(imageInfo.minRowBytes)
    private val pixmap = Pixmap.make(imageInfo, data, data.size)

    override fun getColor(x: Number, y: Number): Int {
        surface.peekPixels(pixmap)
        return pixmap.getColor(x.toInt(), y.toInt())
    }

    override fun drawPoint(x: Number, y: Number, paint: Paint) {
        canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
    }

    override fun drawLine(x0: Number, y0: Number, x1: Number, y1: Number, paint: NativePaint) {
        canvas.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), paint)
    }

    override fun render(drawScope: DrawScope, dstRect: Rect) {
        val targetCanvas = drawScope.drawContext.canvas.nativeCanvas

        targetCanvas.save()

        targetCanvas.translate(dstRect.left, dstRect.top)
        targetCanvas.scale(dstRect.width / width, dstRect.height / height)
        surface.draw(targetCanvas, 0, 0, null)

        targetCanvas.restore()
    }

    override fun loadFrom(other: EditableImage) {
        if (other !is BasicEditableImage) error("BasicEditableImage supports loading only from other BasicEditableImage")

        other.surface.peekPixels(pixmap)
        surface.writePixels(pixmap, 0, 0)
    }

    override fun loadFrom(snapshot: Image) {
        snapshot.peekPixels(pixmap)
        surface.writePixels(pixmap, 0, 0)
    }

    override fun makeSnapshot(): Image {
        return surface.makeImageSnapshot()
    }
    override fun toBufferedImage(): BufferedImage {
        return makeSnapshot().toComposeImageBitmap().toAwtImage() // TODO not the best solution
    }
}
