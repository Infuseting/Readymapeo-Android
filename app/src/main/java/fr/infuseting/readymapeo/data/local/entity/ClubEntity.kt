package fr.infuseting.readymapeo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un club stocké localement.
 */
@Entity(tableName = "clubs")
data class ClubEntity(
    @PrimaryKey val clubId: Int,
    val clubName: String,
    val clubStreet: String,
    val clubCity: String,
    val clubPostalCode: String,
    val isApproved: Boolean,
    val description: String? = null,
    val clubImage: String? = null,
    val ffsoId: String? = null,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
