package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppBrandingEntity
import com.example.data.model.StudentEntity
import com.example.data.model.SubmissionEntity
import com.example.export.ExportResult
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandSlate
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ExamViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    val submissions by viewModel.allSubmissions.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val branding by viewModel.appBranding.collectAsState()
    val exams by viewModel.allExams.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("النتائج الفورية", "إدارة الطلاب", "الهوية والشعار")

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var selectedExamFilter by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("لوحة تحكم الإدارة والمعلم", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = branding?.institutionName ?: "أكاديمية النخبة",
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
                    IconButton(onClick = { viewModel.lockAdmin(); viewModel.navigateHome() }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "قفل اللوحة", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            when (index) {
                                0 -> Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                                1 -> Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                                else -> Icon(imageVector = Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> ResultsTab(
                    submissions = submissions,
                    exams = exams.map { it.title },
                    selectedFilter = selectedExamFilter,
                    onFilterChanged = { selectedExamFilter = it },
                    onExportExcel = { viewModel.exportToExcel(selectedExamFilter) },
                    onSelectSubmission = { subId -> viewModel.navigateTo(AppScreen.SubmissionDetail(subId)) },
                    onDeleteSubmission = { sub -> viewModel.deleteSubmission(sub) }
                )
                1 -> StudentsTab(
                    students = students,
                    onAddStudentClick = { showAddStudentDialog = true },
                    onDeleteStudent = { student -> viewModel.deleteStudent(student) }
                )
                2 -> BrandingSettingsTab(
                    currentBranding = branding,
                    onSaveBranding = { updated ->
                        viewModel.updateBranding(updated)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تم حفظ هوية وشعار التطبيق بنجاح")
                        }
                    }
                )
            }
        }
    }

    // Add Student Dialog
    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { name, code, email, group ->
                viewModel.addStudent(name, code, email, group)
                showAddStudentDialog = false
            }
        )
    }

    // Export Feedback Dialog
    exportResult?.let { result ->
        when (result) {
            is ExportResult.Success -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissExportResult() },
                    icon = {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
                    },
                    title = { Text("تم تصدير الإكسيل بنجاح!", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("تم تجهيز وتصدير ${result.rowCount} صف من نتائج الطلاب بصيغة Excel (CSV) متوافقة تماماً مع الترميز العربي UTF-8 ومشاركتها.")
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.dismissExportResult() }) {
                            Text("تم")
                        }
                    }
                )
            }
            is ExportResult.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissExportResult() },
                    icon = {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(36.dp))
                    },
                    title = { Text("تعذر التصدير", fontWeight = FontWeight.Bold, color = ErrorRed) },
                    text = { Text(result.message) },
                    confirmButton = {
                        Button(onClick = { viewModel.dismissExportResult() }) {
                            Text("حسناً")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ResultsTab(
    submissions: List<SubmissionEntity>,
    exams: List<String>,
    selectedFilter: String?,
    onFilterChanged: (String?) -> Unit,
    onExportExcel: () -> Unit,
    onSelectSubmission: (String) -> Unit,
    onDeleteSubmission: (SubmissionEntity) -> Unit
) {
    val filtered = if (selectedFilter == null) submissions else submissions.filter { it.examTitle == selectedFilter }

    val totalCount = filtered.size
    val passCount = filtered.count { it.isPassed }
    val passRate = if (totalCount > 0) ((passCount.toFloat() / totalCount.toFloat()) * 100f).toInt() else 0
    val avgScore = if (totalCount > 0) filtered.map { it.percentage }.average().toInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Analytics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(title = "المشاركات", value = "$totalCount", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                StatCard(title = "متوسط الدرجات", value = "$avgScore%", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                StatCard(title = "نسبة النجاح", value = "$passRate%", color = SuccessGreen, modifier = Modifier.weight(1f))
            }
        }

        // Export to Excel Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SuccessGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تصدير النتائج إلى ملف Excel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("بصيغة CSV تدعم اللغة العربية UTF-8 بالكامل بدون تشويه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = onExportExcel,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("export_excel_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير إكسيل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Exam Filter Chips
        if (exams.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onFilterChanged(null) },
                        label = { Text("الكل") }
                    )
                    exams.take(3).forEach { examTitle ->
                        FilterChip(
                            selected = selectedFilter == examTitle,
                            onClick = { onFilterChanged(if (selectedFilter == examTitle) null else examTitle) },
                            label = { Text(examTitle.take(15) + "...") }
                        )
                    }
                }
            }
        }

        // List Header
        item {
            Text(
                text = "قائمة إجابات الطلاب المودعة (${filtered.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("لا توجد إجابات مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filtered) { sub ->
                SubmissionItemCard(
                    sub = sub,
                    onClick = { onSelectSubmission(sub.id) },
                    onDelete = { onDeleteSubmission(sub) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun SubmissionItemCard(
    sub: SubmissionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(sub.submittedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("submission_card_${sub.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sub.studentName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = sub.studentCode,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sub.examTitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$dateStr • ${sub.timeSpentSeconds / 60}د",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sub.violationsCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚠️ ${sub.violationsCount} مخالفات غش",
                            fontSize = 10.sp,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (sub.isPassed) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${sub.percentage.toInt()}% (${sub.score}/${sub.totalPoints})",
                        color = if (sub.isPassed) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف النتيجة", tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun StudentsTab(
    students: List<StudentEntity>,
    onAddStudentClick: () -> Unit,
    onDeleteStudent: (StudentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الطلاب المسجلين (${students.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onAddStudentClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_student_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة طالب")
                }
            }
        }

        if (students.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("لا يوجد طلاب مسجلون بعد. اضغط على 'إضافة طالب' لإدراج طالب جديد.")
                    }
                }
            }
        } else {
            items(students) { student ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("الكود: ${student.studentCode} • المجموعة: ${student.groupName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (student.email.isNotBlank()) {
                                Text(student.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        IconButton(onClick = { onDeleteStudent(student) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الطالب", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrandingSettingsTab(
    currentBranding: AppBrandingEntity?,
    onSaveBranding: (AppBrandingEntity) -> Unit
) {
    var appName by remember(currentBranding) { mutableStateOf(currentBranding?.appName ?: "ExamForm Pro") }
    var institutionName by remember(currentBranding) { mutableStateOf(currentBranding?.institutionName ?: "أكاديمية النخبة التعليمية") }
    var slogan by remember(currentBranding) { mutableStateOf(currentBranding?.slogan ?: "منظومة الامتحانات الرقمية الآمنة") }
    var selectedLogoType by remember(currentBranding) { mutableStateOf(currentBranding?.logoType ?: "DEFAULT") }
    var selectedColorHex by remember(currentBranding) { mutableStateOf(currentBranding?.themeColorHex ?: "#673AB7") }
    var adminPin by remember(currentBranding) { mutableStateOf(currentBranding?.adminPin ?: "1234") }
    var instructionsText by remember(currentBranding) { mutableStateOf(currentBranding?.instructionsText ?: "") }
    var globalSecurity by remember(currentBranding) { mutableStateOf(currentBranding?.globalScreenSecurity ?: true) }

    val presetColors = listOf(
        "#673AB7" to "أرجواني نماذج جوجل",
        "#1976D2" to "أزرق ملكي ذكي",
        "#00796B" to "أخضر زمردي",
        "#C2185B" to "قرمزي احترافي",
        "#37474F" to "رمادي أنيق"
    )

    data class LogoOption(val type: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

    val logoPresets = listOf(
        LogoOption("DEFAULT", "الشعار الرسمي", Icons.Default.VerifiedUser),
        LogoOption("GRADUATION", "قبعة التخرج الأكاديمية", Icons.Default.School),
        LogoOption("SHIELD", "درع الأمان والنزاهة", Icons.Default.Security),
        LogoOption("STAR", "نجمة التفوق", Icons.Default.Star),
        LogoOption("ACADEMY", "القفل المحمي", Icons.Default.Lock)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("تخصيص الهوية والشعار المؤسسي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("يمكنك تعديل اسم التطبيق، المؤسسة التعليمية، الشعار ولون السمة ليتوافق مع هويتكم الرسمية.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text("اسم التطبيق والمنظومة") },
                modifier = Modifier.fillMaxWidth().testTag("branding_app_name_input")
            )
        }

        item {
            OutlinedTextField(
                value = institutionName,
                onValueChange = { institutionName = it },
                label = { Text("اسم المؤسسة / المدرسة / الأكاديمية") },
                modifier = Modifier.fillMaxWidth().testTag("branding_institution_input")
            )
        }

        item {
            OutlinedTextField(
                value = slogan,
                onValueChange = { slogan = it },
                label = { Text("الشعار اللفظي أو الوصف الفرعي") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Logo Style Selector
        item {
            Text("اختر رمز الشعار المعتمد:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                logoPresets.forEach { option ->
                    val isSelected = selectedLogoType == option.type
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedLogoType = option.type }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Color Palette Selector
        item {
            Text("لون هوية التطبيق الأساسي:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetColors.forEach { (hex, name) ->
                    val color = parseHexColor(hex, BrandPurple)
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = adminPin,
                onValueChange = { adminPin = it.filter { c -> c.isDigit() } },
                label = { Text("رمز حماية المشرف (PIN Code)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = instructionsText,
                onValueChange = { instructionsText = it },
                label = { Text("إرشادات وسياسة الاختبار للطلاب") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("حماية الشاشة ومنع التصوير في كامل التطبيق", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("يمنع التقاط سكرين شوت أو تسجيل الفيديو في كافة الشاشات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = globalSecurity, onCheckedChange = { globalSecurity = it })
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val updated = (currentBranding ?: AppBrandingEntity()).copy(
                        appName = appName,
                        institutionName = institutionName,
                        slogan = slogan,
                        logoType = selectedLogoType,
                        themeColorHex = selectedColorHex,
                        adminPin = adminPin,
                        instructionsText = instructionsText,
                        globalScreenSecurity = globalSecurity
                    )
                    onSaveBranding(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_branding_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ وتطبيق الهوية فورياً", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("الفوج أ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة طالب جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الطالب *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("رقم القيد / الكود *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = group, onValueChange = { group = it }, label = { Text("المجموعة / الصف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank()) {
                        onConfirm(name, code, email, group)
                    }
                }
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
