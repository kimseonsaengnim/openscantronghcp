package com.omrgrader.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "examinations")
data class ExaminationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val description: String,
    val examDate: Long,
    val questionCount: Int,
    val totalPoints: Double,
    val answerChoices: String,
    val activeAnswerKeyVersion: Int?,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "answer_keys",
    foreignKeys = [
        ForeignKey(
            entity = ExaminationEntity::class,
            parentColumns = ["id"],
            childColumns = ["examinationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("examinationId"), Index(value = ["examinationId", "version"], unique = true)],
)
data class AnswerKeyEntity(
    @PrimaryKey val id: String,
    val examinationId: String,
    val version: Int,
    val answers: String,
    val createdAt: Long,
)

@Entity(
    tableName = "students",
    indices = [Index(value = ["studentNumber"], unique = true)],
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val studentNumber: String,
    val fullName: String,
    val section: String,
    val additionalInformation: String = "",
)

@Entity(
    tableName = "submissions",
    foreignKeys = [
        ForeignKey(
            entity = ExaminationEntity::class,
            parentColumns = ["id"],
            childColumns = ["examinationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("examinationId"), Index("studentId"), Index(value = ["examinationId", "studentId"])],
)
data class SubmissionEntity(
    @PrimaryKey val id: String,
    val examinationId: String,
    val studentId: String?,
    val answerKeyVersion: Int,
    val recognizedAnswers: String,
    val finalAnswers: String,
    val score: Double,
    val maximumScore: Double,
    val percentage: Double?,
    val gradingStatus: String,
    val scannedAt: Long,
)
