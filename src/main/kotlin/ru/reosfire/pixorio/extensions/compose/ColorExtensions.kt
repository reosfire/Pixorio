package ru.reosfire.pixorio.extensions.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Color.rInt: ULong
    get() = if ((value and 0x3fUL) == 0UL) {
        (value shr 48) and 0xffUL
    } else {
        (red * 255).toULong()
    }

val Color.gInt: ULong
    get() = if ((value and 0x3fUL) == 0UL) {
        (value shr 40) and 0xffUL
    } else {
        (green * 255).toULong()
    }

val Color.bInt: ULong
    get() = if ((value and 0x3fUL) == 0UL) {
        (value shr 32) and 0xffUL
    } else {
        (blue * 255).toULong()
    }

val Color.aInt: ULong
    get() = if ((value and 0x3fUL) == 0UL) {
        (value shr 56) and 0xffUL
    } else {
        (alpha * 255).toULong()
    }

val Color.contrastColor: Color
    get() {
        // Essentially we decide between white and black, which one has more contrastRatio with given color.
        // Where contrastRatio(a, b) = (max(a.luminance, b.luminance) + 0.05) / (min(a.luminance, b.luminance) + 0.05)

        val colorLuminance = luminance()
        val clShifted = colorLuminance + 0.05
        val clSquared = clShifted * clShifted

        return if (WHITE_LUMINANCE_SHIFTED > clSquared / BLACK_LUMINANCE_SHIFTED) Color.White else Color.Black
    }

val Color.hsvHue: Float
    get() {
        val delta = max - min
        if (delta == 0f) return 0f

        val hue = if (red > green && red > blue) ((green - blue) / delta)
            else if (green > red && green > blue) (blue - red) / delta + 2f
            else (red - green) / delta + 4f

        return hue * 60
    }

val Color.hsvSaturation: Float
    get() {
        val mx = max
        return if (mx == 0f) 0f else 1 - min / mx
    }

val Color.hsvValue: Float
    get() = max

private val Color.max: Float
    get() = maxOf(red, green, blue)

private val Color.min: Float
    get() = minOf(red, green, blue)

private const val BLACK_LUMINANCE_SHIFTED = 0 + 0.05
private const val WHITE_LUMINANCE_SHIFTED = 1 + 0.05
