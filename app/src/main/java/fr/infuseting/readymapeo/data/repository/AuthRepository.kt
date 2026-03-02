package fr.infuseting.readymapeo.data.repository

import fr.infuseting.readymapeo.data.local.TokenManager
import fr.infuseting.readymapeo.data.model.LoginResponse
import fr.infuseting.readymapeo.data.model.User
import fr.infuseting.readymapeo.data.remote.AuthApiService

/**
 * Repository pour l'authentification.
 * Coordonne les appels réseau et le stockage local du token.
 */
class AuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Connecte l'utilisateur, stocke le token et retourne la réponse.
     */
    suspend fun login(email: String, password: String): LoginResponse {
        val response = authApiService.login(email, password)
        tokenManager.saveToken(response.token)
        return response
    }

    /**
     * Récupère l'utilisateur connecté et sauvegarde son ID.
     */
    suspend fun getCurrentUser(): User? {
        val user = authApiService.getCurrentUser()
        user?.let { tokenManager.saveUserId(it.id) }
        return user
    }

    fun isLoggedIn(): Boolean = tokenManager.fetchToken() != null

    fun logout() {
        tokenManager.clearAll()
    }
}
