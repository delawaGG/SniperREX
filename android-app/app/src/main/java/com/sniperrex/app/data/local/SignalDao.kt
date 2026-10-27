package com.sniperrex.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(signal: SignalEntity)

    @Query("SELECT * FROM signals ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE id = :id")
    suspend fun findById(id: String): SignalEntity?

    @Query("SELECT * FROM signals WHERE id = :id")
    fun observeById(id: String): Flow<SignalEntity?>

    @Query("UPDATE signals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE signals SET aiVerdictJson = :json WHERE id = :id")
    suspend fun updateVerdict(id: String, json: String?)

    /** Deteksi duplikat: pair+timeframe+strategi pada jendela waktu yang sama. */
    @Query(
        "SELECT COUNT(*) FROM signals WHERE pair = :pair AND timeframe = :timeframe " +
            "AND coreStrategy = :strategy AND createdAt >= :since"
    )
    suspend fun countRecent(pair: String, timeframe: String, strategy: String, since: Long): Int

    @Query("DELETE FROM signals WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
