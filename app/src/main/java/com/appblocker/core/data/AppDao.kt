package com.appblocker.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM blocked_domains ORDER BY domain ASC")
    fun observeBlockedDomains(): Flow<List<BlockedDomain>>

    @Query("SELECT * FROM block_events ORDER BY timestamp DESC LIMIT 100")
    fun observeRecentEvents(): Flow<List<BlockEvent>>

    @Query("SELECT * FROM blocked_app_packages ORDER BY package_name ASC")
    fun observeBlockedAppPackages(): Flow<List<BlockedAppPackage>>

    @Query("SELECT COUNT(*) FROM blocked_domains")
    suspend fun blockedDomainCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_domains WHERE domain = :domain)")
    suspend fun hasExactDomain(domain: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlockedDomain(domain: BlockedDomain)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlockedDomains(domains: List<BlockedDomain>)

    @Query("DELETE FROM blocked_domains WHERE id = :id")
    suspend fun deleteBlockedDomain(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Insert
    suspend fun insertBlockEvent(event: BlockEvent)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_app_packages WHERE package_name = :packageName)")
    suspend fun isAppPackageBlocked(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlockedAppPackage(appPackage: BlockedAppPackage)

    @Query("DELETE FROM blocked_app_packages WHERE id = :id")
    suspend fun deleteBlockedAppPackage(id: Long)
}
