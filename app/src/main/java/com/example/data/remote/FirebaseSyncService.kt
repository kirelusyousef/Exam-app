package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ExamEntity
import com.example.data.model.SubmissionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncService(private val context: Context) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // MANDATORY: Always use custom database ID from R.string.firestore_database_id
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance(
            context.getString(R.string.firestore_database_id)
        )

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val currentUserName: String?
        get() = auth.currentUser?.displayName

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    suspend fun syncExamToCloud(exam: ExamEntity): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول بحساب Google لمزامنة الاختبارات سحابياً"))
        return try {
            val examMap = hashMapOf(
                "id" to exam.id,
                "ownerId" to uid,
                "title" to exam.title,
                "description" to exam.description,
                "durationMinutes" to exam.durationMinutes,
                "passingScorePercent" to exam.passingScorePercent,
                "shuffleQuestions" to exam.shuffleQuestions,
                "preventScreenshots" to exam.preventScreenshots,
                "maxViolationsAllowed" to exam.maxViolationsAllowed,
                "createdAt" to exam.createdAt,
                "category" to exam.category
            )
            firestore.collection("users")
                .document(uid)
                .collection("exams")
                .document(exam.id)
                .set(examMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync exam to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncSubmissionToCloud(submission: SubmissionEntity): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول بحساب Google لرفع النتيجة سحابياً"))
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
                "submittedAt" to submission.submittedAt
            )
            firestore.collection("users")
                .document(uid)
                .collection("submissions")
                .document(submission.id)
                .set(subMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync submission to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}
