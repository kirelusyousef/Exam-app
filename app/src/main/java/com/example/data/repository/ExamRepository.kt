package com.example.data.repository

import android.content.Context
import com.example.data.local.ExamDao
import com.example.data.model.AppBrandingEntity
import com.example.data.model.ExamEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionTemplate
import com.example.data.model.QuestionType
import com.example.data.model.StudentEntity
import com.example.data.model.SubmissionEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.util.UUID

class ExamRepository(private val dao: ExamDao) {

    val allExams: Flow<List<ExamEntity>> = dao.getAllExams()
    val allStudents: Flow<List<StudentEntity>> = dao.getAllStudents()
    val allSubmissions: Flow<List<SubmissionEntity>> = dao.getAllSubmissions()
    val appBranding: Flow<AppBrandingEntity?> = dao.getAppBranding()

    fun getQuestionsForExam(examId: String): Flow<List<QuestionEntity>> = dao.getQuestionsForExam(examId)

    suspend fun getQuestionsForExamSync(examId: String): List<QuestionEntity> = dao.getQuestionsForExamSync(examId)

    suspend fun getExamById(examId: String): ExamEntity? = dao.getExamById(examId)

    suspend fun getSubmissionById(id: String): SubmissionEntity? = dao.getSubmissionById(id)

    suspend fun saveExamWithQuestions(exam: ExamEntity, questions: List<QuestionEntity>) {
        dao.insertExam(exam)
        dao.deleteQuestionsForExam(exam.id)
        dao.insertQuestions(questions.mapIndexed { index, q -> q.copy(examId = exam.id, orderIndex = index) })
    }

    suspend fun deleteExam(exam: ExamEntity) {
        dao.deleteQuestionsForExam(exam.id)
        dao.deleteExam(exam)
    }

    suspend fun saveStudent(student: StudentEntity) {
        dao.insertStudent(student)
    }

    suspend fun deleteStudent(student: StudentEntity) {
        dao.deleteStudent(student)
    }

    suspend fun saveSubmission(submission: SubmissionEntity) {
        dao.insertSubmission(submission)
    }

    suspend fun deleteSubmission(submission: SubmissionEntity) {
        dao.deleteSubmission(submission)
    }

    suspend fun saveAppBranding(branding: AppBrandingEntity) {
        dao.insertAppBranding(branding)
    }

    suspend fun initializeDefaultsIfNeeded() {
        // Initialize branding if null
        val existingBranding = dao.getAppBrandingSync()
        if (existingBranding == null) {
            dao.insertAppBranding(
                AppBrandingEntity(
                    id = 1,
                    appName = "ExamForm Pro",
                    institutionName = "أكاديمية النخبة للتقنية والتعليم الذكي",
                    slogan = "منظومة الامتحانات الرقمية المحمية ضد الغش",
                    logoType = "DEFAULT",
                    themeColorHex = "#673AB7",
                    adminPin = "1234",
                    instructionsText = "تعليمات هامة: يُمنع التقاط لقطات الشاشة أو نسخ نصوص الأسئلة أو التبديل بين التطبيقات أثناء سير الاختبار. يتم رصد المخالفات وإرسال التقرير فورياً.",
                    globalScreenSecurity = true
                )
            )
        }

        // Check if exams exist
        val examsList = dao.getAllExams()
        // If exams are empty, seed sample exams and sample submissions
        kotlinx.coroutines.delay(100)
    }

