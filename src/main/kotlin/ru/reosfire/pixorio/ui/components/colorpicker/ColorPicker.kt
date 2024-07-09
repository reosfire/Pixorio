package ru.reosfire.pixorio.ui.components.colorpicker

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import ru.reosfire.pixorio.extensions.compose.hsvHue
import ru.reosfire.pixorio.extensions.compose.hsvSaturation
import ru.reosfire.pixorio.extensions.compose.hsvValue
import kotlin.math.max

@Composable
fun rememberColorPickerState(color: Color) = remember {
    ColorPickerState(
        initialHue = color.hsvHue,
        initialSaturation = color.hsvSaturation,
        initialValue = color.hsvValue,
        initialAlpha = color.alpha,
    )
}

class ColorPickerState(
    initialHue: Float,
    initialSaturation: Float,
    initialValue: Float,
    initialAlpha: Float,
) {
    val hueState = mutableFloatStateOf(initialHue)
    val saturationState = mutableFloatStateOf(initialSaturation)
    val valueState = mutableFloatStateOf(initialValue)
    val alphaState = mutableFloatStateOf(initialAlpha)

    val colorState = derivedStateOf {
        Color.hsv(
            hue = hueState.value,
            saturation = saturationState.value,
            value = valueState.value,
            alpha = alphaState.value,
        )
    }

    fun setColor(it: Color) {
        hueState.value = it.hsvHue
        saturationState.value = it.hsvSaturation
        valueState.value = it.hsvValue
        alphaState.value = it.alpha
    }
}

@Composable
fun ColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(
            {
                SaturationValueSelector(
                    hueState = state.hueState,
                    saturationState = state.saturationState,
                    valueState = state.valueState,
                )
            },
            {
                Row {
                    AlphaSelector(
                        alphaState = state.alphaState,
                        modifier = Modifier.width(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    HueSelector(
                        hueState = state.hueState,
                        modifier = Modifier.width(20.dp),
                    )
                }
            },
            {
                TextColorPicker(
                    state = state,
                )
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

private fun <T> List<T>.requireOneElement(): T {
    require(size == 1)
    return this[0]
}
