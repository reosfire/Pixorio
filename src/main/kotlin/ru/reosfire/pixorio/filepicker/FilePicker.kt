package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import ru.reosfire.pixorio.MainTheme
import ru.reosfire.pixorio.ui.components.common.BasicButton
import ru.reosfire.pixorio.ui.components.common.PixelImage
import java.io.File

data class SelectorOption<T>(
    val title: String,
    val payload: T,
)

@Composable
fun <T> SelectorOptionView(
    option: SelectorOption<T>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = option.title,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.background(MaterialTheme.colors.surface) else Modifier)
            .padding(horizontal = 4.dp)
    )
}

@Composable
fun <T> DropdownSelector(
    options: List<SelectorOption<T>>,
    state: MutableState<SelectorOption<T>>,
) {
    var currentOption by state

    val dropdownState = remember { DropdownMenuState(initialStatus = DropdownMenuState.Status.Closed) }

    Column {
        Text(
            text = currentOption.title,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier
                .clickable { dropdownState.status = DropdownMenuState.Status.Open(Offset.Zero) }
        )

        Box(modifier = Modifier) {
            DropdownMenu(
                dropdownState,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                for (option in options) {
                    SelectorOptionView(
                        option = option,
                        isSelected = option === currentOption,
                        onClick = {
                            dropdownState.status = DropdownMenuState.Status.Closed
                            currentOption = option
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun FilePickerDialog(
    onCancelled: () -> Unit,
    onSelected: (File) -> Unit,
    title: String = "Untitled",
) {
    val rootFiles = File.listRoots()

    val selectedFileState = remember { mutableStateOf<File?>(null) }
    var selectedFile by selectedFileState
    val rootNodes = remember { rootFiles.map { FileNode(it) } }

    val fileTreeState = rememberFileTreeState()
    val coroutineScope = rememberCoroutineScope()

    fun update() {
        coroutineScope.launch {
            selectedFile?.let { selectedFile ->
                rootNodes.forEach { it.tryOpenPath(selectedFile) }
            }

            selectedFile?.let { fileTreeState.scrollToItem(it) }
        }
    }

    var fileName by remember { mutableStateOf("untitled") }
    val extensionsSelectorOptions = listOf(
        SelectorOption("PNG", "png"),
        SelectorOption("JPG", "jpg"),
    )
    val extensionState = remember { mutableStateOf(extensionsSelectorOptions.first()) }

    DialogWindow(
        state = rememberDialogState(size = DpSize(800.dp, 600.dp)),
        onCloseRequest = onCancelled,
        title = title,
    ) {
        MainTheme {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(8.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                        },
                        textStyle = TextStyle.Default.copy(
                            color = MaterialTheme.colors.onBackground,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colors.onBackground, RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colors.background)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    DropdownSelector(
                        options = extensionsSelectorOptions,
                        state = extensionState,
                    )
                }

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
                    state = fileTreeState,
                    selectedFileState = selectedFileState,
                    rootNodes = rootNodes,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth()) {
                    BasicButton(
                        onClick = onCancelled,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Close")
                    }

                    BasicButton(
                        onClick = {
                            val resultFile = selectedFile
                            if (resultFile == null) {
                                onCancelled()
                            } else {
                                onSelected(resultFile)
                            }
                        },
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

data class SpecialLocationUIData(
    val file: File,
    val icon: ImageBitmap,
    val id: Int,
)

@Composable
fun SpecialLocations(
    locations: List<SpecialLocationUIData>,
    selectedFileState: State<File?>,
    onSelected: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    remember {  }
    LazyRow(modifier = modifier) {
        items(locations, key = { it.id }) {
            PixelImage(
                bitmap = it.icon,
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        onSelected(it.file)
                    }
                    .then(if (it.file == selectedFileState.value) Modifier.background(MaterialTheme.colors.surface) else Modifier)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}

private val DESKTOP_BITMAP = useResource("icons/filepicker/desktop.png") { loadImageBitmap(it) }

val specialLocations = listOf(
    SpecialLocationUIData(File(System.getProperty("user.home")), DESKTOP_BITMAP, 0),
)
