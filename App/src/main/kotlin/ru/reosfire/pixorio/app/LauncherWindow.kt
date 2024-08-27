package ru.reosfire.pixorio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import ru.reosfire.pixorio.app.filepicker.FilePickerDialog
import ru.reosfire.pixorio.designsystem.theme.MainTheme

@Composable
fun ApplicationScope.LauncherWindow(
    onContinue: (IntSize) -> Unit
) {
    Window(
        state = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center)),
        onCloseRequest = ::exitApplication,
        title = APP_NAME,
    ) {
        var opened by remember { mutableStateOf(true) }
        if (opened) {
            FilePickerDialog(
                onCancelled = { opened = false },
                onSelected = { opened = false; println(it) },
            )
        }

        val widthState = remember { SizeComponentInputFieldState(64) }
        val heightState = remember { SizeComponentInputFieldState(64) }

        val size by remember {
            derivedStateOf { IntSize(widthState.value, heightState.value) }
        }

        val correct by remember {
            derivedStateOf { widthState.isCorrect && heightState.isCorrect }
        }

        MainTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.surface)
                        .padding(8.dp)
                        .requiredWidth(IntrinsicSize.Max)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier,
                    ) {
                        Text("Width: ", color = MaterialTheme.colors.onSurface)
                        SizeTextField(
                            state = widthState,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(8.dp).fillMaxWidth())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier,
                    ) {
                        Text("Height: ", color = MaterialTheme.colors.onSurface)
                        SizeTextField(
                            state = heightState,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = {
                            if (correct) {
                                onContinue(size)
                            }
                        },
                        enabled = correct,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue", textDecoration = TextDecoration.LineThrough.takeUnless { correct })
                    }
                }
            }
        }
    }
}

private class SizeComponentInputFieldState(
    initialValue: Int,
) {
    val valueState = mutableIntStateOf(initialValue)
    val isCorrectState = mutableStateOf(true)

    var value by valueState
    var isCorrect by isCorrectState
}

@Composable
private fun SizeTextField(
    state: SizeComponentInputFieldState,
    modifier: Modifier = Modifier,
) {
    var text by remember(state) { mutableStateOf(state.value.toString()) }

    TextField(
        value = text,
        isError = !state.isCorrect,
        onValueChange = {
            if (it.length > MAX_TEXT_LENGTH) return@TextField
            text = it

            val parsed = it.toIntOrNull()
            if (parsed != null && parsed in SIZE_RANGE) {
                state.value = parsed
                state.isCorrect = true
            } else {
                state.isCorrect = false
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(textColor = MaterialTheme.colors.onSurface),
        modifier = modifier
    )
}

private const val MAX_TEXT_LENGTH = 4
private val SIZE_RANGE = 1..8192
