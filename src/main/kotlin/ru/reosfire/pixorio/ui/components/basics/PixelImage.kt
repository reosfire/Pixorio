package ru.reosfire.pixorio.ui.components.basics

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap

@Composable
fun PixelImage(
    bitmap: ImageBitmap,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        filterQuality = FilterQuality.None,
        colorFilter = colorFilter,
        modifier = modifier,
    )
}