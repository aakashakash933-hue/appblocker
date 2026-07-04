package com.appblocker.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.appblocker.core.data.AppDao
import com.appblocker.core.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "appblocker.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideDao(database: AppDatabase): AppDao = database.appDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS blocked_app_packages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    package_name TEXT NOT NULL,
                    added_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_blocked_app_packages_package_name ON blocked_app_packages(package_name)")
        }
    }
}
