package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import ru.reosfire.pixorio.MainTheme
import ru.reosfire.pixorio.ui.components.basics.BasicButton
import java.io.File

data class FileNode(
    val file: File,
    val openedState: MutableState<Boolean> = mutableStateOf(false),
    val children: MutableList<FileNode> = mutableStateListOf(),
) {
    var opened by openedState

    fun clearChildrenRecursive() {
        for (child in children) {
            child.clearChildrenRecursive()
        }

        children.clear()
    }
}

@Composable
fun FilePickerDialog(
    onCloseRequest: () -> Unit,
    title: String = "Untitled",
) {
    val rootFiles = File.listRoots()

    var selected by remember { mutableStateOf<File?>(null) }
    val files = remember { rootFiles.map { FileNode(it) }.toMutableStateList() }

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

                Row(Modifier.weight(1f)) {
                    val state = rememberLazyListState()

                    LazyColumn(
                        state = state,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                    ) {
                        val stack = mutableListOf<Pair<FileNode, Int>>()
                        files.fastForEachReversed { file -> stack.add(file to 0) }

                        while (stack.isNotEmpty()) {
                            val (node, depth) = stack.removeLast()

                            item(key = node.file.path) {
                                FileView(
                                    state = node,
                                    isSelected = selected?.path == node.file.path,
                                    onClick = {
                                        node.opened = !node.opened
                                        if (node.opened) {
                                            node.file.listFiles()?.let { innerFiles ->
                                                node.children.addAll(innerFiles.filter { !it.isHidden }.map { file -> FileNode(file) })
                                            }
                                        } else {
                                            node.clearChildrenRecursive()
                                        }

                                        selected = node.file
                                    },
                                    modifier = Modifier.padding(start = (16 * depth).dp)
                                )
                            }

                            node.children.fastForEachReversed { child -> stack.add(child to depth + 1) }
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(state)
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
fun FileView(
    state: FileNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name = state.file.name
    if (name.isEmpty()) name = state.file.path

    val contentColor = if (isSelected) MaterialTheme.colors.onSurface else MaterialTheme.colors.onBackground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .fillMaxWidth()
            .then(if (isSelected) Modifier.background(MaterialTheme.colors.surface) else Modifier)
            .clickable(onClick = onClick)
    ) {
        if (state.file.isDirectory) {
            PixelImage(
                bitmap = if (state.opened) DOWN_ARROW_BITMAP else RIGHT_ARROW_BITMAP,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
            )
        } else {
            Spacer(modifier = Modifier.padding(horizontal = 4.dp).size(8.dp))
        }
        PixelImage(
            bitmap = if (state.file.isDirectory) FOLDER_BITMAP else FILE_BITMAP,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp)
        )
        Text(
            text = name,
            color = contentColor,
            fontWeight = FontWeight.Light,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
    }
}

private val DOWN_ARROW_BITMAP = useResource("icons/filepicker/down_arrow.png") { loadImageBitmap(it) }
private val RIGHT_ARROW_BITMAP = useResource("icons/filepicker/right_arrow.png") { loadImageBitmap(it) }

private val FOLDER_BITMAP = useResource("icons/filepicker/folder.png") { loadImageBitmap(it) }
private val FILE_BITMAP = useResource("icons/filepicker/file.png") { loadImageBitmap(it) }

@Composable
fun PixelImage(
    bitmap: ImageBitmap,
    colorFilter: ColorFilter,
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
