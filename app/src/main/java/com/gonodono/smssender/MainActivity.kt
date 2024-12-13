package com.gonodono.smssender

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gonodono.smssender.ui.SmsScreen
import com.waseemsabir.betterypermissionhelper.BatteryPermissionHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val batteryPermissionHelper = BatteryPermissionHelper.getInstance()
    // List of required permissions
    private val smsPermissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    private val context: Context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for permissions and request them if not already granted
        val requestPermissionsLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { permissions ->
                if (permissions.all { it.value }) {
                    startSmsService()
                } else {
                    // Handle case where permissions are denied
                    showPermissionDeniedMessage()
                }
            }

        if (smsPermissions.any {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
            requestPermissionsLauncher.launch(smsPermissions)
        } else {
            startSmsService()
        }


        setContent {
            val viewModel: MainViewModel by viewModels()
            SmsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
    }

    private fun startSmsService() {

//        val isBatteryPermissionAvailable = batteryPermissionHelper.isBatterySaverPermissionAvailable(context = context, onlyIfSupported = true)
//        if(!isBatteryPermissionAvailable) {
//            batteryPermissionHelper.getPermission(this, open = true, newTask = true)
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intentSettings = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            with(intentSettings) {
                data = Uri.fromParts("package", context.packageName, null)
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            context.startActivity(intentSettings)
        }


        val serviceIntent = Intent(this, SmsForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun showPermissionDeniedMessage() {
        // Show a message to the user explaining why permissions are necessary
        // You can use a dialog, Toast, or Snackbar here
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        stopSmsService()
//    }

    private fun stopSmsService() {
        val serviceIntent = Intent(this, SmsForegroundService::class.java)
        stopService(serviceIntent)
    }
}
