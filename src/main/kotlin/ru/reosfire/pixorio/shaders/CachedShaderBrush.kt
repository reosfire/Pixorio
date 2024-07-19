package ru.reosfire.pixorio.shaders

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.impl.BufferUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class CachedShaderBrush(sourceCode: String, dataSize: Int): ShaderBrush(), AutoCloseable {
    private val bgEffect = RuntimeEffect.makeForShader(sourceCode)

    private val data = Data.makeUninitialized(dataSize)
    protected val byteBuffer: ByteBuffer =
        BufferUtil.getByteBufferFromPointer(data.writableData(), dataSize).order(ByteOrder.LITTLE_ENDIAN)

    private var shader = bgEffect.makeShader(data, null, null)

    override fun createShader(size: Size) = shader

    override fun close() {
        shader.close()
        data.close()
        bgEffect.close()
    }
}
