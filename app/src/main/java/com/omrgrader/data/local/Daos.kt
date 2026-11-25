package com.omrgrader.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExaminationDao {
    @Query("SELECT * FROM examinations WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<ExaminationEntity>>

    @Query("SELECT * FROM examinations WHERE id = :id")
    suspend fun findById(id: String): ExaminationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(examination: ExaminationEntity)

    @Query(
        """UPDATE examinations SET activeAnswerKeyVersion = :version, updatedAt = :updatedAt
        WHERE id = :examinationId""",
    )
    suspend fun activateAnswerKey(examinationId: String, version: Int, updatedAt: Long)
}

@Dao
interface AnswerKeyDao {
    @Query(
        "SELECT * FROM answer_keys WHERE examinationId = :examinationId ORDER BY version DESC LIMIT 1",
    )
    suspend fun latest(examinationId: String): AnswerKeyEntity?

    @Query("SELECT * FROM answer_keys WHERE examinationId = :examinationId AND version = :version")
    suspend fun find(examinationId: String, version: Int): AnswerKeyEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(answerKey: AnswerKeyEntity)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE studentNumber = :studentNumber LIMIT 1")
    suspend fun findByNumber(studentNumber: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(student: StudentEntity)
}

data class SubmissionWithStudent(
    @Embedded val submission: SubmissionEntity,
    @Relation(parentColumn = "studentId", entityColumn = "id")
    val student: StudentEntity?,
)

@Dao
interface SubmissionDao {
    @Transaction
    @Query("SELECT * FROM submissions WHERE examinationId = :examinationId ORDER BY scannedAt")
    fun observeForExam(examinationId: String): Flow<List<SubmissionWithStudent>>

    @Transaction
    @Query("SELECT * FROM submissions WHERE examinationId = :examinationId ORDER BY scannedAt")
    suspend fun listForExam(examinationId: String): List<SubmissionWithStudent>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(submission: SubmissionEntity)
}
