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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.window.DialogWindow
import ru.reosfire.pixorio.MainTheme
import ru.reosfire.pixorio.ui.components.basics.BasicButton
import java.io.File

data class FileNode(
    val file: File,
    val children: MutableList<FileNode> = mutableStateListOf(),
) {
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
                    modifier = Modifier,
                    color = MaterialTheme.colors.onBackground,
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
                                var opened by remember { mutableStateOf(false) }

                                FileView(
                                    file = node.file,
                                    isSelected = selected?.path == node.file.path,
                                    onClick = {
                                        opened = !opened
                                        if (opened) {
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
    file: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name = file.name
    if (name.isEmpty()) name = file.path

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (isSelected) Modifier.background(MaterialTheme.colors.surface) else Modifier)
            .clickable(onClick = onClick)
    ) {
        if (file.isDirectory) {
            Image(
                bitmap = useResource("icons/brushes/pencil.png") { loadImageBitmap(it) },
                contentDescription = "adf",
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
        Text(
            text = name,
            color = if (isSelected) MaterialTheme.colors.onSurface else MaterialTheme.colors.onBackground,
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
            lineHeight = 12.sp,
        )
    }
}
