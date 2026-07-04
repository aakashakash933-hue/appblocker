package com.appblocker.core.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appblocker.core.data.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InstalledPackageReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: AppRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        val packageName = intent.data?.schemeSpecificPart ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (repository.isAppPackageBlocked(packageName)) {
                    context.startActivity(
                        Intent(context, BlockedInstallActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(BlockedInstallActivity.EXTRA_PACKAGE_NAME, packageName)
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
