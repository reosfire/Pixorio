package ru.reosfire.pixorio.app.shaders

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.intellij.lang.annotations.Language
import java.nio.ByteBuffer

class CheckeredShaderBrush: CachedShaderBrush(CHECKERED_BG_SHADER, 44) {
    fun setUniforms(
        squareSize: Float,
        offset: Offset,
        color0: Color = Color(0.7f, 0.7f, 0.7f),
        color1: Color = Color(0.8f, 0.8f, 0.8f),
    ) {
        byteBuffer.position(0)
        byteBuffer
            .putFloat(squareSize)
            .putFloat(offset.x)
            .putFloat(offset.y)
            .putColor(color0)
            .putColor(color1)
    }
}

private fun ByteBuffer.putColor(color: Color) =
    putFloat(color.red)
    .putFloat(color.green)
    .putFloat(color.blue)
    .putFloat(color.alpha)

@Language("SKSL")
private const val CHECKERED_BG_SHADER = """
uniform float squareSize;
uniform float offsetX;
uniform float offsetY;
uniform vec4 color0;
uniform vec4 color1;

vec4 main(vec2 pixel) {
    int x = int((pixel.x - offsetX) / squareSize);
    int y = int((pixel.y - offsetY) / squareSize);
    
    float sum = float(x + y);
    
    if (int(mod(sum, 2.0)) == 0)
        return color0;
    else
        return color1;
}
"""
