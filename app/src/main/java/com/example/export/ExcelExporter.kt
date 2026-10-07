package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.SubmissionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    fun exportSubmissionsToExcel(
        context: Context,
        submissions: List<SubmissionEntity>,
        examTitleFilter: String? = null
    ): ExportResult {
        if (submissions.isEmpty()) {
            return ExportResult.Error("لا توجد نتائج مسجلة لتصديرها")
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "نتائج_الامتحانات_${timestamp}.csv"
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Write UTF-8 BOM so Microsoft Excel correctly displays Arabic text
                    writer.write("\uFEFF")

                    // Title header
                    val title = examTitleFilter?.let { "تقرير نتائج: $it" } ?: "تقرير النتائج الشامل للاختبارات"
                    writer.write(escapeCsv(title) + "\n")
                    writer.write(escapeCsv("تاريخ الاستخراج: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())) + "\n\n")

                    // Table headers
                    val headers = listOf(
                        "م",
                        "كود الطالب",
                        "اسم الطالب",
                        "عنوان الاختبار",
                        "الدرجة المحرزة",
                        "الدرجة الكاملة",
                        "النسبة المئوية",
                        "الحالة",
                        "الوقت المستغرق (دقيقة)",
                        "تنبيهات الغش والتبديل",
                        "تاريخ ووقت التقديم"
                    )
                    writer.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

                    // Data rows
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    submissions.forEachIndexed { index, sub ->
                        val durationMinutes = String.format(Locale.US, "%.1f", sub.timeSpentSeconds / 60.0)
                        val status = if (sub.isPassed) "ناجح" else "راسب"
                        val formattedDate = dateFormat.format(Date(sub.submittedAt))

                        val row = listOf(
                            (index + 1).toString(),
                            sub.studentCode,
                            sub.studentName,
                            sub.examTitle,
                            sub.score.toString(),
                            sub.totalPoints.toString(),
                            "${sub.percentage.toInt()}%",
                            status,
                            durationMinutes,
                            sub.violationsCount.toString(),
                            formattedDate
                        )
                        writer.write(row.joinToString(",") { escapeCsv(it) } + "\n")
                    }
                }
            }

            // Generate content URI with FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            return ExportResult.Success(
                file = file,
                uri = uri,
                rowCount = submissions.size
            )
        } catch (e: Exception) {
            return ExportResult.Error("فشل تصدير ملف الإكسيل: ${e.localizedMessage}")
        }
    }

    fun shareExportedFile(context: Context, success: ExportResult.Success) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "تصدير نتائج الامتحانات - إكسيل")
            putExtra(Intent.EXTRA_TEXT, "مرفق ملف نتائج الامتحانات المستخرج بصيغة متوافقة مع Microsoft Excel و Google Sheets.")
            putExtra(Intent.EXTRA_STREAM, success.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "فتح أو مشاركة نتائج الامتحانات عبر إكسيل")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        var str = value
        if (str.contains("\"")) {
            str = str.replace("\"", "\"\"")
        }
        return if (str.contains(",") || str.contains("\n") || str.contains("\r") || str.contains("\"")) {
            "\"$str\""
        } else {
            str
        }
    }
}

sealed class ExportResult {
    data class Success(
        val file: File,
        val uri: android.net.Uri,
        val rowCount: Int
    ) : ExportResult()

    data class Error(val message: String) : ExportResult()
}
