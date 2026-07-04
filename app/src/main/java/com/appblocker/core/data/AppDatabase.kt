package com.appblocker.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BlockedDomain::class, AppSetting::class, BlockEvent::class, BlockedAppPackage::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
