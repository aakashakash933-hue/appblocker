package com.appblocker.core.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceAdminController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    val componentName: ComponentName = ComponentName(context, AppDeviceAdminReceiver::class.java)

    fun isAdminActive(): Boolean = dpm.isAdminActive(componentName)

    fun applyProtection() {
        if (!isAdminActive()) return
        runCatching {
            dpm.setUninstallBlocked(componentName, context.packageName, true)
            dpm.addUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
        }
    }

    fun setInstallBlock(enabled: Boolean) {
        if (!isAdminActive()) return
        runCatching {
            if (enabled) {
                dpm.addUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
            } else {
                dpm.clearUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
            }
        }
    }

    fun removeProtection() {
        if (!isAdminActive()) return
        runCatching {
            dpm.setUninstallBlocked(componentName, context.packageName, false)
            dpm.clearUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
        }
        dpm.removeActiveAdmin(componentName)
    }
}
