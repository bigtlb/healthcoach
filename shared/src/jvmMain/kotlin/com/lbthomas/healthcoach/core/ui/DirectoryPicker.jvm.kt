package com.lbthomas.healthcoach.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.EventQueue
import javax.swing.JFileChooser
import javax.swing.UIManager

@Composable
actual fun rememberDirectoryPicker(onDirectorySelected: (String) -> Unit): () -> Unit {
    return remember(onDirectorySelected) {
        {
            EventQueue.invokeLater {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                } catch (_: Throwable) {
                    // Fallback to default L&F
                }
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Select Sync Folder"
                    isAcceptAllFileFilterUsed = false
                    isFileHidingEnabled = false // Allow selecting and navigating hidden (.*) directories
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                    onDirectorySelected(chooser.selectedFile.absolutePath)
                }
            }
        }
    }
}
