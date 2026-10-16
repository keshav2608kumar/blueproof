package com.blueproof.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.blueproof.data.db.dao.AttendanceDao
import com.blueproof.data.db.dao.AuditLogDao
import com.blueproof.data.db.dao.SessionDao
import com.blueproof.data.db.dao.StudentDao
import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.data.db.entity.AuditLogEntity
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.db.entity.StudentEntity

@Database(
    entities = [
        StudentEntity::class,
        SessionEntity::class,
        AttendanceRecordEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BlueProofDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: BlueProofDatabase? = null

        fun getInstance(context: Context): BlueProofDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlueProofDatabase::class.java,
                    "blueproof_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
