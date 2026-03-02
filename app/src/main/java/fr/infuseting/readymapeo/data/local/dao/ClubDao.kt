package fr.infuseting.readymapeo.data.local.dao

import androidx.room.*
import fr.infuseting.readymapeo.data.local.entity.ClubEntity
import fr.infuseting.readymapeo.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour les opérations sur les clubs et leurs membres.
 */
@Dao
interface ClubDao {

    // ── Clubs ────────────────────────────────────────────────

    @Query("SELECT * FROM clubs ORDER BY clubName ASC")
    fun observeAllClubs(): Flow<List<ClubEntity>>

    @Query("SELECT * FROM clubs WHERE clubId = :clubId")
    fun observeClubById(clubId: Int): Flow<ClubEntity?>

    @Query("SELECT * FROM clubs WHERE clubId = :clubId")
    suspend fun getClubById(clubId: Int): ClubEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubs(clubs: List<ClubEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(club: ClubEntity)

    @Query("DELETE FROM clubs")
    suspend fun deleteAllClubs()

    // ── Membres ──────────────────────────────────────────────

    @Query("SELECT * FROM users WHERE clubId = :clubId AND status = 'approved' ORDER BY name ASC")
    fun observeApprovedMembers(clubId: Int): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE clubId = :clubId AND status = 'pending' ORDER BY name ASC")
    fun observePendingMembers(clubId: Int): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE clubId = :clubId")
    suspend fun deleteUsersByClub(clubId: Int)

    @Query("DELETE FROM users WHERE id = :userId AND clubId = :clubId")
    suspend fun deleteUser(userId: Int, clubId: Int)

    @Query("UPDATE users SET status = 'approved' WHERE id = :userId AND clubId = :clubId")
    suspend fun approveUser(userId: Int, clubId: Int)
}
