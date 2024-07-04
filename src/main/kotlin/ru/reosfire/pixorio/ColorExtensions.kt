package ru.reosfire.pixorio

import androidx.compose.ui.graphics.Color

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

val Color.contrast: Color
    get() = if (hsvSaturation > 0.7f) Color.Black else Color.White

val Color.hsvHue: Float
    get() {
        if (red == green && green == blue) return 0f

        val delta = max - min

        val hue = if (red > green && red > blue) ((green - blue) / delta) % 6
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
    get() = maxOf(red, green, blue)