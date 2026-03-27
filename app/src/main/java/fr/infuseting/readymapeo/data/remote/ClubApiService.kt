package fr.infuseting.readymapeo.data.remote

import fr.infuseting.readymapeo.data.model.Club
import fr.infuseting.readymapeo.data.model.UpdateClubRequest
import fr.infuseting.readymapeo.data.model.User
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service API pour la gestion des clubs.
 */
class ClubApiService(private val apiClient: ApiClient) {

    data class ClubDetailsPayload(
        val club: Club,
        val approvedMembers: List<User>,
        val pendingMembers: List<User>
    )

    /**
     * Récupère les clubs gérés par l'utilisateur connecté.
     * GET /api/user/managed-clubs
     */
    suspend fun getManagedClubs(): List<Club> {
        val response = apiClient.get("/api/user/managed-clubs")
        return parseClubList(response)
    }

    suspend fun getClubDetails(clubId: Int): Club {
        return getClubDetailsWithMembers(clubId).club
    }

    /**
     * Récupère le club et ses membres depuis GET /api/clubs/{id}.
     */
    suspend fun getClubDetailsWithMembers(clubId: Int): ClubDetailsPayload {
        val response = apiClient.get("/api/clubs/$clubId")
        val json = JSONObject(response)

        val payloadRoot = json.optJSONObject("data") ?: json
        val clubJson = payloadRoot.optJSONObject("club") ?: payloadRoot

        // Détecter isManager aussi bien au niveau du payload que dans l'objet club
        val isManagerFlag = when {
            payloadRoot.has("isManager") && !payloadRoot.isNull("isManager") -> payloadRoot.optBoolean("isManager")
            payloadRoot.has("is_manager") && !payloadRoot.isNull("is_manager") -> payloadRoot.optBoolean("is_manager")
            clubJson.has("isManager") && !clubJson.isNull("isManager") -> clubJson.optBoolean("isManager")
            clubJson.has("is_manager") && !clubJson.isNull("is_manager") -> clubJson.optBoolean("is_manager")
            else -> false
        }

        // Récupérer membres approuvés et pending : privilégier payloadRoot (parfois l'API expose ces tableaux dans data)
        val approvedArr = payloadRoot.optJSONArray("members") ?: clubJson.optJSONArray("members")
        val pendingArr = payloadRoot.optJSONArray("pending_members") ?: clubJson.optJSONArray("pending_members")

        return ClubDetailsPayload(
            club = parseClub(clubJson, isManagerFlag),
            approvedMembers = parseUserList(approvedArr),
            pendingMembers = parseUserList(pendingArr)
        )
    }

    /**
     * Met à jour un club.
     * PUT /api/clubs/{id}
     * Retourne le corps de réponse (JSON) pour que l'appelant puisse vérifier le champ `status`.
     */
    suspend fun updateClub(clubId: Int, request: UpdateClubRequest): String {
        val body = JSONObject().apply {
            request.clubName?.let { put("club_name", it) }
            request.clubStreet?.let { put("club_street", it) }
            request.clubCity?.let { put("club_city", it) }
            request.clubPostalCode?.let { put("club_postal_code", it) }
            request.ffsoId?.let { put("ffso_id", it) }
            request.description?.let { put("description", it) }
        }.toString()

        return apiClient.put("/api/clubs/$clubId", body)
    }

    /**
     * Approuve un membre en attente.
     * POST /api/clubs/{clubId}/members/{userId}/approve
     */
    suspend fun approveMember(clubId: Int, userId: Int) {
        apiClient.post("/api/clubs/$clubId/members/$userId/approve")
    }

    /**
     * Rejette un membre en attente.
     * POST /api/clubs/{clubId}/members/{userId}/reject
     */
    suspend fun rejectMember(clubId: Int, userId: Int) {
        apiClient.post("/api/clubs/$clubId/members/$userId/reject")
    }

    /**
     * Retire un membre du club.
     * DELETE /api/clubs/{clubId}/members/{userId}
     */
    suspend fun removeMember(clubId: Int, userId: Int) {
        apiClient.delete("/api/clubs/$clubId/members/$userId")
    }

    private fun parseClubList(response: String): List<Club> {
        val trimmed = response.trim()
        val arr = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val json = JSONObject(trimmed)
            json.optJSONArray("data") ?: JSONArray()
        }
        return List(arr.length()) { parseClub(arr.getJSONObject(it)) }
    }

    companion object {
        fun parseClub(json: JSONObject, isManager : Boolean = false): Club {
            return Club(
                clubId = json.getInt("club_id"),
                clubName = json.getString("club_name"),
                clubStreet = json.optString("club_street", ""),
                clubCity = json.optString("club_city", ""),
                clubPostalCode = json.optString("club_postal_code", ""),
                isApproved = json.optBoolean("is_approved", false),
                isManager =  isManager,
                description = json.optNullableString("description"),
                clubImage = json.optNullableString("club_image"),
                ffsoId = json.optNullableString("ffso_id")
            )
        }

        fun parseUserList(arr: JSONArray?): List<User> {
            if (arr == null) return emptyList()
            return List(arr.length()) { AuthApiService.parseUser(arr.getJSONObject(it)) }
        }

        private fun JSONObject.optNullableString(key: String): String? {
            if (!has(key) || isNull(key)) return null
            val value = optString(key, "")
            return if (value.isBlank()) null else value
        }
    }
}
