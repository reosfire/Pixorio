package ru.reosfire.pixorio.ui.components.colorpicker

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntSize
import org.intellij.lang.annotations.Language
import ru.reosfire.pixorio.shaders.CachedShaderBrush
import ru.reosfire.pixorio.draggable
import ru.reosfire.pixorio.extensions.compose.contrastColor
import kotlin.math.min

@Composable
fun SaturationValueSelector(
    hueState: FloatState,
    saturationState: MutableFloatState,
    valueState: MutableFloatState
) {
    val pointerPosition = remember { derivedStateOf { Offset(saturationState.value, 1 - valueState.value) } }

    fun updateSaturationValue(position: Offset, size: IntSize) {
        saturationState.value = (position.x / size.width).coerceIn(0f, 1f)
        valueState.value = 1 - (position.y / size.height).coerceIn(0f, 1f)
    }

    fun updatePosition(position: Offset, size: IntSize) {
        updateSaturationValue(position, size)
    }

    val svSpaceShaderBrush = remember { SVSpaceShaderBrush() }

    Layout(
        modifier = Modifier
            .draggable {
                updatePosition(it.position, size)
            }
            .drawWithCache {
                svSpaceShaderBrush.setUniforms(hueState.value, size)

                val circleColor =
                    Color.hsv(hueState.value, saturationState.value, valueState.value).contrastColor.copy(alpha = 0.5f)

                onDrawBehind {
                    drawRect(svSpaceShaderBrush)
                    drawCircle(circleColor, 4f, Offset(pointerPosition.value.x * size.width, pointerPosition.value.y * size.height))
                }
            },
        measurePolicy = {_, constraints ->
            val minMaxSide = min(constraints.maxWidth, constraints.maxHeight)
            layout(minMaxSide, minMaxSide) { }
        }
    )
}

private class SVSpaceShaderBrush: CachedShaderBrush(SV_SPACE_SHADER, 12) {
    fun setUniforms(hue: Float, size: Size) {
        byteBuffer
            .putFloat(0, hue / 360)
            .putFloat(4, size.width)
            .putFloat(8, size.height)
    }
}

@Language("SKSL")
private const val SV_SPACE_SHADER = """
uniform float hue;
uniform float width;
uniform float height;

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec4 main(vec2 pixel) {
    float saturation = pixel.x / width;
    float value = pixel.y / height;
    
    return hsv2rgb(vec3(hue, saturation, 1.0 - value)).rgb1;
}
"""
