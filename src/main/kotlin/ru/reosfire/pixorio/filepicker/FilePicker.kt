package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import ru.reosfire.pixorio.MainTheme
import ru.reosfire.pixorio.ui.components.basics.BasicButton
import ru.reosfire.pixorio.ui.components.basics.PixelImage
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
                    state = fileTreeState,
                    selectedFileState = selectedFileState,
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
    LazyRow(modifier = modifier) {
        items(locations, key = { it.id }) {
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
    SpecialLocationUIData(File(System.getProperty("user.home")), FILE_BITMAP, 0),
)
