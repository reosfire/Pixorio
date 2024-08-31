package ru.reosfire.pixorio.app.filepicker

import androidx.compose.foundation.focusable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import ru.reosfire.pixorio.designsystem.componentes.CommonTextField
import java.io.File

@Composable
fun TextFilePicker(
    state: State<File?>,
    onSelected: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentFile by state

    var text by remember(currentFile) {
        mutableStateOf(currentFile.toStr())
    }

    val focusManager = LocalFocusManager.current

    CommonTextField(
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
        singleLine = true,
        modifier = modifier
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                    focusManager.clearFocus()
                    true
                }
                else false
            }
            .focusable()
            .onFocusChanged { text = currentFile.toStr() }
    )
}

private fun File?.toStr(): String {
    return this?.path ?: ""
}
