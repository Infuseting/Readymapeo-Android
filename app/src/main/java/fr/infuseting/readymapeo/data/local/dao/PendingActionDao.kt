package fr.infuseting.readymapeo.data.local.dao

import androidx.room.*
import fr.infuseting.readymapeo.data.local.entity.PendingActionEntity

/**
 * DAO pour la file d'attente des actions hors-ligne.
 */
@Dao
interface PendingActionDao {

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAllPendingActions(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions")
    suspend fun getPendingCount(): Int

    @Insert
    suspend fun insert(action: PendingActionEntity)

    @Delete
    suspend fun delete(action: PendingActionEntity)

    @Query("DELETE FROM pending_actions")
    suspend fun deleteAll()
}