    suspend fun seedSampleDataIfEmpty() {
        val existingExams = dao.getExamById("seed-exam-1")
        if (existingExams != null) return

        // 1. First Exam: Full Google Forms style exam
        val exam1 = ExamEntity(
            id = "seed-exam-1",
            title = "الاختبار الشامل في تقنية المعلومات والذكاء الاصطناعي",
            description = "اختبار تجريبي شامل يحاكي نماذج جوجل مع قيود أمان تمنع النسخ والتقاط الشاشة. أجب عن كافة الأسئلة بدقة.",
            durationMinutes = 15,
            passingScorePercent = 60,
            shuffleQuestions = false,
            allowReviewAnswers = true,
            maxViolationsAllowed = 3,
            preventScreenshots = true,
            category = "حاسب وتقنية"
        )

        val questions1 = listOf(
            QuestionEntity(
                id = "q1-1",
                examId = exam1.id,
                orderIndex = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "ما هي وحدة المعالجة المركزية (CPU) في الحاسوب؟",
                options = listOf(
                    "العقل المدبر والمسؤول عن تنفيذ التعليمات الحسابية والمنطقية",
                    "وحدة التخزين الدائم للملفات الكبيرة",
                    "بطاقة إخراج الرسوميات وعرض الشاشة فقط",
                    "بروتوكول شبكي لنقل البيانات عبر الإنترنت"
                ),
                correctAnswers = listOf("العقل المدبر والمسؤول عن تنفيذ التعليمات الحسابية والمنطقية"),
                points = 10,
                explanation = "وحدة المعالجة المركزية CPU هي المعالج الرئيسي للحاسب والمسؤول عن فك تشفير وتنفيذ كافة التعليمات البرمجية.",
                isRequired = true
            ),
            QuestionEntity(
                id = "q1-2",
                examId = exam1.id,
                orderIndex = 1,
                questionType = QuestionType.TRUE_FALSE,
                questionText = "ذاكرة الوصول العشوائي (RAM) تحتفظ ببياناتها حتى بعد فصل التيار الكهربائي.",
                options = listOf("صحيح", "خطأ"),
                correctAnswers = listOf("خطأ"),
                points = 10,
                explanation = "ذاكرة RAM هي ذاكرة متطايرة (Volatile) تفقد محتوياتها بمجرد انقطاع التيار الكهربائي.",
                isRequired = true
            ),
            QuestionEntity(
                id = "q1-3",
                examId = exam1.id,
                orderIndex = 2,
                questionType = QuestionType.CHECKBOXES,
                questionText = "اختر جميع لغات البرمجة الشائعة لتطوير تطبيقات الأندرويد والأنظمة الحديثة:",
                options = listOf("Kotlin", "Java", "Python", "HTML"),
                correctAnswers = listOf("Kotlin", "Java"),
                points = 15,
                explanation = "تعتبر Kotlin هي اللغة المفضلة الرسمية لـ Android، وتدعمها منصة Java بالكامل. بينما HTML لغة توصيف صفحات الويب.",
                isRequired = true
            ),
            QuestionEntity(
                id = "q1-4",
                examId = exam1.id,
                orderIndex = 3,
                questionType = QuestionType.SHORT_ANSWER,
                questionText = "ما هو المصطلح المقابل لاختصار (AI) باللغة العربية؟",
                options = emptyList(),
                correctAnswers = listOf("الذكاء الاصطناعي", "ذكاء اصطناعي", "الذكاء الإسطناعي"),
                points = 15,
                explanation = "AI اختصار لـ Artificial Intelligence والتي تعني الذكاء الاصطناعي.",
                isRequired = true
            ),
            QuestionEntity(
                id = "q1-5",
                examId = exam1.id,
                orderIndex = 4,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "أي من بروتوكولات التصفح التالية يوفر اتصالاً مشفراً وآمناً (SSL/TLS)؟",
                options = listOf("HTTPS", "HTTP", "FTP", "Telnet"),
                correctAnswers = listOf("HTTPS"),
                points = 10,
                explanation = "HTTPS هو الإصدار الآمن والمشفر لبروتوكول نقل النص التشعبي عبر طبقة أمان النقل.",
                isRequired = true
            ),
            QuestionEntity(
                id = "q1-6",
                examId = exam1.id,
                orderIndex = 5,
                questionType = QuestionType.RATING_SCALE,
                questionText = "ما مدى استيعابك للمفاهيم الأساسية لأمن المعلومات حتى الآن؟ (1: مبتدئ ، 5: متمكن)",
                options = listOf("1", "2", "3", "4", "5"),
                correctAnswers = listOf("5", "4", "3", "2", "1"),
                points = 5,
                explanation = "سؤال استبياني تقييمي يحصل فيه الطالب على الدرجة بمجرد الإجابة.",
                isRequired = false
            )
        )

        dao.insertExam(exam1)
        dao.insertQuestions(questions1)

        // 2. Second Exam: Cybersecurity Quick Quiz
        val exam2 = ExamEntity(
            id = "seed-exam-2",
            title = "اختبار أساسيات الأمن السيبراني ومكافحة التصيد",
            description = "تقييم سريع للتحقق من وعي الطلاب بالتهديدات الرقمية وحماية كلمات المرور.",
            durationMinutes = 10,
            passingScorePercent = 50,
            shuffleQuestions = false,
            allowReviewAnswers = true,
            maxViolationsAllowed = 2,
            preventScreenshots = true,
            category = "أمن سيبراني"
        )

        val questions2 = listOf(
            QuestionEntity(
                id = "q2-1",
                examId = exam2.id,
                orderIndex = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "ما هو هجوم 'التصيد الاحتيالي' (Phishing)؟",
                options = listOf(
                    "خداع المستخدم لسرقة بياناته الحساسة عبر رسائل وروابط مزيفة",
                    "إتلاف اللوحة الأم للحاسب بجهد كهربائي عالي",
                    "تسريع سرعة الإنترنت دون اشتراك قانوني",
                    "مسح سلة المهملات بشكل دائم"
                ),
                correctAnswers = listOf("خداع المستخدم لسرقة بياناته الحساسة عبر رسائل وروابط مزيفة"),
                points = 15,
                explanation = "التصيد الاحتيالي هو هندسة اجتماعية تهدف لانتحال صفة جهة موثوقة لسرقة كلمات المرور والبيانات البنكية.",
                isRequired = true
            ),
            QuestionEntity(
                id = "q2-2",
                examId = exam2.id,
                orderIndex = 1,
                questionType = QuestionType.TRUE_FALSE,
                questionText = "استخدام نفس كلمة المرور لجميع حساباتك الشخصية ممارسة أمنية سليمة.",
                options = listOf("صحيح", "خطأ"),
                correctAnswers = listOf("خطأ"),
                points = 15,
                explanation = "إعادة استخدام كلمة المرور يعرض جميع حساباتك للخطر في حال تسرب قاعدة بيانات موقع واحد.",
                isRequired = true
            ),
            QuestionEntity(
                id = "q2-3",
                examId = exam2.id,
                orderIndex = 2,
                questionType = QuestionType.CHECKBOXES,
                questionText = "ما هي مقومات كلمة المرور القوية؟ (حدد كل ما ينطبق)",
                options = listOf(
                    "تحتوي على حروف كبيرة وصغيرة وأرقام ورموز",
                    "لا تقل عن 12 إلى 16 خانة",
                    "تتضمن اسمك وتاريخ ميلادك الشخصي",
                    "تكون فريدة لكل خدمة أو حساب"
                ),
                correctAnswers = listOf(
                    "تحتوي على حروف كبيرة وصغيرة وأرقام ورموز",
                    "لا تقل عن 12 إلى 16 خانة",
                    "تكون فريدة لكل خدمة أو حساب"
                ),
                points = 20,
                explanation = "كلمة المرور القوية يجب أن تكون طويلة، معقدة، ولا تحتوي على معلومات شخصية يسهل تخمينها.",
                isRequired = true
            )
        )

        dao.insertExam(exam2)
        dao.insertQuestions(questions2)

        // Seed initial students
        val student1 = StudentEntity(
            id = "std-1",
            name = "أحمد محمد إبراهيم",
            studentCode = "ST-101",
            email = "ahmed.m@academy.edu",
            groupName = "هندسة البرمجيات"
        )
        val student2 = StudentEntity(
            id = "std-2",
            name = "سارة عبد الرحمن حسن",
            studentCode = "ST-102",
            email = "sara.h@academy.edu",
            groupName = "نظم المعلومات"
        )
        val student3 = StudentEntity(
            id = "std-3",
            name = "يوسف كيرلس عادل",
            studentCode = "ST-103",
            email = "yousef.k@academy.edu",
            groupName = "أمن سيبراني"
        )

        dao.insertStudent(student1)
        dao.insertStudent(student2)
        dao.insertStudent(student3)

        // Seed initial submissions to showcase the dashboard and Excel export instantly
        val sub1 = SubmissionEntity(
            id = "sub-1",
            examId = exam1.id,
            examTitle = exam1.title,
            studentId = student1.id,
            studentName = student1.name,
            studentCode = student1.studentCode,
            score = 55,
            totalPoints = 60,
            percentage = 91.6f,
            isPassed = true,
            answersMapJson = "{}",
            timeSpentSeconds = 540,
            violationsCount = 0,
            submittedAt = System.currentTimeMillis() - 86400000L
        )

        val sub2 = SubmissionEntity(
            id = "sub-2",
            examId = exam1.id,
            examTitle = exam1.title,
            studentId = student2.id,
            studentName = student2.name,
            studentCode = student2.studentCode,
            score = 48,
            totalPoints = 60,
            percentage = 80.0f,
            isPassed = true,
            answersMapJson = "{}",
            timeSpentSeconds = 620,
            violationsCount = 1,
            submittedAt = System.currentTimeMillis() - 43200000L
        )

        val sub3 = SubmissionEntity(
            id = "sub-3",
            examId = exam2.id,
            examTitle = exam2.title,
            studentId = student3.id,
            studentName = student3.name,
            studentCode = student3.studentCode,
            score = 50,
            totalPoints = 50,
            percentage = 100.0f,
            isPassed = true,
            answersMapJson = "{}",
            timeSpentSeconds = 310,
            violationsCount = 0,
            submittedAt = System.currentTimeMillis() - 14400000L
        )

        dao.insertSubmission(sub1)
        dao.insertSubmission(sub2)
        dao.insertSubmission(sub3)
    }

