package com.omrgrader.domain

enum class ResponseClass {
    CORRECT,
    INCORRECT,
    BLANK,
    MULTIPLE,
    INVALID,
    PENDING_REVIEW,
}

data class ScoringRules(
    val pointsPerCorrect: Double = 1.0,
    val deductionPerIncorrect: Double = 0.0,
    val pointsForBlank: Double = 0.0,
) {
    init {
        require(pointsPerCorrect >= 0.0 && pointsPerCorrect.isFinite())
        require(deductionPerIncorrect >= 0.0 && deductionPerIncorrect.isFinite())
        require(pointsForBlank.isFinite())
    }
}

data class QuestionGrade(
    val questionNumber: Int,
    val response: String,
    val classification: ResponseClass,
    val points: Double,
)

data class GradingResult(
    val questions: List<QuestionGrade>,
    val correctCount: Int,
    val incorrectCount: Int,
    val blankCount: Int,
    val invalidCount: Int,
    val pendingReviewCount: Int,
    val score: Double,
    val maximumScore: Double,
    val percentage: Double?,
    val gradingStatus: String,
)

object GradingEngine {
    const val BLANK = "BLANK"
    const val MULTIPLE = "MULTIPLE"
    const val INVALID = "INVALID"
    const val PENDING_REVIEW = "PENDING_REVIEW"

    fun grade(
        answerKey: List<String>,
        responses: List<String>,
        rules: ScoringRules = ScoringRules(),
    ): GradingResult {
        require(answerKey.isNotEmpty()) { "An answer key must contain at least one answer." }
        require(answerKey.size == responses.size) { "Response count must match the answer key." }
        require(answerKey.none { it.isBlank() }) { "Answer key entries cannot be blank." }

        val questions = answerKey.zip(responses).mapIndexed { index, (correct, response) ->
            val classification = when {
                response == PENDING_REVIEW -> ResponseClass.PENDING_REVIEW
                response == MULTIPLE -> ResponseClass.MULTIPLE
                response == INVALID -> ResponseClass.INVALID
                response == BLANK || response.isBlank() -> ResponseClass.BLANK
                response == correct -> ResponseClass.CORRECT
                else -> ResponseClass.INCORRECT
            }
            val points = when (classification) {
                ResponseClass.CORRECT -> rules.pointsPerCorrect
                ResponseClass.INCORRECT -> -rules.deductionPerIncorrect
                ResponseClass.BLANK -> rules.pointsForBlank
                ResponseClass.MULTIPLE, ResponseClass.INVALID, ResponseClass.PENDING_REVIEW -> 0.0
            }
            QuestionGrade(index + 1, response, classification, points)
        }

        val maximumScore = answerKey.size * rules.pointsPerCorrect
        val score = questions.sumOf(QuestionGrade::points)
        val pendingCount = questions.count { it.classification == ResponseClass.PENDING_REVIEW }
        return GradingResult(
            questions = questions,
            correctCount = questions.count { it.classification == ResponseClass.CORRECT },
            incorrectCount = questions.count { it.classification == ResponseClass.INCORRECT },
            blankCount = questions.count { it.classification == ResponseClass.BLANK },
            invalidCount = questions.count {
                it.classification == ResponseClass.MULTIPLE || it.classification == ResponseClass.INVALID
            },
            pendingReviewCount = pendingCount,
            score = score,
            maximumScore = maximumScore,
            percentage = maximumScore.takeIf { it > 0.0 }?.let { score / it * 100.0 },
            gradingStatus = if (pendingCount > 0) "PENDING_REVIEW" else "GRADED",
        )
    }
}

object AnswerSerialization {
    const val SEPARATOR = "\u001f"
}
