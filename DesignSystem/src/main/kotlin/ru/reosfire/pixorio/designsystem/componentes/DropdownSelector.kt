package ru.reosfire.pixorio.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

data class SelectorOption<T>(
    val title: String,
    val payload: T,
)

@Stable
class DropdownSelectorState<T>(
    val options: List<SelectorOption<T>>,
    val selectedOptionState: MutableState<SelectorOption<T>>,
) {
    val selectedOption by selectedOptionState
}

@Composable
fun <T> rememberDropdownSelectorState(
    options: List<SelectorOption<T>>,
    initiallySelected: SelectorOption<T> = options.first(),
): DropdownSelectorState<T> {
    return remember { DropdownSelectorState(options, mutableStateOf(initiallySelected)) }
}

@Composable
fun <T> DropdownSelector(
    state: DropdownSelectorState<T>,
) {
    var selectedOption by state.selectedOptionState

    val dropdownState = remember { DropdownMenuState(initialStatus = DropdownMenuState.Status.Closed) }

    Column {
        Text(
            text = selectedOption.title,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier
                .clickable { dropdownState.status = DropdownMenuState.Status.Open(Offset.Zero) }
        )

        Box(modifier = Modifier) {
            DropdownMenu(
                dropdownState,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                for (option in state.options) {
                    SelectorOptionView(
                        option = option,
                        isSelected = option == selectedOption,
                        onClick = {
                            dropdownState.status = DropdownMenuState.Status.Closed
                            selectedOption = option
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> SelectorOptionView(
    option: SelectorOption<T>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = option.title,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.background(MaterialTheme.colors.surface) else Modifier)
            .padding(horizontal = 4.dp)
    )
}
