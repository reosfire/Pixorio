package ru.reosfire.pixorio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.application

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
            AppWindow(bitmapSize)
        }
    }
}
