package fr.infuseting.readymapeo.data.model

/**
 * Représente un club sportif.
 */
data class Club(
    val clubId: Int,
    val clubName: String,
    val clubStreet: String,
    val clubCity: String,
    val clubPostalCode: String,
    val isApproved: Boolean,
    val description: String? = null,
    val clubImage: String? = null,
    val ffsoId: String? = null
)
