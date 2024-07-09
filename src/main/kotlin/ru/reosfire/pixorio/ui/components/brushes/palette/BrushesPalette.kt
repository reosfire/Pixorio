package ru.reosfire.pixorio.ui.components.brushes.palette

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.unit.dp
import ru.reosfire.pixorio.brushes.AbstractBrush

class BrushUiData(
    val name: String,
    val factorize: (NativePaint) -> AbstractBrush
)

@Composable
fun BrushesPalette(
    brushes: List<BrushUiData>,
    onBrushSelect: (BrushUiData) -> Unit,
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
            BrushItem(
                brush = brush,
                onClick = { onBrushSelect(brush) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrushItem(
    brush: BrushUiData,
    onClick: () -> Unit,
) {
    TooltipArea(
        tooltip = {
            Text(brush.name)
        },
    ) {
        Text(
            text = brush.name.first().toString(),
            modifier = Modifier
                .size(28.dp)
                .background(Color.White)
                .clickable(onClick = onClick)
        )
    }
}