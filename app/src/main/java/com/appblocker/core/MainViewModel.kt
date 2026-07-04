package com.appblocker.core

import android.content.Context
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
            isAdminActive = adminController.isAdminActive()
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
            mutableState.value = mutableState.value.copy(installBlockEnabled = install, contentFilterEnabled = filter)
            if (adminController.isAdminActive()) adminController.applyProtection()
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
        if (adminController.isAdminActive()) adminController.applyProtection()
        mutableState.value = mutableState.value.copy(isAdminActive = adminController.isAdminActive())
    }

    fun setInstallBlock(enabled: Boolean, password: String? = null) {
        if (!enabled && !passwordOk(password)) return
        viewModelScope.launch {
            repository.setBoolean(AppRepository.INSTALL_BLOCK_ENABLED, enabled)
            adminController.setInstallBlock(enabled)
            mutableState.value = mutableState.value.copy(installBlockEnabled = enabled, message = "Install blocking ${if (enabled) "enabled" else "disabled"}.")
        }
    }

    fun setContentFilter(enabled: Boolean, password: String? = null) {
        if (!enabled && !passwordOk(password)) return
        viewModelScope.launch {
            repository.setBoolean(AppRepository.CONTENT_FILTER_ENABLED, enabled)
            mutableState.value = mutableState.value.copy(contentFilterEnabled = enabled, message = "Content filter ${if (enabled) "enabled" else "disabled"}.")
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
        adminController.removeProtection()
        ContentFilterVpnService.stop(context)
        mutableState.value = mutableState.value.copy(
            isAdminActive = false,
            installBlockEnabled = false,
            contentFilterEnabled = false,
            message = "Protection removed. You can now uninstall AppBlocker from Android Settings."
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
}

data class UiState(
    val hasPassword: Boolean = false,
    val isAdminActive: Boolean = false,
    val installBlockEnabled: Boolean = true,
    val contentFilterEnabled: Boolean = true,
    val blockedDomains: List<BlockedDomain> = emptyList(),
    val blockedAppPackages: List<BlockedAppPackage> = emptyList(),
    val recentEvents: List<BlockEvent> = emptyList(),
    val message: String? = null
)