    // Built-in Question Templates for quick exam creation
    fun getQuestionTemplates(): List<QuestionTemplate> {
        return listOf(
            QuestionTemplate(
                title = "سؤال اختيار من متعدد (4 خيارات)",
                description = "سؤال قياسي بنمط الراديو مع خيار صحيح واحد",
                type = QuestionType.MULTIPLE_CHOICE,
                defaultQuestionText = "ما هو الخيار الصحيح للعبارة التالية؟",
                defaultOptions = listOf("الخيار الأول", "الخيار الثاني", "الخيار الثالث", "الخيار الرابع"),
                defaultCorrectAnswers = listOf("الخيار الأول"),
                defaultPoints = 10,
                defaultExplanation = "تفسير سبب صحة الإجابة."
            ),
            QuestionTemplate(
                title = "سؤال صح أو خطأ",
                description = "سؤال ثنائي سريع ومباشر للتحقق من المفاهيم",
                type = QuestionType.TRUE_FALSE,
                defaultQuestionText = "تعتبر العبارة التالية صحيحة تماماً.",
                defaultOptions = listOf("صحيح", "خطأ"),
                defaultCorrectAnswers = listOf("صحيح"),
                defaultPoints = 5,
                defaultExplanation = "توضيح الدليل العلمي للعبارة."
            ),
            QuestionTemplate(
                title = "سؤال مربعات اختيار (متعدد الإجابات)",
                description = "يتيح للطالب اختيار أكثر من إجابة صحيحة من القائمة",
                type = QuestionType.CHECKBOXES,
                defaultQuestionText = "اختر جميع الإجابات الصحيحة التي تنطبق:",
                defaultOptions = listOf("السمة الأولى الصحيحة", "السمة الثانية الصحيحة", "خاصية غير صحيحة", "السمة الثالثة الصحيحة"),
                defaultCorrectAnswers = listOf("السمة الأولى الصحيحة", "السمة الثانية الصحيحة", "السمة الثالثة الصحيحة"),
                defaultPoints = 15,
                defaultExplanation = "يجب اختيار كافة الخصائص الصحيحة للحصول على الدرجة."
            ),
            QuestionTemplate(
                title = "سؤال إجابة قصيرة / أكمل الفراغ",
                description = "إدخال نصي قصير مع تصحيح فوري قائم على الكلمات المفتاحية",
                type = QuestionType.SHORT_ANSWER,
                defaultQuestionText = "اكتب المصطلح العلمي الدال على هذا التعريف:",
                defaultOptions = emptyList(),
                defaultCorrectAnswers = listOf("المصطلح المستهدف"),
                defaultPoints = 10,
                defaultExplanation = "المصطلح المقبول في المعجم العلمي."
            ),
            QuestionTemplate(
                title = "سؤال مقالي / تعليل وشرح",
                description = "مساحة لكتابة إجابة تفصيلية أو تعليل منطقي",
                type = QuestionType.ESSAY,
                defaultQuestionText = "اشرح بالتفصيل ما يلي مبيناً الأسباب والنتائج:",
                defaultOptions = emptyList(),
                defaultCorrectAnswers = emptyList(),
                defaultPoints = 20,
                defaultExplanation = "عناصر الإجابة النموذجية التي يقيمها المعلم."
            ),
            QuestionTemplate(
                title = "مقياس تقييم ومستوى (1-5)",
                description = "تقييم مدرج للآراء أو مستوى الصعوبة",
                type = QuestionType.RATING_SCALE,
                defaultQuestionText = "ما هو تقييمك لمستوى صعوبة هذا الموضوع؟",
                defaultOptions = listOf("1", "2", "3", "4", "5"),
                defaultCorrectAnswers = listOf("1", "2", "3", "4", "5"),
                defaultPoints = 5,
                defaultExplanation = "سؤال استبياني تفاعلي."
            )
        )
    }
}
