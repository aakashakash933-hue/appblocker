package com.appblocker.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_domains",
    indices = [Index(value = ["domain"], unique = true)]
)
data class BlockedDomain(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val category: String,
    @ColumnInfo(name = "added_at") val addedAt: Long
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "block_events")
data class BlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val timestamp: Long
)

@Entity(
    tableName = "blocked_app_packages",
    indices = [Index(value = ["package_name"], unique = true)]
)
data class BlockedAppPackage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "added_at") val addedAt: Long
)
