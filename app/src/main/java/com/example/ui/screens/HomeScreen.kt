package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.ui.components.AdminPinDialog
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ExamViewModel

/**
 * Clean, lightweight home screen focusing EXCLUSIVELY on exams as requested.
 * All extraneous banners, metric boxes, and slogans have been removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    val exams by viewModel.allExams.collectAsState()
    val branding by viewModel.appBranding.collectAsState()
    val isOnlineSyncing by viewModel.isOnlineSyncing.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var selectedExamToTake by remember { mutableStateOf<ExamEntity?>(null) }
    var studentNameInput by remember { mutableStateOf("") }
    var studentCodeInput by remember { mutableStateOf("") }
    var studentInputError by remember { mutableStateOf(false) }

    // Excel Import feedback state
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var isImportSuccess by remember { mutableStateOf(false) }

    // Excel file picker launcher (.xlsx and .csv)
    val excelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importExamFromExcel(uri) { success, message ->
                isImportSuccess = success
                importResultMessage = message
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "الامتحانات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${exams.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Online Sync Button
                    IconButton(
                        onClick = { viewModel.syncOnlineData() },
                        enabled = !isOnlineSyncing,
                        modifier = Modifier.testTag("sync_online_button")
                    ) {
                        if (isOnlineSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "تحديث ومزامنة سحابية أونلاين",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Import Exam from Excel Button
                    IconButton(
                        onClick = {
                            excelFilePicker.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/vnd.ms-excel",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.testTag("import_excel_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "رفع امتحان من إكسيل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Admin Dashboard Button
                    IconButton(
                        onClick = {
                            if (viewModel.isAdminUnlocked.value) {
                                viewModel.navigateTo(AppScreen.AdminDashboard)
                            } else {
                                showPinDialog = true
                            }
                        },
                        modifier = Modifier.testTag("admin_dashboard_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "لوحة تحكم الإدارة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.initExamBuilder(null)
                    viewModel.navigateTo(AppScreen.BuildExam(null))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_exam_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إنشاء اختبار")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إنشاء نموذج", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (exams.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "لا توجد امتحانات متاحة حالياً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يمكنك إنشاء نموذج اختبار جديد أو رفعه مباشرة عبر ملف Excel (.xlsx)",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.initExamBuilder(null)
                                        viewModel.navigateTo(AppScreen.BuildExam(null))
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إنشاء اختبار")
                                }
                                OutlinedButton(
                                    onClick = {
                                        excelFilePicker.launch(arrayOf("*/*"))
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("رفع من Excel")
                                }
                            }
                        }
                    }
                }
            } else {
                // EXCLUSIVELY display exam cards
                items(exams, key = { it.id }) { exam ->
                    ExamCard(
                        exam = exam,
                        onTakeExam = {
                            selectedExamToTake = exam
                        },
                        onEditExam = {
                            viewModel.initExamBuilder(exam.id)
                            viewModel.navigateTo(AppScreen.BuildExam(exam.id))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Admin PIN Dialog
    if (showPinDialog) {
        AdminPinDialog(
            onDismiss = { showPinDialog = false },
            onPinSuccess = {
                showPinDialog = false
                viewModel.navigateTo(AppScreen.AdminDashboard)
            },
            checkPin = { pin -> viewModel.checkAdminPin(pin) }
        )
    }

    // Excel Import Result Dialog
    importResultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { importResultMessage = null },
            icon = {
                Icon(
                    imageVector = if (isImportSuccess) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = if (isImportSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isImportSuccess) "تم استيراد الامتحان ورفعه سحابياً!" else "تنبيه استيراد Excel",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(text = message) },
            confirmButton = {
                Button(onClick = { importResultMessage = null }) {
                    Text("حسناً")
                }
            }
        )
    }

    // Student Enrollment Dialog before taking exam
    selectedExamToTake?.let { exam ->
        AlertDialog(
            onDismissRequest = { selectedExamToTake = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تسجيل دخول الطالب للاختبار",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "المدة: ${if (exam.durationMinutes > 0) "${exam.durationMinutes} دقيقة" else "مفتوحة"} | النجاح من ${exam.passingScorePercent}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🛡️ ملاحظة أمنية: تمنع المنظومة لقطات الشاشة أو النسخ، وتُسجل أي مغادرة للاختبار تلقائياً في النتائج.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    OutlinedTextField(
                        value = studentNameInput,
                        onValueChange = {
                            studentNameInput = it
                            studentInputError = false
                        },
                        label = { Text("اسم الطالب الكامل *") },
                        singleLine = true,
                        isError = studentInputError && studentNameInput.isBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_name_input")
                    )

                    OutlinedTextField(
                        value = studentCodeInput,
                        onValueChange = { studentCodeInput = it },
                        label = { Text("رقم القيد / كود الطالب (اختياري)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (studentNameInput.isNotBlank()) {
                            val name = studentNameInput.trim()
                            val code = studentCodeInput.ifBlank { "ST-${System.currentTimeMillis().toString().takeLast(4)}" }
                            val examId = exam.id
                            selectedExamToTake = null
                            viewModel.startExamSession(examId, name, code)
                            viewModel.navigateTo(AppScreen.TakeExam(examId))
                        } else {
                            studentInputError = true
                        }
                    },
                    modifier = Modifier.testTag("start_exam_button")
                ) {
                    Text("بدء الاختبار الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedExamToTake = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ExamCard(
    exam: ExamEntity,
    onTakeExam: () -> Unit,
    onEditExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exam_card_${exam.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = exam.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "أونلاين ☁️",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (exam.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = exam.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (exam.durationMinutes > 0) "${exam.durationMinutes} دقيقة" else "بدون مؤقت",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "محمي من لقطات الشاشة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTakeExam,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("take_exam_btn_${exam.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("دخول الاختبار")
                }

                OutlinedButton(
                    onClick = onEditExam,
                    modifier = Modifier.testTag("edit_exam_btn_${exam.id}")
                ) {
                    Text("تعديل النموذج")
                }
            }
        }
    }
}
