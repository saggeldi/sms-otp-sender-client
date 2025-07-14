package com.gonodono.smssender

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.gonodono.smssender.ui.SmsScreen
import com.waseemsabir.betterypermissionhelper.BatteryPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val batteryPermissionHelper = BatteryPermissionHelper.getInstance()
    // List of required permissions
    private val smsPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else {
        arrayOf(
            Manifest.permission.SEND_SMS,
        )
    }
    private val context: Context = this
    val viewModel: MainViewModel by viewModels()
    private val socketStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status") ?: return
            viewModel.changeStatus(status)
        }
    }

    private var permissionsGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Check for permissions and request them if not already granted
        val requestPermissionsLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { permissions ->
                if (permissions.all { it.value }) {
                    permissionsGranted = true
                    // Delay starting service to ensure permission state is fully updated
                    lifecycleScope.launch {
                        delay(500) // Small delay to ensure system state is updated
                        startSmsService()
                    }
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
            permissionsGranted = true
            startSmsService()
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            socketStatusReceiver,
            IntentFilter("SOCKET_STATUS")
        )

        setContent {
            SmsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permissions were granted but service hasn't started yet
        if (permissionsGranted && !isServiceRunning()) {
            lifecycleScope.launch {
                delay(1000) // Give system time to settle after returning from settings
                startSmsService()
            }
        }
    }

    private fun startSmsService() {
        // Double check permissions before starting service
        if (!permissionsGranted || smsPermissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            return
        }

        // Handle battery optimization in background without blocking service startup
        lifecycleScope.launch {
            handleBatteryOptimization()
        }

        // Start the service immediately
        try {
            val serviceIntent = Intent(this, SmsForegroundService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            // Log error or handle service startup failure
            e.printStackTrace()
        }
    }

    private suspend fun handleBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            delay(2000) // Wait 2 seconds before showing battery optimization dialog

            val intentSettings = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            with(intentSettings) {
                data = Uri.fromParts("package", context.packageName, null)
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }

            try {
                context.startActivity(intentSettings)
            } catch (e: Exception) {
                // Handle case where battery optimization settings are not available
                e.printStackTrace()
            }
        }
    }

    private fun isServiceRunning(): Boolean {
        return try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Integer.MAX_VALUE).any {
                it.service.className == SmsForegroundService::class.java.name
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun showPermissionDeniedMessage() {
        // Show a message to the user explaining why permissions are necessary
        // You can use a dialog, Toast, or Snackbar here
    }

    private fun stopSmsService() {
        val serviceIntent = Intent(this, SmsForegroundService::class.java)
        stopService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(socketStatusReceiver)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}