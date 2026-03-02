package fr.infuseting.readymapeo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * File d'attente des actions effectuées hors-ligne.
 * Ces actions seront rejouées lors du retour de la connectivité.
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String,     // UPDATE_CLUB, APPROVE_MEMBER, REJECT_MEMBER, REMOVE_MEMBER
    val clubId: Int,
    val userId: Int? = null,    // Pour les actions sur les membres
    val payload: String? = null, // Body JSON pour UPDATE_CLUB
    val createdAt: Long = System.currentTimeMillis()
)
