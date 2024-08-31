package ru.reosfire.pixorio.app.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import ru.reosfire.pixorio.designsystem.componentes.*
import ru.reosfire.pixorio.designsystem.theme.MainTheme
import java.io.File

@Composable
fun FilePickerDialog(
    onCancelled: () -> Unit,
    onSelected: (File) -> Unit,
    title: String = "Untitled",
) {
    val selectedFileState = remember { mutableStateOf<File?>(null) }
    var selectedFile by selectedFileState
    val fileTreeModel = remember {
        FileTreeModel(filesFilter = { !it.isHidden && it.isDirectory })
    }

    val fileTreeState = rememberFileTreeState()
    val coroutineScope = rememberCoroutineScope()

    fun updateTree() {
        coroutineScope.launch {
            selectedFile?.let { selectedFile ->
                fileTreeModel.tryOpenPath(selectedFile)

                fileTreeState.scrollToItem(selectedFile)
            }
        }
    }

    var fileName by remember { mutableStateOf("untitled") }
    val extensionSelectorOptions = listOf(
        SelectorOption("PNG", "png"),
        SelectorOption("JPG", "jpg"),
    )
    val extensionSelectorState = rememberDropdownSelectorState(extensionSelectorOptions)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    CommonTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                            .weight(1f)
                    )

                    DropdownSelector(extensionSelectorState, Modifier.fillMaxHeight())
                }

                SpecialLocations(
                    locations = SPECIAL_LOCATIONS,
                    selectedFileState = selectedFileState,
                    onSelected = {
                        selectedFile = it
                        updateTree()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                TextFilePicker(
                    state = selectedFileState,
                    onSelected = {
                        selectedFile = it
                        updateTree()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                FileTree(
                    model = fileTreeModel,
                    state = fileTreeState,
                    selectedFileState = selectedFileState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth()) {
                    CommonButton(
                        onClick = onCancelled,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Close")
                    }

                    CommonButton(
                        onClick = {
                            val resultFile = selectedFile
                            if (resultFile == null) {
                                onCancelled()
                            } else {
                                onSelected(File("$resultFile${File.separator}$fileName.${extensionSelectorState.selectedOption.payload}"))
                            }
                        },
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
