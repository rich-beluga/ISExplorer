package com.rich_beluga.isexplorer

import android.animation.ValueAnimator
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FilePanelController(
    private val recyclerView: RecyclerView,
    private val pathView: TextView,
    private val loadingIndicator: LoadingIndicator,
    initialPath: String,
    private val scope: CoroutineScope,
    private val onActivated: (FilePanelController) -> Unit,
    private val onContextMenu: (anchorView: View, item: FileItem, panel: FilePanelController) -> Unit
) {

    companion object {
        private const val MIN_LOADING_VISIBLE_MS = 350L
    }

    var currentDir: File = File(initialPath)
        private set

    var selectionMode: Boolean = false
        private set

    private val dragHelper = DragSelectTouchHelper().also { helper ->
        helper.onRangeSelect = { start, end ->
            adapter.setRangeSelected(start, end)
        }
        helper.onDragEnd = {
        }
    }

    private val adapter = FileAdapter(
        onItemClick     = { item           -> handleItemClick(item) },
        onItemLongClick = { view, item, pos -> handleItemLongClick(view, item, pos) }
    )

    init {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter        = adapter

        recyclerView.addOnItemTouchListener(dragHelper)

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) { outRect.top = 2.dp; outRect.bottom = 2.dp }
        })

        pathView.setOnClickListener { onActivated(this) }
        loadDirectory(currentDir)
    }

    fun loadDirectory(dir: File) {
        val targetDir = if (dir.isDirectory) dir else dir.parentFile ?: dir
        currentDir    = targetDir
        pathView.text = currentDir.absolutePath

        dragHelper.cancel()

        recyclerView.alpha      = 0f
        recyclerView.visibility = View.INVISIBLE
        loadingIndicator.visibility = View.VISIBLE

        scope.launch {
            val startMs = System.currentTimeMillis()
            val items   = withContext(Dispatchers.IO) { buildFileList(targetDir) }

            val elapsed = System.currentTimeMillis() - startMs
            if (elapsed < MIN_LOADING_VISIBLE_MS) delay(MIN_LOADING_VISIBLE_MS - elapsed)

            loadingIndicator.visibility = View.GONE
            adapter.submitList(items)
            exitSelectionMode()

            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(
                recyclerView.context, R.anim.layout_animation_slide_in
            )
            recyclerView.scheduleLayoutAnimation()
            recyclerView.animate().alpha(1f).setDuration(180).start()
        }
    }

    fun refresh() = loadDirectory(currentDir)

    private fun handleItemClick(item: FileItem) {
        onActivated(this)
        if (selectionMode) {
            if (!item.isParentLink) adapter.toggleSelection(item)
            if (!adapter.isAnySelected()) exitSelectionMode()
            return
        }
        when {
            item.isParentLink -> loadDirectory(currentDir.parentFile ?: currentDir)
            item.isDirectory  -> loadDirectory(item.file)
        }
    }

    private fun handleItemLongClick(anchorView: View, item: FileItem, position: Int): Boolean {
        if (item.isParentLink) return false
        onActivated(this)

        if (!selectionMode) {
            selectionMode = true
            adapter.setSelectionModeEnabled(true)
        }
        adapter.setItemSelected(position, true)

        dragHelper.startDragSelect(position)

        onContextMenu(anchorView, item, this)
        return true
    }

    fun exitSelectionMode() {
        selectionMode = false
        dragHelper.cancel()
        adapter.setSelectionModeEnabled(false)
        adapter.clearSelection()
    }

    fun getSelectedFiles(): List<File> = adapter.getSelectedItems().map { it.file }

    fun setActive(active: Boolean) {
        val ctx = pathView.context
        val currentColor = (pathView.background as? android.graphics.drawable.ColorDrawable)?.color
            ?: resolveAttrColor(ctx, MaterialR.attr.colorSurfaceContainerHigh)
        val targetColor = resolveAttrColor(
            ctx, if (active) MaterialR.attr.colorPrimaryContainer
                 else MaterialR.attr.colorSurfaceContainerHigh
        )
        ValueAnimator.ofArgb(currentColor, targetColor).apply {
            duration = 220
            addUpdateListener { anim ->
                pathView.setBackgroundColor(anim.animatedValue as Int)
                val textAttr = if (active) MaterialR.attr.colorOnPrimaryContainer
                               else MaterialR.attr.colorOnSurface
                pathView.setTextColor(resolveAttrColor(ctx, textAttr))
            }
            start()
        }
    }

    private fun buildFileList(dir: File): List<FileItem> {
        val children = dir.listFiles()?.toList() ?: emptyList()
        val dirs  = children.filter  { it.isDirectory }.sortedBy { it.name.lowercase() }.map { FileItem(it) }
        val files = children.filter  { !it.isDirectory }.sortedBy { it.name.lowercase() }.map { FileItem(it) }
        return buildList {
            dir.parentFile?.let { add(FileItem(dir, isParentLink = true)) }
            addAll(dirs)
            addAll(files)
        }
    }
}
