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
        val currentLuminance = luminance()

        val whiteScore = contrastRatio(currentLuminance, Color.White.luminance())
        val blackScore = contrastRatio(currentLuminance, Color.Black.luminance())

        return if (whiteScore > blackScore) Color.White else Color.Black
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
    get() = max

val Color.hsvValue: Float
    get() {
        val mx = max
        return if (mx == 0f) 0f else 1 - min / mx
    }

private val Color.max: Float
    get() = maxOf(red, green, blue)

private val Color.min: Float
    get() = minOf(red, green, blue)

private fun contrastRatio(firstLuminance: Float, secondLuminance: Float): Float =
    if (firstLuminance < secondLuminance) {
        (secondLuminance + 0.05f) / (firstLuminance + 0.05f)
    } else {
        (firstLuminance + 0.05f) / (secondLuminance + 0.05f)
    }
