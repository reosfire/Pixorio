package ru.reosfire.pixorio.app.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.reosfire.pixorio.designsystem.componentes.PixelImage

private val DOWN_ARROW_BITMAP = useResource("icons/filepicker/down_arrow.png") { loadImageBitmap(it) }
private val RIGHT_ARROW_BITMAP = useResource("icons/filepicker/right_arrow.png") { loadImageBitmap(it) }

@Composable
fun FileNodeView(
    state: FileNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = state.file

    var name = file.name
    if (name.isEmpty()) name = file.path

    if (file.isDirectory) {
        DirectoryView(
            name = name,
            opened = state.openedState,
            isSelected = isSelected,
            onClick = onClick,
            modifier = modifier,
        )
    } else if (file.isFile) {
        FileView(
            name = name,
            isSelected = isSelected,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun DirectoryView(
    name: String,
    opened: MutableState<Boolean>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSelected) MaterialTheme.colors.onSurface else MaterialTheme.colors.onBackground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .then(if (isSelected) Modifier.background(MaterialTheme.colors.surface) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        PixelImage(
            bitmap = if (opened.value) DOWN_ARROW_BITMAP else RIGHT_ARROW_BITMAP,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = Modifier
                .padding(end = 4.dp)
                .size(8.dp)
        )
        Text(
            text = name,
            color = contentColor,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun FileView(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSelected) MaterialTheme.colors.onSurface else MaterialTheme.colors.onBackground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(2.dp))
            .then(if (isSelected) Modifier.background(MaterialTheme.colors.surface) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = name,
            color = contentColor,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
    }
}
