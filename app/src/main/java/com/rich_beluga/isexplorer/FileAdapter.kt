package com.rich_beluga.isexplorer

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.R as MaterialR
import java.text.DateFormat
import java.util.Date

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,

    private val onItemLongClick: (anchorView: View, item: FileItem, position: Int) -> Boolean
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    private val items         = mutableListOf<FileItem>()
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

    fun setRangeSelected(start: Int, end: Int) {
        val lo = minOf(start, end).coerceAtLeast(0)
        val hi = maxOf(start, end).coerceAtMost(items.size - 1)
        selectedItems.clear()
        for (i in lo..hi) {
            val it = items.getOrNull(i) ?: continue
            if (!it.isParentLink) selectedItems.add(it)
        }
        notifyDataSetChanged()
    }

    fun setItemSelected(position: Int, selected: Boolean) {
        val item = items.getOrNull(position) ?: return
        if (item.isParentLink) return
        if (selected) selectedItems.add(item) else selectedItems.remove(item)
        notifyItemChanged(position)
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<FileItem> = selectedItems.toList()

    fun isAnySelected(): Boolean = selectedItems.isNotEmpty()

    fun getItemAt(position: Int): FileItem? = items.getOrNull(position)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView  = view as MaterialCardView
        val iconContainer: FrameLayout = view.findViewById(R.id.iconContainer)
        val imageIcon: ImageView    = view.findViewById(R.id.imageIcon)
        val textName: TextView      = view.findViewById(R.id.textName)
        val textInfo: TextView      = view.findViewById(R.id.textInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item     = items[position]
        val ctx      = holder.itemView.context
        val category = item.category

        holder.textName.text = item.name
        holder.textInfo.text = when {
            item.isParentLink -> ctx.getString(R.string.label_parent_dir)
            item.isDirectory  -> ctx.getString(R.string.label_folder)
            else              -> buildFileInfoString(item)
        }

        val containerColor   = resolveAttrColor(ctx, category.containerColorAttr)
        val onContainerColor = resolveAttrColor(ctx, category.onContainerColorAttr)
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(containerColor)
        holder.imageIcon.setImageResource(category.iconRes)
        holder.imageIcon.imageTintList = ColorStateList.valueOf(onContainerColor)

        val isSelected = selectionModeEnabled && !item.isParentLink && selectedItems.contains(item)

        val targetBgAttr = if (isSelected) MaterialR.attr.colorSecondaryContainer
                           else             MaterialR.attr.colorSurfaceContainerLow
        val targetBg     = resolveAttrColor(ctx, targetBgAttr)
        val targetStroke = if (isSelected) resolveAttrColor(ctx, MaterialR.attr.colorSecondary)
                           else            0x00000000

        val currentBg = holder.card.cardBackgroundColor?.defaultColor
            ?: resolveAttrColor(ctx, MaterialR.attr.colorSurfaceContainerLow)

        if (currentBg != targetBg) {
            ValueAnimator.ofArgb(currentBg, targetBg).apply {
                duration = 120
                addUpdateListener { holder.card.setCardBackgroundColor(it.animatedValue as Int) }
                start()
            }
        } else {
            holder.card.setCardBackgroundColor(targetBg)
        }
        holder.card.strokeColor = targetStroke

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener { view ->

            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnLongClickListener false
            onItemLongClick(view, item, pos)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun buildFileInfoString(item: FileItem): String {
        val bytes = item.file.length()
        val sizeText = when {
            bytes < 1_024         -> "$bytes Б"
            bytes < 1_048_576     -> "%.1f КБ".format(bytes / 1_024.0)
            bytes < 1_073_741_824 -> "%.1f МБ".format(bytes / 1_048_576.0)
            else                  -> "%.2f ГБ".format(bytes / 1_073_741_824.0)
        }
        val dateText = DateFormat.getDateInstance(DateFormat.SHORT)
            .format(Date(item.file.lastModified()))
        return "$sizeText • $dateText"
    }
}
