package ru.reosfire.pixorio

import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.application
import ru.reosfire.pixorio.filepicker.FilePickerDialog

sealed class ApplicationState {
    data object Launcher : ApplicationState()
    data object Editor : ApplicationState()
}

fun main() = application {
    var bitmapSize by remember { mutableStateOf(IntSize.Zero) }
    var applicationState by remember { mutableStateOf<ApplicationState>(ApplicationState.Launcher) }

    when (applicationState) {
        ApplicationState.Launcher ->
            LauncherWindow(
                onContinue = {
                    bitmapSize = it
                    applicationState = ApplicationState.Editor
                }
            )
        ApplicationState.Editor -> {
            AppWindow(
                bitmapSize = bitmapSize,
                onCloseRequest = {
                    applicationState = ApplicationState.Launcher
                }
            )
        }
    }

    var opened by remember { mutableStateOf(true) }
    if (opened) {
        FilePickerDialog(
            onCloseRequest = { opened = false }
        )
    }
}
