package com.omrgrader.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omrgrader.data.GraderRepository
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OpenScantronApp(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        val export = state.pendingCsvExport
        if (uri == null) {
            viewModel.clearPendingCsvExport()
        } else if (export != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val output = requireNotNull(context.contentResolver.openOutputStream(uri, "w")) {
                            "Unable to open the selected export destination."
                        }
                        output.use {
                            it.write(export.content.toByteArray(Charsets.UTF_8))
                            it.flush()
                        }
                    }
                    viewModel.clearPendingCsvExport()
                    snackbarHostState.showSnackbar("CSV exported.")
                } catch (exception: Exception) {
                    viewModel.clearPendingCsvExport()
                    viewModel.showError(exception)
                }
            }
        }
    }

    LaunchedEffect(state.pendingCsvExport) {
        state.pendingCsvExport?.let { createDocument.launch(it.fileName) }
    }
    LaunchedEffect(state.errorMessage, state.message) {
        val text = state.errorMessage ?: state.message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Open Scantron") },
                navigationIcon = {
                    if (navController.currentBackStackEntry?.destination?.route != "home") {
                        TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                DashboardScreen(
                    state = state,
                    onCreate = viewModel::createExamination,
                    onOpenExam = { id -> navController.navigate("exam/$id") },
                )
            }
            composable(
                route = "exam/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                LaunchedEffect(id) { viewModel.selectExamination(id) }
                ExaminationScreen(
                    state = state,
                    onSaveKey = { answers -> viewModel.saveAnswerKey(id, answers) },
                    onAddSubmission = { number, name, section, responses ->
                        viewModel.addManualSubmission(id, number, name, section, responses)
                    },
                    onExport = { viewModel.prepareCsvExport(id) },
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    state: AppUiState,
    onCreate: (String, String, String, String) -> Unit,
    onOpenExam: (String) -> Unit,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Examinations", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${state.examinations.size} active",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { showCreateDialog = true }) { Text("Create examination") }
        Spacer(Modifier.height(16.dp))
        if (state.examinations.isEmpty()) {
            Text("No examinations yet. Create one to add an answer key and grade submissions.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.examinations, key = { it.id }) { exam ->
                    Card(onClick = { onOpenExam(exam.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(exam.title, fontWeight = FontWeight.SemiBold)
                            Text("${exam.subject.ifBlank { "No subject" }} • ${exam.questionCount} questions")
                            Text(
                                if (exam.activeAnswerKeyVersion == null) "Answer key not set"
                                else "Answer key v${exam.activeAnswerKeyVersion}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
    if (showCreateDialog) {
        CreateExaminationDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, subject, count, choices ->
                onCreate(title, subject, count, choices)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CreateExaminationDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var questionCount by rememberSaveable { mutableStateOf("20") }
    var choices by rememberSaveable { mutableStateOf("A, B, C, D") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create examination") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(subject, { subject = it }, label = { Text("Subject") }, singleLine = true)
                OutlinedTextField(
                    questionCount,
                    { questionCount = it },
                    label = { Text("Question count (1–200)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    choices,
                    { choices = it },
                    label = { Text("Answer labels, comma-separated") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(title, subject, questionCount, choices) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExaminationScreen(
    state: AppUiState,
    onSaveKey: (List<String>) -> Unit,
    onAddSubmission: (String, String, String, List<String>) -> Unit,
    onExport: () -> Unit,
) {
    val exam = state.selectedExamination
    var keyText by rememberSaveable(exam?.id) { mutableStateOf("") }
    var studentNumber by rememberSaveable(exam?.id) { mutableStateOf("") }
    var studentName by rememberSaveable(exam?.id) { mutableStateOf("") }
    var section by rememberSaveable(exam?.id) { mutableStateOf("") }
    var responseText by rememberSaveable(exam?.id) { mutableStateOf("") }

    LaunchedEffect(state.answerKey?.id) {
        val key = state.answerKey
        if (key != null) {
            keyText = key.answers.split(GraderRepository.ANSWER_SEPARATOR).joinToString("\n")
        }
    }
    if (exam == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            if (state.isBusy) CircularProgressIndicator()
            else Text("Loading examination…")
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(exam.title, style = MaterialTheme.typography.headlineSmall)
        Text("${exam.subject.ifBlank { "No subject" }} • ${exam.questionCount} questions")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Answer key", style = MaterialTheme.typography.titleLarge)
                Text("Current version: ${state.answerKey?.version ?: "not set"}")
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("One answer label per line") },
                    minLines = 4,
                )
                Button(
                    enabled = !state.isBusy,
                    onClick = {
                        onSaveKey(keyText.lineSequence().map(String::trim).filter(String::isNotEmpty).toList())
                    },
                ) { Text("Save new key version") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Manually enter a submission", style = MaterialTheme.typography.titleLarge)
                Text("Use one comma-separated answer per question. Use BLANK or MULTIPLE when applicable.")
                OutlinedTextField(
                    studentNumber,
                    { studentNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Student number") },
                    singleLine = true,
                )
                OutlinedTextField(
                    studentName,
                    { studentName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Student name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    section,
                    { section = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Section (optional)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    responseText,
                    { responseText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Responses, comma-separated") },
                    minLines = 3,
                )
                Button(
                    enabled = !state.isBusy && state.answerKey != null,
                    onClick = {
                        onAddSubmission(
                            studentNumber,
                            studentName,
                            section,
                            responseText.split(','),
                        )
                    },
                ) { Text("Grade and save submission") }
                OutlinedButton(onClick = onExport, enabled = !state.isBusy) {
                    Text("Export submissions CSV")
                }
            }
        }

        Text("Saved submissions (${state.submissions.size})", style = MaterialTheme.typography.titleLarge)
        if (state.submissions.isEmpty()) {
            Text("No submissions saved for this examination.")
        } else {
            state.submissions.forEach { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(row.student?.fullName ?: "Unassigned student", fontWeight = FontWeight.SemiBold)
                        Text(row.student?.studentNumber.orEmpty())
                        Text(
                            "Score ${formatScore(row.submission.score)} / " +
                                "${formatScore(row.submission.maximumScore)} " +
                                "(${row.submission.percentage?.let(::formatScore) ?: "N/A"}%)",
                        )
                        Text("Graded with answer key v${row.submission.answerKeyVersion}")
                    }
                }
            }
        }
    }
}

private fun formatScore(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
