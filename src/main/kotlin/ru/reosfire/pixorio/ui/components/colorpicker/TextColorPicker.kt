package ru.reosfire.pixorio.ui.components.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import ru.reosfire.pixorio.extensions.compose.*

private const val LOWER_CASE_HEX_DIGITS = "0123456789abcdef"
private const val UPPER_CASE_HEX_DIGITS = "0123456789ABCDEF"

private val HEX_DIGITS_TO_DECIMAL = IntArray(128) { -1 }.apply {
    LOWER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
    UPPER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
}

@Composable
fun TextColorPicker(
    state: ColorPickerState,
) {
    val color by state.colorState

    var text by remember(color) { mutableStateOf(color.toHexString()) }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color.Black,
        backgroundColor = color.contrastColor.copy(0.4f),
    )

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = {
                val parsedColor = tryParseColor(it)
                if (parsedColor != null) {
                    text = parsedColor.toHexString()
                    state.setColor(parsedColor)
                } else {
                    text = it
                }
            },
            textStyle = TextStyle.Default.copy(
                color = color.contrastColor,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            cursorBrush = SolidColor(color.contrastColor),
            modifier = Modifier
                .background(color)
                .fillMaxWidth()
                .onPreviewKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                        focusManager.clearFocus()
                        true
                    }
                    else false
                }
                .focusRequester(focusRequester)
                .focusable()
                .onFocusChanged { text = color.toHexString() }
        )
    }
}

private fun tryParseColor(string: String): Color? {
    if (string.length != 9) return null
    if (string[0] != '#') return null

    val r = tryParseComponent(string[1], string[2]) ?: return null
    val g = tryParseComponent(string[3], string[4]) ?: return null
    val b = tryParseComponent(string[5], string[6]) ?: return null
    val a = tryParseComponent(string[7], string[8]) ?: return null

    return Color(r, g, b, a)
}

private fun tryParseComponent(charA: Char, charB: Char): Int? {
    val a = tryParseChar(charA) ?: return null
    val b = tryParseChar(charB) ?: return null

    return (a shl 4) + b
}

private fun tryParseChar(char: Char): Int? {
    val code = char.code
    if (code > 127 || HEX_DIGITS_TO_DECIMAL[code] < 0) return null
    return HEX_DIGITS_TO_DECIMAL[code]
}


private fun Color.toHexString(): String {
    return "#${rInt.toHex()}${gInt.toHex()}${bInt.toHex()}${aInt.toHex()}"
}

private fun ULong.toHex(): String {
    return String(
        charArrayOf(
            UPPER_CASE_HEX_DIGITS[(this / 16u).toInt()],
            UPPER_CASE_HEX_DIGITS[(this % 16u).toInt()],
        )
    )
}
