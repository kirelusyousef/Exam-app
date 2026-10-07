package com.example.export

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.SubmissionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Helper class that collects quiz response data from Cloud Firestore
 * and generates a standard Microsoft Excel (.xlsx) spreadsheet file for download and sharing.
 */
object FirestoreQuizExcelHelper {

    private const val TAG = "FirestoreQuizExcel"
    const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    data class QuizResponseData(
        val submissionId: String,
        val examTitle: String,
        val studentName: String,
        val studentCode: String,
        val score: Int,
        val totalPoints: Int,
        val percentage: Double,
        val isPassed: Boolean,
        val timeSpentSeconds: Int,
        val violationsCount: Int,
        val submittedAtDate: String
    )

    /**
     * Queries Firestore for all quiz responses under the authenticated user's account,
     * formats the data, and writes a real Microsoft Excel (.xlsx) OpenXML workbook.
     */
    suspend fun fetchResponsesAndGenerateXlsx(
        context: Context,
        examIdFilter: String? = null,
        fallbackLocalData: List<SubmissionEntity> = emptyList()
    ): Result<File> {
        return try {
            val responses = mutableListOf<QuizResponseData>()
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid

            if (uid != null) {
                try {
                    val db = FirebaseFirestore.getInstance(
                        context.getString(R.string.firestore_database_id)
                    )

                    var query = db.collection("users")
                        .document(uid)
                        .collection("submissions")

                    val snapshot = if (examIdFilter != null) {
                        query.whereEqualTo("examId", examIdFilter).get().await()
                    } else {
                        query.get().await()
                    }

                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

                    for (doc in snapshot.documents) {
                        val id = doc.getString("id") ?: doc.id
                        val examTitle = doc.getString("examTitle") ?: "اختبار"
                        val studentName = doc.getString("studentName") ?: "طالب"
                        val studentCode = doc.getString("studentCode") ?: "—"
                        val score = (doc.get("score") as? Number)?.toInt() ?: 0
                        val totalPoints = (doc.get("totalPoints") as? Number)?.toInt() ?: 0
                        val percentage = (doc.get("percentage") as? Number)?.toDouble() ?: 0.0
                        val isPassed = doc.getBoolean("isPassed") ?: (percentage >= 50.0)
                        val timeSpentSeconds = (doc.get("timeSpentSeconds") as? Number)?.toInt() ?: 0
                        val violationsCount = (doc.get("violationsCount") as? Number)?.toInt() ?: 0
                        val submittedAtTimestamp = (doc.get("submittedAt") as? Number)?.toLong() ?: System.currentTimeMillis()
                        val dateFormatted = dateFormat.format(Date(submittedAtTimestamp))

                        responses.add(
                            QuizResponseData(
                                submissionId = id,
                                examTitle = examTitle,
                                studentName = studentName,
                                studentCode = studentCode,
                                score = score,
                                totalPoints = totalPoints,
                                percentage = percentage,
                                isPassed = isPassed,
                                timeSpentSeconds = timeSpentSeconds,
                                violationsCount = violationsCount,
                                submittedAtDate = dateFormatted
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore query skipped/failed, falling back to local database: ${e.message}")
                }
            }

            // If Firestore returned no results (e.g. offline or unauthenticated), incorporate local database submissions
            if (responses.isEmpty() && fallbackLocalData.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                val filteredLocal = if (examIdFilter != null) fallbackLocalData.filter { it.examId == examIdFilter } else fallbackLocalData
                filteredLocal.forEach { sub ->
                    responses.add(
                        QuizResponseData(
                            submissionId = sub.id,
                            examTitle = sub.examTitle,
                            studentName = sub.studentName,
                            studentCode = sub.studentCode,
                            score = sub.score,
                            totalPoints = sub.totalPoints,
                            percentage = sub.percentage.toDouble(),
                            isPassed = sub.isPassed,
                            timeSpentSeconds = sub.timeSpentSeconds,
                            violationsCount = sub.violationsCount,
                            submittedAtDate = dateFormat.format(Date(sub.submittedAt))
                        )
                    )
                }
            }

            if (responses.isEmpty()) {
                return Result.failure(IllegalStateException("لا توجد استجابات مسجلة لتصدير ملف الإكسيل"))
            }

            // Generate .xlsx OpenXML package
            val file = generateXlsxFile(context, responses)
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Excel XLSX file: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates an Open Packaging Conventions (OPC) ZIP archive (.xlsx)
     * containing valid SpreadsheetML XML documents recognized natively by Microsoft Excel.
     */
    private fun generateXlsxFile(
        context: Context,
        dataList: List<QuizResponseData>
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "نتائج_الامتحانات_${timestamp}.xlsx"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, fileName)

        // Headers and string pool
        val headers = listOf(
            "م",
            "كود الطالب",
            "اسم الطالب",
            "عنوان الاختبار",
            "الدرجة المحرزة",
            "الدرجة الكاملة",
            "النسبة المئوية",
            "النتيجة",
            "الوقت المستغرق (دقيقة)",
            "مخالفات الغش",
            "تاريخ التقديم"
        )

        val stringPool = mutableListOf<String>()
        fun addString(str: String): Int {
            val idx = stringPool.indexOf(str)
            return if (idx >= 0) idx else {
                stringPool.add(str)
                stringPool.size - 1
            }
        }

        // Register header strings
        headers.forEach { addString(it) }

        // Build worksheet rows
        val sheetRowsXml = StringBuilder()

        // 1. Header row (row 1) with style s="1"
        sheetRowsXml.append("""<row r="1">""")
        headers.forEachIndexed { colIndex, _ ->
            val colLetter = getColumnLetter(colIndex + 1)
            val strIndex = addString(headers[colIndex])
            sheetRowsXml.append("""<c r="$colLetter"1"" t="s" s="1"><v>$strIndex</v></c>""")
        }
        sheetRowsXml.append("</row>")

        // 2. Data rows (row 2 onwards)
        dataList.forEachIndexed { rowIndex, item ->
            val r = rowIndex + 2
            val durationMin = String.format(Locale.US, "%.1f", item.timeSpentSeconds / 60.0)
            val statusStr = if (item.isPassed) "ناجح" else "راسب"

            val c1 = """<c r="A$r"><v>${rowIndex + 1}</v></c>"""
            val c2 = """<c r="B$r" t="s"><v>${addString(item.studentCode)}</v></c>"""
            val c3 = """<c r="C$r" t="s"><v>${addString(item.studentName)}</v></c>"""
            val c4 = """<c r="D$r" t="s"><v>${addString(item.examTitle)}</v></c>"""
            val c5 = """<c r="E$r"><v>${item.score}</v></c>"""
            val c6 = """<c r="F$r"><v>${item.totalPoints}</v></c>"""
            val c7 = """<c r="G$r" t="s"><v>${addString("${item.percentage.toInt()}%")}</v></c>"""
            val c8 = """<c r="H$r" t="s" s="${if (item.isPassed) 2 else 3}"><v>${addString(statusStr)}</v></c>"""
            val c9 = """<c r="I$r"><v>$durationMin</v></c>"""
            val c10 = """<c r="J$r"><v>${item.violationsCount}</v></c>"""
            val c11 = """<c r="K$r" t="s"><v>${addString(item.submittedAtDate)}</v></c>"""

            sheetRowsXml.append("""<row r="$r">$c1$c2$c3$c4$c5$c6$c7$c8$c9$c10$c11</row>""")
        }

        // Shared strings XML
        val sharedStringsXml = StringBuilder()
        sharedStringsXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sharedStringsXml.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${stringPool.size}" uniqueCount="${stringPool.size}">""")
        for (str in stringPool) {
            val escaped = escapeXml(str)
            sharedStringsXml.append("""<si><t>$escaped</t></si>""")
        }
        sharedStringsXml.append("</sst>")

        // Sheet XML
        val sheetXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheetViews>
        <sheetView rightToLeft="1" tabSelected="1" workbookViewId="0"/>
    </sheetViews>
    <sheetFormatPr defaultRowHeight="20"/>
    <cols>
        <col min="1" max="1" width="6" customWidth="1"/>
        <col min="2" max="2" width="14" customWidth="1"/>
        <col min="3" max="3" width="22" customWidth="1"/>
        <col min="4" max="4" width="30" customWidth="1"/>
        <col min="5" max="5" width="12" customWidth="1"/>
        <col min="6" max="6" width="12" customWidth="1"/>
        <col min="7" max="7" width="12" customWidth="1"/>
        <col min="8" max="8" width="10" customWidth="1"/>
        <col min="9" max="9" width="16" customWidth="1"/>
        <col min="10" max="10" width="14" customWidth="1"/>
        <col min="11" max="11" width="20" customWidth="1"/>
    </cols>
    <sheetData>
        $sheetRowsXml
    </sheetData>
</worksheet>"""

        // Styles XML (Header bold + colors)
        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="4">
        <font><sz val="11"/><name val="Segoe UI"/></font>
        <font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Segoe UI"/></font>
        <font><b/><color rgb="FF2E7D32"/><sz val="11"/><name val="Segoe UI"/></font>
        <font><b/><color rgb="FFD32F2F"/><sz val="11"/><name val="Segoe UI"/></font>
    </fonts>
    <fills count="4">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF673AB7"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFE8F5E9"/></patternFill></fill>
    </fills>
    <borders count="1">
        <border><left/><right/><top/><bottom/></border>
    </borders>
    <cellXfs count="4">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="2" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="center"/></xf>
        <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center"/></xf>
    </cellXfs>
</styleSheet>"""

        // Content Types
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

        // Root rels
        val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        // Workbook rels
        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

        // Workbook
        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="نتائج الاختبارات" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>"""

        // Pack into ZIP (.xlsx)
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            addZipEntry(zos, "[Content_Types].xml", contentTypesXml)
            addZipEntry(zos, "_rels/.rels", rootRelsXml)
            addZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml)
            addZipEntry(zos, "xl/workbook.xml", workbookXml)
            addZipEntry(zos, "xl/styles.xml", stylesXml)
            addZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
            addZipEntry(zos, "xl/sharedStrings.xml", sharedStringsXml.toString())
        }

        return outputFile
    }

    /**
     * Creates an Android Intent chooser to download, share, or open the generated .xlsx file.
     */
    fun openOrShareXlsx(context: Context, xlsxFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            xlsxFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME_TYPE
            putExtra(Intent.EXTRA_SUBJECT, "تصدير نتائج الاختبارات - Excel (.xlsx)")
            putExtra(Intent.EXTRA_TEXT, "مرفق مصنف إكسيل (.xlsx) يحتوي على تقرير نتائج الاختبارات من منصة الامتحانات.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "تنزيل أو فتح مصنف Excel (.xlsx)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun getColumnLetter(colNum: Int): String {
        var n = colNum
        val result = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            result.insert(0, ('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return result.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
