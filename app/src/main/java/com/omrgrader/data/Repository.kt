package com.omrgrader.data

import androidx.room.withTransaction
import com.omrgrader.data.local.AnswerKeyEntity
import com.omrgrader.data.local.ExaminationDao
import com.omrgrader.data.local.ExaminationEntity
import com.omrgrader.data.local.GraderDatabase
import com.omrgrader.data.local.StudentEntity
import com.omrgrader.data.local.SubmissionEntity
import com.omrgrader.data.local.SubmissionWithStudent
import com.omrgrader.domain.AnswerSerialization
import com.omrgrader.domain.GradingEngine
import com.omrgrader.domain.ScoringRules
import com.omrgrader.export.CsvExam
import com.omrgrader.export.CsvExportSubmission
import com.omrgrader.export.CsvStudent
import com.omrgrader.export.CsvSubmission
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class GraderRepository @Inject constructor(
    private val database: GraderDatabase,
) {
    fun observeExaminations(): Flow<List<ExaminationEntity>> =
        database.examinationDao().observeActive()

    suspend fun createExamination(
        title: String,
        subject: String,
        questionCount: Int,
        answerChoices: List<String>,
    ): ExaminationEntity {
        require(title.isNotBlank()) { "Exam title is required." }
        require(questionCount in 1..200) { "Question count must be between 1 and 200." }
        require(answerChoices.size >= 2 && answerChoices.distinct().size == answerChoices.size) {
            "Provide at least two unique answer labels."
        }
        require(answerChoices.all {
            it.isNotBlank() && '\u001f' !in it && ',' !in it && '\n' !in it && '\r' !in it
        }) {
            "Answer labels cannot be blank or contain commas, line breaks, or the reserved separator."
        }
        val now = System.currentTimeMillis()
        val exam = ExaminationEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            subject = subject.trim(),
            description = "",
            examDate = now,
            questionCount = questionCount,
            totalPoints = questionCount.toDouble(),
            answerChoices = answerChoices.joinToString(ANSWER_SEPARATOR),
            activeAnswerKeyVersion = null,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
        )
        database.examinationDao().insert(exam)
        return exam
    }

    suspend fun findExamination(examinationId: String): ExaminationEntity? =
        database.examinationDao().findById(examinationId)

    suspend fun answerKey(examinationId: String): AnswerKeyEntity? {
        val exam = requireNotNull(findExamination(examinationId)) { "Examination not found." }
        val version = exam.activeAnswerKeyVersion ?: return null
        return database.answerKeyDao().find(examinationId, version)
    }

    suspend fun saveAnswerKey(examinationId: String, answers: List<String>): AnswerKeyEntity =
        database.withTransaction {
            val exams = database.examinationDao()
            val exam = requireNotNull(exams.findById(examinationId)) { "Examination not found." }
            val choices = exam.answerChoices.split(ANSWER_SEPARATOR)
            require(answers.size == exam.questionCount) {
                "Enter exactly ${exam.questionCount} answers."
            }
            require(answers.all { it in choices }) {
                "Every answer must match one of the exam's answer labels."
            }
            val answerString = answers.joinToString(ANSWER_SEPARATOR)
            val latest = database.answerKeyDao().latest(examinationId)
            if (latest?.answers == answerString) return@withTransaction latest

            val key = AnswerKeyEntity(
                id = UUID.randomUUID().toString(),
                examinationId = examinationId,
                version = (latest?.version ?: 0) + 1,
                answers = answerString,
                createdAt = System.currentTimeMillis(),
            )
            database.answerKeyDao().insert(key)
            exams.activateAnswerKey(examinationId, key.version, key.createdAt)
            key
        }

    suspend fun addManualSubmission(
        examinationId: String,
        studentNumber: String,
        studentName: String,
        section: String,
        responses: List<String>,
    ): SubmissionEntity = database.withTransaction {
        val exam = requireNotNull(database.examinationDao().findById(examinationId)) {
            "Examination not found."
        }
        val activeVersion = requireNotNull(exam.activeAnswerKeyVersion) {
            "Save an answer key before grading submissions."
        }
        val key = requireNotNull(database.answerKeyDao().find(examinationId, activeVersion)) {
            "The active answer key is missing."
        }
        val answerKeyAnswers = key.answers.split(ANSWER_SEPARATOR)
        require(responses.size == exam.questionCount) {
            "Enter exactly ${exam.questionCount} responses."
        }
        val choices = exam.answerChoices.split(ANSWER_SEPARATOR)
        require(responses.all { it in choices || it in ALLOWED_SPECIAL_RESPONSES }) {
            "Responses must be an answer label, BLANK, MULTIPLE, INVALID, or PENDING_REVIEW."
        }

        val cleanedNumber = studentNumber.trim()
        val cleanedName = studentName.trim()
        require(cleanedNumber.isNotEmpty() && cleanedName.isNotEmpty()) {
            "Student number and name are required."
        }
        val studentDao = database.studentDao()
        val existingStudent = studentDao.findByNumber(cleanedNumber)
        val student = existingStudent ?: StudentEntity(
            id = UUID.randomUUID().toString(),
            studentNumber = cleanedNumber,
            fullName = cleanedName,
            section = section.trim(),
        ).also { studentDao.insert(it) }

        val result = GradingEngine.grade(answerKeyAnswers, responses, ScoringRules())
        val submission = SubmissionEntity(
            id = UUID.randomUUID().toString(),
            examinationId = examinationId,
            studentId = student.id,
            answerKeyVersion = activeVersion,
            recognizedAnswers = responses.joinToString(ANSWER_SEPARATOR),
            finalAnswers = responses.joinToString(ANSWER_SEPARATOR),
            score = result.score,
            maximumScore = result.maximumScore,
            percentage = result.percentage,
            gradingStatus = result.gradingStatus,
            scannedAt = System.currentTimeMillis(),
        )
        database.submissionDao().insert(submission)
        submission
    }

    fun observeSubmissions(examinationId: String): Flow<List<SubmissionWithStudent>> =
        database.submissionDao().observeForExam(examinationId)

    suspend fun exportSubmissions(examinationId: String): Pair<CsvExam, List<CsvExportSubmission>> {
        val exam = requireNotNull(findExamination(examinationId)) { "Examination not found." }
        val records = database.submissionDao().listForExam(examinationId)
        val csvExam = CsvExam(
            id = exam.id,
            title = exam.title,
            subject = exam.subject,
            examDate = exam.examDate,
            questionCount = exam.questionCount,
        )
        return csvExam to records.map { record ->
            val key = requireNotNull(
                database.answerKeyDao().find(examinationId, record.submission.answerKeyVersion),
            ) {
                "Answer key v${record.submission.answerKeyVersion} is missing for submission ${record.submission.id}."
            }
            CsvExportSubmission(
                submission = CsvSubmission(
                    id = record.submission.id,
                    finalAnswers = record.submission.finalAnswers,
                    answerKeyVersion = record.submission.answerKeyVersion,
                    score = record.submission.score,
                    maximumScore = record.submission.maximumScore,
                    percentage = record.submission.percentage,
                    gradingStatus = record.submission.gradingStatus,
                    scannedAt = record.submission.scannedAt,
                ),
                student = record.student?.let {
                    CsvStudent(it.studentNumber, it.fullName, it.section)
                },
                answerKey = key.answers.split(ANSWER_SEPARATOR),
            )
        }
    }

    companion object {
        const val ANSWER_SEPARATOR = AnswerSerialization.SEPARATOR
        val ALLOWED_SPECIAL_RESPONSES =
            setOf(GradingEngine.BLANK, GradingEngine.MULTIPLE, GradingEngine.INVALID, GradingEngine.PENDING_REVIEW)
    }
}
