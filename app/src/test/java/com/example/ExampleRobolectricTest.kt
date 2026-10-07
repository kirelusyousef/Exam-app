package com.example

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.example.security.ExamSecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.data.model.SubmissionEntity
import com.example.export.FirestoreQuizExcelHelper
import kotlinx.coroutines.runBlocking
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ExamForm Pro", appName)
  }

  @Test
  fun `flag secure prevents screenshots and screen recording`() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    
    // Enable security
    ExamSecurityHelper.enableWindowSecurity(activity)
    assertTrue("FLAG_SECURE must be active on window", ExamSecurityHelper.isWindowSecure(activity))
    val flagsAfterEnable = activity.window.attributes.flags
    assertTrue((flagsAfterEnable and WindowManager.LayoutParams.FLAG_SECURE) != 0)

    // Disable security
    ExamSecurityHelper.disableWindowSecurity(activity)
    assertFalse("FLAG_SECURE must be removed from window", ExamSecurityHelper.isWindowSecure(activity))
  }

  @Test
  fun `generate valid openxml xlsx spreadsheet from quiz responses`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleSubmissions = listOf(
        SubmissionEntity(
            id = "test-sub-1",
            examId = "exam-1",
            examTitle = "اختبار الذكاء الاصطناعي",
            studentId = "ST-101",
            studentName = "أحمد محمد",
            studentCode = "ST-101",
            score = 90,
            totalPoints = 100,
            percentage = 90f,
            isPassed = true,
            answersMapJson = "{}",
            timeSpentSeconds = 600,
            violationsCount = 0
        )
    )

    val result = FirestoreQuizExcelHelper.fetchResponsesAndGenerateXlsx(
        context = context,
        examIdFilter = null,
        fallbackLocalData = sampleSubmissions
    )

    assertTrue("Excel generation must succeed", result.isSuccess)
    val xlsxFile = result.getOrThrow()
    assertTrue("File must exist", xlsxFile.exists())
    assertTrue("File size must be greater than 0", xlsxFile.length() > 0)
    assertTrue("File extension must be xlsx", xlsxFile.name.endsWith(".xlsx"))

    // Verify it is a valid OPC / OpenXML ZIP archive
    val zip = ZipFile(xlsxFile)
    assertNotNull(zip.getEntry("[Content_Types].xml"))
    assertNotNull(zip.getEntry("xl/workbook.xml"))
    assertNotNull(zip.getEntry("xl/worksheets/sheet1.xml"))
    zip.close()
  }
}
