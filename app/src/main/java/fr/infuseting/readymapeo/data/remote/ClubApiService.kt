package fr.infuseting.readymapeo.data.remote

import fr.infuseting.readymapeo.data.model.Club
import fr.infuseting.readymapeo.data.model.UpdateClubRequest
import fr.infuseting.readymapeo.data.model.User
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service API pour la gestion des clubs.
 * Utilise HttpURLConnection via ApiClient + parsing JSON manuel avec org.json.
 */
class ClubApiService(private val apiClient: ApiClient) {

    /**
     * Récupère les clubs gérés par l'utilisateur connecté.
     * GET /api/me/managed-clubs
     */
    suspend fun getManagedClubs(): List<Club> {
        val response = apiClient.get("/api/me/managed-clubs")
        return parseClubList(response)
    }

    /**
     * Récupère les détails d'un club.
     * GET /api/clubs/{id}
     */
    suspend fun getClubDetails(clubId: Int): Club {
        val response = apiClient.get("/api/clubs/$clubId")
        val json = JSONObject(response)

        // L'API peut retourner { "club": {...} } ou directement {...}
        val clubJson = when {
            json.has("club") -> json.getJSONObject("club")
            json.has("data") -> {
                val data = json.get("data")
                if (data is JSONObject) {
                    if (data.has("club")) data.getJSONObject("club") else data
                } else json
            }
            else -> json
        }
        return parseClub(clubJson)
    }

    /**
     * Récupère les membres d'un club (approuvés et en attente).
     * GET /api/clubs/{id}/members
     */
    suspend fun getClubMembers(clubId: Int): Pair<List<User>, List<User>> {
        val response = apiClient.get("/api/clubs/$clubId/members")

        // Essai de parser comme { "members": [...], "pending_members": [...] }
        return try {
            val json = JSONObject(response)
            val approved = parseUserList(json.optJSONArray("members"))
            val pending = parseUserList(json.optJSONArray("pending_members"))
            Pair(approved, pending)
        } catch (e: Exception) {
            // Fallback : array brut avec pivot.status
            try {
                val arr = JSONArray(response)
                val approved = mutableListOf<User>()
                val pending = mutableListOf<User>()
                for (i in 0 until arr.length()) {
                    val userJson = arr.getJSONObject(i)
                    val status = userJson.optJSONObject("pivot")
                        ?.optString("status", "approved") ?: "approved"
                    val user = AuthApiService.parseUser(userJson)
                    if (status.equals("approved", ignoreCase = true)) {
                        approved.add(user)
                    } else {
                        pending.add(user)
                    }
                }
                Pair(approved, pending)
            } catch (e2: Exception) {
                Pair(emptyList(), emptyList())
            }
        }
    }

    /**
     * Met à jour un club.
     * PUT /api/clubs/{id}
     */
    suspend fun updateClub(clubId: Int, request: UpdateClubRequest): Club {
        val body = JSONObject().apply {
            request.clubName?.let { put("club_name", it) }
            request.clubStreet?.let { put("club_street", it) }
            request.clubCity?.let { put("club_city", it) }
            request.clubPostalCode?.let { put("club_postal_code", it) }
            request.ffsoId?.let { put("ffso_id", it) }
            request.description?.let { put("description", it) }
        }.toString()

        val response = apiClient.put("/api/clubs/$clubId", body)
        return parseClub(JSONObject(response))
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

    // ── Parsing Helpers ─────────────────────────────────────

    private fun parseClubList(response: String): List<Club> {
        return try {
            // Cas : { "data": [...] }
            val json = JSONObject(response)
            val arr = json.getJSONArray("data")
            List(arr.length()) { parseClub(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            try {
                // Cas : array direct [...]
                val arr = JSONArray(response)
                List(arr.length()) { parseClub(arr.getJSONObject(it)) }
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    companion object {
        fun parseClub(json: JSONObject): Club {
            return Club(
                clubId = json.getInt("club_id"),
                clubName = json.getString("club_name"),
                clubStreet = json.optString("club_street", ""),
                clubCity = json.optString("club_city", ""),
                clubPostalCode = json.optString("club_postal_code", ""),
                isApproved = json.optBoolean("is_approved", false),
                description = json.optString("description", null),
                clubImage = json.optString("club_image", null),
                ffsoId = json.optString("ffso_id", null)
            )
        }

        fun parseUserList(arr: JSONArray?): List<User> {
            if (arr == null) return emptyList()
            return List(arr.length()) { AuthApiService.parseUser(arr.getJSONObject(it)) }
        }
    }
}
