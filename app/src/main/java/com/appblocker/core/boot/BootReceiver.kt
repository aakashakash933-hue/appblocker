package com.appblocker.core.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appblocker.core.admin.DeviceAdminController
import com.appblocker.core.vpn.ContentFilterVpnService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var adminController: DeviceAdminController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        if (adminController.isDeviceOwner()) {
            try {
                adminController.applyProtection()
            } catch (e: SecurityException) {
                android.util.Log.e("BootReceiver", "Failed to apply protection on boot", e)
            }
        }
        ContentFilterVpnService.start(context)
    }
}
