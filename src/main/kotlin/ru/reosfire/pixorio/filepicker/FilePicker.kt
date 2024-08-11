package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import ru.reosfire.pixorio.MainTheme
import ru.reosfire.pixorio.ui.components.basics.BasicButton
import ru.reosfire.pixorio.ui.components.basics.PixelImage
import java.io.File

data class FileNode(
    val file: File,
    val openedState: MutableState<Boolean> = mutableStateOf(false),
    val children: MutableList<FileNode> = mutableStateListOf(),
) {
    var opened by openedState

    fun toggle() {
        opened = !opened
        if (opened) {
            updateChildren()
        } else {
            clearChildrenRecursive()
        }
    }

    private fun clearChildrenRecursive() {
        for (child in children) {
            child.clearChildrenRecursive()
        }

        children.clear()
    }

    private fun updateChildren() {
        file.listFiles()?.let { innerFiles ->
            children.addAll(innerFiles.filter { !it.isHidden }.map { file -> FileNode(file) })
        }
    }
}

@Composable
fun FilePickerDialog(
    onCloseRequest: () -> Unit,
    title: String = "Untitled",
) {
    val rootFiles = File.listRoots()

    var selected by remember { mutableStateOf<File?>(null) }
    val rootNodes = remember { rootFiles.map { FileNode(it) }.toMutableStateList() }

    fun LazyListScope.inflate(
        node: FileNode,
        depth: Int = 0,
    ) {
        item(key = node.file.path) {
            FileNodeView(
                state = node,
                isSelected = selected?.path == node.file.path,
                onClick = {
                    node.toggle()
                    selected = node.file
                },
                modifier = Modifier
                    .padding(start = (16 * depth).dp).fillMaxSize(),
            )
        }

        node.children.forEach { child -> inflate(child, depth + 1) }
    }

    DialogWindow(
        state = rememberDialogState(size = DpSize(800.dp, 600.dp)),
        onCloseRequest = onCloseRequest,
        title = title,
    ) {
        MainTheme {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                Text(
                    text = selected?.path ?: "",
                    maxLines = 1,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier,
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val lazyListState = rememberLazyListState()
                    val horizontalScrollState = rememberScrollState()

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState),
                    ) {
                        rootNodes.forEach { inflate(it) }
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(lazyListState),
                        modifier = Modifier.fillMaxHeight().align(Alignment.TopEnd)
                    )

                    HorizontalScrollbar(
                        adapter = rememberScrollbarAdapter(horizontalScrollState),
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)
                    )
                }

                Row(Modifier.fillMaxWidth()) {
                    BasicButton(
                        onClick = onCloseRequest,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Close")
                    }

                    BasicButton(
                        onClick = {  },
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

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

private val DOWN_ARROW_BITMAP = useResource("icons/filepicker/down_arrow.png") { loadImageBitmap(it) }
private val RIGHT_ARROW_BITMAP = useResource("icons/filepicker/right_arrow.png") { loadImageBitmap(it) }

private val FOLDER_BITMAP = useResource("icons/filepicker/folder.png") { loadImageBitmap(it) }
private val FILE_BITMAP = useResource("icons/filepicker/file.png") { loadImageBitmap(it) }
