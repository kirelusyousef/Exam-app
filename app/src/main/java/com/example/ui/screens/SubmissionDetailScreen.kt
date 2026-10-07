package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuestionEntity
import com.example.data.model.SubmissionEntity
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ExamViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionDetailScreen(
    submissionId: String,
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    var submission by remember { mutableStateOf<SubmissionEntity?>(null) }
    var questions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var parsedAnswers by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    LaunchedEffect(submissionId) {
        val sub = viewModel.getSubmissionById(submissionId)
        submission = sub
        if (sub != null) {
            questions = viewModel.getQuestionsForExam(sub.examId)
            try {
                val json = JSONObject(sub.answersMapJson)
                val map = mutableMapOf<String, List<String>>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = json.getJSONArray(k)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }
                    map[k] = list
                }
                parsedAnswers = map
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("مراجعة إجابات الطالب بالتفصيل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (submission == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("جاري استرجاع تفاصيل التقديم...")
            }
            return@Scaffold
        }

        val sub = submission!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Student & Exam Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sub.studentName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("كود الطالب: ${sub.studentCode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = if (sub.isPassed) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (sub.isPassed) "ناجح (${sub.percentage.toInt()}%)" else "راسب (${sub.percentage.toInt()}%)",
                                    color = if (sub.isPassed) SuccessGreen else ErrorRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(sub.examTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الدرجة: ${sub.score} من ${sub.totalPoints}", fontSize = 12.sp)
                            Text("الوقت: ${sub.timeSpentSeconds / 60}د", fontSize = 12.sp)
                            Text("مخالفات الغش: ${sub.violationsCount}", fontSize = 12.sp, color = if (sub.violationsCount > 0) ErrorRed else SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("تفاصيل الأسئلة وإجابات الطالب (${questions.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            itemsIndexed(questions) { index, q ->
                val studentSelected = parsedAnswers[q.id] ?: emptyList()
                val isStudentAnswerCorrect = when {
                    q.correctAnswers.isEmpty() -> true
                    q.correctAnswers.size == 1 && studentSelected.size == 1 -> q.correctAnswers.first().equals(studentSelected.first(), ignoreCase = true)
                    else -> q.correctAnswers.toSet() == studentSelected.toSet()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isStudentAnswerCorrect) SuccessGreen.copy(alpha = 0.4f) else ErrorRed.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سؤال ${index + 1}: ${q.questionType.titleAr}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isStudentAnswerCorrect) SuccessGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isStudentAnswerCorrect) "+${q.points} درجات" else "0 / ${q.points}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStudentAnswerCorrect) SuccessGreen else ErrorRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(q.questionText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Student Answer Display
                        Text("إجابة الطالب المودعة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isStudentAnswerCorrect) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isStudentAnswerCorrect) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isStudentAnswerCorrect) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (studentSelected.isNotEmpty()) studentSelected.joinToString(", ") else "لم يتم الإجابة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isStudentAnswerCorrect) SuccessGreen else ErrorRed
                                )
                            }
                        }

                        if (!isStudentAnswerCorrect && q.correctAnswers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("الإجابة النموذجية الصحيحة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessGreen.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = q.correctAnswers.joinToString(", "),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        if (q.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 الشرح والتفسير: ${q.explanation}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
