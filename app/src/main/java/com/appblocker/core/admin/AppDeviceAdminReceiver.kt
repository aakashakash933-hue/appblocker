package com.appblocker.core.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.appblocker.core.R

class AppDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        DeviceAdminController(context).applyProtection()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.device_admin_disable_warning)
}
