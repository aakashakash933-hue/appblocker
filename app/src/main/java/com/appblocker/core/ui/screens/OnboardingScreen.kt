package com.appblocker.core.ui.screens

import android.content.Context
import android.net.VpnService
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.appblocker.core.UiState
import com.appblocker.core.ui.components.PermissionCard
import com.appblocker.core.ui.components.QrCodeGenerator
import com.appblocker.core.ui.theme.Dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PermissionRationale(
    val title: String,
    val explanation: String,
    val onConfirm: () -> Unit
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    state: UiState,
    snackbarHostState: SnackbarHostState,
    requestDeviceAdmin: () -> Unit,
    requestVpn: () -> Unit,
    requestOverlay: () -> Unit,
    requestBatteryExemption: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("What was your first school?") }
    var answer by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val securityQuestions = listOf(
        "What was your first school?",
        "What is your mother's maiden name?",
        "What city were you born in?",
        "What was the name of your first pet?",
        "What is your favorite book?"
    )

    // Check permissions live
    var isVpnGranted by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }
    var isBatteryGranted by remember { mutableStateOf(false) }

    var rationaleToShow by remember { mutableStateOf<PermissionRationale?>(null) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false, // Force sequential wizard progression
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomeSlide()
                    1 -> OnboardingPasswordSlide(
                        password = password,
                        onPasswordChange = { password = it },
                        confirm = confirm,
                        onConfirmChange = { confirm = it },
                        question = question,
                        onQuestionChange = { question = it },
                        answer = answer,
                        onAnswerChange = { answer = it },
                        securityQuestions = securityQuestions,
                        dropdownExpanded = dropdownExpanded,
                        onDropdownExpandedChange = { dropdownExpanded = it }
                    )
                    2 -> OnboardingDeviceOwnerSlide(
                        state = state
                    )
                    3 -> OnboardingPermissionsSlide(
                        state = state,
                        isVpnGranted = isVpnGranted,
                        isOverlayGranted = isOverlayGranted,
                        isBatteryGranted = isBatteryGranted,
                        showRationale = { rationale -> rationaleToShow = rationale },
                        requestDeviceAdmin = requestDeviceAdmin,
                        requestVpn = requestVpn,
                        requestOverlay = requestOverlay,
                        requestBatteryExemption = requestBatteryExemption
                    )
                }
            }

            // Bottom bar with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingLarge),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator dots
                Row {
                    repeat(4) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (active) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                // Action buttons
                Row {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(Dimensions.PaddingSmall))
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < 3) {
                                if (pagerState.currentPage == 1) {
                                    // Validate password
                                    if (password.length < 6) {
                                        scope.launch { snackbarHostState.showSnackbar("Password must be at least 6 characters.") }
                                        return@Button
                                    }
                                    if (password != confirm) {
                                        scope.launch { snackbarHostState.showSnackbar("Passwords do not match.") }
                                        return@Button
                                    }
                                    if (answer.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Security answer cannot be blank.") }
                                        return@Button
                                    }
                                }
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                // Final Page: trigger password creation and initialize permissions
                                onCreate(password, confirm, question, answer)
                            }
                        }
                    ) {
                        Text(if (pagerState.currentPage == 3) "Finish & Start" else "Next")
                    }
                }
            }
        }
    }

    // Explicit Permission Rationale Dialog
    rationaleToShow?.let { rationale ->
        AlertDialog(
            onDismissRequest = { rationaleToShow = null },
            title = { Text(rationale.title, fontWeight = FontWeight.Bold) },
            text = { Text(rationale.explanation) },
            confirmButton = {
                Button(onClick = rationale.onConfirm) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { rationaleToShow = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OnboardingWelcomeSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.PaddingLarge)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
        }
        Spacer(modifier = Modifier.height(Dimensions.PaddingExtraLarge))
        Text(
            text = "Welcome to AppBlocker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
        Text(
            text = "Keep your focus sharp and your children protected. Setup a parent password to block distracting content and secure this application.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimensions.PaddingMedium),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingPasswordSlide(
    password: String,
    onPasswordChange: (String) -> Unit,
    confirm: String,
    onConfirmChange: (String) -> Unit,
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    securityQuestions: List<String>,
    dropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.PaddingLarge)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Parent Lock Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Create a parent PIN or password. This password will be required to disable protection, edit blocklists, or uninstall the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = Dimensions.PaddingSmall)
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password (min 6 chars)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
        OutlinedTextField(
            value = confirm,
            onValueChange = onConfirmChange,
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        Text(
            text = "Security Question",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Used to recover access if you forget the password.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))

        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = onDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = question,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { onDropdownExpandedChange(false) }
            ) {
                securityQuestions.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onQuestionChange(item)
                            onDropdownExpandedChange(false)
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            label = { Text("Security Answer") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingDeviceOwnerSlide(
    state: UiState
) {
    val clipboardManager = LocalClipboardManager.current
    var devModeExpanded by remember { mutableStateOf(false) }

    val qrPayload = """
        android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME=com.appblocker.core/com.appblocker.core.admin.AppDeviceAdminReceiver
        android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION=https://github.com/aakashakash933-hue/appblocker/raw/main/app-debug.apk
    """.trimIndent()

    val qrBitmap = remember(qrPayload) {
        QrCodeGenerator.generateQrCode(qrPayload, 400)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.PaddingLarge)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Device Owner Protection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
        Text(
            text = "Android requires Device Owner configuration to enforce uninstall protection. Please note this app cannot be set as Device Owner if accounts (like Google) already exist on the target phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))

        // Status Card
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (state.isDeviceOwner) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(Dimensions.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.isDeviceOwner) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (state.isDeviceOwner) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(Dimensions.IconMedium)
                )
                Spacer(modifier = Modifier.width(Dimensions.PaddingMedium))
                Column {
                    Text(
                        text = if (state.isDeviceOwner) "Device Owner Configured" else "Device Owner Setup Required",
                        fontWeight = FontWeight.Bold,
                        color = if (state.isDeviceOwner) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    Text(
                        text = if (state.isDeviceOwner) "Full protection is active on this device." else "Uninstall protection cannot be enforced yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isDeviceOwner) Color(0xFF2E7D32).copy(alpha = 0.8f) else Color(0xFFE65100).copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

        // Production Setup (QR)
        Text(
            text = "Production Setup (Recommended)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Factory reset the child's device. On the first welcome screen of the setup wizard, tap rapidly 6 times. This launches a QR reader. Scan the QR code below:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))

        // QR Code Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.PaddingSmall),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.PaddingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Device Owner Provisioning QR",
                            modifier = Modifier.size(160.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(160.dp),
                            tint = Color.Black
                        )
                    }
                    Text(
                        text = "Scan during initial setup",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = Dimensions.PaddingSmall)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))

        // Developer Setup (ADB)
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Dimensions.PaddingMedium)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { devModeExpanded = !devModeExpanded }
                        .padding(vertical = Dimensions.PaddingExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Testing / Developer Fallback (ADB)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = devModeExpanded) {
                    Column(modifier = Modifier.padding(top = Dimensions.PaddingSmall)) {
                        Text(
                            text = "For local developer testing without factory-resetting: remove all Google and local accounts from settings, enable USB debugging, and run the command:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
                        
                        val adbCommand = "adb shell dpm set-device-owner com.appblocker.core/com.appblocker.core.admin.AppDeviceAdminReceiver"
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimensions.PaddingSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = adbCommand,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(adbCommand))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy command",
                                        modifier = Modifier.size(Dimensions.IconSmall)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPermissionsSlide(
    state: UiState,
    isVpnGranted: Boolean,
    isOverlayGranted: Boolean,
    isBatteryGranted: Boolean,
    showRationale: (PermissionRationale) -> Unit,
    requestDeviceAdmin: () -> Unit,
    requestVpn: () -> Unit,
    requestOverlay: () -> Unit,
    requestBatteryExemption: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.PaddingLarge)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Enable Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Configure system settings to activate blocking. We recommend granting all items below for reliable background safety.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimensions.PaddingMedium)
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingSmall)) {
            PermissionCard(
                title = "Device Administrator",
                description = "Prevents children or unauthorized users from uninstalling AppBlocker without entering the parent password.",
                icon = Icons.Default.Security,
                isGranted = state.isAdminActive,
                onRequestPermission = {
                    showRationale(
                        PermissionRationale(
                            title = "Device Administrator Rationale",
                            explanation = "Device Administrator is required to secure AppBlocker from being bypassed. When you click Continue, Android will ask you to activate the Device Administrator. Please click 'Activate'.",
                            onConfirm = {
                                showRationale(
                                    PermissionRationale("", "", {}) // dummy to dismiss
                                )
                                requestDeviceAdmin()
                            }
                        )
                    )
                }
            )

            PermissionCard(
                title = "Local Content Filter (VPN)",
                description = "Enables DNS blocking to intercept and filter domains from your blocklist locally.",
                icon = Icons.Default.Dns,
                isGranted = isVpnGranted,
                onRequestPermission = {
                    showRationale(
                        PermissionRationale(
                            title = "Content Filter VPN Rationale",
                            explanation = "Local VPN is used to inspect DNS queries locally on this device without connecting to any external cloud, blocking adult content. When you click Continue, please click 'Allow' or 'OK'.",
                            onConfirm = {
                                showRationale(
                                    PermissionRationale("", "", {})
                                )
                                requestVpn()
                            }
                        )
                    )
                }
            )

            PermissionCard(
                title = "System Alert Overlay",
                description = "Allows the app to display a warning shield on top of blocked apps when they are launched.",
                icon = Icons.Default.Web,
                isGranted = isOverlayGranted,
                onRequestPermission = {
                    showRationale(
                        PermissionRationale(
                            title = "System Overlay Rationale",
                            explanation = "Draw Over Other Apps permission is required to display the blocked screen instantly when a restricted site is launched. When you click Continue, please toggle on 'Allow display over other apps' for AppBlocker.",
                            onConfirm = {
                                showRationale(
                                    PermissionRationale("", "", {})
                                )
                                requestOverlay()
                            }
                        )
                    )
                }
            )

            PermissionCard(
                title = "Battery Exemption",
                description = "Ensures background blocking filters continue running seamlessly without being terminated by Android OS.",
                icon = Icons.Default.BatteryAlert,
                isGranted = isBatteryGranted,
                onRequestPermission = {
                    showRationale(
                        PermissionRationale(
                            title = "Battery Exemption Rationale",
                            explanation = "Battery Exemption is required to keep our filters running in the background. Android will ask to ignore battery optimization. Please click 'Allow' on the dialog.",
                            onConfirm = {
                                showRationale(
                                    PermissionRationale("", "", {})
                                )
                                requestBatteryExemption()
                            }
                        )
                    )
                }
            )
        }
    }
}
