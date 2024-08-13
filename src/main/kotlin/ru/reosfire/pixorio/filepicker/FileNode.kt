package ru.reosfire.pixorio.filepicker

import androidx.compose.runtime.*
import java.io.File
import java.nio.file.Path

data class FileNode(
    val file: File,
    val openedState: MutableState<Boolean> = mutableStateOf(false),
    val children: MutableList<FileNode> = mutableStateListOf(),
) {
    var opened by openedState

    fun toggle() {
        opened = !opened
        if (opened) {
            updateChildren()
        } else {
            clearChildrenRecursive()
        }
    }

    fun tryOpenPath(selectedFile: File) {
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

    private fun clearChildrenRecursive() {
        for (child in children) {
            child.clearChildrenRecursive()
        }

        children.clear()
    }

    private fun updateChildren() {
        file.listFiles()?.let { innerFiles ->
            children.addAll(innerFiles.filter { !it.isHidden }.map { file -> FileNode(file) })
        }
    }
}
