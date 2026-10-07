package com.example

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.example.security.ExamSecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
