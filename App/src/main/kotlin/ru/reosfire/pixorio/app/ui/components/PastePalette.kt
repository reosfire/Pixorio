package ru.reosfire.pixorio.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import ru.reosfire.pixorio.designsystem.modifiers.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ru.reosfire.pixorio.app.brushes.AbstractBrush
import ru.reosfire.pixorio.app.brushes.library.ImageBrush


@Composable
fun PastePalette(
    brushes: List<ImageBrush>,
    onSelect: (ImageBrush) -> Unit,
    selectedBrush: AbstractBrush,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.FixedSize(size = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(10.dp)
    ) {
        items(brushes.size) { index ->
            val brush = brushes[index]
            PasteItem(
                brush = brush,
                isSelected = brush == selectedBrush,
                onClick = { onSelect(brush) },
            )
        }
    }
}

@Composable
private fun PasteItem(
    brush: ImageBrush,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = remember { brush.image.toComposeImageBitmap() }
    Image(
        image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = modifier
            .size(28.dp)
            .then(if (isSelected) Modifier.border(1.dp, Color.White) else Modifier)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
    )
}
