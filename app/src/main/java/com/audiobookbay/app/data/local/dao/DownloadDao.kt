package com.audiobookbay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.audiobookbay.app.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY addedTimestamp DESC")
    fun observeAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE infoHash = :infoHash LIMIT 1")
    fun observeDownloadByInfoHash(infoHash: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE infoHash = :infoHash LIMIT 1")
    suspend fun getDownloadByInfoHash(infoHash: String): DownloadEntity?

    @Upsert
    suspend fun upsertDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE infoHash = :infoHash")
    suspend fun deleteDownloadByInfoHash(infoHash: String)

    @Query("UPDATE downloads SET status = :status WHERE infoHash = :infoHash")
    suspend fun updateDownloadStatus(infoHash: String, status: String)
}
