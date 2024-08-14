package ru.reosfire.pixorio.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import java.io.File

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
