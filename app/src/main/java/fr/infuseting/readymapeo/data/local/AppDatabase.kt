package fr.infuseting.readymapeo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fr.infuseting.readymapeo.data.local.dao.ClubDao
import fr.infuseting.readymapeo.data.local.dao.PendingActionDao
import fr.infuseting.readymapeo.data.local.entity.ClubEntity
import fr.infuseting.readymapeo.data.local.entity.PendingActionEntity
import fr.infuseting.readymapeo.data.local.entity.UserEntity

/**
 * Base de données Room principale de l'application.
 */
@Database(
    entities = [ClubEntity::class, UserEntity::class, PendingActionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clubDao(): ClubDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "readymapeo_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
