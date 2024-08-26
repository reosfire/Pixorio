package ru.reosfire.pixorio.app.ui.components.colorpicker

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import org.intellij.lang.annotations.Language
import ru.reosfire.pixorio.app.draggable
import ru.reosfire.pixorio.app.shaders.CachedShaderBrush

@Composable
@Preview
fun HueSelector(
    hueState: MutableFloatState,
    modifier: Modifier = Modifier,
) {
    var hue by hueState

    fun updateHue(y: Float) {
        hue = y.coerceIn(0f, 360f)
    }

    val backgroundBrush = remember { HueSelectorShaderBrush() }

    Layout(
        modifier = modifier
            .draggable {
                updateHue(it.position.y / size.height * 360f)
            }
            .drawWithCache {
                val barBackgroundColor = Color.Black.copy(alpha = 0.8f)
                val barColor = Color.hsv(hue, 1f, 1f)

                val barLeftEnd = Offset(0f, hue / 360f * size.height)
                val barRightEnd = Offset(size.width, hue / 360f * size.height)

                backgroundBrush.setUniforms(size.height)

                onDrawBehind {
                    drawRect(backgroundBrush)

                    drawLine(barBackgroundColor, barLeftEnd, barRightEnd, 5f)
                    drawLine(barColor, barLeftEnd, barRightEnd, 3f)
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private class HueSelectorShaderBrush: CachedShaderBrush(HUE_SELECTOR_SHADER, 4) {
    fun setUniforms(height: Float) {
        byteBuffer.putFloat(0, height)
    }
}

@Language("SKSL")
private const val HUE_SELECTOR_SHADER = """
uniform float height;

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec4 main(vec2 pixel) {
    float hue = pixel.y / height;
    
    return hsv2rgb(vec3(hue, 1.0, 1.0)).rgb1;
}
"""
