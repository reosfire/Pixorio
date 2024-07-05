package ru.reosfire.pixorio.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import ru.reosfire.pixorio.extensions.compose.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min


@Composable
fun ColorPicker(
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }

    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var saturationValue by remember { mutableStateOf(Offset.Zero) }

    var color by remember { mutableStateOf(Color.White) }

    fun updateColor() {
        color = Color.hsv(
            hue = hue,
            saturation = saturationValue.x,
            value = 1 - saturationValue.y,
            alpha
        )
        onColorChanged(color)
    }

    fun updateSaturationValue(position: Offset, size: IntSize) {
        saturationValue = Offset(
            (position.x / size.width).coerceIn(0f, 1f),
            (position.y / size.height).coerceIn(0f, 1f),
        )

        updateColor()
    }

    fun updatePosition(position: Offset, size: IntSize) {
        pointerPosition = Offset(position.x.coerceIn(0f, size.width.toFloat()), position.y.coerceIn(0f, size.height.toFloat()))
        updateSaturationValue(position, size)
    }

    Layout(
        contents = listOf(
            {
                SaturationValueSelector(
                    hue = hue,
                    pointerPosition = pointerPosition,
                    onPositionChanged = ::updatePosition
                )
            },
            {
                Row {
                    AlphaSelector(
                        alpha = alpha,
                        onHueChanged = {
                            alpha = it
                            updateColor()
                        },
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    HueSelector(
                        hue = hue,
                        onHueChanged = {
                            hue = it
                            updateColor()
                        },
                        modifier = Modifier.width(20.dp)
                    )
                }
            },
            {
                val customTextSelectionColors = TextSelectionColors(
                    handleColor = Color.Black,
                    backgroundColor = color.contrastColor.copy(0.4f),
                )

                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                    BasicTextField(
                        value = color.toHexString(),
                        onValueChange = {  },
                        textStyle = TextStyle.Default.copy(
                            color = color.contrastColor,
                            textAlign = TextAlign.Center,

                            ),
                        cursorBrush = SolidColor(color.contrastColor),
                        modifier = Modifier.background(color).fillMaxWidth()
                    )
                }
            }
        ),
        modifier = modifier,
        measurePolicy = { (svSelectorMeasurables, hueSelectorMeasurables, textMeasurables), constraints ->
            val svSelector = svSelectorMeasurables.requireOneElement()
            val hueSelector = hueSelectorMeasurables.requireOneElement()
            val text = textMeasurables.requireOneElement()

            val textPlaceable = text.measure(constraints.copy(minWidth = 0, minHeight = 0))

            val availableHeight = constraints.maxHeight - textPlaceable.height

            val hueSelectorPlaceable = hueSelector.measure(
                constraints.copy(
                    maxWidth = constraints.maxWidth,
                    minWidth = 0,
                    maxHeight = availableHeight,
                    minHeight = 0,
                ),
            )

            val svSelectorPlaceable = svSelector.measure(
                constraints.copy(
                    maxWidth = constraints.maxWidth - hueSelectorPlaceable.width,
                    minWidth = 0,
                    maxHeight = availableHeight,
                    minHeight = 0,
                )
            )

            layout(constraints.maxWidth, constraints.maxHeight) {
                svSelectorPlaceable.place(0, 0)
                hueSelectorPlaceable.place(constraints.maxWidth - hueSelectorPlaceable.width, 0)
                textPlaceable.place(0, max(svSelectorPlaceable.height, hueSelectorPlaceable.height))
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SaturationValueSelector(
    hue: Float,
    pointerPosition: Offset,
    onPositionChanged: (Offset, IntSize) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    val runtimeEffect = RuntimeEffect.makeForShader(SV_SPACE_SHADER)
    val shaderDataBuffer = remember { ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN) }

    Layout(
        modifier = Modifier
            .onPointerEvent(PointerEventType.Press) {
                onPositionChanged(it.changes.first().position, size)

                pressed = true
            }.onPointerEvent(PointerEventType.Move) {
                if (pressed) {
                    onPositionChanged(it.changes.first().position, size)
                }
            }.onPointerEvent(PointerEventType.Release) {
                pressed = false
            }.drawWithCache {
                val shaderData = shaderDataBuffer.createSVShaderData(hue, size)
                val shader = runtimeEffect.makeShader(
                    uniforms = shaderData,
                    children = null,
                    localMatrix = null,
                )
                val shaderBrush = ShaderBrush(shader)
                val circleColor = Color.White.copy(alpha = 0.5f)

                onDrawBehind {
                    drawRect(shaderBrush)
                    drawCircle(circleColor, 5f, pointerPosition)
                }
            },
        measurePolicy = {_, constraints ->
            val minMaxSide = min(constraints.maxWidth, constraints.maxHeight)
            layout(minMaxSide, minMaxSide) { }
        }
    )
}

private fun Color.toHexString(): String {
    return "#${rInt.toHex()}${gInt.toHex()}${bInt.toHex()}${aInt.toHex()}"
}

private fun ULong.toHex(): String {
    return toString(16).padStart(2, '0')
}

private fun <T> List<T>.requireOneElement(): T {
    require(size == 1)
    return this[0]
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
