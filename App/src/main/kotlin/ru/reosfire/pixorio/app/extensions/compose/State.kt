package ru.reosfire.pixorio.app.extensions.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
fun <T> rememberDerived(calculation: () -> T) =
    remember { derivedStateOf(calculation) }
