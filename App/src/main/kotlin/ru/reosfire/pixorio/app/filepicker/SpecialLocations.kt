package ru.reosfire.pixorio.app.filepicker

import androidx.compose.foundation.background
import ru.reosfire.pixorio.designsystem.modifiers.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import ru.reosfire.pixorio.designsystem.componentes.PixelImage
import java.io.File

data class SpecialLocationUIData(
    val file: File,
    val icon: ImageBitmap,
    val id: Int,
)

@Composable
fun SpecialLocations(
    locations: List<SpecialLocationUIData>,
    selectedFileState: State<File?>,
    onSelected: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    remember {  }
    LazyRow(modifier = modifier) {
        items(locations, key = { it.id }) {
            PixelImage(
                bitmap = it.icon,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        onSelected(it.file)
                    }
                    .then(if (it.file == selectedFileState.value) Modifier.background(MaterialTheme.colors.surface) else Modifier)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}

private val DESKTOP_BITMAP = useResource("icons/filepicker/desktop.png") { loadImageBitmap(it) }
private val HOME_BITMAP = useResource("icons/filepicker/home.png") { loadImageBitmap(it) }
private val PICTURES_BITMAP = useResource("icons/filepicker/pictures.png") { loadImageBitmap(it) }

private fun buildSpecialLocations(): List<SpecialLocationUIData> {
    val result = mutableListOf<SpecialLocationUIData>()

    var id = 0

    fun tryAdd(file: File, icon: ImageBitmap) {
        if (file.exists()) result.add(SpecialLocationUIData(file, icon, id++))
    }

    // Home
    val homeFile = File(System.getProperty("user.home"))
    tryAdd(homeFile, HOME_BITMAP)

    // Desktop
    val desktopFile = File("${homeFile.path}${File.separator}Desktop")
    tryAdd(desktopFile, DESKTOP_BITMAP)

    // Pictures
    val picturesFile = File("${homeFile.path}${File.separator}Pictures")
    tryAdd(picturesFile, PICTURES_BITMAP)

    return result
}

val SPECIAL_LOCATIONS = buildSpecialLocations()
