package com.appblocker.core.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appblocker.core.UiState
import com.appblocker.core.ui.components.ShieldState
import com.appblocker.core.ui.components.StatusShield
import com.appblocker.core.ui.theme.Dimensions

@Composable
fun DashboardScreen(
    state: UiState,
    onInstallToggle: (Boolean) -> Unit,
    onFilterToggle: (Boolean) -> Unit,
    onOpenBlocklist: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isFullyProtected = state.isDeviceOwner && state.installBlockEnabled && state.contentFilterEnabled
    val isAnyProtectionEnabled = state.installBlockEnabled || state.contentFilterEnabled

    val shieldState = when {
        isFullyProtected -> ShieldState.Active
        !isAnyProtectionEnabled -> ShieldState.Blocked
        else -> ShieldState.Warning
    }

    val statusText = when (shieldState) {
        ShieldState.Active -> "Fully Protected"
        ShieldState.Warning -> "Attention Required"
        ShieldState.Blocked -> "Protection Paused"
    }

    val statusSubtext = when (shieldState) {
        ShieldState.Active -> "Device Owner locks and DNS content filters are active"
        ShieldState.Warning -> "Some protection components are disabled or require setup"
        ShieldState.Blocked -> "Enable VPN filter or install blocker to start protection"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(Dimensions.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
        
        // Status Shield
        StatusShield(state = shieldState)
        
        Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
        
        // Status Messaging
        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = statusSubtext,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge, vertical = Dimensions.PaddingExtraSmall)
        )

        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

        // Device Owner Warning Card
        AnimatedVisibility(visible = !state.isDeviceOwner) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimensions.PaddingMedium)
                    .clickable { onOpenSettings() }
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.PaddingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(Dimensions.IconLarge)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.PaddingMedium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Owner Setup Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "AppBlocker must be set as Device Owner to prevent uninstalls and restrict apps. Tap to configure.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Private DNS Warning Card
        AnimatedVisibility(visible = state.isPrivateDnsActive && state.contentFilterEnabled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimensions.PaddingMedium)
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.PaddingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(Dimensions.IconLarge)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.PaddingMedium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Private DNS Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Private DNS is active on this device, which bypasses content filtering. Set Private DNS to 'Off' or 'Automatic' in Android Network Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Toggles Section
        Text(
            text = "Protection Toggles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimensions.PaddingSmall)
        )

        // Install Blocker Card
        ToggleCard(
            title = "Block New Installs",
            subtitle = "Requires active Device Owner status",
            checked = state.installBlockEnabled,
            onCheckedChange = onInstallToggle
        )

        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))

        // Content Filter Card
        ToggleCard(
            title = "Content Filter (VPN)",
            subtitle = "Intercept and block domain lists",
            checked = state.contentFilterEnabled,
            onCheckedChange = onFilterToggle
        )

        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

        // Shortcuts Section
        Text(
            text = "Quick Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimensions.PaddingSmall)
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingSmall)) {
            ShortcutRow(
                title = "Manage Blocklist",
                subtitle = "${state.blockedDomains.size} domains blacklisted",
                icon = Icons.Default.Dns,
                onClick = onOpenBlocklist
            )

            ShortcutRow(
                title = "Blocked Apps",
                subtitle = "${state.blockedAppPackages.size} packages blacklisted",
                icon = Icons.Default.MobileOff,
                onClick = onOpenApps
            )

            ShortcutRow(
                title = "Recent Logs",
                subtitle = "View filtered network activity",
                icon = Icons.Default.History,
                onClick = onOpenLogs
            )

            ShortcutRow(
                title = "System Settings & Device Owner Guidance",
                subtitle = "Manage credentials, QR configuration & uninstalls",
                icon = Icons.Default.Settings,
                onClick = onOpenSettings
            )
        }
        
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(Dimensions.PaddingMedium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
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
}
