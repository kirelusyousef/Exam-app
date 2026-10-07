package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class QuestionType(val titleAr: String, val iconName: String) {
    MULTIPLE_CHOICE("اختيار من متعدد", "radio_button_checked"),
    CHECKBOXES("مربعات اختيار (متعدد)", "check_box"),
    TRUE_FALSE("صح أو خطأ", "check_circle"),
    SHORT_ANSWER("إجابة قصيرة / أكمل", "short_text"),
    ESSAY("سؤال مقالي", "notes"),
    RATING_SCALE("مقياس تقييم (1-5)", "linear_scale")
}

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val durationMinutes: Int = 15, // 0 = no limit
    val passingScorePercent: Int = 50,
    val shuffleQuestions: Boolean = false,
    val allowReviewAnswers: Boolean = true,
    val maxViolationsAllowed: Int = 3,
    val preventScreenshots: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val category: String = "عام"
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val examId: String,
    val orderIndex: Int = 0,
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val questionText: String,
    val options: List<String> = emptyList(),
    val correctAnswers: List<String> = emptyList(),
    val points: Int = 10,
    val explanation: String = "",
    val isRequired: Boolean = true
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val studentCode: String,
    val email: String = "",
    val groupName: String = "الفوج أ",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val examId: String,
    val examTitle: String,
    val studentId: String,
    val studentName: String,
    val studentCode: String,
    val score: Int,
    val totalPoints: Int,
    val percentage: Float,
    val isPassed: Boolean,
    val answersMapJson: String, // questionId -> list of selected answer strings
    val timeSpentSeconds: Int,
    val violationsCount: Int,
    val submittedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_branding")
data class AppBrandingEntity(
    @PrimaryKey
    val id: Int = 1,
    val appName: String = "ExamForm Pro",
    val institutionName: String = "أكاديمية النخبة التعليمية",
    val slogan: String = "منظومة الامتحانات الرقمية الآمنة",
    val logoType: String = "DEFAULT", // DEFAULT, GRADUATION, SHIELD, ACADEMY, STAR
    val themeColorHex: String = "#673AB7", // Google Forms Purple
    val adminPin: String = "1234",
    val instructionsText: String = "يرجى الإجابة عن كافة الأسئلة بدقة. يُمنع منعاً باتاً تصغير التطبيق أو أخذ لقطات شاشة، حيث سيتم تسجيل أي مخالفة تلقائياً.",
    val globalScreenSecurity: Boolean = true
)

data class QuestionTemplate(
    val title: String,
    val description: String,
    val type: QuestionType,
    val defaultQuestionText: String,
    val defaultOptions: List<String>,
    val defaultCorrectAnswers: List<String>,
    val defaultPoints: Int = 10,
    val defaultExplanation: String = ""
)
