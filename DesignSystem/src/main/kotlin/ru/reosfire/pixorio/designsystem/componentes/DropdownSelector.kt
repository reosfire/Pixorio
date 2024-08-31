package ru.reosfire.pixorio.designsystem.componentes

import androidx.compose.foundation.background
import ru.reosfire.pixorio.designsystem.modifiers.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
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
    modifier: Modifier = Modifier,
) {
    var selectedOption by state.selectedOptionState

    val dropdownState = remember { DropdownMenuState(initialStatus = DropdownMenuState.Status.Closed) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { dropdownState.status = DropdownMenuState.Status.Open(Offset.Zero) }
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = selectedOption.title,
                color = MaterialTheme.colors.onBackground,
            )

            PixelImage(
                bitmap = DOWN_ARROW_BITMAP,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
                modifier = Modifier.size(8.dp),
            )
        }

        Box {
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

private val DOWN_ARROW_BITMAP = useResource("icons/down_arrow.png") { loadImageBitmap(it) }
