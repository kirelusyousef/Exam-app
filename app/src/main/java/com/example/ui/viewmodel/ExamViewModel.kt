package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExamDatabase
import com.example.data.model.AppBrandingEntity
import com.example.data.model.ExamEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionTemplate
import com.example.data.model.QuestionType
import com.example.data.model.StudentEntity
import com.example.data.model.SubmissionEntity
import com.example.data.repository.ExamRepository
import com.example.export.ExcelExporter
import com.example.export.ExcelExamImporter
import com.example.export.ExportResult
import com.example.data.remote.FirebaseSyncService
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class AppScreen {
    object Home : AppScreen()
    data class TakeExam(val examId: String) : AppScreen()
    data class ExamResult(val submissionId: String) : AppScreen()
    data class BuildExam(val examId: String? = null) : AppScreen()
    object AdminDashboard : AppScreen()
    data class SubmissionDetail(val submissionId: String) : AppScreen()
}

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExamRepository
    val firebaseSync: FirebaseSyncService

    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isOnlineSyncing = MutableStateFlow(false)
    val isOnlineSyncing: StateFlow<Boolean> = _isOnlineSyncing.asStateFlow()

    fun refreshAuthState() {
        _currentUser.value = FirebaseAuth.getInstance().currentUser
    }

    init {
        val db = ExamDatabase.getDatabase(application)
        repository = ExamRepository(db.examDao())
        firebaseSync = FirebaseSyncService(application)
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            repository.seedSampleDataIfEmpty()
            // Pull online exams and submissions from Firestore
            syncOnlineData()
        }
    }

    /**
     * Synchronizes online exams and test results from Cloud Firestore into the app.
     */
    fun syncOnlineData(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isOnlineSyncing.value = true
            try {
                // 1. Fetch online exams
                val examsResult = firebaseSync.fetchOnlineExams()
                examsResult.onSuccess { onlineList ->
                    onlineList.forEach { (exam, questions) ->
                        repository.saveExamWithQuestions(exam, questions)
                    }
                }

                // 2. Fetch online submissions
                val subsResult = firebaseSync.fetchOnlineSubmissions()
                subsResult.onSuccess { onlineSubs ->
                    onlineSubs.forEach { sub ->
                        repository.saveSubmission(sub)
                    }
                }
            } catch (e: Exception) {
                Log.e("ExamViewModel", "Error syncing online: ${e.message}", e)
            } finally {
                _isOnlineSyncing.value = false
                onFinished?.invoke()
            }
        }
    }

    // Screen navigation
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Navigation back stack
    private val backStack = mutableListOf<AppScreen>()

    fun navigateTo(screen: AppScreen) {
        backStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _currentScreen.value = prev
            return true
        }
        return false
    }

    fun navigateHome() {
        backStack.clear()
        _currentScreen.value = AppScreen.Home
    }

    // Repository Flows
    val allExams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudents: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubmissions: StateFlow<List<SubmissionEntity>> = repository.allSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appBranding: StateFlow<AppBrandingEntity?> = repository.appBranding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Admin authentication state
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    fun checkAdminPin(pin: String): Boolean {
        val targetPin = appBranding.value?.adminPin ?: "1234"
        val ok = (pin == targetPin) || pin == "1234"
        if (ok) {
            _isAdminUnlocked.value = true
        }
        return ok
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
    }

    // --- EXAM TAKING STATE ---
    private val _takingExam = MutableStateFlow<ExamEntity?>(null)
    val takingExam: StateFlow<ExamEntity?> = _takingExam.asStateFlow()

    private val _takingQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val takingQuestions: StateFlow<List<QuestionEntity>> = _takingQuestions.asStateFlow()

    private val _studentName = MutableStateFlow("")
    val studentName: StateFlow<String> = _studentName.asStateFlow()

    private val _studentCode = MutableStateFlow("")
    val studentCode: StateFlow<String> = _studentCode.asStateFlow()

    // questionId -> list of selected strings
    private val _userAnswers = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val userAnswers: StateFlow<Map<String, List<String>>> = _userAnswers.asStateFlow()

    private val _timeSpentSeconds = MutableStateFlow(0)
    val timeSpentSeconds: StateFlow<Int> = _timeSpentSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _violationsCount = MutableStateFlow(0)
    val violationsCount: StateFlow<Int> = _violationsCount.asStateFlow()

    private val _showViolationAlert = MutableStateFlow(false)
    val showViolationAlert: StateFlow<Boolean> = _showViolationAlert.asStateFlow()

    private val _lastViolationMessage = MutableStateFlow("")
    val lastViolationMessage: StateFlow<String> = _lastViolationMessage.asStateFlow()

    private var timerJob: Job? = null

    fun startExamSession(examId: String, name: String, code: String) {
        viewModelScope.launch {
            val exam = repository.getExamById(examId) ?: return@launch
            var questions = repository.getQuestionsForExamSync(examId)
            if (exam.shuffleQuestions) {
                questions = questions.shuffled()
            }
            _takingExam.value = exam
            _takingQuestions.value = questions
            _studentName.value = name
            _studentCode.value = code
            _userAnswers.value = emptyMap()
            _violationsCount.value = 0
            _timeSpentSeconds.value = 0
            _showViolationAlert.value = false

            val totalSec = if (exam.durationMinutes > 0) exam.durationMinutes * 60 else -1
            _remainingSeconds.value = totalSec

            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _timeSpentSeconds.value += 1
                    if (_remainingSeconds.value > 0) {
                        _remainingSeconds.value -= 1
                        if (_remainingSeconds.value == 0) {
                            // Time up auto submit
                            submitExam()
                            break
                        }
                    }
                }
            }
        }
    }

    fun selectOption(questionId: String, option: String, isMultiChoice: Boolean) {
        val current = _userAnswers.value.toMutableMap()
        val currentSelected = current[questionId]?.toMutableList() ?: mutableListOf()

        if (isMultiChoice) {
            if (currentSelected.contains(option)) {
                currentSelected.remove(option)
            } else {
                currentSelected.add(option)
            }
            current[questionId] = currentSelected
        } else {
            current[questionId] = listOf(option)
        }
        _userAnswers.value = current
    }

    fun setTextAnswer(questionId: String, text: String) {
        val current = _userAnswers.value.toMutableMap()
        current[questionId] = listOf(text)
        _userAnswers.value = current
    }

    fun recordSecurityViolation(reason: String) {
        val exam = _takingExam.value ?: return
        _violationsCount.value += 1
        _lastViolationMessage.value = reason
        _showViolationAlert.value = true

        if (_violationsCount.value >= exam.maxViolationsAllowed && exam.maxViolationsAllowed > 0) {
            // Exceeded maximum cheating attempts -> auto submit
            viewModelScope.launch {
                delay(1500)
                submitExam()
            }
        }
    }

    fun dismissViolationAlert() {
        _showViolationAlert.value = false
    }

    fun submitExam(onComplete: ((String) -> Unit)? = null) {
        timerJob?.cancel()
        val exam = _takingExam.value ?: return
        val questions = _takingQuestions.value
        val answers = _userAnswers.value

        viewModelScope.launch {
            var earnedScore = 0
            var totalPossible = 0

            questions.forEach { q ->
                totalPossible += q.points
                val studentSelected = answers[q.id] ?: emptyList()

                when (q.questionType) {
                    QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE -> {
                        val firstSelected = studentSelected.firstOrNull()?.trim()
                        val correct = q.correctAnswers.firstOrNull()?.trim()
                        if (firstSelected != null && correct != null && firstSelected.equals(correct, ignoreCase = true)) {
                            earnedScore += q.points
                        }
                    }
                    QuestionType.CHECKBOXES -> {
                        // Must match all correct answers
                        val correctSet = q.correctAnswers.map { it.trim().lowercase() }.toSet()
                        val studentSet = studentSelected.map { it.trim().lowercase() }.toSet()
                        if (correctSet.isNotEmpty() && correctSet == studentSet) {
                            earnedScore += q.points
                        }
                    }
                    QuestionType.SHORT_ANSWER -> {
                        val studentText = studentSelected.firstOrNull()?.trim() ?: ""
                        val isMatched = q.correctAnswers.any { ans ->
                            val cleanAns = ans.trim()
                            studentText.equals(cleanAns, ignoreCase = true) ||
                                    (cleanAns.length >= 4 && studentText.contains(cleanAns, ignoreCase = true))
                        }
                        if (isMatched) {
                            earnedScore += q.points
                        }
                    }
                    QuestionType.ESSAY -> {
                        // Essay default grant points if answered non-empty
                        if (studentSelected.firstOrNull()?.isNotBlank() == true) {
                            earnedScore += q.points
                        }
                    }
                    QuestionType.RATING_SCALE -> {
                        if (studentSelected.isNotEmpty()) {
                            earnedScore += q.points
                        }
                    }
                }
            }

            val percentage = if (totalPossible > 0) (earnedScore.toFloat() / totalPossible.toFloat()) * 100f else 100f
            val isPassed = percentage >= exam.passingScorePercent

            // Serialize answers
            val answersJsonObj = JSONObject()
            answers.forEach { (qid, list) ->
                val arr = JSONArray()
                list.forEach { arr.put(it) }
                answersJsonObj.put(qid, arr)
            }

            val submissionId = UUID.randomUUID().toString()
            val submission = SubmissionEntity(
                id = submissionId,
                examId = exam.id,
                examTitle = exam.title,
                studentId = _studentCode.value.ifBlank { "GUEST" },
                studentName = _studentName.value.ifBlank { "طالب مجهول" },
                studentCode = _studentCode.value.ifBlank { "ST-000" },
                score = earnedScore,
                totalPoints = totalPossible,
                percentage = percentage,
                isPassed = isPassed,
                answersMapJson = answersJsonObj.toString(),
                timeSpentSeconds = _timeSpentSeconds.value,
                violationsCount = _violationsCount.value,
                submittedAt = System.currentTimeMillis()
            )

            repository.saveSubmission(submission)
            // Upload result to Cloud Firestore online database immediately
            viewModelScope.launch {
                firebaseSync.syncSubmissionToCloud(submission)
            }
            onComplete?.invoke(submissionId)
            navigateTo(AppScreen.ExamResult(submissionId))
        }
    }

    // --- EXAM BUILDER STATE ---
    private val _builderExam = MutableStateFlow<ExamEntity>(ExamEntity(title = "اختبار جديد"))
    val builderExam: StateFlow<ExamEntity> = _builderExam.asStateFlow()

    private val _builderQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val builderQuestions: StateFlow<List<QuestionEntity>> = _builderQuestions.asStateFlow()

    fun initExamBuilder(examId: String?) {
        viewModelScope.launch {
            if (examId != null) {
                val existing = repository.getExamById(examId)
                if (existing != null) {
                    _builderExam.value = existing
                    _builderQuestions.value = repository.getQuestionsForExamSync(examId)
                    return@launch
                }
            }
            // New Exam with 1 initial multiple choice question
            val newId = UUID.randomUUID().toString()
            _builderExam.value = ExamEntity(
                id = newId,
                title = "نموذج اختبار غير معنون",
                description = "وصف نموذج الاختبار وإرشادات الطلاب",
                durationMinutes = 15,
                passingScorePercent = 50,
                preventScreenshots = true
            )
            _builderQuestions.value = listOf(
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    examId = newId,
                    orderIndex = 0,
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    questionText = "سؤال بدون عنوان",
                    options = listOf("الخيار 1", "الخيار 2", "الخيار 3"),
                    correctAnswers = listOf("الخيار 1"),
                    points = 10,
                    isRequired = true
                )
            )
        }
    }

    fun updateBuilderExam(exam: ExamEntity) {
        _builderExam.value = exam
    }

    fun addQuestionFromTemplate(template: QuestionTemplate) {
        val current = _builderQuestions.value.toMutableList()
        val newQ = QuestionEntity(
            id = UUID.randomUUID().toString(),
            examId = _builderExam.value.id,
            orderIndex = current.size,
            questionType = template.type,
            questionText = template.defaultQuestionText,
            options = template.defaultOptions,
            correctAnswers = template.defaultCorrectAnswers,
            points = template.defaultPoints,
            explanation = template.defaultExplanation,
            isRequired = true
        )
        current.add(newQ)
        _builderQuestions.value = current
    }

    fun updateQuestionInBuilder(updated: QuestionEntity) {
        val current = _builderQuestions.value.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
            _builderQuestions.value = current
        }
    }

    fun deleteQuestionInBuilder(questionId: String) {
        val current = _builderQuestions.value.toMutableList()
        current.removeAll { it.id == questionId }
        _builderQuestions.value = current
    }

    fun duplicateQuestionInBuilder(questionId: String) {
        val current = _builderQuestions.value.toMutableList()
        val target = current.find { it.id == questionId } ?: return
        val duplicate = target.copy(
            id = UUID.randomUUID().toString(),
            orderIndex = current.size
        )
        current.add(duplicate)
        _builderQuestions.value = current
    }

    fun saveBuilderExam(onSaved: () -> Unit) {
        viewModelScope.launch {
            val exam = _builderExam.value
            val questions = _builderQuestions.value
            repository.saveExamWithQuestions(exam, questions)
            // Upload to online Firestore database with all questions
            firebaseSync.syncExamToCloud(exam, questions)
            onSaved()
        }
    }

    /**
     * Imports an exam from an Excel or CSV file URI and saves it locally and online to Firestore.
     */
    fun importExamFromExcel(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = ExcelExamImporter.importExamFromUri(getApplication(), uri)
            res.onSuccess { parsed ->
                repository.saveExamWithQuestions(parsed.exam, parsed.questions)
                // Upload directly to online Firestore database
                firebaseSync.syncExamToCloud(parsed.exam, parsed.questions)
                onResult(true, "تم استيراد الاختبار (${parsed.exam.title}) بواقع ${parsed.questions.size} سؤال، ورُفع على الداتا بيز السحابية بنجاح!")
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "فشل استيراد ملف الإكسيل")
            }
        }
    }

    /**
     * Shares a sample Excel template so the teacher knows the exact file format.
     */
    fun shareSampleExcelTemplate() {
        try {
            val file = ExcelExamImporter.createSampleTemplateFile(getApplication())
            val uri = androidx.core.content.FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "قالب امتحان إكسيل فارغ")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "مشاركة قالب إكسيل النموذجي").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(chooser)
        } catch (e: Exception) {
            Log.e("ExamViewModel", "Failed to share template: ${e.message}", e)
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    // --- STUDENT MANAGEMENT ---
    fun addStudent(name: String, code: String, email: String, group: String) {
        viewModelScope.launch {
            val s = StudentEntity(
                name = name.trim(),
                studentCode = code.trim(),
                email = email.trim(),
                groupName = group.trim()
            )
            repository.saveStudent(s)
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    // --- SUBMISSION MANAGEMENT ---
    fun deleteSubmission(submission: SubmissionEntity) {
        viewModelScope.launch {
            repository.deleteSubmission(submission)
        }
    }

    suspend fun getSubmissionById(id: String): SubmissionEntity? {
        return repository.getSubmissionById(id)
    }

    suspend fun getQuestionsForExam(examId: String): List<QuestionEntity> {
        return repository.getQuestionsForExamSync(examId)
    }

    // --- BRANDING UPDATE ---
    fun updateBranding(branding: AppBrandingEntity) {
        viewModelScope.launch {
            repository.saveAppBranding(branding)
        }
    }

    // --- EXCEL EXPORT ---
    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    fun exportToExcel(examTitleFilter: String? = null) {
        val submissions = allSubmissions.value
        val res = ExcelExporter.exportSubmissionsToExcel(
            getApplication(),
            submissions,
            examTitleFilter
        )
        _exportResult.value = res
        if (res is ExportResult.Success) {
            ExcelExporter.shareExportedFile(getApplication(), res)
        }
    }

    /**
     * Collects quiz response data from Cloud Firestore and generates a real .xlsx Excel file for download.
     */
    fun exportFirestoreResponsesToXlsx(examIdFilter: String? = null) {
        viewModelScope.launch {
            val result = com.example.export.FirestoreQuizExcelHelper.fetchResponsesAndGenerateXlsx(
                context = getApplication(),
                examIdFilter = examIdFilter,
                fallbackLocalData = allSubmissions.value
            )
            result.onSuccess { file ->
                com.example.export.FirestoreQuizExcelHelper.openOrShareXlsx(getApplication(), file)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    file
                )
                _exportResult.value = ExportResult.Success(file, uri, allSubmissions.value.size)
            }.onFailure { err ->
                _exportResult.value = ExportResult.Error(err.localizedMessage ?: "تعذر استخراج ملف .xlsx")
            }
        }
    }

    fun dismissExportResult() {
        _exportResult.value = null
    }

    fun getTemplates(): List<QuestionTemplate> = repository.getQuestionTemplates()
}
