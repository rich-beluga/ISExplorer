package com.rich_beluga.isexplorer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.rich_beluga.isexplorer.databinding.ActivityMainBinding
import java.io.IOException
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val defaultPath = "/storage/emulated/0/"

    private lateinit var leftPanel: FilePanelController
    private lateinit var rightPanel: FilePanelController

    private var activePanel: FilePanelController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        checkStoragePermission()
        setupPanels()
        setupActionChips()
        setupBackNavigation()
    }

    private fun setupPanels() {
        leftPanel = FilePanelController(
            recyclerView    = binding.panelLeft.recyclerView,
            pathView        = binding.panelLeft.textPath,
            loadingIndicator = binding.panelLeft.loadingIndicator,
            initialPath     = defaultPath,
            scope           = lifecycleScope,
            onActivated     = { panel -> setActivePanel(panel) }
        )

        rightPanel = FilePanelController(
            recyclerView    = binding.panelRight.recyclerView,
            pathView        = binding.panelRight.textPath,
            loadingIndicator = binding.panelRight.loadingIndicator,
            initialPath     = defaultPath,
            scope           = lifecycleScope,
            onActivated     = { panel -> setActivePanel(panel) }
        )

        setActivePanel(leftPanel)
    }

    private fun setActivePanel(panel: FilePanelController) {
        activePanel = panel
        leftPanel.setActive(panel === leftPanel)
        rightPanel.setActive(panel === rightPanel)
    }

    private fun targetPanel(): FilePanelController =
        if (activePanel === leftPanel) rightPanel else leftPanel

    private fun setupActionChips() {
        binding.chipCopy.setOnClickListener   { performFileOperation(isMove = false) }
        binding.chipMove.setOnClickListener   { performFileOperation(isMove = true)  }
        binding.chipDelete.setOnClickListener { confirmDelete() }
        binding.chipCancel.setOnClickListener {
            leftPanel.exitSelectionMode()
            rightPanel.exitSelectionMode()
        }
    }


    private fun performFileOperation(isMove: Boolean) {
        val source = activePanel ?: return
        val selectedFiles = source.getSelectedFiles()
        if (selectedFiles.isEmpty()) {
            toast(getString(R.string.toast_select_first))
            return
        }
        val destinationDir = targetPanel().currentDir

        thread {
            var error: String? = null
            for (file in selectedFiles) {
                try {
                    if (isMove) FileOperationsUtil.move(file, destinationDir)
                    else FileOperationsUtil.copy(file, destinationDir)
                } catch (e: IOException) {
                    error = e.message
                }
            }
            runOnUiThread {
                leftPanel.refresh()
                rightPanel.refresh()
                toast(error ?: if (isMove) getString(R.string.toast_moved)
                               else getString(R.string.toast_copied))
            }
        }
    }

    private fun confirmDelete() {
        val source = activePanel ?: return
        val selectedFiles = source.getSelectedFiles()
        if (selectedFiles.isEmpty()) {
            toast(getString(R.string.toast_select_first))
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirm_title, selectedFiles.size))
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.confirm_delete) { _, _ ->
                thread {
                    var error: String? = null
                    for (file in selectedFiles) {
                        try {
                            FileOperationsUtil.deleteRecursively(file)
                        } catch (e: IOException) {
                            error = e.message
                        }
                    }
                    runOnUiThread {
                        leftPanel.refresh()
                        rightPanel.refresh()
                        toast(error ?: getString(R.string.toast_deleted))
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            val panel  = activePanel ?: leftPanel
            val parent = panel.currentDir.parentFile
            if (parent != null) {
                panel.loadDirectory(parent)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            toast(getString(R.string.permission_required))
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
