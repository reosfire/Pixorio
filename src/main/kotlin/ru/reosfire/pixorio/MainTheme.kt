package ru.reosfire.pixorio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

fun appDarkColors() = darkColors(
    primary = Color(0xFFFFFFFF),
//    primaryVariant = Color.Black,
//    secondary = Color.Black,
//    secondaryVariant = Color.Black,
    background = Color(0xFF000000),
    surface = Color(0xFF555555),
//    error = Color.Black,
    onPrimary = Color(0xFF000000),
//    onSecondary = Color.Black,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
//    onError = Color.Black,
)

fun appLightColors() = lightColors(
//    primary = Color(0xCEC8D2FF),
//    primaryVariant = Color.Black,
//    secondary = Color.Black,
//    secondaryVariant = Color.Black,
//    background = Color.Black,
    surface = Color(0xFFBBBBBB),
//    error = Color.Black,
//    onPrimary = Color(0x353137FF),
//    onSecondary = Color.Black,
//    onBackground = Color.Black,
//    onSurface = Color.Black,
//    onError = Color.Black,
)

@Composable
fun MainTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) appDarkColors() else appLightColors()

    val textSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.onSurface,
        backgroundColor = MaterialTheme.colors.surface,
    )

    CompositionLocalProvider(
        LocalTextSelectionColors provides textSelectionColors,
        LocalTextStyle provides MaterialTheme.typography.overline,
    ) {
        MaterialTheme(
            colors = colors,
            content = content,
        )
    }
}
