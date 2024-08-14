package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.max

data class FileTreeState(
    val scrollState: ScrollState,
    val offsetsIndex: MutableMap<File, Int> = mutableMapOf(),
) {
    suspend fun scrollToItem(file: File) {
        offsetsIndex[file]?.let { offset -> scrollState.scrollTo(offset) }
    }
}

@Composable
fun rememberFileTreeState(): FileTreeState {
    return FileTreeState(
        rememberScrollState()
    )
}

data class FileTreeItem(
    val node: FileNode,
    val content: @Composable () -> Unit,
)

data class Measured<T>(
    val item: T,
    val placeables: List<Placeable>,
) {
    val totalHeight = placeables.sumOf { it.height }
}

@Composable
fun FileTree(
    state: FileTreeState,
    selectedFileState: MutableState<File?>,
    rootNodes: List<FileNode>,
    modifier: Modifier = Modifier,
) {
    var selectedFile by selectedFileState

    fun inflate(
        node: FileNode,
        depth: Int = 0,
        result: MutableList<FileTreeItem>
    ) {
        result += FileTreeItem(node) {
            FileNodeView(
                state = node,
                isSelected = selectedFile == node.file,
                onClick = {
                    node.toggle()
                    selectedFile = node.file
                },
                modifier = Modifier
                    .padding(start = (16 * depth).dp)
                    .fillMaxWidth()
            )
        }

        node.children.forEachIndexed { i, child ->
            inflate(child, depth + 1, result)
        }
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier.onPlaced { containerSize = it.size }) {
        val horizontalScrollState = rememberScrollState()

        SubcomposeLayout(
            modifier = Modifier
                .align(Alignment.TopStart)
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(state.scrollState)
                .fillMaxSize()
        ) { constraints ->
            val items = mutableListOf<FileTreeItem>()
            rootNodes.forEach { inflate(it, result = items) }

            var idx = 0

            var resultWidth = containerSize.width
            items.forEach { item ->
                subcompose(idx++, item.content).forEach {
                    resultWidth = max(resultWidth, it.measure(Constraints()).width)
                }
            }

            val measuredItems = items.map { item ->
                Measured(
                    item = item,
                    placeables = subcompose(item.node.file.path, item.content).map {
                        it.measure(Constraints(minWidth = resultWidth))
                    },
                )
            }

            layout(resultWidth, measuredItems.sumOf { it.totalHeight }) {
                var y = 0

                measuredItems.forEach { measuredItem ->
                    state.offsetsIndex[measuredItem.item.node.file] = y

                    measuredItem.placeables.forEach { placeable ->
                        placeable.placeRelative(0, y)
                        y += placeable.height
                    }
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(state.scrollState),
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.TopEnd)
        )

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
        )
    }
}
