package com.rich_beluga.isexplorer

import java.io.File

data class FileItem(
    val file: File,
    val isParentLink: Boolean = false
) {
    val name: String
        get() = if (isParentLink) ".." else file.name

    val isDirectory: Boolean
        get() = isParentLink || file.isDirectory

    val category: FileCategory
        get() = when {
            isParentLink  -> FileCategory.PARENT_DIR
            isDirectory   -> FileCategory.FOLDER
            else          -> FileTypeRegistry.getCategory(file.extension)
        }
}
