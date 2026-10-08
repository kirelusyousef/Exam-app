package com.example.export

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import com.example.data.model.ExamEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionType
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ParsedExamResult(
    val exam: ExamEntity,
    val questions: List<QuestionEntity>,
    val parsedRowCount: Int,
    val warnings: List<String> = emptyList()
)

object ExcelExamImporter {

    private const val TAG = "ExcelExamImporter"

    /**
     * Imports an exam and its questions from a user-selected URI (supports .xlsx and .csv).
     */
    fun importExamFromUri(
        context: Context,
        uri: Uri,
        customExamTitle: String? = null
    ): Result<ParsedExamResult> {
        return try {
            val contentResolver = context.contentResolver
            val fileName = queryFileName(context, uri) ?: "اختبار_مستورد_${System.currentTimeMillis()}"
            val isXlsx = fileName.endsWith(".xlsx", ignoreCase = true)

            val rows: List<List<String>> = contentResolver.openInputStream(uri)?.use { stream ->
                if (isXlsx) {
                    parseXlsxInputStream(stream)
                } else {
                    parseCsvInputStream(stream)
                }
            } ?: return Result.failure(IllegalArgumentException("تعذر فتح ملف الإكسيل المحدد"))

            if (rows.isEmpty()) {
                return Result.failure(IllegalArgumentException("ملف الإكسيل فارغ ولا يحتوي على بيانات"))
            }

            val examTitle = customExamTitle?.takeIf { it.isNotBlank() }
                ?: cleanFileNameToExamTitle(fileName)

            val parsed = convertRowsToExam(rows, examTitle)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import exam from Excel: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parses standard CSV input stream with multi-delimiter support (comma, semicolon, tab)
     * and UTF-8 handling (with or without BOM).
     */
    private fun parseCsvInputStream(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            var line: String? = reader.readLine()
            // Strip BOM if present
            if (line != null && line.startsWith("\uFEFF")) {
                line = line.substring(1)
            }

            while (line != null) {
                if (line.isNotBlank()) {
                    val tokens = parseCsvLine(line)
                    if (tokens.any { it.isNotBlank() }) {
                        rows.add(tokens)
                    }
                }
                line = reader.readLine()
            }
        }
        return rows
    }

    private fun parseCsvLine(line: String): List<String> {
        // Detect delimiter: comma, semicolon, or tab
        val delimiter = when {
            line.contains(";") && !line.contains(",") -> ';'
            line.contains("\t") -> '\t'
            else -> ','
        }

        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Parses Microsoft Excel OpenXML (.xlsx) directly using Android's XmlPullParser
     * without requiring any heavy third-party libraries.
     */
    private fun parseXlsxInputStream(inputStream: InputStream): List<List<String>> {
        val bytes = inputStream.readBytes()
        val sharedStrings = mutableListOf<String>()

        // 1. Extract sharedStrings.xml
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                    val stringBytes = zis.readBytes()
                    parseSharedStringsXml(ByteArrayInputStream(stringBytes), sharedStrings)
                    break
                }
                entry = zis.nextEntry
            }
        }

