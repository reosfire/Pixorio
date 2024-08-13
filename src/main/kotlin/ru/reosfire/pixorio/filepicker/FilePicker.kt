package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import ru.reosfire.pixorio.MainTheme
import ru.reosfire.pixorio.ui.components.basics.BasicButton
import ru.reosfire.pixorio.ui.components.basics.PixelImage
import ru.reosfire.pixorio.utils.WrappedInt
import java.io.File

@Composable
fun FilePickerDialog(
    onCloseRequest: () -> Unit,
    title: String = "Untitled",
) {
    val rootFiles = File.listRoots()

    val selectedFileState = remember { mutableStateOf<File?>(null) }
    var selectedFile by selectedFileState
    val rootNodes = remember { rootFiles.map { FileNode(it) } }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val filesIndex = remember { mutableMapOf<File, Int>() }

    fun update() {
        selectedFile?.let { selectedFile ->
            rootNodes.forEach { it.tryOpenPath(selectedFile) }
        }

        coroutineScope.launch {
            filesIndex[selectedFile]?.let {
                lazyListState.scrollToItem(it)
            }
        }
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
                SpecialLocations(
                    locations = specialLocations,
                    selectedFileState = selectedFileState,
                    onSelected = {
                        selectedFile = it
                        update()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                TextFilePicker(
                    state = selectedFileState,
                    onSelected = {
                        selectedFile = it
                        update()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                FileTree(
                    lazyListState = lazyListState,
                    selectedFileState = selectedFileState,
                    filesIndex = filesIndex,
                    rootNodes = rootNodes,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

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
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTree(
    lazyListState: LazyListState,
    selectedFileState: MutableState<File?>,
    filesIndex: MutableMap<File, Int>,
    rootNodes: List<FileNode>,
    modifier: Modifier = Modifier,
) {
    var selectedFile by selectedFileState

    fun LazyListScope.inflate(
        node: FileNode,
        itemIndex: WrappedInt,
        depth: Int = 0,
    ) {
        item(key = node.file.path) {
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

        filesIndex[node.file] = itemIndex.value++

        node.children.forEachIndexed { i, child ->
            inflate(child, itemIndex, depth + 1)
        }
    }

    Box(modifier = modifier) {
        val horizontalScrollState = rememberScrollState()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
            ,
            content = {
                val counter = WrappedInt()
                rootNodes.forEach { inflate(it, counter) }
            },
        )

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
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

@Composable
fun TextFilePicker(
    state: State<File?>,
    onSelected: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentFile by state

    var text by remember(currentFile) {
        mutableStateOf(currentFile?.path ?: "")
    }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.onSurface,
        backgroundColor = MaterialTheme.colors.surface,
    )

    val focusManager = LocalFocusManager.current

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = {
                val file = File(it.trim())
                if (file.exists()) {
                    onSelected(file)
                    text = it
                } else {
                    text = it
                }
            },
            textStyle = TextStyle.Default.copy(
                color = MaterialTheme.colors.onBackground,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
            modifier = modifier
                .background(MaterialTheme.colors.background)
                .onPreviewKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                        focusManager.clearFocus()
                        true
                    }
                    else false
                }
                .focusable()
        )
    }
}

data class SpecialLocationUIData(
    val file: File,
    val icon: ImageBitmap,
)

@Composable
fun SpecialLocations(
    locations: List<SpecialLocationUIData>,
    selectedFileState: State<File?>,
    onSelected: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier) {
        items(locations, key = { it.file.path }) {
            PixelImage(
                bitmap = it.icon,
                modifier = Modifier
                    .size(24.dp)
                    .then(if (it.file == selectedFileState.value) Modifier else Modifier)
                    .clickable {
                        onSelected(it.file)
                    }
            )
        }
    }
}

private val FOLDER_BITMAP = useResource("icons/filepicker/folder.png") { loadImageBitmap(it) }
private val FILE_BITMAP = useResource("icons/filepicker/file.png") { loadImageBitmap(it) }

val specialLocations = listOf(
    SpecialLocationUIData(File(System.getProperty("user.home")), FILE_BITMAP),

)
