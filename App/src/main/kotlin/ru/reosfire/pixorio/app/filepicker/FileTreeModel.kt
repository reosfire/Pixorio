package ru.reosfire.pixorio.app.filepicker

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileFilter
import java.nio.file.Path

@Stable
class FileTreeModel(
    val filesFilter: FileFilter = FileFilter { true },
    rootFiles: Array<File> = File.listRoots(),
) {
    val nodes = rootFiles.map { FileNode(file = it, root = this) }

    suspend fun tryOpenPath(file: File) {
        nodes.forEach { it.tryOpenPath(file) }
    }
}

@Stable
data class FileNode(
    val file: File,
    val depth: Int = 0,
    val openedState: MutableState<Boolean> = mutableStateOf(false),
    val children: MutableList<FileNode> = mutableStateListOf(),
    private val root: FileTreeModel,
) {
    var opened by openedState

    suspend fun toggle() {
        opened = !opened
        if (opened) {
            updateChildren()
        } else {
            clearChildrenRecursive()
        }
    }

    suspend fun tryOpenPath(selectedFile: File) {
        val nodePath = file.toPath()
        val selectedFilePath = selectedFile.toPath()

        if (nodePath.root != selectedFilePath.root) return

        val pathTrace = mutableListOf<Path>(nodePath.root)

        selectedFilePath.forEach {
            pathTrace.add(pathTrace.last().resolve(it))
        }

        var currentNode = this

        for (i in 0..<pathTrace.size) {
            if (!currentNode.opened) {
                currentNode.updateChildren()
                currentNode.opened = true
            }

            if (i + 1 < pathTrace.size) {
                currentNode = currentNode.children.find { it.file.toPath() == pathTrace[i + 1] } ?: break
            }
        }
    }

    private suspend fun clearChildrenRecursive() {
        coroutineScope {
            val jobs = children.map { node ->
                launch {
                    node.clearChildrenRecursive()
                }
            }

            jobs.joinAll()

            children.clear()
        }
    }

    private suspend fun updateChildren() {
        withContext(Dispatchers.IO) {
            file.listFiles(root.filesFilter)?.let { innerFiles ->
                children.addAll(innerFiles.map { file -> FileNode(file = file, depth = depth + 1, root = root) })
            }
        }
    }
}
