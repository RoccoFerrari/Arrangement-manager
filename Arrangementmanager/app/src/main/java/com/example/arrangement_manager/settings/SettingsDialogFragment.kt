package com.example.arrangement_manager.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.arrangement_manager.R

/**
 * A DialogFragment for managing app settings.
 *
 * This fragment provides a user interface to control app permissions, specifically
 * the notification permission on Android 13 (Tiramisu) and above.
 */
class SettingsDialogFragment : DialogFragment(R.layout.dialog_settings) {

    /**
     * An ActivityResultLauncher for requesting the POST_NOTIFICATIONS permission.
     *
     * The callback handles the result of the permission request. If granted, the switch is
     * turned on. If denied, the switch is turned off.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val notificationSwitch = dialog?.findViewById<Switch>(R.id.settings_switch)
            notificationSwitch?.isChecked = true
        } else {
            val notificationSwitch = dialog?.findViewById<Switch>(R.id.settings_switch)
            notificationSwitch?.isChecked = false
        }
    }

    /**
     * Called when the fragment is visible to the user and the dialog is displayed.
     *
     * Sets the width of the dialog to be 75% of the screen width for better display on
     * various devices.
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val newWidth = (width * 0.75).toInt()
            window.setLayout(newWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notificationSwitch = view.findViewById<Switch>(R.id.settings_switch)

        // Set the initial state of the switch based on notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationSwitch.isChecked = hasNotificationPermission()
        } else {
            notificationSwitch.isChecked = true
        }

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // If the user turns the switch on, request permission if needed
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!hasNotificationPermission()) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                // If the user turns the switch off, open app settings for manual deactivation
                // as the permission can't be revoked programmatically
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationSwitch.isChecked = true
                    openAppSettings()
                }
            }
        }
    }

    /**
     * Checks if the app has the POST_NOTIFICATIONS permission.
     *
     * This method is only available on Android 13 (Tiramisu) and above.
     * @return `true` if the permission is granted, `false` otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Opens the app's settings screen.
     *
     * This allows the user to manually manage permissions, notifications, and other
     * app-specific settings.
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

}