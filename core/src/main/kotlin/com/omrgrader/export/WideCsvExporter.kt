package com.omrgrader.export

import com.omrgrader.domain.AnswerSerialization
import com.omrgrader.domain.GradingEngine
import com.omrgrader.domain.GradingResult
import com.omrgrader.domain.ScoringRules
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CsvExam(
    val id: String,
    val title: String,
    val subject: String,
    val examDate: Long,
    val questionCount: Int,
)

data class CsvStudent(
    val studentNumber: String,
    val fullName: String,
    val section: String,
)

data class CsvSubmission(
    val id: String,
    val finalAnswers: String,
    val answerKeyVersion: Int,
    val score: Double,
    val maximumScore: Double,
    val percentage: Double?,
    val gradingStatus: String,
    val scannedAt: Long,
)

data class CsvExportSubmission(
    val submission: CsvSubmission,
    val student: CsvStudent?,
    val answerKey: List<String>,
)

object WideCsvExporter {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun generate(exam: CsvExam, submissions: List<CsvExportSubmission>): String {
        val questionHeaders = (1..exam.questionCount).map { "Q${it.toString().padStart(3, '0')}" }
        val headers = listOf(
            "submission_id",
            "exam_id",
            "exam_title",
            "subject",
            "student_id",
            "student_name",
            "section",
            "exam_date",
            "scanned_at",
            "answer_key_version",
            "batch_id",
            "grading_status",
        ) + questionHeaders + listOf(
            "correct_count",
            "incorrect_count",
            "blank_count",
            "invalid_count",
            "pending_review_count",
            "score",
            "max_score",
            "percentage",
            "passing_status",
        )

        return buildString {
            appendCsvRow(headers)
            submissions.forEach { record ->
                val submission = record.submission
                val answers = submission.finalAnswers.split(AnswerSerialization.SEPARATOR)
                require(answers.size == exam.questionCount) {
                    "Submission ${submission.id} has an invalid response count."
                }
                val grading = GradingEngine.grade(record.answerKey, answers, ScoringRules())
                val scannedAt = Instant.ofEpochMilli(submission.scannedAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val row = listOf(
                    submission.id,
                    exam.id,
                    exam.title,
                    exam.subject,
                    record.student?.studentNumber.orEmpty(),
                    record.student?.fullName.orEmpty(),
                    record.student?.section.orEmpty(),
                    Instant.ofEpochMilli(exam.examDate).atZone(ZoneId.systemDefault()).format(dateFormatter),
                    scannedAt,
                    submission.answerKeyVersion.toString(),
                    "",
                    submission.gradingStatus,
                ) + answers + grading.toCounts() + listOf(
                    submission.score.asCsvNumber(),
                    submission.maximumScore.asCsvNumber(),
                    submission.percentage?.asCsvNumber().orEmpty(),
                    "",
                )
                appendCsvRow(row)
            }

        }
    }

    private fun GradingResult?.toCounts(): List<String> = if (this == null) {
        listOf("", "", "", "", "")
    } else {
        listOf(
            correctCount.toString(),
            incorrectCount.toString(),
            blankCount.toString(),
            invalidCount.toString(),
            pendingReviewCount.toString(),
        )
    }

    private fun Double.asCsvNumber(): String =
        String.format(Locale.ROOT, "%.2f", this).trimEnd('0').trimEnd('.')

    private fun StringBuilder.appendCsvRow(cells: List<String>) {
        append(cells.joinToString(",") { cell ->
            if (cell.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
                "\"${cell.replace("\"", "\"\"")}\""
            } else {
                cell
            }
        })
        append("\r\n")
    }
}
