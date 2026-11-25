package com.omrgrader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExaminationEntity::class,
        AnswerKeyEntity::class,
        StudentEntity::class,
        SubmissionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GraderDatabase : RoomDatabase() {
    abstract fun examinationDao(): ExaminationDao
    abstract fun answerKeyDao(): AnswerKeyDao
    abstract fun studentDao(): StudentDao
    abstract fun submissionDao(): SubmissionDao
}
