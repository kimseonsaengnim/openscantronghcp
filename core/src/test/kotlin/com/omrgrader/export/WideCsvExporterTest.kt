package com.omrgrader.export

import com.omrgrader.domain.AnswerSerialization
import org.junit.Assert.assertTrue
import org.junit.Test

class WideCsvExporterTest {
    @Test
    fun emitsQuestionColumnsAndEscapesSpreadsheetText() {
        val exam = CsvExam(
            id = "exam-1",
            title = "Reading, \"Final\"",
            subject = "English",
            examDate = 1_700_000_000_000,
            questionCount = 2,
        )
        val submission = CsvSubmission(
            id = "submission-1",
            answerKeyVersion = 1,
            finalAnswers = listOf("A", "MULTIPLE").joinToString(AnswerSerialization.SEPARATOR),
            score = 1.0,
            maximumScore = 2.0,
            percentage = 50.0,
            gradingStatus = "GRADED",
            scannedAt = 1_700_000_000_000,
        )
        val student = CsvStudent("S-1", "Ada, \"A\"", "Class 1")
        val csv = WideCsvExporter.generate(
            exam,
            listOf(CsvExportSubmission(submission, student, listOf("A", "B"))),
        )

        assertTrue(csv.startsWith("submission_id,exam_id,exam_title,subject"))
        assertTrue(csv.contains("Q001,Q002"))
        assertTrue(csv.contains("\"Reading, \"\"Final\"\"\""))
        assertTrue(csv.contains("\"Ada, \"\"A\"\"\""))
        assertTrue(csv.contains(",A,MULTIPLE,1,0,0,1,0,1,2,50,"))
        assertTrue(csv.endsWith("\r\n"))
    }
}
