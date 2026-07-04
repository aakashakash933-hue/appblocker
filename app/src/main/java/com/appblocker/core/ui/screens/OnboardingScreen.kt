package com.appblocker.core.ui.screens

import android.content.Context
import android.net.VpnService
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.appblocker.core.UiState
import com.appblocker.core.ui.components.PermissionCard
import com.appblocker.core.ui.theme.Dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val pagerState = rememberPagerState(pageCount = { 3 })
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
                    2 -> OnboardingPermissionsSlide(
                        state = state,
                        isVpnGranted = isVpnGranted,
                        isOverlayGranted = isOverlayGranted,
                        isBatteryGranted = isBatteryGranted,
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
                    repeat(3) { index ->
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
                            if (pagerState.currentPage < 2) {
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
                        Text(if (pagerState.currentPage == 2) "Finish & Start" else "Next")
                    }
                }
            }
        }
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
        // Styled Lock Shield Artwork
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
private fun OnboardingPermissionsSlide(
    state: UiState,
    isVpnGranted: Boolean,
    isOverlayGranted: Boolean,
    isBatteryGranted: Boolean,
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
                onRequestPermission = requestDeviceAdmin
            )

            PermissionCard(
                title = "Local Content Filter (VPN)",
                description = "Enables DNS blocking to intercept and filter domains from your blocklist locally.",
                icon = Icons.Default.Dns,
                isGranted = isVpnGranted,
                onRequestPermission = requestVpn
            )

            PermissionCard(
                title = "System Alert Overlay",
                description = "Allows the app to display a warning shield on top of blocked apps when they are launched.",
                icon = Icons.Default.Web,
                isGranted = isOverlayGranted,
                onRequestPermission = requestOverlay
            )

            PermissionCard(
                title = "Battery Exemption",
                description = "Ensures background blocking filters continue running seamlessly without being terminated by Android OS.",
                icon = Icons.Default.BatteryAlert,
                isGranted = isBatteryGranted,
                onRequestPermission = requestBatteryExemption
            )
        }
    }
}
