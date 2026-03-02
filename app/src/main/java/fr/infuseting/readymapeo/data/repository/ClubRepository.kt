package fr.infuseting.readymapeo.data.repository

import android.util.Log
import fr.infuseting.readymapeo.data.local.dao.ClubDao
import fr.infuseting.readymapeo.data.local.dao.PendingActionDao
import fr.infuseting.readymapeo.data.local.entity.ClubEntity
import fr.infuseting.readymapeo.data.local.entity.PendingActionEntity
import fr.infuseting.readymapeo.data.local.entity.UserEntity
import fr.infuseting.readymapeo.data.model.Club
import fr.infuseting.readymapeo.data.model.UpdateClubRequest
import fr.infuseting.readymapeo.data.model.User
import fr.infuseting.readymapeo.data.remote.ClubApiService
import fr.infuseting.readymapeo.sync.ConnectivityObserver
import fr.infuseting.readymapeo.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * Repository principal pour les clubs — source de vérité unique.
 *
 * Pattern :
 * - Lecture → Room (via Flow). Refresh réseau en arrière-plan si en ligne.
 * - Écriture → si en ligne : API + Room. Si hors-ligne : Room + PendingAction.
 */
class ClubRepository(
    private val clubDao: ClubDao,
    private val pendingActionDao: PendingActionDao,
    private val clubApiService: ClubApiService,
    private val connectivityObserver: ConnectivityObserver
) {
    companion object {
        private const val TAG = "ClubRepository"
    }

    // ── Lectures (Flow depuis Room) ──────────────────────────

    fun observeClubs(): Flow<List<Club>> {
        return clubDao.observeAllClubs().map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun observeClub(clubId: Int): Flow<Club?> {
        return clubDao.observeClubById(clubId).map { it?.toModel() }
    }

    fun observeApprovedMembers(clubId: Int): Flow<List<User>> {
        return clubDao.observeApprovedMembers(clubId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun observePendingMembers(clubId: Int): Flow<List<User>> {
        return clubDao.observePendingMembers(clubId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    // ── Refresh depuis le réseau ─────────────────────────────

    /**
     * Rafraîchit la liste des clubs depuis l'API et met à jour Room.
     */
    suspend fun refreshClubs() {
        if (!connectivityObserver.isOnline()) {
            Log.d(TAG, "Hors-ligne : utilisation du cache local.")
            return
        }
        try {
            val clubs = clubApiService.getManagedClubs()
            clubDao.insertClubs(clubs.map { it.toEntity() })
            Log.d(TAG, "Clubs rafraîchis : ${clubs.size} club(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur refresh clubs: ${e.message}")
        }
    }

    /**
     * Rafraîchit les détails d'un club et ses membres.
     */
    suspend fun refreshClubDetails(clubId: Int) {
        if (!connectivityObserver.isOnline()) return
        try {
            val club = clubApiService.getClubDetails(clubId)
            clubDao.insertClub(club.toEntity())

            val (approved, pending) = clubApiService.getClubMembers(clubId)
            clubDao.deleteUsersByClub(clubId)
            clubDao.insertUsers(
                approved.map { it.toEntity(clubId, "approved") } +
                        pending.map { it.toEntity(clubId, "pending") }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur refresh détails club $clubId: ${e.message}")
        }
    }

    // ── Écritures (online / offline) ─────────────────────────

    /**
     * Met à jour un club. Si hors-ligne, enregistre dans la file d'attente.
     */
    suspend fun updateClub(clubId: Int, request: UpdateClubRequest) {
        // Mise à jour locale immédiate
        val current = clubDao.getClubById(clubId)
        if (current != null) {
            clubDao.insertClub(
                current.copy(
                    clubName = request.clubName ?: current.clubName,
                    clubStreet = request.clubStreet ?: current.clubStreet,
                    clubCity = request.clubCity ?: current.clubCity,
                    clubPostalCode = request.clubPostalCode ?: current.clubPostalCode,
                    ffsoId = request.ffsoId ?: current.ffsoId,
                    description = request.description ?: current.description
                )
            )
        }

        if (connectivityObserver.isOnline()) {
            clubApiService.updateClub(clubId, request)
        } else {
            // Enregistrer pour synchronisation ultérieure
            val payload = JSONObject().apply {
                request.clubName?.let { put("club_name", it) }
                request.clubStreet?.let { put("club_street", it) }
                request.clubCity?.let { put("club_city", it) }
                request.clubPostalCode?.let { put("club_postal_code", it) }
                request.ffsoId?.let { put("ffso_id", it) }
                request.description?.let { put("description", it) }
            }.toString()
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = SyncManager.ACTION_UPDATE_CLUB,
                    clubId = clubId,
                    payload = payload
                )
            )
            Log.d(TAG, "Hors-ligne : mise à jour du club $clubId mise en file d'attente.")
        }
    }

    suspend fun approveMember(clubId: Int, userId: Int) {
        // Mise à jour locale
        clubDao.approveUser(userId, clubId)

        if (connectivityObserver.isOnline()) {
            clubApiService.approveMember(clubId, userId)
        } else {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = SyncManager.ACTION_APPROVE_MEMBER,
                    clubId = clubId,
                    userId = userId
                )
            )
        }
    }

    suspend fun rejectMember(clubId: Int, userId: Int) {
        clubDao.deleteUser(userId, clubId)

        if (connectivityObserver.isOnline()) {
            clubApiService.rejectMember(clubId, userId)
        } else {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = SyncManager.ACTION_REJECT_MEMBER,
                    clubId = clubId,
                    userId = userId
                )
            )
        }
    }

    suspend fun removeMember(clubId: Int, userId: Int) {
        clubDao.deleteUser(userId, clubId)

        if (connectivityObserver.isOnline()) {
            clubApiService.removeMember(clubId, userId)
        } else {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = SyncManager.ACTION_REMOVE_MEMBER,
                    clubId = clubId,
                    userId = userId
                )
            )
        }
    }

    // ── Mapping helpers ──────────────────────────────────────

    private fun ClubEntity.toModel() = Club(
        clubId = clubId,
        clubName = clubName,
        clubStreet = clubStreet,
        clubCity = clubCity,
        clubPostalCode = clubPostalCode,
        isApproved = isApproved,
        description = description,
        clubImage = clubImage,
        ffsoId = ffsoId
    )

    private fun Club.toEntity() = ClubEntity(
        clubId = clubId,
        clubName = clubName,
        clubStreet = clubStreet,
        clubCity = clubCity,
        clubPostalCode = clubPostalCode,
        isApproved = isApproved,
        description = description,
        clubImage = clubImage,
        ffsoId = ffsoId
    )

    private fun UserEntity.toModel() = User(
        id = id,
        name = name,
        email = email,
        docId = docId,
        adhId = adhId
    )

    private fun User.toEntity(clubId: Int, status: String) = UserEntity(
        id = id,
        name = name,
        email = email,
        clubId = clubId,
        status = status,
        docId = docId,
        adhId = adhId
    )
}
