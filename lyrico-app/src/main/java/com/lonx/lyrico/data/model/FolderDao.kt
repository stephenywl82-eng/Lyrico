package com.lonx.lyrico.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {


    /** 所有文件夹 */
    @Query("SELECT * FROM folders ORDER BY path")
    fun getAllFolders(): Flow<List<FolderEntity>>

    /** 所有未忽略的文件夹路径 */
    @Query("SELECT path FROM folders WHERE isIgnored = 0")
    suspend fun getNotIgnoredFolderPaths(): List<String>

    /** 所有已忽略的文件夹路径 */
    @Query("SELECT path FROM folders WHERE isIgnored = 1")
    suspend fun getIgnoredFolderPaths(): List<String>

    /** 根据路径查找 */
    @Query("SELECT * FROM folders WHERE path = :path LIMIT 1")
    suspend fun findByPath(path: String): FolderEntity?

    /** 文件夹是否被忽略 */
    @Query("SELECT isIgnored FROM folders WHERE path = :path LIMIT 1")
    suspend fun isIgnored(path: String): Boolean?


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    /** upsert 并返回 id */
    @Transaction
    suspend fun upsertAndGetId(
        path: String,
        addedBySaf: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val existing = findByPath(path)

        return if (existing == null) {
            insert(FolderEntity(
                path = path,
                addedBySaf = addedBySaf,
                lastScanned = now,
                dbUpdateTime = now,
                songCount = 0 // 新文件夹初始为 0
            ))
        } else {
            update(existing.copy(
                lastScanned = now,
                dbUpdateTime = now
            ))
            existing.id
        }
    }


    @Query("""
        UPDATE folders 
        SET isIgnored = :ignored, dbUpdateTime = :updateTime 
        WHERE id = :folderId
    """)
    suspend fun setIgnored(
        folderId: Long,
        ignored: Boolean,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE folders 
        SET isIgnored = :ignored, addedBySaf = 1, dbUpdateTime = :updateTime
        WHERE path = :path
    """)
    suspend fun setIgnoredByPath(
        path: String,
        ignored: Boolean,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE folders 
        SET songCount = (SELECT COUNT(*) FROM songs WHERE folderId = :folderId),
            dbUpdateTime = :updateTime
        WHERE id = :folderId
    """)
    suspend fun refreshSongCount(
        folderId: Long,
        updateTime: Long = System.currentTimeMillis()
    )
    @Query("""
        UPDATE folders 
        SET songCount = (SELECT COUNT(*) FROM songs WHERE songs.folderId = folders.id),
            dbUpdateTime = :updateTime
    """)
    suspend fun refreshAllSongCounts(updateTime: Long = System.currentTimeMillis())
    @Query("""
        UPDATE folders 
        SET songCount = songCount + :delta, dbUpdateTime = :updateTime
        WHERE id = :folderId
    """)
    suspend fun updateSongCount(
        folderId: Long,
        delta: Int,
        updateTime: Long = System.currentTimeMillis()
    )


    /** 清理无歌曲的文件夹 */
    @Query("""
        DELETE FROM folders 
        WHERE songCount = 0 AND isIgnored = 0
    """)
    suspend fun deleteEmptyFolders()
}
