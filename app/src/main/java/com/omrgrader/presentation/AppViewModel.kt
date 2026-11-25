package com.omrgrader.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omrgrader.data.GraderRepository
import com.omrgrader.data.local.AnswerKeyEntity
import com.omrgrader.data.local.ExaminationEntity
import com.omrgrader.data.local.SubmissionWithStudent
import com.omrgrader.export.WideCsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingCsvExport(val fileName: String, val content: String)

data class AppUiState(
    val examinations: List<ExaminationEntity> = emptyList(),
    val selectedExamination: ExaminationEntity? = null,
    val answerKey: AnswerKeyEntity? = null,
    val submissions: List<SubmissionWithStudent> = emptyList(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val pendingCsvExport: PendingCsvExport? = null,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: GraderRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()
    private var submissionJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.observeExaminations().collect { exams ->
                    mutableState.update { it.copy(examinations = exams) }
                }
            } catch (exception: Exception) {
                showError(exception)
            }
        }
    }

    fun createExamination(title: String, subject: String, count: String, choices: String) {
        viewModelScope.launch {
            runOperation {
                val labels = choices.split(',').map(String::trim).filter(String::isNotEmpty)
                val exam = repository.createExamination(
                    title = title,
                    subject = subject,
                    questionCount = count.toIntOrNull()
                        ?: throw IllegalArgumentException("Enter a valid question count."),
                    answerChoices = labels,
                )
                loadExamination(exam.id)
                mutableState.update { it.copy(message = "Examination created.") }
            }
        }
    }

    fun selectExamination(id: String) {
        viewModelScope.launch {
            runOperation { loadExamination(id) }
        }
    }

    private suspend fun loadExamination(id: String) {
        val exam = requireNotNull(repository.findExamination(id)) { "Examination not found." }
        val key = repository.answerKey(id)
        submissionJob?.cancel()
        mutableState.update {
            it.copy(selectedExamination = exam, answerKey = key, submissions = emptyList())
        }
        submissionJob = viewModelScope.launch {
            try {
                repository.observeSubmissions(id).collect { submissions ->
                    mutableState.update { state -> state.copy(submissions = submissions) }
                }
            } catch (exception: Exception) {
                showError(exception)
            }
        }
    }

    fun saveAnswerKey(examinationId: String, enteredAnswers: List<String>) {
        viewModelScope.launch {
            runOperation {
                val saved = repository.saveAnswerKey(examinationId, enteredAnswers)
                mutableState.update {
                    it.copy(
                        answerKey = saved,
                        message = "Answer key version ${saved.version} saved.",
                    )
                }
            }
        }
    }

    fun addManualSubmission(
        examinationId: String,
        studentNumber: String,
        studentName: String,
        section: String,
        enteredResponses: List<String>,
    ) {
        viewModelScope.launch {
            runOperation {
                val exam = requireNotNull(repository.findExamination(examinationId)) {
                    "Examination not found."
                }
                val choices = exam.answerChoices.split(GraderRepository.ANSWER_SEPARATOR)
                val responses = enteredResponses.map { entered ->
                    val token = entered.trim()
                    if (token.equals("blank", true)) {
                        "BLANK"
                    } else if (token.equals("multiple", true)) {
                        "MULTIPLE"
                    } else if (token.equals("invalid", true)) {
                        "INVALID"
                    } else if (token.equals("pending_review", true)) {
                        "PENDING_REVIEW"
                    } else {
                        choices.firstOrNull { it.equals(token, ignoreCase = true) } ?: token
                    }
                }
                repository.addManualSubmission(
                    examinationId,
                    studentNumber,
                    studentName,
                    section,
                    responses,
                )
                mutableState.update { it.copy(message = "Submission graded and saved.") }
            }
        }
    }

    fun prepareCsvExport(examinationId: String) {
        viewModelScope.launch {
            runOperation {
                val (exam, submissions) = repository.exportSubmissions(examinationId)
                val safeId = exam.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
                val filename = "EXAM_${safeId}_STUDENT_ANSWERS.csv"
                val content = WideCsvExporter.generate(exam, submissions)
                mutableState.update {
                    it.copy(pendingCsvExport = PendingCsvExport(filename, content))
                }
            }
        }
    }

    fun clearPendingCsvExport() {
        mutableState.update { it.copy(pendingCsvExport = null) }
    }

    fun clearMessages() {
        mutableState.update { it.copy(message = null, errorMessage = null) }
    }

    fun showError(exception: Exception) {
        mutableState.update {
            it.copy(
                isBusy = false,
                errorMessage = exception.message ?: "The operation failed. Please try again.",
            )
        }
    }

    private suspend fun runOperation(block: suspend () -> Unit) {
        mutableState.update { it.copy(isBusy = true, errorMessage = null) }
        try {
            block()
        } catch (exception: Exception) {
            showError(exception)
        } finally {
            mutableState.update { it.copy(isBusy = false) }
        }
    }
}
