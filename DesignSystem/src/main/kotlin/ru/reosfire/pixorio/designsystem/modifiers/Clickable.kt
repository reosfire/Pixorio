package ru.reosfire.pixorio.designsystem.modifiers

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

@Composable
fun Modifier.clickable(onClick: () -> Unit) =
    this.pointerHoverIcon(PointerIcon.Hand)
        .clickable(onClick = onClick)
