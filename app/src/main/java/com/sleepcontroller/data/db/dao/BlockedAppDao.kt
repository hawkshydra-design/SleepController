package com.sleepcontroller.data.db.dao

import androidx.room.*
import com.sleepcontroller.data.db.entity.BlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1 AND isPermanentlyWhitelisted = 0")
    fun getBlockedApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE isPermanentlyWhitelisted = 1")
    fun getWhitelistedApps(): Flow<List<BlockedApp>>

    @Query("SELECT packageName FROM blocked_apps WHERE isBlocked = 1 AND isPermanentlyWhitelisted = 0")
    suspend fun getBlockedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_apps WHERE packageName = :packageName AND isBlocked = 1 AND isPermanentlyWhitelisted = 0)")
    suspend fun isAppBlocked(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedApp>)

    @Update
    suspend fun update(app: BlockedApp)

    @Delete
    suspend fun delete(app: BlockedApp)

    @Query("UPDATE blocked_apps SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun setBlocked(packageName: String, isBlocked: Boolean)

    @Query("UPDATE blocked_apps SET isBlocked = 1 WHERE category = :category AND isPermanentlyWhitelisted = 0")
    suspend fun blockByCategory(category: String)

    @Query("UPDATE blocked_apps SET isBlocked = 0 WHERE isPermanentlyWhitelisted = 0")
    suspend fun unblockAll()

    @Query("UPDATE blocked_apps SET isBlocked = 1 WHERE isPermanentlyWhitelisted = 0")
    suspend fun blockAll()
}
