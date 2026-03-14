package fr.infuseting.readymapeo.sync

import android.util.Log
import fr.infuseting.readymapeo.data.local.dao.PendingActionDao
import fr.infuseting.readymapeo.data.remote.ClubApiService
import fr.infuseting.readymapeo.data.model.UpdateClubRequest
import org.json.JSONObject

/**
 * Gestionnaire de synchronisation.
 * Rejoue les actions en attente (PendingAction) lorsque le réseau redevient disponible.
 */
class SyncManager(
    private val pendingActionDao: PendingActionDao,
    private val clubApiService: ClubApiService
) {
    companion object {
        private const val TAG = "SyncManager"

        // Types d'actions
        const val ACTION_UPDATE_CLUB = "UPDATE_CLUB"
        const val ACTION_APPROVE_MEMBER = "APPROVE_MEMBER"
        const val ACTION_REJECT_MEMBER = "REJECT_MEMBER"
        const val ACTION_REMOVE_MEMBER = "REMOVE_MEMBER"
    }

    /**
     * Tente de rejouer toutes les actions en attente.
     * Les actions réussies sont supprimées de la file.
     * Les actions échouées restent pour un prochain essai.
     */
    suspend fun syncPendingActions() {
        val actions = pendingActionDao.getAllPendingActions()
        if (actions.isEmpty()) {
            Log.d(TAG, "Aucune action en attente à synchroniser.")
            return
        }

        Log.d(TAG, "Synchronisation de ${actions.size} action(s) en attente...")

        for (action in actions) {
            try {
                when (action.actionType) {
                    ACTION_UPDATE_CLUB -> {
                        val payload = action.payload?.let { JSONObject(it) }
                        if (payload != null) {
                            val request = UpdateClubRequest(
                                clubName = payload.optNullableString("club_name"),
                                clubStreet = payload.optNullableString("club_street"),
                                clubCity = payload.optNullableString("club_city"),
                                clubPostalCode = payload.optNullableString("club_postal_code"),
                                ffsoId = payload.optNullableString("ffso_id"),
                                description = payload.optNullableString("description")
                            )
                            clubApiService.updateClub(action.clubId, request)
                        }
                    }

                    ACTION_APPROVE_MEMBER -> {
                        action.userId?.let {
                            clubApiService.approveMember(action.clubId, it)
                        }
                    }

                    ACTION_REJECT_MEMBER -> {
                        action.userId?.let {
                            clubApiService.rejectMember(action.clubId, it)
                        }
                    }

                    ACTION_REMOVE_MEMBER -> {
                        action.userId?.let {
                            clubApiService.removeMember(action.clubId, it)
                        }
                    }
                }

                // Succès : supprimer l'action de la file
                pendingActionDao.delete(action)
                Log.d(TAG, "Action ${action.actionType} (id=${action.id}) synchronisée avec succès.")

            } catch (e: Exception) {
                Log.e(TAG, "Échec de la synchronisation de l'action ${action.actionType} (id=${action.id}): ${e.message}")
                // On continue avec les autres actions
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return if (value.isBlank()) null else value
    }
}
