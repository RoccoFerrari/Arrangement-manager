package com.example.arrangement_manager

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(entities = [User::class, Table_::class, MenuItem::class], version = 5)
abstract class ArrangementDatabase : RoomDatabase() {
    abstract fun arrangementDao(): ArrangementDAO

    companion object {
        @Volatile
        private var INSTANCE: ArrangementDatabase? = null
        fun getDatabase(context: Context, scope: CoroutineScope): ArrangementDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArrangementDatabase::class.java,
                    "arrangement_database"
                )   .addCallback(ArrangementDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
    // Callback per il databse: viene chiamato quando il databe viene creato o aperto
    private class ArrangementDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
        }
    }

}