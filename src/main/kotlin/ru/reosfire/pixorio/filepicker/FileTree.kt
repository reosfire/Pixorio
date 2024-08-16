package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
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
                    scope.launch {
                        node.toggle()
                    }
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

    val verticalScrollState = state.scrollState
    val horizontalScrollState = rememberScrollState()

    val endPadding = if (verticalScrollState.canScroll) SCROLL_BAR_THICKNESS else 0
    val bottomPadding = if (horizontalScrollState.canScroll) SCROLL_BAR_THICKNESS else 0

    var containerWidth by remember { mutableIntStateOf(0) }
    Box(modifier = modifier) {
        SubcomposeLayout(
            modifier = Modifier
                .layout { measurable, constraints ->
                    containerWidth = constraints.maxWidth
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        measurable.measure(constraints).place(0, 0)
                    }
                }
                .padding(end = endPadding.dp)
                .align(Alignment.TopStart)
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(state.scrollState)
        ) { constraints ->
            val items = mutableListOf<FileTreeItem>()
            rootNodes.forEach { inflate(it, result = items) }

            var idx = 0

            var resultWidth = containerWidth - endPadding
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

            layout(resultWidth, measuredItems.sumOf { it.totalHeight } + bottomPadding) {
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

        val showVerticalScroll = verticalScrollState.canScroll
        val showHorizontalScroll = horizontalScrollState.canScroll

        if (showVerticalScroll) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(verticalScrollState),
                modifier = Modifier
                    .then(if (showHorizontalScroll) Modifier.padding(bottom = SCROLL_BAR_THICKNESS.dp) else Modifier)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
            )
        }

        if (showHorizontalScroll) {
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(horizontalScrollState),
                modifier = Modifier
                    .then(if (showVerticalScroll) Modifier.padding(end = SCROLL_BAR_THICKNESS.dp) else Modifier)
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
            )
        }
    }
}

val ScrollState.canScroll: Boolean
    get() = canScrollForward || canScrollBackward


private const val SCROLL_BAR_THICKNESS = 12
