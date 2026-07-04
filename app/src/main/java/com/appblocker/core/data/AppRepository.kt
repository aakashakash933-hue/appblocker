package com.appblocker.core.data

import android.content.Context
import com.appblocker.core.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppDao
) {
    val blockedDomains: Flow<List<BlockedDomain>> = dao.observeBlockedDomains()
    val recentEvents: Flow<List<BlockEvent>> = dao.observeRecentEvents()
    val blockedAppPackages: Flow<List<BlockedAppPackage>> = dao.observeBlockedAppPackages()

    suspend fun preloadDefaultBlocklist() = withContext(Dispatchers.IO) {
        if (dao.blockedDomainCount() > 0) return@withContext
        val json = context.resources.openRawResource(R.raw.default_blocklist)
            .bufferedReader()
            .use { it.readText() }
        val array = JSONArray(json)
        val now = System.currentTimeMillis()
        val domains = buildList {
            for (i in 0 until array.length()) {
                normalizeDomain(array.getString(i))?.let {
                    add(BlockedDomain(domain = it, category = "adult", addedAt = now))
                }
            }
        }
        dao.insertBlockedDomains(domains)
    }

    suspend fun addDomain(input: String) {
        val domain = normalizeDomain(input) ?: return
        dao.insertBlockedDomain(BlockedDomain(domain = domain, category = "custom", addedAt = System.currentTimeMillis()))
    }

    suspend fun removeDomain(id: Long) = dao.deleteBlockedDomain(id)

    suspend fun isDomainBlocked(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        val labels = normalized.split(".")
        for (i in labels.indices) {
            if (dao.hasExactDomain(labels.drop(i).joinToString("."))) return true
        }
        return false
    }

    suspend fun recordBlocked(domain: String) {
        dao.insertBlockEvent(BlockEvent(domain = domain, timestamp = System.currentTimeMillis()))
    }

    suspend fun addBlockedAppPackage(input: String) {
        val packageName = normalizePackageName(input) ?: return
        dao.insertBlockedAppPackage(BlockedAppPackage(packageName = packageName, addedAt = System.currentTimeMillis()))
    }

    suspend fun removeBlockedAppPackage(id: Long) = dao.deleteBlockedAppPackage(id)

    suspend fun isAppPackageBlocked(packageName: String): Boolean =
        normalizePackageName(packageName)?.let { dao.isAppPackageBlocked(it) } == true

    suspend fun setBoolean(key: String, value: Boolean) = dao.setSetting(AppSetting(key, value.toString()))
    suspend fun getBoolean(key: String, default: Boolean): Boolean = dao.getSetting(key)?.toBooleanStrictOrNull() ?: default

    companion object {
        const val INSTALL_BLOCK_ENABLED = "install_block_enabled"
        const val CONTENT_FILTER_ENABLED = "content_filter_enabled"

        fun normalizeDomain(input: String): String? {
            val cleaned = input.trim()
                .lowercase()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .trim('.')
            if (cleaned.isBlank() || cleaned.any { it.isWhitespace() }) return null
            return cleaned
        }

        fun normalizePackageName(input: String): String? {
            val cleaned = input.trim().lowercase()
            val valid = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")
            return cleaned.takeIf { valid.matches(it) }
        }
    }
}
