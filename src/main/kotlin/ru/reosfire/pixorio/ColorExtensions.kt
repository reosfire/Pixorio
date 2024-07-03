package ru.reosfire.pixorio

import androidx.compose.ui.graphics.Color

import kotlin.math.max

private val Color.max: Float
    get() = maxOf(red, green, blue)

private val Color.min: Float
    get() = maxOf(red, green, blue)

val Color.saturation: Float
    get() = max

val Color.contrast: Color
    get() = if (saturation > 0.7f) Color.Black else Color.White