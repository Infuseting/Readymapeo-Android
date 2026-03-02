package fr.infuseting.readymapeo.data.model

/**
 * Représente un utilisateur / membre d'un club.
 */
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val docId: Int? = null,
    val adhId: Int? = null,
    val roles: List<String>? = null
)
