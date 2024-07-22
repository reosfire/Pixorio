package ru.reosfire.pixorio.extensions.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

fun Size.toInt() = IntSize(width.toInt(), height.toInt())
fun Size.round() = IntSize(width.roundToInt(), height.roundToInt())

operator fun IntSize.times(factor: Float) = Size(width * factor, height * factor)

operator fun IntSize.contains(offset: IntOffset) =
    offset.x in 0..<width && offset.y in 0..<height
operator fun IntSize.contains(offset: Offset) =
    offset.x >= 0 && offset.y >= 0 && offset.x < width && offset.y < height
