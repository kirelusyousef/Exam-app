package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppBrandingEntity
import com.example.data.model.ExamEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.StudentEntity
import com.example.data.model.SubmissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    // Exams
    @Query("SELECT * FROM exams ORDER BY createdAt DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :examId LIMIT 1")
    suspend fun getExamById(examId: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("DELETE FROM questions WHERE examId = :examId")
    suspend fun deleteQuestionsForExam(examId: String)

    // Questions
    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY orderIndex ASC")
    fun getQuestionsForExam(examId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForExamSync(examId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    // Students
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // Submissions
    @Query("SELECT * FROM submissions ORDER BY submittedAt DESC")
    fun getAllSubmissions(): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE examId = :examId ORDER BY submittedAt DESC")
    fun getSubmissionsForExam(examId: String): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE id = :id LIMIT 1")
    suspend fun getSubmissionById(id: String): SubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)

    @Delete
    suspend fun deleteSubmission(submission: SubmissionEntity)

    @Query("DELETE FROM submissions WHERE id = :id")
    suspend fun deleteSubmissionById(id: String)

    // Branding
    @Query("SELECT * FROM app_branding WHERE id = 1 LIMIT 1")
    fun getAppBranding(): Flow<AppBrandingEntity?>

    @Query("SELECT * FROM app_branding WHERE id = 1 LIMIT 1")
    suspend fun getAppBrandingSync(): AppBrandingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppBranding(branding: AppBrandingEntity)
}
