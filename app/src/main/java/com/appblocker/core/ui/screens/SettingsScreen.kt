package com.appblocker.core.ui.screens

import android.content.Context
import android.net.VpnService
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appblocker.core.UiState
import com.appblocker.core.ui.theme.Dimensions
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    requestDeviceAdmin: () -> Unit,
    requestVpn: () -> Unit,
    requestOverlay: () -> Unit,
    requestBatteryExemption: () -> Unit,
    onNavigateToRemove: () -> Unit,
    onNavigateToReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isVpnGranted by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }
    var isBatteryGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isVpnGranted = VpnService.prepare(context) == null
            isOverlayGranted = Settings.canDrawOverlays(context)
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            isBatteryGranted = pm.isIgnoringBatteryOptimizations(context.packageName)
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingLarge)
        ) {
            
            // Group 1: Password & Protection Management
            Column {
                SettingsGroupHeader(title = "Security & Protection")
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRowItem(
                            title = "Forgot Password",
                            subtitle = "Answer security question to reset password",
                            icon = Icons.Default.LockReset,
                            onClick = onNavigateToReset
                        )
                        SettingsRowItem(
                            title = "Remove App Protection",
                            subtitle = "Deactivate administration & content filter safely",
                            icon = Icons.Default.DeleteForever,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = onNavigateToRemove
                        )
                    }
                }
            }

            // Group 2: System Permissions check
            Column {
                SettingsGroupHeader(title = "Permissions Checklist")
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        PermissionRowItem(
                            title = "Device Administrator",
                            isGranted = state.isAdminActive,
                            icon = Icons.Default.Security,
                            onClick = requestDeviceAdmin
                        )
                        PermissionRowItem(
                            title = "Local VPN",
                            isGranted = isVpnGranted,
                            icon = Icons.Default.Dns,
                            onClick = requestVpn
                        )
                        PermissionRowItem(
                            title = "System Overlay",
                            isGranted = isOverlayGranted,
                            icon = Icons.Default.Web,
                            onClick = requestOverlay
                        )
                        PermissionRowItem(
                            title = "Battery Exemption",
                            isGranted = isBatteryGranted,
                            icon = Icons.Default.BatteryAlert,
                            onClick = requestBatteryExemption
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = Dimensions.PaddingSmall)
    )
}

@Composable
private fun SettingsRowItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Dimensions.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(Dimensions.IconMedium)
        )
        Spacer(modifier = Modifier.width(Dimensions.PaddingMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(Dimensions.IconSmall)
        )
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Dimensions.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.IconMedium)
        )
        Spacer(modifier = Modifier.width(Dimensions.PaddingMedium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = if (isGranted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ) {
            Text(
                text = if (isGranted) "Active" else "Action Needed",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) Color(0xFF2E7D32) else Color(0xFFE65100),
                modifier = Modifier.padding(horizontal = Dimensions.PaddingSmall, vertical = 4.dp)
            )
        }
    }
}
