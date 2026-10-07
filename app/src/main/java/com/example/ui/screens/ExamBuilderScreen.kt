package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionTemplate
import com.example.data.model.QuestionType
import com.example.ui.components.TemplateSelectionDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ExamViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamBuilderScreen(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    val exam by viewModel.builderExam.collectAsState()
    val questions by viewModel.builderQuestions.collectAsState()

    var showTemplateDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val totalExamPoints = questions.sumOf { it.points }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("محرر نموذج الامتحان", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${questions.size} أسئلة • الإجمالي: $totalExamPoints درجة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveBuilderExam {
                                viewModel.navigateHome()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_builder_exam_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ ونشر")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTemplateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_question_from_template_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.LibraryAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة من قالب", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Google Forms style Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        OutlinedTextField(
                            value = exam.title,
                            onValueChange = { viewModel.updateBuilderExam(exam.copy(title = it)) },
                            label = { Text("عنوان نموذج الامتحان") },
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("builder_exam_title_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = exam.description,
                            onValueChange = { viewModel.updateBuilderExam(exam.copy(description = it)) },
                            label = { Text("وصف الاختبار والتعليمات العامة") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("builder_exam_desc_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = if (exam.durationMinutes > 0) exam.durationMinutes.toString() else "",
                                onValueChange = {
                                    val minutes = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0
                                    viewModel.updateBuilderExam(exam.copy(durationMinutes = minutes))
                                },
                                label = { Text("المدة (بالدقائق)") },
                                placeholder = { Text("0 = بدون وقت") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("builder_duration_input")
                            )

                            OutlinedTextField(
                                value = exam.passingScorePercent.toString(),
                                onValueChange = {
                                    val percent = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 50
                                    viewModel.updateBuilderExam(exam.copy(passingScorePercent = percent.coerceIn(0, 100)))
                                },
                                label = { Text("نسبة النجاح %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("builder_passing_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle Advanced Security settings
                        TextButton(
                            onClick = { showAdvancedSettings = !showAdvancedSettings }
                        ) {
                            Text(if (showAdvancedSettings) "إخفاء إعدادات الأمان المتقدمة ▲" else "إظهار إعدادات الأمان ومكافحة الغش ▼")
                        }

                        AnimatedVisibility(visible = showAdvancedSettings) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("منع لقطات الشاشة وتسجيل الفيديو", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("يفعّل FLAG_SECURE لحجب الشاشة كلياً عند محاولة التصوير", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = exam.preventScreenshots,
                                        onCheckedChange = { viewModel.updateBuilderExam(exam.copy(preventScreenshots = it)) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("الحد الأقصى لمخالفات الخروج من التطبيق", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("تسليم الاختبار تلقائياً بعد هذا العدد من المخالفات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedTextField(
                                        value = exam.maxViolationsAllowed.toString(),
                                        onValueChange = {
                                            val v = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 3
                                            viewModel.updateBuilderExam(exam.copy(maxViolationsAllowed = v.coerceIn(1, 10)))
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(70.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Questions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة أسئلة النموذج (${questions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showTemplateDialog = true }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة سؤال من قالب")
                    }
                }
            }

            // Editable Question Cards
            itemsIndexed(questions) { index, question ->
                QuestionEditorCard(
                    index = index + 1,
                    question = question,
                    onUpdate = { updated -> viewModel.updateQuestionInBuilder(updated) },
                    onDelete = { viewModel.deleteQuestionInBuilder(question.id) },
                    onDuplicate = { viewModel.duplicateQuestionInBuilder(question.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showTemplateDialog) {
        TemplateSelectionDialog(
            templates = viewModel.getTemplates(),
            onTemplateSelected = { template ->
                viewModel.addQuestionFromTemplate(template)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}

@Composable
fun QuestionEditorCard(
    index: Int,
    question: QuestionEntity,
    onUpdate: (QuestionEntity) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTypeMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_question_card_${question.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Type dropdown & Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Question Type Selector Dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { showTypeMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "سؤال $index: ${question.questionType.titleAr}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        QuestionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.titleAr) },
                                onClick = {
                                    showTypeMenu = false
                                    val defaultOpts = when (type) {
                                        QuestionType.TRUE_FALSE -> listOf("صحيح", "خطأ")
                                        QuestionType.MULTIPLE_CHOICE, QuestionType.CHECKBOXES -> listOf("الخيار 1", "الخيار 2", "الخيار 3")
                                        else -> emptyList()
                                    }
                                    val defaultAns = when (type) {
                                        QuestionType.TRUE_FALSE -> listOf("صحيح")
                                        QuestionType.MULTIPLE_CHOICE, QuestionType.CHECKBOXES -> listOf("الخيار 1")
                                        else -> emptyList()
                                    }
                                    onUpdate(question.copy(questionType = type, options = defaultOpts, correctAnswers = defaultAns))
                                }
                            )
                        }
                    }
                }

                // Points field
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الدرجة:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = question.points.toString(),
                        onValueChange = {
                            val pts = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                            onUpdate(question.copy(points = pts))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(68.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question Text Input
            OutlinedTextField(
                value = question.questionText,
                onValueChange = { onUpdate(question.copy(questionText = it)) },
                label = { Text("نص السؤال *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_text_input_${question.id}")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options & Correct Answer Configuration
            when (question.questionType) {
                QuestionType.MULTIPLE_CHOICE, QuestionType.CHECKBOXES -> {
                    Text(
                        text = "الخيارات (انقر على الدائرة لتحديد الإجابة الصحيحة):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    question.options.forEachIndexed { optIndex, optText ->
                        val isCorrect = question.correctAnswers.contains(optText)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Correct Answer toggle
                            IconButton(
                                onClick = {
                                    val newCorrect = if (question.questionType == QuestionType.MULTIPLE_CHOICE) {
                                        listOf(optText)
                                    } else {
                                        if (isCorrect) question.correctAnswers - optText else question.correctAnswers + optText
                                    }
                                    onUpdate(question.copy(correctAnswers = newCorrect))
                                }
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Check,
                                    contentDescription = "تحديد كإجابة صحيحة",
                                    tint = if (isCorrect) SuccessGreen else MaterialTheme.colorScheme.outline
                                )
                            }

                            OutlinedTextField(
                                value = optText,
                                onValueChange = { newText ->
                                    val newOptions = question.options.toMutableList()
                                    newOptions[optIndex] = newText
                                    // Also update correct answers if this was correct
                                    val newCorrect = question.correctAnswers.map { if (it == optText) newText else it }
                                    onUpdate(question.copy(options = newOptions, correctAnswers = newCorrect))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    val newOptions = question.options.toMutableList()
                                    newOptions.removeAt(optIndex)
                                    val newCorrect = question.correctAnswers.filter { it != optText }
                                    onUpdate(question.copy(options = newOptions, correctAnswers = newCorrect))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الخيار",
                                    tint = ErrorRed
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            val nextNum = question.options.size + 1
                            val newOptions = question.options + "الخيار $nextNum"
                            onUpdate(question.copy(options = newOptions))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة خيار جديد")
                    }
                }

                QuestionType.TRUE_FALSE -> {
                    Text("الإجابة الصحيحة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("صحيح", "خطأ").forEach { choice ->
                            val isCorrect = question.correctAnswers.firstOrNull() == choice
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCorrect) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCorrect) SuccessGreen else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdate(question.copy(correctAnswers = listOf(choice))) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isCorrect,
                                        onClick = { onUpdate(question.copy(correctAnswers = listOf(choice))) }
                                    )
                                    Text(choice, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                QuestionType.SHORT_ANSWER -> {
                    OutlinedTextField(
                        value = question.correctAnswers.firstOrNull() ?: "",
                        onValueChange = { onUpdate(question.copy(correctAnswers = listOf(it))) },
                        label = { Text("الكلمة / الإجابة الصحيحة الدقيقة (للتصحيح التلقائي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                QuestionType.ESSAY -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "سؤال مقالي: يتم تقييمه تلقائياً بمنح الدرجة عند الإجابة أو عبر مراجعة المعلم في لوحة التحكم.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                QuestionType.RATING_SCALE -> {
                    Text("مقياس تقييم من 1 إلى 5 (سؤال استبياني)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanation field
            OutlinedTextField(
                value = question.explanation,
                onValueChange = { onUpdate(question.copy(explanation = it)) },
                label = { Text("تفسير الإجابة وشرح الحل (يظهر للطالب بعد الامتحان)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions: Required toggle, Duplicate, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = question.isRequired,
                        onCheckedChange = { onUpdate(question.copy(isRequired = it)) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مطلوب (إلزامي)", fontSize = 12.sp)
                }

                Row {
                    IconButton(onClick = onDuplicate) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "تكرار السؤال")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف السؤال", tint = ErrorRed)
                    }
                }
            }
        }
    }
}
