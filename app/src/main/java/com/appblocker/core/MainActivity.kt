package com.appblocker.core

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.appblocker.core.admin.AppDeviceAdminReceiver
import com.appblocker.core.ui.AppBlockerApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                viewModel.refreshProtectionState()
            }
            val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                viewModel.startFilterIfEnabled()
            }
            val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            AppBlockerApp(
                viewModel = viewModel,
                requestDeviceAdmin = {
                    val component = ComponentName(this, AppDeviceAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "AppBlocker needs device admin to block new installs and protect removal until your password is entered.")
                    adminLauncher.launch(intent)
                },
                requestVpn = {
                    val intent = VpnService.prepare(this)
                    if (intent != null) vpnLauncher.launch(intent) else viewModel.startFilterIfEnabled()
                },
                requestOverlay = {
                    if (!Settings.canDrawOverlays(this)) {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    }
                },
                requestBatteryExemption = {
                    val pm = getSystemService(PowerManager::class.java)
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                    }
                }
            )
        }
    }
}
