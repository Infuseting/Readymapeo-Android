package fr.infuseting.readymapeo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un membre de club stocké localement.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val clubId: Int,           // FK vers le club
    val status: String,        // "approved" ou "pending"
    val docId: Int? = null,
    val adhId: Int? = null
)
