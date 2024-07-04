package ru.reosfire.pixorio.extensions.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

fun Size.toInt() = IntSize(width.toInt(), height.toInt())
fun Size.round() = IntSize(width.roundToInt(), height.roundToInt())
