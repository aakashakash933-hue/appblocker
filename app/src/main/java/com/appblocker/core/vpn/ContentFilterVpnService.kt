package com.appblocker.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.appblocker.core.R
import com.appblocker.core.data.AppRepository
import com.appblocker.core.ui.BlockedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

@AndroidEntryPoint
class ContentFilterVpnService : VpnService() {
    @Inject lateinit var repository: AppRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var worker: Job? = null
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        if (worker?.isActive != true) {
            running = true
            worker = scope.launch { runVpnLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        worker?.cancel()
        vpnInterface?.close()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runVpnLoop() {
        if (!repository.getBoolean(AppRepository.CONTENT_FILTER_ENABLED, true)) return
        vpnInterface = Builder()
            .setSession(getString(R.string.vpn_session_name))
            .addAddress("10.8.0.2", 32)
            .addDnsServer(FILTERED_DNS)
            .addRoute(FILTERED_DNS, 32)
            .establish()

        val descriptor = vpnInterface?.fileDescriptor ?: return
        val input = FileInputStream(descriptor)
        val output = FileOutputStream(descriptor)
        val buffer = ByteArray(32767)

        while (running) {
            val length = input.read(buffer)
            if (length <= 0) continue
            val query = DnsPacket.parse(buffer, length)
            if (query == null) {
                continue
            }

            val blocked = repository.isDomainBlocked(query.domain)
            if (blocked) {
                repository.recordBlocked(query.domain)
                showBlockedOverlay(query.domain)
                val response = DnsPacket.blockedResponse(query.dnsPayload)
                output.write(DnsPacket.buildUdpIpResponse(query, response))
            } else {
                forwardDns(query)?.let { dnsResponse ->
                    output.write(DnsPacket.buildUdpIpResponse(query, dnsResponse))
                }
            }
        }
    }

    private fun forwardDns(query: DnsQueryPacket): ByteArray? = runCatching {
        DatagramSocket().use { socket ->
            protect(socket)
            socket.soTimeout = 3000
            val resolver = InetAddress.getByName(FILTERED_DNS)
            socket.send(DatagramPacket(query.dnsPayload, query.dnsPayload.size, resolver, 53))
            val response = ByteArray(4096)
            val responsePacket = DatagramPacket(response, response.size)
            socket.receive(responsePacket)
            response.copyOf(responsePacket.length)
        }
    }.getOrNull()

    private fun showBlockedOverlay(domain: String) {
        val intent = Intent(this, BlockedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(BlockedActivity.EXTRA_DOMAIN, domain)
        startActivity(intent)
    }

    private fun notification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "AppBlocker filter", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("AppBlocker is filtering content")
            .setContentText("Adult-content DNS filtering is active on this device.")
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "appblocker_filter"
        private const val NOTIFICATION_ID = 10
        private const val FILTERED_DNS = "1.1.1.3"

        fun start(context: Context) {
            val intent = Intent(context, ContentFilterVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ContentFilterVpnService::class.java))
        }
    }
}
