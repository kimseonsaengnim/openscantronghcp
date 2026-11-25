package com.omrgrader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GradingEngineTest {
    @Test
    fun gradesAnswersAndKeepsUncertainResponsesOutOfCorrectCounts() {
        val result = GradingEngine.grade(
            answerKey = listOf("A", "B", "C", "D", "A"),
            responses = listOf("A", "D", "BLANK", "MULTIPLE", "PENDING_REVIEW"),
            rules = ScoringRules(pointsPerCorrect = 2.0, deductionPerIncorrect = 0.5),
        )

        assertEquals(1, result.correctCount)
        assertEquals(1, result.incorrectCount)
        assertEquals(1, result.blankCount)
        assertEquals(1, result.invalidCount)
        assertEquals(1, result.pendingReviewCount)
        assertEquals(1.5, result.score, 0.0)
        assertEquals(10.0, result.maximumScore, 0.0)
        assertEquals("PENDING_REVIEW", result.gradingStatus)
    }

    @Test
    fun avoidsPercentageDivisionByZero() {
        val result = GradingEngine.grade(
            answerKey = listOf("A"),
            responses = listOf("A"),
            rules = ScoringRules(pointsPerCorrect = 0.0),
        )

        assertEquals(0.0, result.maximumScore, 0.0)
        assertNull(result.percentage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsResponseCountMismatch() {
        GradingEngine.grade(answerKey = listOf("A", "B"), responses = listOf("A"))
    }
}
