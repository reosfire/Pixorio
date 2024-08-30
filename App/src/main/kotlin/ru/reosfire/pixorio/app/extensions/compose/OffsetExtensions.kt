package ru.reosfire.pixorio.app.extensions.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset

fun Offset.toInt() = IntOffset(x.toInt(), y.toInt())
