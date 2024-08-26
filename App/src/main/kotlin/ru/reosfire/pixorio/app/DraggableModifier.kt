package ru.reosfire.pixorio.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.draggable(
    onChange: AwaitPointerEventScope.(PointerInputChange) -> Unit
): Modifier {
    var pressed by remember { mutableStateOf(false) }

    return onPointerEvent(PointerEventType.Press) {
        onChange(it.changes.first())
        pressed = true
    }.onPointerEvent(PointerEventType.Move) {
        if (pressed) {
            onChange(it.changes.first())
        }
    }.onPointerEvent(PointerEventType.Release) {
        pressed = false
    }
}