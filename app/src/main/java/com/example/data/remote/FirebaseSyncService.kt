package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ExamEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionType
import com.example.data.model.SubmissionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Service handling cloud operations with Firebase Firestore.
 * Supports public online exams sharing and live submission tracking
 * without requiring Google Console setup.
 */
class FirebaseSyncService(private val context: Context) {

    private val TAG = "FirebaseSyncService"
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // MANDATORY: Always use custom database ID from R.string.firestore_database_id
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance(
            context.getString(R.string.firestore_database_id)
        )

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Uploads an exam and all its questions directly to Cloud Firestore.
     */
    suspend fun syncExamToCloud(
        exam: ExamEntity,
        questions: List<QuestionEntity> = emptyList()
    ): Result<Unit> {
        return try {
            val questionsListMaps = questions.map { q ->
                hashMapOf(
                    "id" to q.id,
                    "examId" to exam.id,
                    "orderIndex" to q.orderIndex,
                    "questionType" to q.questionType.name,
                    "questionText" to q.questionText,
                    "options" to q.options,
                    "correctAnswers" to q.correctAnswers,
                    "points" to q.points,
                    "explanation" to q.explanation,
                    "isRequired" to q.isRequired
                )
            }

            val examMap = hashMapOf(
                "id" to exam.id,
                "title" to exam.title,
                "description" to exam.description,
                "durationMinutes" to exam.durationMinutes,
                "passingScorePercent" to exam.passingScorePercent,
                "shuffleQuestions" to exam.shuffleQuestions,
                "preventScreenshots" to exam.preventScreenshots,
                "maxViolationsAllowed" to exam.maxViolationsAllowed,
                "createdAt" to exam.createdAt,
                "category" to exam.category,
                "isActive" to exam.isActive,
                "questionsCount" to questions.size,
                "questions" to questionsListMaps,
                "syncedOnlineAt" to System.currentTimeMillis()
            )

            // 1. Save to root public online collection
            firestore.collection("exams")
                .document(exam.id)
                .set(examMap)
                .await()

            // 2. Also save under user account if signed in
            currentUserId?.let { uid ->
                try {
                    firestore.collection("users")
                        .document(uid)
                        .collection("exams")
                        .document(exam.id)
                        .set(examMap)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Optional user-specific sync note: ${e.message}")
                }
            }

            Log.d(TAG, "Exam ${exam.id} successfully synced online to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync exam to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all available online exams from Cloud Firestore.
     */
    suspend fun fetchOnlineExams(): Result<List<Pair<ExamEntity, List<QuestionEntity>>>> {
        return try {
            val snapshot = firestore.collection("exams").get().await()
            val list = mutableListOf<Pair<ExamEntity, List<QuestionEntity>>>()

            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: continue
                val description = doc.getString("description") ?: ""
                val duration = (doc.get("durationMinutes") as? Number)?.toInt() ?: 15
                val passingScore = (doc.get("passingScorePercent") as? Number)?.toInt() ?: 50
                val shuffle = doc.getBoolean("shuffleQuestions") ?: false
                val preventScreenshots = doc.getBoolean("preventScreenshots") ?: true
                val maxViolations = (doc.get("maxViolationsAllowed") as? Number)?.toInt() ?: 3
                val createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()
                val category = doc.getString("category") ?: "عام"
                val isActive = doc.getBoolean("isActive") ?: true

                val exam = ExamEntity(
                    id = id,
                    title = title,
                    description = description,
                    durationMinutes = duration,
                    passingScorePercent = passingScore,
                    shuffleQuestions = shuffle,
                    preventScreenshots = preventScreenshots,
                    maxViolationsAllowed = maxViolations,
                    createdAt = createdAt,
                    category = category,
                    isActive = isActive
                )

                val questionsData = doc.get("questions") as? List<Map<String, Any?>> ?: emptyList()
                val questions = questionsData.mapIndexed { idx, qMap ->
                    val qId = qMap["id"] as? String ?: "${id}_q_$idx"
                    val qTypeStr = qMap["questionType"] as? String ?: QuestionType.MULTIPLE_CHOICE.name
                    val qType = try {
                        QuestionType.valueOf(qTypeStr)
                    } catch (_: Exception) {
                        QuestionType.MULTIPLE_CHOICE
                    }
                    val qText = qMap["questionText"] as? String ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val options = qMap["options"] as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val correct = qMap["correctAnswers"] as? List<String> ?: emptyList()
                    val points = (qMap["points"] as? Number)?.toInt() ?: 10
                    val explanation = qMap["explanation"] as? String ?: ""
                    val isRequired = qMap["isRequired"] as? Boolean ?: true

                    QuestionEntity(
                        id = qId,
                        examId = id,
                        orderIndex = idx,
                        questionType = qType,
                        questionText = qText,
                        options = options,
                        correctAnswers = correct,
                        points = points,
                        explanation = explanation,
                        isRequired = isRequired
                    )
                }

                list.add(Pair(exam, questions))
            }

            // Sort by createdAt descending
            list.sortByDescending { it.first.createdAt }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch online exams from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads student quiz results to Cloud Firestore.
     */
    suspend fun syncSubmissionToCloud(submission: SubmissionEntity): Result<Unit> {
        return try {
            val subMap = hashMapOf(
                "id" to submission.id,
                "examId" to submission.examId,
                "examTitle" to submission.examTitle,
                "studentId" to submission.studentId,
                "studentName" to submission.studentName,
                "studentCode" to submission.studentCode,
                "score" to submission.score,
                "totalPoints" to submission.totalPoints,
                "percentage" to submission.percentage.toDouble(),
                "isPassed" to submission.isPassed,
                "answersMapJson" to submission.answersMapJson,
                "timeSpentSeconds" to submission.timeSpentSeconds,
                "violationsCount" to submission.violationsCount,
                "submittedAt" to submission.submittedAt,
                "syncedOnlineAt" to System.currentTimeMillis()
            )

            // 1. Save to root public submissions collection for instant teacher review
            firestore.collection("submissions")
                .document(submission.id)
                .set(subMap)
                .await()

            // 2. Also save to user collection if signed in
            currentUserId?.let { uid ->
                try {
                    firestore.collection("users")
                        .document(uid)
                        .collection("submissions")
                        .document(submission.id)
                        .set(subMap)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Optional user-specific submission sync note: ${e.message}")
                }
            }

            Log.d(TAG, "Submission ${submission.id} successfully uploaded online to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync submission to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all submissions from Cloud Firestore for the teacher dashboard.
     */
    suspend fun fetchOnlineSubmissions(): Result<List<SubmissionEntity>> {
        return try {
            val snapshot = firestore.collection("submissions").get().await()
            val list = mutableListOf<SubmissionEntity>()

            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val examId = doc.getString("examId") ?: ""
                val examTitle = doc.getString("examTitle") ?: "اختبار"
                val studentId = doc.getString("studentId") ?: ""
                val studentName = doc.getString("studentName") ?: "طالب"
                val studentCode = doc.getString("studentCode") ?: "—"
                val score = (doc.get("score") as? Number)?.toInt() ?: 0
                val totalPoints = (doc.get("totalPoints") as? Number)?.toInt() ?: 0
                val percentage = (doc.get("percentage") as? Number)?.toFloat() ?: 0f
                val isPassed = doc.getBoolean("isPassed") ?: (percentage >= 50f)
                val answersMapJson = doc.getString("answersMapJson") ?: "{}"
                val timeSpentSeconds = (doc.get("timeSpentSeconds") as? Number)?.toInt() ?: 0
                val violationsCount = (doc.get("violationsCount") as? Number)?.toInt() ?: 0
                val submittedAt = (doc.get("submittedAt") as? Number)?.toLong() ?: System.currentTimeMillis()

                list.add(
                    SubmissionEntity(
                        id = id,
                        examId = examId,
                        examTitle = examTitle,
                        studentId = studentId,
                        studentName = studentName,
                        studentCode = studentCode,
                        score = score,
                        totalPoints = totalPoints,
                        percentage = percentage,
                        isPassed = isPassed,
                        answersMapJson = answersMapJson,
                        timeSpentSeconds = timeSpentSeconds,
                        violationsCount = violationsCount,
                        submittedAt = submittedAt
                    )
                )
            }

            list.sortByDescending { it.submittedAt }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch online submissions: ${e.message}", e)
            Result.failure(e)
        }
    }
}
