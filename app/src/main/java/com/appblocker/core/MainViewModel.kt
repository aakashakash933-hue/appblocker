package com.appblocker.core

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appblocker.core.admin.DeviceAdminController
import com.appblocker.core.data.AppRepository
import com.appblocker.core.data.BlockEvent
import com.appblocker.core.data.BlockedAppPackage
import com.appblocker.core.data.BlockedDomain
import com.appblocker.core.security.PasswordManager
import com.appblocker.core.vpn.ContentFilterVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository,
    private val passwordManager: PasswordManager,
    private val adminController: DeviceAdminController
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        UiState(
            hasPassword = passwordManager.hasPassword(),
            isAdminActive = adminController.isAdminActive(),
            isDeviceOwner = adminController.isDeviceOwner(),
            isPrivateDnsActive = checkPrivateDns(context)
        )
    )

    val state: StateFlow<UiState> = combine(
        mutableState,
        repository.blockedDomains,
        repository.recentEvents,
        repository.blockedAppPackages
    ) { state, domains, events, appPackages ->
        state.copy(blockedDomains = domains, recentEvents = events, blockedAppPackages = appPackages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mutableState.value)

    init {
        viewModelScope.launch {
            repository.preloadDefaultBlocklist()
            val install = repository.getBoolean(AppRepository.INSTALL_BLOCK_ENABLED, true)
            val filter = repository.getBoolean(AppRepository.CONTENT_FILTER_ENABLED, true)
            
            val active = adminController.isAdminActive()
            val owner = adminController.isDeviceOwner()
            val privateDns = checkPrivateDns(context)
            var message: String? = null
            if (active && !owner) {
                message = "Not fully protected — device owner setup required."
            } else if (active && owner) {
                try {
                    adminController.applyProtection()
                } catch (e: SecurityException) {
                    message = "Failed to apply protection: ${e.message}"
                }
            }
            if (privateDns && filter) {
                message = "Warning: Private DNS is active, bypassing content filters."
            }

            mutableState.value = mutableState.value.copy(
                installBlockEnabled = install,
                contentFilterEnabled = filter,
                isAdminActive = active,
                isDeviceOwner = owner,
                isPrivateDnsActive = privateDns,
                message = message
            )
            startFilterIfEnabled()
        }
    }

    fun completeOnboarding(password: String, confirm: String, question: String, answer: String): Boolean {
        if (password != confirm) {
            mutableState.value = mutableState.value.copy(message = "Passwords do not match.")
            return false
        }
        val created = passwordManager.createPassword(password, question, answer)
        mutableState.value = mutableState.value.copy(
            hasPassword = passwordManager.hasPassword(),
            message = if (created) "Password saved. Continue permission setup." else "Use at least 6 characters and a security answer."
        )
        return created
    }

    fun refreshProtectionState() {
        val active = adminController.isAdminActive()
        val owner = adminController.isDeviceOwner()
        val privateDns = checkPrivateDns(context)
        var message: String? = null
        if (active && !owner) {
            message = "Not fully protected — device owner setup required."
        } else if (active && owner) {
            try {
                adminController.applyProtection()
            } catch (e: SecurityException) {
                message = "Failed to apply protection: ${e.message}"
            }
        }
        if (privateDns && mutableState.value.contentFilterEnabled) {
            message = "Warning: Private DNS is active, bypassing content filters."
        }

        mutableState.value = mutableState.value.copy(
            isAdminActive = active,
            isDeviceOwner = owner,
            isPrivateDnsActive = privateDns,
            message = message
        )
    }

    fun setInstallBlock(enabled: Boolean, password: String? = null) {
        if (!enabled && !passwordOk(password)) return
        viewModelScope.launch {
            repository.setBoolean(AppRepository.INSTALL_BLOCK_ENABLED, enabled)
            var message: String? = "Install blocking ${if (enabled) "enabled" else "disabled"}."
            if (adminController.isDeviceOwner()) {
                try {
                    adminController.setInstallBlock(enabled)
                } catch (e: SecurityException) {
                    message = "Failed to set install block: ${e.message}"
                }
            } else if (adminController.isAdminActive()) {
                message = "Cannot set install block: device owner setup required."
            }
            mutableState.value = mutableState.value.copy(
                installBlockEnabled = enabled,
                message = message
            )
        }
    }

    fun setContentFilter(enabled: Boolean, password: String? = null) {
        if (!enabled && !passwordOk(password)) return
        viewModelScope.launch {
            repository.setBoolean(AppRepository.CONTENT_FILTER_ENABLED, enabled)
            val privateDns = checkPrivateDns(context)
            var message = "Content filter ${if (enabled) "enabled" else "disabled"}."
            if (enabled && privateDns) {
                message = "Warning: Private DNS is active, bypassing content filters."
            }
            mutableState.value = mutableState.value.copy(
                contentFilterEnabled = enabled,
                isPrivateDnsActive = privateDns,
                message = message
            )
            if (enabled) ContentFilterVpnService.start(context) else ContentFilterVpnService.stop(context)
        }
    }

    fun startFilterIfEnabled() {
        viewModelScope.launch {
            if (repository.getBoolean(AppRepository.CONTENT_FILTER_ENABLED, true)) {
                ContentFilterVpnService.start(context)
            }
        }
    }

    fun addDomain(domain: String) = viewModelScope.launch { repository.addDomain(domain) }
    fun removeDomain(id: Long) = viewModelScope.launch { repository.removeDomain(id) }
    fun addBlockedAppPackage(packageName: String) = viewModelScope.launch { repository.addBlockedAppPackage(packageName) }
    fun removeBlockedAppPackage(id: Long) = viewModelScope.launch { repository.removeBlockedAppPackage(id) }

    fun removeProtection(password: String) {
        if (!passwordOk(password)) return
        var message: String? = "Protection removed. You can now uninstall AppBlocker from Android Settings."
        try {
            adminController.removeProtection()
        } catch (e: SecurityException) {
            message = "Failed to remove protection: ${e.message}"
        }
        ContentFilterVpnService.stop(context)
        mutableState.value = mutableState.value.copy(
            isAdminActive = false,
            isDeviceOwner = false,
            installBlockEnabled = false,
            contentFilterEnabled = false,
            message = message
        )
    }

    fun verifyPassword(password: String): Boolean = passwordOk(password)

    fun securityQuestion(): String = passwordManager.securityQuestion()

    fun resetPassword(newPassword: String, answer: String) {
        val ok = passwordManager.resetWithSecurityAnswer(newPassword, answer)
        mutableState.value = mutableState.value.copy(message = if (ok) "Password reset." else "Security answer did not match.")
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    private fun passwordOk(password: String?): Boolean {
        val result = passwordManager.verifyPassword(password.orEmpty())
        val message = when (result) {
            PasswordManager.AuthResult.Success -> null
            is PasswordManager.AuthResult.Failed -> "Wrong password. ${result.attemptsRemaining} attempts remaining."
            is PasswordManager.AuthResult.Locked -> "Too many attempts. Try again after the lockout expires."
        }
        mutableState.value = mutableState.value.copy(message = message)
        return result is PasswordManager.AuthResult.Success
    }

    private fun checkPrivateDns(context: Context): Boolean {
        return try {
            val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
            val specifier = Settings.Global.getString(context.contentResolver, "private_dns_specifier")
            mode == "hostname" || !specifier.isNullOrBlank()
        } catch (e: Exception) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val lp = cm?.getLinkProperties(activeNetwork)
            !lp?.privateDnsServerName.isNullOrBlank()
        }
    }
}

data class UiState(
    val hasPassword: Boolean = false,
    val isAdminActive: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isPrivateDnsActive: Boolean = false,
    val installBlockEnabled: Boolean = true,
    val contentFilterEnabled: Boolean = true,
    val blockedDomains: List<BlockedDomain> = emptyList(),
    val blockedAppPackages: List<BlockedAppPackage> = emptyList(),
    val recentEvents: List<BlockEvent> = emptyList(),
    val message: String? = null
)
