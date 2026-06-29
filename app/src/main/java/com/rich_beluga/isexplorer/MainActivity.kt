package com.rich_beluga.isexplorer

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.caverock.androidsvg.SVG
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rich_beluga.isexplorer.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val defaultPath = "/storage/emulated/0/"

    private lateinit var leftPanel: FilePanelController
    private lateinit var rightPanel: FilePanelController
    private var activePanel: FilePanelController? = null

    private data class ContextAction(
        val iconRes:  Int,
        val labelRes: Int,
        val actionId: Int,
        val isDanger: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyEdgeToEdge()
        checkStoragePermission()
        setupPanels()
        setupBackNavigation()
    }

    private fun applyEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars     = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, wi ->
            val bars = wi.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.root.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupPanels() {
        leftPanel = FilePanelController(
            recyclerView       = binding.panelLeft.recyclerView,
            pathView           = binding.panelLeft.textPath,
            loadingIndicator   = binding.panelLeft.loadingIndicator,
            clearSelectionChip = binding.panelLeft.chipClearSelection,
            initialPath        = defaultPath,
            scope              = lifecycleScope,
            onActivated        = { setActivePanel(it) },
            onContextMenu      = { view, trigger, files, panel ->
                showContextMenu(view, trigger, files, panel)
            }
        )
        rightPanel = FilePanelController(
            recyclerView       = binding.panelRight.recyclerView,
            pathView           = binding.panelRight.textPath,
            loadingIndicator   = binding.panelRight.loadingIndicator,
            clearSelectionChip = binding.panelRight.chipClearSelection,
            initialPath        = defaultPath,
            scope              = lifecycleScope,
            onActivated        = { setActivePanel(it) },
            onContextMenu      = { view, trigger, files, panel ->
                showContextMenu(view, trigger, files, panel)
            }
        )
        setActivePanel(leftPanel)
    }

    private fun setActivePanel(panel: FilePanelController) {
        activePanel = panel
        leftPanel.setActive(panel === leftPanel)
        rightPanel.setActive(panel === rightPanel)
    }

    private fun targetPanel() = if (activePanel === leftPanel) rightPanel else leftPanel

    private fun isIshakTarget(item: FileItem): Boolean {
        if (item.isParentLink) return false
        return if (item.isDirectory) item.file.name.equals("ishak", ignoreCase = true)
               else item.file.nameWithoutExtension.equals("ishak", ignoreCase = true)
    }

    private fun showContextMenu(
        anchorView:     View,
        triggerItem:    FileItem?,
        filesToOperate: List<File>,
        panel:          FilePanelController
    ) {
        val ishakItem = triggerItem?.takeIf { isIshakTarget(it) }
        if (ishakItem != null) showIshakContextMenu(ishakItem, filesToOperate, panel)
        else                    showNormalContextMenu(triggerItem, filesToOperate, panel)
    }

    private fun showNormalContextMenu(
        triggerItem: FileItem?,
        filesToOperate: List<File>,
        sourcePanel: FilePanelController
    ) {
        val title = if (filesToOperate.size == 1) filesToOperate.first().name
                    else getString(R.string.ctx_menu_title_multiple, filesToOperate.size)

        val actions = listOf(
            ContextAction(R.drawable.ic_action_copy,   R.string.action_copy,     R.id.ctx_copy),
            ContextAction(R.drawable.ic_action_move,   R.string.action_move,     R.id.ctx_move),
            ContextAction(R.drawable.ic_action_delete, R.string.action_delete,   R.id.ctx_delete, isDanger = true),
            ContextAction(R.drawable.ic_action_cancel, R.string.action_deselect, R.id.ctx_deselect)
        )

        showActionsBottomSheet(
            title     = "Selected: {item.file.name}",
            actions   = actions,
            showIshak = false,
            onAction  = { id -> handleNormalMenuAction(id, filesToOperate, sourcePanel) },
            onDismiss = { }
        )
    }

    private fun handleNormalMenuAction(id: Int, files: List<File>, panel: FilePanelController) {
        when (id) {
            R.id.ctx_copy     -> performOperation(files, panel, isMove = false)
            R.id.ctx_move     -> performOperation(files, panel, isMove = true)
            R.id.ctx_delete   -> confirmDelete(files, panel)
            R.id.ctx_deselect -> panel.exitSelectionMode()
        }
    }

    private fun showIshakContextMenu(
        ishakItem: FileItem,
        filesToOperate: List<File>,
        sourcePanel: FilePanelController
    ) {
        val tempPopup = PopupMenu(this, binding.root)
        MenuInflater(this).inflate(R.menu.context_menu_test, tempPopup.menu)
        val menu = tempPopup.menu

        val ishakActions = buildList {
            for (i in 0 until menu.size()) {
                val mi = menu.getItem(i)
                val icon = if (mi.itemId == R.id.ishak_delete) R.drawable.ic_action_delete
                           else R.drawable.ic_ishak_easter
                add(ContextAction(icon, 0, mi.itemId, mi.itemId == R.id.ishak_delete))
            }
            add(ContextAction(R.drawable.ic_action_copy,   R.string.action_copy,     R.id.ctx_copy))
            add(ContextAction(R.drawable.ic_action_move,   R.string.action_move,     R.id.ctx_move))
            add(ContextAction(R.drawable.ic_action_delete, R.string.action_delete,   R.id.ctx_delete, isDanger = true))
            add(ContextAction(R.drawable.ic_action_cancel, R.string.action_deselect, R.id.ctx_deselect))
        }

        val menuTitles = (0 until menu.size()).associate { i ->
            menu.getItem(i).itemId to (menu.getItem(i).title?.toString() ?: "")
        }

        showActionsBottomSheet(
            title      = "Main dev: @rich_beluga\nSelected: ${ishakItem.file.name}",
            actions    = ishakActions,
            showIshak  = true,
            onAction   = { id ->
                if (menuTitles.containsKey(id)) handleIshakMenuAction(id, ishakItem, filesToOperate, sourcePanel)
                else handleNormalMenuAction(id, filesToOperate, sourcePanel)
            },
            onDismiss  = { },
            menuTitles = menuTitles
        )
    }

    private fun handleIshakMenuAction(
        id: Int,
        item: FileItem,
        files: List<File>,
        panel: FilePanelController
    ) {
        when (id) {
            /* Unused strings since v0.5.2-beta (commit #bfd0fb8) */
            R.id.ishak_pet    -> toast("unused string #1")
            R.id.ishak_feed   -> toast("unused string #2")
            R.id.ishak_ride   -> toast("unused string #3")
            R.id.ishak_rename -> toast("unused string #4")
            R.id.ishak_delete -> { confirmDelete(listOf(item.file), panel); return }
        }
        panel.exitSelectionMode()
    }

    private fun showActionsBottomSheet(
        title: String,
        actions: List<ContextAction>,
        showIshak: Boolean,
        onAction: (id: Int) -> Unit,
        onDismiss: () -> Unit,
        menuTitles: Map<Int, String> = emptyMap()
    ) {
        val dialog   = BottomSheetDialog(this)
        val rootView = layoutInflater.inflate(R.layout.dialog_context_menu, null)

        rootView.findViewById<ImageView>(R.id.ivIshakIcon).apply {
            if (showIshak) {
                visibility = View.VISIBLE
                loadIshakEasterIcon(this)
            } else {
                visibility = View.GONE
            }
        }

        rootView.findViewById<TextView>(R.id.tvMenuTitle).text = title

        val grid = rootView.findViewById<GridLayout>(R.id.gridActions)
        grid.removeAllViews()

        for (action in actions) {
            val cell = layoutInflater.inflate(R.layout.item_context_action, grid, false)

            cell.findViewById<ImageView>(R.id.ivActionIcon).apply {
                setImageResource(action.iconRes)
                val tintAttr = if (action.isDanger) android.R.attr.colorError
                               else MaterialR.attr.colorOnSurface
                imageTintList = ColorStateList.valueOf(resolveAttrColor(context, tintAttr))
            }

            cell.findViewById<TextView>(R.id.tvActionLabel).apply {
                text = when {
                    action.labelRes != 0                    -> getString(action.labelRes)
                    menuTitles.containsKey(action.actionId) -> menuTitles[action.actionId] ?: ""
                    else                                    -> ""
                }
                val textAttr = if (action.isDanger) android.R.attr.colorError
                               else MaterialR.attr.colorOnSurface
                setTextColor(resolveAttrColor(context, textAttr))
            }

            cell.setOnClickListener {
                dialog.dismiss()
                onAction(action.actionId)
            }

            grid.addView(cell)
        }

        dialog.setContentView(rootView)
        dialog.setOnCancelListener { onDismiss() }
        dialog.show()
    }

    private fun loadIshakEasterIcon(imageView: ImageView) {
        try {
            val svg = SVG.getFromAsset(assets, "emoji.svg")
            imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            imageView.setImageDrawable(PictureDrawable(svg.renderToPicture()))
        } catch (_: Exception) {
            imageView.setLayerType(View.LAYER_TYPE_NONE, null)
            imageView.setImageResource(R.drawable.ic_ishak_easter)
        }
    }

    private fun performOperation(files: List<File>, panel: FilePanelController, isMove: Boolean) {
        val dest = targetPanel().currentDir
        thread {
            var err: String? = null
            for (f in files) try {
                if (isMove) FileOperationsUtil.move(f, dest)
                else        FileOperationsUtil.copy(f, dest)
            } catch (e: IOException) { err = e.message }
            runOnUiThread {
                leftPanel.refresh()
                rightPanel.refresh()
                panel.exitSelectionMode()
                toast(err ?: if (isMove) getString(R.string.toast_moved)
                             else getString(R.string.toast_copied))
            }
        }
    }

    private fun confirmDelete(files: List<File>, panel: FilePanelController) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_confirm_title, files.size))
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.confirm_delete) { _, _ ->
                thread {
                    var err: String? = null
                    for (f in files) try { FileOperationsUtil.deleteRecursively(f) }
                                      catch (e: IOException) { err = e.message }
                    runOnUiThread {
                        leftPanel.refresh()
                        rightPanel.refresh()
                        panel.exitSelectionMode()
                        toast(err ?: getString(R.string.toast_deleted))
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> }
            .show()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            val panel = activePanel ?: leftPanel
            if (panel.selectionMode) {
                panel.exitSelectionMode()
                return@addCallback
            }
            val parent = panel.currentDir.parentFile
            if (parent != null) panel.loadDirectory(parent)
            else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            toast(getString(R.string.permission_required))
            try {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
