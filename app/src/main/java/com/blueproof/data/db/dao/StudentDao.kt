package com.blueproof.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.blueproof.data.db.entity.StudentEntity

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: StudentEntity): Long

    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveStudents(): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY name ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    suspend fun findByStudentId(studentId: String): StudentEntity?

    @Query("SELECT * FROM students WHERE deviceId = :deviceId LIMIT 1")
    suspend fun findByDeviceId(deviceId: String): StudentEntity?

    @Query("SELECT COUNT(*) FROM students")
    suspend fun countStudents(): Int

    @Update
    suspend fun update(student: StudentEntity)

    @Query("DELETE FROM students WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: String): Int
}
