package ru.reosfire.pixorio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState

@Composable
fun ApplicationScope.LauncherWindow(
    onContinue: (IntSize) -> Unit
) {
    Window(
        state = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center)),
        onCloseRequest = ::exitApplication,
        title = "Pixorio",
    ) {
        val widthState = remember { SizeComponentInputFieldState(64) }
        val heightState = remember { SizeComponentInputFieldState(64) }

        val size by remember {
            derivedStateOf { IntSize(widthState.value, heightState.value) }
        }

        val correct by remember {
            derivedStateOf { widthState.isCorrect && heightState.isCorrect }
        }

        MaterialTheme {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray)
                        .padding(8.dp)
                        .requiredWidth(IntrinsicSize.Max)
                ) {
                    SizeComponentInputField(
                        label = "Width: ",
                        state = widthState,
                        modifier = Modifier
                    )
                    SizeComponentInputField(
                        label = "Height: ",
                        state = heightState,
                        modifier = Modifier
                    )
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
private fun SizeComponentInputField(
    label: String,
    state: SizeComponentInputFieldState,
    modifier: Modifier = Modifier,
) {
    var text by remember(state) { mutableStateOf(state.value.toString()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(label)
        TextField(
            value = text,
            isError = !state.isCorrect,
            onValueChange = {
                if (it.length > MAX_TEXT_LENGTH) return@TextField
                text = it

                val parsed = it.toIntOrNull()
                if (parsed != null) {
                    state.value = parsed
                    state.isCorrect = true
                } else {
                    state.isCorrect = false
                }
            },
            colors = TextFieldDefaults.textFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

const val MAX_TEXT_LENGTH = 10
