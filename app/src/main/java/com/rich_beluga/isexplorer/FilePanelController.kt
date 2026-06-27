package com.rich_beluga.isexplorer

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Rect
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.loadingindicator.LoadingIndicator
import com.google.android.material.R as MaterialR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FilePanelController(
    private val recyclerView: RecyclerView,
    private val pathView: TextView,
    private val loadingIndicator: LoadingIndicator,
    initialPath: String,
    private val scope: CoroutineScope,
    private val onActivated: (FilePanelController) -> Unit
) {

    var currentDir: File = File(initialPath)
        private set

    var selectionMode: Boolean = false
        private set

    private val adapter = FileAdapter(
        onItemClick = { item -> handleItemClick(item) },
        onItemLongClick = { item -> handleItemLongClick(item) }
    )

    init {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = adapter

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) {
                outRect.top = 2.dp
                outRect.bottom = 2.dp
            }
        })

        pathView.setOnClickListener { onActivated(this) }

        loadDirectory(currentDir)
    }

    fun loadDirectory(dir: File) {
        val targetDir = if (dir.isDirectory) dir else dir.parentFile ?: dir
        currentDir = targetDir
        pathView.text = currentDir.absolutePath

        loadingIndicator.visibility = View.VISIBLE
        recyclerView.alpha = 0f
        recyclerView.visibility = View.INVISIBLE

        scope.launch {
            val items = withContext(Dispatchers.IO) {
                buildFileList(targetDir)
            }

            adapter.submitList(items)

            loadingIndicator.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            val layoutAnim = AnimationUtils.loadLayoutAnimation(
                recyclerView.context,
                R.anim.layout_animation_slide_in
            )
            recyclerView.layoutAnimation = layoutAnim
            recyclerView.scheduleLayoutAnimation()

            recyclerView.animate()
                .alpha(1f)
                .setDuration(180)
                .start()

            exitSelectionMode()
        }
    }

    fun refresh() = loadDirectory(currentDir)

    private fun handleItemClick(item: FileItem) {
        onActivated(this)

        if (selectionMode) {
            adapter.toggleSelection(item)
            return
        }
        when {
            item.isParentLink -> loadDirectory(currentDir.parentFile ?: currentDir)
            item.isDirectory  -> loadDirectory(item.file)
        }
    }

    private fun handleItemLongClick(item: FileItem): Boolean {
        if (item.isParentLink) return false
        onActivated(this)
        if (!selectionMode) {
            selectionMode = true
            adapter.setSelectionModeEnabled(true)
        }
        adapter.toggleSelection(item)
        return true
    }

    fun exitSelectionMode() {
        selectionMode = false
        adapter.setSelectionModeEnabled(false)
        adapter.clearSelection()
    }

    fun getSelectedFiles(): List<File> = adapter.getSelectedItems().map { it.file }

    fun setActive(active: Boolean) {
        val ctx = pathView.context
        val currentColor = (pathView.background as? android.graphics.drawable.ColorDrawable)?.color
            ?: resolveAttrColor(ctx, MaterialR.attr.colorSurfaceContainerHigh)

        val targetAttr = if (active)
            MaterialR.attr.colorPrimaryContainer
        else
            MaterialR.attr.colorSurfaceContainerHigh

        val targetColor = resolveAttrColor(ctx, targetAttr)

        ValueAnimator.ofArgb(currentColor, targetColor).apply {
            duration = 220
            addUpdateListener { anim ->
                pathView.setBackgroundColor(anim.animatedValue as Int)
                val textAttr = if (active)
                    MaterialR.attr.colorOnPrimaryContainer
                else
                    MaterialR.attr.colorOnSurface
                pathView.setTextColor(resolveAttrColor(ctx, textAttr))
            }
            start()
        }
    }

    private fun buildFileList(dir: File): List<FileItem> {
        val children = dir.listFiles()?.toList() ?: emptyList()

        val dirs  = children.filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }
            .map { FileItem(it) }

        val files = children.filter { !it.isDirectory }
            .sortedBy { it.name.lowercase() }
            .map { FileItem(it) }

        val result = mutableListOf<FileItem>()
        dir.parentFile?.let { result.add(FileItem(dir, isParentLink = true)) }
        result.addAll(dirs)
        result.addAll(files)
        return result
    }
}
