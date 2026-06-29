package com.rich_beluga.isexplorer

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
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
    private val clearSelectionChip: Chip,
    initialPath: String,
    private val scope: CoroutineScope,
    private val onActivated: (FilePanelController) -> Unit,
    private val onContextMenu: (
        anchorView: View,
        triggerItem: FileItem?,
        filesToOperate: List<File>,
        panel: FilePanelController
    ) -> Unit
) {

    companion object {
        private const val MIN_LOADING_VISIBLE_MS = 350L
    }

    var currentDir: File = File("/storage/emulated/0/")
        private set

    var selectionMode: Boolean = false
        private set

    private val adapter = FileAdapter(
        onItemClick     = { item       -> handleItemClick(item) },
        onItemLongClick = { view, item -> handleItemLongClick(view, item) }
    )

    private val swipeCallback = object : ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val pos  = viewHolder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return
            val item = adapter.getItemAt(pos) ?: return

            if (item.isParentLink) {
                adapter.notifyItemChanged(pos)
                return
            }

            onActivated(this@FilePanelController)

            if (!selectionMode) {
                selectionMode = true
                adapter.setSelectionModeEnabled(true)
            }
            adapter.toggleSelection(item)

            if (!adapter.isAnySelected()) {
                exitSelectionMode()
            }

            adapter.notifyItemChanged(pos)
            updateClearChip()
        }

        override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
            val pos  = vh.bindingAdapterPosition
            val item = if (pos != RecyclerView.NO_POSITION) adapter.getItemAt(pos) else null
            if (item?.isParentLink == true) return 0
            return makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.2f

        override fun getSwipeEscapeVelocity(defaultValue: Float) = defaultValue * 2.5f

        override fun onChildDraw(
            c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
            dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                val fraction = (kotlin.math.abs(dX) / vh.itemView.width).coerceIn(0f, 1f)
                vh.itemView.alpha = 1f - fraction * 0.35f
                super.onChildDraw(c, rv, vh, dX * 0.55f, dY, actionState, isCurrentlyActive)
            } else {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }

        override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
            super.clearView(rv, vh)
            vh.itemView.alpha = 1f
        }
    }

    init {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = adapter

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.top    = 2.dp
                outRect.bottom = 2.dp
            }
        })

        pathView.setOnClickListener { onActivated(this) }

        clearSelectionChip.setOnClickListener { exitSelectionMode() }
        clearSelectionChip.visibility = View.GONE

        loadDirectory(File("/storage/emulated/0/").also { currentDir = it })
    }

    fun loadDirectory(dir: File) {
        val targetDir = if (dir.isDirectory) dir else dir.parentFile ?: dir
        currentDir    = targetDir
        pathView.text = currentDir.absolutePath

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
            if (!item.isParentLink) {
                adapter.toggleSelection(item)
                if (!adapter.isAnySelected()) exitSelectionMode()
                else updateClearChip()
            }
            return
        }
        when {
            item.isParentLink -> loadDirectory(currentDir.parentFile ?: currentDir)
            item.isDirectory  -> loadDirectory(item.file)
        }
    }

    private fun handleItemLongClick(anchorView: View, item: FileItem): Boolean {
        if (item.isParentLink) return false
        onActivated(this)

        val isSelected = adapter.isItemSelected(item)

        if (isSelected) {
            val files = adapter.getSelectedItems().map { it.file }
            onContextMenu(anchorView, null, files, this)
        } else {
            onContextMenu(anchorView, item, listOf(item.file), this)
        }
        return true
    }

    fun exitSelectionMode() {
        selectionMode = false
        adapter.setSelectionModeEnabled(false)
        adapter.clearSelection()
        clearSelectionChip.visibility = View.GONE
    }

    fun getSelectedFiles(): List<File> = adapter.getSelectedItems().map { it.file }

    private fun updateClearChip() {
        clearSelectionChip.visibility =
            if (adapter.isAnySelected()) View.VISIBLE else View.GONE
    }

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
        val dirs  = children.filter  { it.isDirectory  }.sortedBy { it.name.lowercase() }.map { FileItem(it) }
        val files = children.filter  { !it.isDirectory }.sortedBy { it.name.lowercase() }.map { FileItem(it) }
        return buildList {
            dir.parentFile?.let { add(FileItem(dir, isParentLink = true)) }
            addAll(dirs)
            addAll(files)
        }
    }
}
