package com.rich_beluga.isexplorer

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Date

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Boolean
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    private val items = mutableListOf<FileItem>()
    private val selectedItems = mutableSetOf<FileItem>()
    private var selectionModeEnabled = false

    fun submitList(newItems: List<FileItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setSelectionModeEnabled(enabled: Boolean) {
        selectionModeEnabled = enabled
        notifyDataSetChanged()
    }

    fun toggleSelection(item: FileItem) {
        if (!selectedItems.add(item)) selectedItems.remove(item)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<FileItem> = selectedItems.toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: FrameLayout = view.findViewById(R.id.iconContainer)
        val imageIcon: ImageView = view.findViewById(R.id.imageIcon)
        val textName: TextView = view.findViewById(R.id.textName)
        val textInfo: TextView = view.findViewById(R.id.textInfo)
        val checkSelect: CheckBox = view.findViewById(R.id.checkSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val category = item.category

        holder.textName.text = item.name

        holder.textInfo.text = when {
            item.isParentLink  -> ctx.getString(R.string.label_parent_dir)
            item.isDirectory   -> ctx.getString(R.string.label_folder)
            else               -> buildFileInfoString(item)
        }

        val containerColor = resolveAttrColor(ctx, category.containerColorAttr)
        val onContainerColor = resolveAttrColor(ctx, category.onContainerColorAttr)

        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(containerColor)
        holder.imageIcon.setImageResource(category.iconRes)
        holder.imageIcon.imageTintList = ColorStateList.valueOf(onContainerColor)

        val shouldShowCheckbox = selectionModeEnabled && !item.isParentLink
        if (shouldShowCheckbox && holder.checkSelect.visibility != View.VISIBLE) {
            holder.checkSelect.visibility = View.VISIBLE
            holder.checkSelect.scaleX = 0f
            holder.checkSelect.scaleY = 0f
            holder.checkSelect.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(150)
                .start()
        } else if (!shouldShowCheckbox) {
            holder.checkSelect.visibility = View.GONE
        }
        holder.checkSelect.isChecked = selectedItems.contains(item)

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener { onItemLongClick(item) }
    }

    override fun getItemCount(): Int = items.size

    private fun buildFileInfoString(item: FileItem): String {
        val bytes = item.file.length()
        val sizeText = when {
            bytes < 1024 -> "$bytes Б"
            bytes < 1024 * 1024 -> "%.1f КБ".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f МБ".format(bytes / 1024.0 / 1024)
            else -> "%.2f ГБ".format(bytes / 1024.0 / 1024 / 1024)
        }
        val dateText = DateFormat.getDateInstance(DateFormat.SHORT)
            .format(Date(item.file.lastModified()))
        return "$sizeText • $dateText"
    }
}
