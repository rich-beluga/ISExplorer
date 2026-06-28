package com.rich_beluga.isexplorer

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class DragSelectTouchHelper : RecyclerView.OnItemTouchListener {

    var isDragSelecting = false
        private set

    var startPosition: Int = RecyclerView.NO_POSITION
        private set

    private var lastPosition: Int = RecyclerView.NO_POSITION

    var onRangeSelect: ((start: Int, end: Int) -> Unit)? = null

    var onDragEnd: (() -> Unit)? = null

    fun startDragSelect(position: Int) {
        isDragSelecting = true
        startPosition  = position
        lastPosition   = position
    }

    fun cancel() {
        isDragSelecting = false
        startPosition  = RecyclerView.NO_POSITION
        lastPosition   = RecyclerView.NO_POSITION
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isDragSelecting) return false

        return when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                updateSelection(rv, e)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endDrag()
                false
            }
            else -> true
        }
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        when (e.action) {
            MotionEvent.ACTION_MOVE  -> updateSelection(rv, e)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> endDrag()
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }

    private fun updateSelection(rv: RecyclerView, e: MotionEvent) {
        val child = rv.findChildViewUnder(e.x, e.y) ?: return
        val pos   = rv.getChildAdapterPosition(child)
        if (pos == RecyclerView.NO_POSITION || pos == lastPosition) return

        lastPosition = pos
        onRangeSelect?.invoke(startPosition, pos)
    }

    private fun endDrag() {
        if (isDragSelecting) {
            isDragSelecting = false
            onDragEnd?.invoke()
        }
    }
}
