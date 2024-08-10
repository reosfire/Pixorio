package ru.reosfire.pixorio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.*
import ru.reosfire.pixorio.extensions.compose.useNativeCanvas
import ru.reosfire.pixorio.extensions.skiko.toBufferedImage
import java.awt.image.BufferedImage

interface EditableImage {
    val size: IntSize

    fun getColor(x: Number, y: Number): Int

    fun drawPoint(x: Number, y: Number, paint: Paint)
    fun drawLine(x0: Number, y0: Number, x1: Number, y1: Number, paint: NativePaint)
    fun drawImageRect(
        image: Image,
        src: Rect,
        dst: Rect,
    )

    fun render(drawScope: DrawScope, dstRect: Rect)

    fun loadFrom(other: EditableImage)
    fun loadFrom(snapshot: Image)

    fun makeSnapshot(): Image
    fun makeSnapshot(rect: IRect): Image?
    fun toBufferedImage(): BufferedImage
}

inline val EditableImage.height: Int
    get() = size.height

inline val EditableImage.width: Int
    get() = size.width

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

    override fun drawImageRect(image: Image, src: Rect, dst: Rect) {
        canvas.drawImageRect(image, src, dst)
    }

    override fun render(drawScope: DrawScope, dstRect: Rect) {
        drawScope.useNativeCanvas {
            translate(dstRect.left, dstRect.top)
            scale(dstRect.width / width, dstRect.height / height)
            surface.draw(this, 0, 0, null)
        }
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

    override fun makeSnapshot(rect: IRect): Image? {
        return surface.makeImageSnapshot(rect)
    }

    override fun toBufferedImage(): BufferedImage {
        return surface.toBufferedImage()
    }
}
