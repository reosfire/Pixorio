package ru.reosfire.pixorio.app.extensions.compose

import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

inline fun DrawScope.withNativeCanvas(block: NativeCanvas.() -> Unit) {
    block(drawContext.canvas.nativeCanvas)
}

inline fun DrawScope.useNativeCanvas(block: NativeCanvas.() -> Unit) {
    val nativeCanvas = drawContext.canvas.nativeCanvas
    nativeCanvas.save()

    block(nativeCanvas)

    nativeCanvas.restore()
}
