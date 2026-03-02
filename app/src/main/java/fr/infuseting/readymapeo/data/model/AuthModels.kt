package fr.infuseting.readymapeo.data.model

/**
 * Requête de login envoyée à POST /api/login.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Réponse du serveur après un login réussi.
 */
data class LoginResponse(
    val message: String,
    val token: String
)

/**
 * Requête de mise à jour d'un club (PUT /api/clubs/{id}).
 */
data class UpdateClubRequest(
    val clubName: String? = null,
    val clubStreet: String? = null,
    val clubCity: String? = null,
    val clubPostalCode: String? = null,
    val ffsoId: String? = null,
    val description: String? = null
)
