package com.appblocker.core.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appblocker.core.admin.DeviceAdminController
import com.appblocker.core.vpn.ContentFilterVpnService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        DeviceAdminController(context).applyProtection()
        ContentFilterVpnService.start(context)
    }
}
