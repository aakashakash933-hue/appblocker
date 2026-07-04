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

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    fun applyProtection() {
        if (!isAdminActive()) return
        if (!isDeviceOwner()) {
            throw SecurityException("App must be Device Owner to block uninstall and app installation.")
        }
        try {
            dpm.setUninstallBlocked(componentName, context.packageName, true)
            dpm.addUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
        } catch (e: SecurityException) {
            android.util.Log.e("DeviceAdminController", "SecurityException in applyProtection", e)
            throw e
        }
    }

    fun setInstallBlock(enabled: Boolean) {
        if (!isAdminActive()) return
        if (!isDeviceOwner()) {
            throw SecurityException("App must be Device Owner to block app installation.")
        }
        try {
            if (enabled) {
                dpm.addUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
            } else {
                dpm.clearUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("DeviceAdminController", "SecurityException in setInstallBlock", e)
            throw e
        }
    }

    fun removeProtection() {
        if (!isAdminActive()) return
        if (isDeviceOwner()) {
            try {
                dpm.setUninstallBlocked(componentName, context.packageName, false)
                dpm.clearUserRestriction(componentName, UserManager.DISALLOW_INSTALL_APPS)
            } catch (e: SecurityException) {
                android.util.Log.e("DeviceAdminController", "SecurityException in removeProtection", e)
                throw e
            }
        }
        dpm.removeActiveAdmin(componentName)
    }
}
