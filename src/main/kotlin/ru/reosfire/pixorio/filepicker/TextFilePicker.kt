package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
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
            .border(1.dp, MaterialTheme.colors.onBackground, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 4.dp)
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
