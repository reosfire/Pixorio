package ru.reosfire.pixorio.ui.components.brushes.palette

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import ru.reosfire.pixorio.brushes.AbstractBrush

data class BrushUiData(
    val name: String,
    val iconResource: String,
    val brush: AbstractBrush
)

@Composable
fun BrushesPalette(
    brushes: List<BrushUiData>,
    onBrushSelect: (BrushUiData) -> Unit,
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
            val brushUi = brushes[index]
            println("${brushUi.brush} == $selectedBrush    :    ${brushUi.brush == selectedBrush}")
            BrushItem(
                brush = brushUi,
                isSelected = brushUi.brush == selectedBrush,
                onClick = {
                    onBrushSelect(brushUi)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrushItem(
    brush: BrushUiData,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    TooltipArea(
        tooltip = {
            Text(brush.name)
        },
    ) {
        Image(
            bitmap = useResource(brush.iconResource) { loadImageBitmap(it) },
            contentDescription = brush.name.first().toString(),
            filterQuality = FilterQuality.None,
            modifier = modifier
                .size(28.dp)
                .then(if (isSelected) Modifier.border(1.dp, Color.White) else Modifier)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick),
        )
    }
}