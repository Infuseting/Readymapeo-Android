package fr.infuseting.readymapeo.data.remote

import fr.infuseting.readymapeo.data.model.LoginResponse
import fr.infuseting.readymapeo.data.model.User
import org.json.JSONObject

/**
 * Service d'authentification utilisant l'API REST.
 */
class AuthApiService(private val apiClient: ApiClient) {

    /**
     * Authentifie l'utilisateur et retourne le token.
     * POST /api/login
     */
    suspend fun login(email: String, password: String): LoginResponse {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()

        val response = apiClient.post("/api/login", body)
        val json = JSONObject(response)

        return LoginResponse(
            message = json.optString("message", ""),
            token = json.getString("token")
        )
    }

    /**
     * Récupère l'utilisateur actuellement connecté.
     * GET /api/user
     */
    suspend fun getCurrentUser(): User? {
        return try {
            val response = apiClient.get("/api/user")
            parseUser(JSONObject(response))
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun parseUser(json: JSONObject): User {
            return User(
                id = json.getInt("id"),
                name = json.getString("name"),
                email = json.getString("email"),
                docId = if (json.has("doc_id") && !json.isNull("doc_id")) json.getInt("doc_id") else null,
                adhId = if (json.has("adh_id") && !json.isNull("adh_id")) json.getInt("adh_id") else null,
                roles = if (json.has("roles") && !json.isNull("roles")) {
                    val arr = json.getJSONArray("roles")
                    List(arr.length()) { arr.getString(it) }
                } else null
            )
        }
    }
}
