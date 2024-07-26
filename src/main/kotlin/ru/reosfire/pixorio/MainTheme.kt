package ru.reosfire.pixorio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun appDarkColors() = darkColors(
    primary = Color(0xFFCEC8D2),
//    primaryVariant = Color.Black,
//    secondary = Color.Black,
//    secondaryVariant = Color.Black,
//    background = Color.Black,
    surface = Color(0xFF252025),
//    error = Color.Black,
    onPrimary = Color(0xFF353137),
//    onSecondary = Color.Black,
//    onBackground = Color.Black,
    onSurface = Color(0xFFCEC8D2),
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

    MaterialTheme(
        colors = colors,
        content = content,
    )
}
