package ru.reosfire.pixorio.app.filepicker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.reosfire.pixorio.app.extensions.compose.rememberDerived
import java.io.File
import kotlin.math.max
import kotlin.math.min

@Stable
data class FileTreeState(
    val scrollState: ScrollState,
    val offsetsIndex: MutableMap<File, Int> = mutableMapOf(),
    var itemHeight: Int = 0,
) {
    suspend fun scrollToItem(file: File) {
        offsetsIndex[file]?.let { offset -> scrollState.animateScrollTo(offset * itemHeight) }
    }
}

@Composable
fun rememberFileTreeState(): FileTreeState = remember {
    FileTreeState(ScrollState(0))
}

@Composable
fun FileTree(
    model: FileTreeModel,
    state: FileTreeState,
    selectedFileState: MutableState<File?>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedFile by selectedFileState

    fun inflate(
        node: FileNode,
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
                    .padding(start = (16 * node.depth).dp)
            )
        }

        node.children.forEach { child ->
            inflate(child, result)
        }
    }

    val items by remember(model) {
        derivedStateOf {
            mutableListOf<FileTreeItem>().also { container ->
                model.nodes.forEach { inflate(it, result = container) }

                container.forEachIndexed { i, item -> state.offsetsIndex[item.node.file] = i }
            }
        }
    }

    Box(modifier = modifier) {
        val verticalScrollState = state.scrollState
        val horizontalScrollState = rememberScrollState()

        val bottomPadding by rememberDerived { if (horizontalScrollState.canScroll) SCROLL_BAR_THICKNESS else 0 }

        SubcomposeLayout(
            modifier = Modifier
                .fillMaxSize() // Set min constraints to max possible which will be passed down by scroll modifiers
                .align(Alignment.TopStart)
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(state.scrollState)
        ) { constraints ->
            val itemHeight = subcompose(-1, items.first().content).sumOf { it.measure(Constraints()).height }
            state.itemHeight = itemHeight

            val startIndex = verticalScrollState.value / itemHeight
            val endIndex = min(items.size, startIndex + constraints.minHeight / itemHeight + 2)

            val composedItems = (startIndex..<endIndex).map {
                val item = items[it]
                Composed(
                    item = item,
                    measurables = subcompose(item.node.file.path, item.content)
                )
            }

            val resultWidth = max(
                constraints.minWidth,
                composedItems.maxOf { it.maxIntrinsicWidth(itemHeight) }
            )

            val measuredItems = composedItems.map { composed ->
                Measured(
                    item = composed.item,
                    placeables = composed.measure(Constraints.fixedWidth(resultWidth)),
                )
            }

            layout(resultWidth, constraints.constrainHeight(items.size * itemHeight + bottomPadding)) {
                var y = itemHeight * startIndex

                measuredItems.forEach { measuredItem ->
                    measuredItem.placeables.forEach { placeable ->
                        placeable.placeRelative(0, y)
                        y += placeable.height
                    }
                }
            }
        }

        val showVerticalScroll by rememberDerived { verticalScrollState.canScroll }
        val showHorizontalScroll by rememberDerived { horizontalScrollState.canScroll }

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

private data class FileTreeItem(
    val node: FileNode,
    val content: @Composable () -> Unit,
)

private data class Composed<T>(
    val item: T,
    val measurables: List<Measurable>,
) {
    fun maxIntrinsicWidth(height: Int): Int {
        return if (measurables.isEmpty()) 0
        else measurables.maxOf { it.maxIntrinsicWidth(height) }
    }

    fun measure(constraints: Constraints) = measurables.map { it.measure(constraints) }
}

private data class Measured<T>(
    val item: T,
    val placeables: List<Placeable>,
)

private inline val ScrollState.canScroll: Boolean
    get() = canScrollForward || canScrollBackward

private const val SCROLL_BAR_THICKNESS = 12
