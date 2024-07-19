package ru.reosfire.pixorio.shaders

import androidx.compose.ui.geometry.Offset
import org.intellij.lang.annotations.Language

class CheckeredShaderBrush: CachedShaderBrush(CHECKERED_BG_SHADER, 12) {
    fun setUniforms(squareSize: Float, offset: Offset) {
        byteBuffer
            .putFloat(0, squareSize)
            .putFloat(4, offset.x)
            .putFloat(8, offset.y)
    }
}

@Language("SKSL")
private const val CHECKERED_BG_SHADER = """
uniform float squareSize;
uniform float offsetX;
uniform float offsetY;

vec4 main(vec2 pixel) {
    int x = int((pixel.x - offsetX) / squareSize);
    int y = int((pixel.y - offsetY) / squareSize);
    
    float sum = float(x + y);
    
    if (int(mod(sum, 2.0)) == 0)
        return vec4(0.7, 0.7, 0.7, 1.0);
    else
        return vec4(0.8, 0.8, 0.8, 1.0);
}
"""