        // 2. Extract sheet1.xml
        val rows = mutableListOf<List<String>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true)) {
                    val sheetBytes = zis.readBytes()
                    parseSheetXml(ByteArrayInputStream(sheetBytes), sharedStrings, rows)
                    break
                }
                entry = zis.nextEntry
            }
        }

        return rows
    }

    private fun parseSharedStringsXml(input: InputStream, outList: MutableList<String>) {
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var eventType = parser.eventType
        var currentText = java.lang.StringBuilder()
        var insideTextTag = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        insideTextTag = true
                        currentText.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideTextTag) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        insideTextTag = false
                    } else if (parser.name == "si") {
                        outList.add(currentText.toString())
                        currentText.setLength(0)
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseSheetXml(
        input: InputStream,
        sharedStrings: List<String>,
        outRows: MutableList<List<String>>
    ) {
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var eventType = parser.eventType

        var currentRow = mutableMapOf<Int, String>()
        var currentCellRef = ""
        var currentCellType = ""
        var insideValueTag = false
        var cellValueText = java.lang.StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "row") {
                        currentRow = mutableMapOf()
                    } else if (parser.name == "c") {
                        currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                        currentCellType = parser.getAttributeValue(null, "t") ?: ""
                    } else if (parser.name == "v") {
                        insideValueTag = true
                        cellValueText.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideValueTag) {
                        cellValueText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "v") {
                        insideValueTag = false
                        val raw = cellValueText.toString().trim()
                        val colIndex = cellRefToColIndex(currentCellRef)
                        val text = if (currentCellType == "s") {
                            val strIndex = raw.toIntOrNull() ?: -1
                            if (strIndex in sharedStrings.indices) sharedStrings[strIndex] else raw
                        } else {
                            raw
                        }
                        currentRow[colIndex] = text
                    } else if (parser.name == "row") {
                        if (currentRow.isNotEmpty()) {
                            val maxCol = currentRow.keys.maxOrNull() ?: 0
                            val rowList = (0..maxCol).map { idx -> currentRow[idx] ?: "" }
                            if (rowList.any { it.isNotBlank() }) {
                                outRows.add(rowList)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun cellRefToColIndex(ref: String): Int {
        var col = 0
        for (c in ref) {
            if (c in 'A'..'Z') {
                col = col * 26 + (c - 'A' + 1)
            } else {
                break
            }
        }
        return if (col > 0) col - 1 else 0
    }

    /**
     * Converts raw parsed rows into ExamEntity and List<QuestionEntity>.
     */
    private fun convertRowsToExam(
        rawRows: List<List<String>>,
        examTitle: String
    ): ParsedExamResult {
        if (rawRows.isEmpty()) {
            throw IllegalArgumentException("لا توجد بيانات صالحة")
        }

        // Check if row 0 is header
        val firstRow = rawRows[0].map { it.trim() }
        val hasHeader = firstRow.any {
            it.contains("سؤال", ignoreCase = true) ||
            it.contains("question", ignoreCase = true) ||
            it.contains("النوع", ignoreCase = true) ||
            it.contains("type", ignoreCase = true) ||
            it.contains("خيارات", ignoreCase = true) ||
            it.contains("options", ignoreCase = true)
        }

        var colQuestion = 0
        var colType = 1
        var colOptions = 2
        var colCorrect = 3
        var colPoints = 4
        var colExplanation = 5

        var optACol = -1
        var optBCol = -1
        var optCCol = -1
        var optDCol = -1

        val dataRows = if (hasHeader) {
            firstRow.forEachIndexed { index, name ->
                val lower = name.lowercase()
                when {
                    lower.contains("سؤال") || lower.contains("question") || lower.contains("نص") -> colQuestion = index
                    lower.contains("نوع") || lower.contains("type") -> colType = index
                    lower.contains("خيارات") || lower.contains("options") || lower.contains("بدائل") -> colOptions = index
                    lower.contains("صحيحة") || lower.contains("correct") || lower.contains("إجابة") || lower.contains("حل") -> colCorrect = index
                    lower.contains("درجة") || lower.contains("نقاط") || lower.contains("point") || lower.contains("score") -> colPoints = index
                    lower.contains("شرح") || lower.contains("تفسير") || lower.contains("explanation") -> colExplanation = index
                    lower.contains("أ") || lower.contains("a") || lower.contains("خيار 1") -> optACol = index
                    lower.contains("ب") || lower.contains("b") || lower.contains("خيار 2") -> optBCol = index
                    lower.contains("ج") || lower.contains("c") || lower.contains("خيار 3") -> optCCol = index
                    lower.contains("د") || lower.contains("d") || lower.contains("خيار 4") -> optDCol = index
                }
            }
            rawRows.drop(1)
        } else {
            rawRows
        }

        val examId = UUID.randomUUID().toString()
        val questions = mutableListOf<QuestionEntity>()
        val warnings = mutableListOf<String>()

        dataRows.forEachIndexed { index, row ->
            val questionText = row.getOrNull(colQuestion)?.trim().orEmpty()
            if (questionText.isBlank()) return@forEachIndexed

            val typeRaw = row.getOrNull(colType)?.trim().orEmpty()
            val optionsRaw = row.getOrNull(colOptions)?.trim().orEmpty()
            val correctRaw = row.getOrNull(colCorrect)?.trim().orEmpty()
            val pointsRaw = row.getOrNull(colPoints)?.trim().orEmpty()
            val explanation = row.getOrNull(colExplanation)?.trim().orEmpty()

            // Resolve options
            val optionsList = mutableListOf<String>()
            if (optACol >= 0 && row.getOrNull(optACol)?.isNotBlank() == true) {
                listOfNotNull(row.getOrNull(optACol), row.getOrNull(optBCol), row.getOrNull(optCCol), row.getOrNull(optDCol))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .let { optionsList.addAll(it) }
            } else if (optionsRaw.isNotBlank()) {
                val split = when {
                    optionsRaw.contains("|") -> optionsRaw.split("|")
                    optionsRaw.contains(";") -> optionsRaw.split(";")
                    optionsRaw.contains(",") -> optionsRaw.split(",")
                    else -> listOf(optionsRaw)
                }
                optionsList.addAll(split.map { it.trim() }.filter { it.isNotBlank() })
            }

            // Determine question type
            val questionType = when {
                typeRaw.contains("صح") || typeRaw.contains("true") || typeRaw.contains("خطأ") -> QuestionType.TRUE_FALSE
                typeRaw.contains("مربعات") || typeRaw.contains("checkbox") -> QuestionType.CHECKBOXES
                typeRaw.contains("أكمل") || typeRaw.contains("قصيرة") || typeRaw.contains("short") -> QuestionType.SHORT_ANSWER
                typeRaw.contains("تقييم") || typeRaw.contains("rating") -> QuestionType.RATING_SCALE
                typeRaw.contains("متعدد") || typeRaw.contains("mc") -> QuestionType.MULTIPLE_CHOICE
                optionsList.size == 2 && optionsList.any { it.contains("صح") } -> QuestionType.TRUE_FALSE
                optionsList.isNotEmpty() -> QuestionType.MULTIPLE_CHOICE
                else -> QuestionType.SHORT_ANSWER
            }

            // If TRUE_FALSE and no options provided, provide standard options
            if (questionType == QuestionType.TRUE_FALSE && optionsList.isEmpty()) {
                optionsList.add("صح")
                optionsList.add("خطأ")
            }

            // Resolve correct answer(s)
            val correctAnswers = mutableListOf<String>()
            if (correctRaw.isNotBlank()) {
                val corrects = if (correctRaw.contains("|")) correctRaw.split("|") else listOf(correctRaw)
                corrects.forEach { ans ->
                    val clean = ans.trim()
                    // If correct is a 1-based number (e.g. "1" or "2"), map to option text if possible
                    val num = clean.toIntOrNull()
                    if (num != null && num in 1..optionsList.size) {
                        correctAnswers.add(optionsList[num - 1])
                    } else {
                        // Look for exact or matching option
                        val match = optionsList.firstOrNull { it.equals(clean, ignoreCase = true) }
                        correctAnswers.add(match ?: clean)
                    }
                }
            } else if (optionsList.isNotEmpty()) {
                // Default to first option
                correctAnswers.add(optionsList.first())
                warnings.add("سؤال #${index + 1}: لم تُحدد إجابة صحيحة، تم تعيين الخيار الأول تلقائياً.")
            }

            val points = pointsRaw.toIntOrNull() ?: 10

            questions.add(
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    examId = examId,
                    orderIndex = questions.size,
                    questionType = questionType,
                    questionText = questionText,
                    options = optionsList,
                    correctAnswers = correctAnswers,
                    points = points,
                    explanation = explanation,
                    isRequired = true
                )
            )
        }

        if (questions.isEmpty()) {
            throw IllegalArgumentException("لم يتم العثور على أي أسئلة صالحة في ملف الإكسيل")
        }

        val exam = ExamEntity(
            id = examId,
            title = examTitle,
            description = "تم استيراد هذا الاختبار آلياً من ملف إكسيل يحتوي على ${questions.size} سؤال.",
            durationMinutes = 20,
            passingScorePercent = 50,
            shuffleQuestions = false,
            preventScreenshots = true,
            createdAt = System.currentTimeMillis(),
            category = "نماذج مستوردة"
        )

        return ParsedExamResult(
            exam = exam,
            questions = questions,
            parsedRowCount = questions.size,
            warnings = warnings
        )
    }

    /**
     * Generates a ready-to-use sample template (.csv with UTF-8 BOM) that teachers can
     * fill with questions and re-import into the application and Firestore database.
     */
    fun createSampleTemplateFile(context: Context): File {
        val dir = File(context.cacheDir, "templates").apply { mkdirs() }
        val file = File(dir, "قالب_امتحان_إكسيل_نموذجي.csv")

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write("\uFEFF") // UTF-8 BOM for Microsoft Excel
                // Header row
                writer.write("السؤال,نوع السؤال,الخيارات,الإجابة الصحيحة,الدرجة,الشرح\n")
                // Sample rows
                writer.write("\"ما هي عاصمة جمهورية مصر العربية؟\",\"اختيار من متعدد\",\"القاهرة|الإسكندرية|الجيزة|الأقصر\",\"القاهرة\",10,\"القاهرة هي العاصمة السياسية\"\n")
                writer.write("\"تُعد الشمس كوكباً وليست نجماً\",\"صح أو خطأ\",\"صح|خطأ\",\"خطأ\",10,\"الشمس هي النجم المركزي للمجموعة الشمسية\"\n")
                writer.write("\"ما هو أصغر كوكب في المجموعة الشمسية؟\",\"اختيار من متعدد\",\"عطارد|المريخ|الزهرة|المشتري\",\"عطارد\",10,\"عطارد هو الأقرب والأصغر\"\n")
                writer.write("\"اذكر لغة البرمجة الرسمية المعتمدة لتطبيقات أندرويد\",\"إجابة قصيرة\",\"Kotlin\",\"Kotlin\",10,\"تم اعتماد Kotlin رسمياً من Google عام 2017\"\n")
                writer.write("\"أي من التالي يُعتبر من وحدات الإدخال؟\",\"مربعات اختيار\",\"لوحة المفاتيح|الفأرة|الشاشة|السماعات\",\"لوحة المفاتيح|الفأرة\",10,\"لوحة المفاتيح والفأرة وحدات إدخال\"\n")
            }
        }
        return file
    }

    private fun cleanFileNameToExamTitle(fileName: String): String {
        return fileName
            .replace(".xlsx", "", ignoreCase = true)
            .replace(".csv", "", ignoreCase = true)
            .replace(".xls", "", ignoreCase = true)
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .ifBlank { "اختبار إكسيل جديد" }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val colIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (colIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(colIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name == null) {
            name = uri.path?.let { File(it).name }
        }
        return name
    }
}
