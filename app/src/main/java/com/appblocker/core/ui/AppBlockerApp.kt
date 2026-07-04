package com.appblocker.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appblocker.core.MainViewModel
import com.appblocker.core.UiState
import com.appblocker.core.ui.components.PasswordBottomSheet
import com.appblocker.core.ui.screens.*
import com.appblocker.core.ui.theme.AppBlockerTheme
import com.appblocker.core.ui.theme.Dimensions
import kotlinx.coroutines.launch

@Composable
fun AppBlockerApp(
    viewModel: MainViewModel,
    requestDeviceAdmin: () -> Unit,
    requestVpn: () -> Unit,
    requestOverlay: () -> Unit,
    requestBatteryExemption: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    AppBlockerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!state.hasPassword) {
                OnboardingScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    requestDeviceAdmin = requestDeviceAdmin,
                    requestVpn = requestVpn,
                    requestOverlay = requestOverlay,
                    requestBatteryExemption = requestBatteryExemption,
                    onCreate = { password, confirm, question, answer ->
                        if (viewModel.completeOnboarding(password, confirm, question, answer)) {
                            requestDeviceAdmin()
                            requestVpn()
                            requestOverlay()
                            requestBatteryExemption()
                        }
                    }
                )
            } else {
                MainNav(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    requestDeviceAdmin = requestDeviceAdmin,
                    requestVpn = requestVpn,
                    requestOverlay = requestOverlay,
                    requestBatteryExemption = requestBatteryExemption,
                    onInstallToggle = viewModel::setInstallBlock,
                    onFilterToggle = viewModel::setContentFilter,
                    onAddDomain = viewModel::addDomain,
                    onRemoveDomain = viewModel::removeDomain,
                    onAddBlockedApp = viewModel::addBlockedAppPackage,
                    onRemoveBlockedApp = viewModel::removeBlockedAppPackage,
                    onRemoveProtection = viewModel::removeProtection,
                    onResetPassword = viewModel::resetPassword,
                    securityQuestion = viewModel::securityQuestion,
                    verifyPassword = viewModel::verifyPassword,
                    requirePassword = { action ->
                        scope.launch { action() }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainNav(
    state: UiState,
    snackbarHostState: SnackbarHostState,
    requestDeviceAdmin: () -> Unit,
    requestVpn: () -> Unit,
    requestOverlay: () -> Unit,
    requestBatteryExemption: () -> Unit,
    onInstallToggle: (Boolean, String?) -> Unit,
    onFilterToggle: (Boolean, String?) -> Unit,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (Long) -> Unit,
    onAddBlockedApp: (String) -> Unit,
    onRemoveBlockedApp: (Long) -> Unit,
    onRemoveProtection: (String) -> Unit,
    onResetPassword: (String, String) -> Unit,
    securityQuestion: () -> String,
    verifyPassword: (String) -> Boolean,
    requirePassword: (suspend () -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "dashboard"
    var passwordAction by remember { mutableStateOf<PasswordAction?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomNav(navController, route) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    state = state,
                    onInstallToggle = { enabled ->
                        if (enabled) {
                            onInstallToggle(true, null)
                        } else {
                            passwordAction = PasswordAction("Disable Install Block") { onInstallToggle(false, it) }
                        }
                    },
                    onFilterToggle = { enabled ->
                        if (enabled) {
                            requestVpn()
                            onFilterToggle(true, null)
                        } else {
                            passwordAction = PasswordAction("Disable Content Filter") { onFilterToggle(false, it) }
                        }
                    },
                    onOpenBlocklist = {
                        passwordAction = PasswordAction("Open Blocklist") {
                            if (verifyPassword(it)) navController.navigate("blocklist")
                        }
                    },
                    onOpenApps = {
                        passwordAction = PasswordAction("Open Blocked Apps") {
                            if (verifyPassword(it)) navController.navigate("apps")
                        }
                    },
                    onOpenLogs = { navController.navigate("logs") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("blocklist") {
                BlocklistScreen(
                    state = state,
                    onAddDomain = onAddDomain,
                    onRemoveDomain = onRemoveDomain,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("apps") {
                BlockedAppsScreen(
                    state = state,
                    onAddPackage = onAddBlockedApp,
                    onRemovePackage = onRemoveBlockedApp,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("logs") {
                LogsScreen(
                    state = state,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    requestDeviceAdmin = requestDeviceAdmin,
                    requestVpn = requestVpn,
                    requestOverlay = requestOverlay,
                    requestBatteryExemption = requestBatteryExemption,
                    onNavigateToRemove = { navController.navigate("remove") },
                    onNavigateToReset = { navController.navigate("reset") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("remove") {
                RemoveProtectionScreen(
                    onRemoveProtection = { password -> onRemoveProtection(password) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("reset") {
                ResetPasswordScreen(
                    question = securityQuestion(),
                    onResetPassword = onResetPassword,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    passwordAction?.let { action ->
        PasswordBottomSheet(
            title = action.title,
            onDismiss = { passwordAction = null },
            onConfirm = { password ->
                val current = action
                passwordAction = null
                requirePassword { current.run(password) }
            }
        )
    }
}

@Composable
private fun BottomNav(navController: NavController, route: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        NavigationBarItem(
            selected = route == "dashboard" || route == "settings" || route == "remove" || route == "reset",
            onClick = { navController.navigate("dashboard") },
            icon = { Icon(Icons.Outlined.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = route == "logs",
            onClick = { navController.navigate("logs") },
            icon = { Icon(Icons.Outlined.Dns, null) },
            label = { Text("Logs") }
        )
    }
}

@Composable
private fun RemoveProtectionScreen(
    onRemoveProtection: (String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimensions.PaddingLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimensions.IconExtraLarge)
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            Text(
                text = "Remove Protection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
            Text(
                text = "Enter the parent password. Deactivating admin protection removes the VPN content filter and allows uninstalling this application.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Parent Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            Button(
                onClick = { onRemoveProtection(password) },
                enabled = password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Deactivate and Remove")
            }
            Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun ResetPasswordScreen(
    question: String,
    onResetPassword: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var answer by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimensions.PaddingLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.IconExtraLarge)
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            Text(
                text = "Reset Parent Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            Text(
                text = question.ifBlank { "Security Question" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Security Answer") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("New Password (min 6 chars)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            Button(
                onClick = { onResetPassword(password, answer) },
                enabled = answer.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm and Reset")
            }
            Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

private data class PasswordAction(
    val title: String,
    val run: (String) -> Unit
)
