package ru.reosfire.pixorio.ui.components.colorpicker

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntSize
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import ru.reosfire.pixorio.draggable
import ru.reosfire.pixorio.extensions.compose.contrastColor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

@Composable
fun SaturationValueSelector(
    hueState: FloatState,
    saturationState: MutableFloatState,
    valueState: MutableFloatState
) {
    val runtimeEffect = RuntimeEffect.makeForShader(SV_SPACE_SHADER)
    val shaderDataBuffer = remember { ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN) }

    val pointerPosition = remember { derivedStateOf { Offset(saturationState.value, 1 - valueState.value) } }

    fun updateSaturationValue(position: Offset, size: IntSize) {
        saturationState.value = (position.x / size.width).coerceIn(0f, 1f)
        valueState.value = 1 - (position.y / size.height).coerceIn(0f, 1f)
    }

    fun updatePosition(position: Offset, size: IntSize) {
        updateSaturationValue(position, size)
    }

    Layout(
        modifier = Modifier
            .draggable {
                updatePosition(it.position, size)
            }
            .drawWithCache {
                val shaderData = shaderDataBuffer.createSVShaderData(hueState.value, size)
                val shader = runtimeEffect.makeShader(
                    uniforms = shaderData,
                    children = null,
                    localMatrix = null,
                )
                val shaderBrush = ShaderBrush(shader)

                val circleColor =
                    Color.hsv(hueState.value, saturationState.value, valueState.value).contrastColor.copy(alpha = 0.5f)

                onDrawBehind {
                    drawRect(shaderBrush)
                    drawCircle(circleColor, 4f, Offset(pointerPosition.value.x * size.width, pointerPosition.value.y * size.height))
                }
            },
        measurePolicy = {_, constraints ->
            val minMaxSide = min(constraints.maxWidth, constraints.maxHeight)
            layout(minMaxSide, minMaxSide) { }
        }
    )
}

private fun ByteBuffer.createSVShaderData(
    hue: Float,
    size: Size,
) = Data.makeFromBytes(
    this.clear()
        .putFloat(hue / 360)
        .putFloat(size.width)
        .putFloat(size.height)
        .array()
)

@Language("GLSL")
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
