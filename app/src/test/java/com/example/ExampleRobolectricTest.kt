package com.example

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.AppThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-port")
class ExampleRobolectricTest {

  @get:Rule
  val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GateX Labs", appName)
  }

  private fun dismissSplashIfVisible() {
    composeRule.mainClock.advanceTimeBy(3000L)
    composeRule.waitForIdle()
  }

  @Test
  fun `pre opening splash displays GateX Labs branding and enters dashboard`() {
    composeRule.onNodeWithTag("pre_opening_splash").assertIsDisplayed()
    composeRule.onNodeWithText("SBBU UNIVERSITY NAWABSHAH").assertIsDisplayed()
    composeRule.onNodeWithText("SALMAN 26BSIT-70").assertIsDisplayed()
    composeRule.onNodeWithTag("pre_opening_splash").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("GATEX LABS").assertIsDisplayed()
  }

  @Test
  fun `main activity launches in vertical portrait orientation without crash`() {
    dismissSplashIfVisible()
    assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, composeRule.activity.requestedOrientation)
    composeRule.onNodeWithText("GATEX LABS").assertIsDisplayed()
  }

  @Test
  fun `eighteen distinct app themes including floral and aesthetic are registered`() {
    assertEquals(18, AppThemePreset.entries.size)
    assertEquals(18, AppThemePreset.entries.map { it.id }.toSet().size)
    assertEquals(18, AppThemePreset.entries.map { it.displayName }.toSet().size)
    assertTrue(AppThemePreset.entries.any { it.isDark } && AppThemePreset.entries.any { !it.isDark })
    assertEquals(4, AppThemePreset.entries.count { it.category == com.example.ui.theme.ThemeCategory.FLORAL })
    assertEquals(4, AppThemePreset.entries.count { it.category == com.example.ui.theme.ThemeCategory.AESTHETIC })
    AppThemePreset.entries.forEach { preset ->
      assertEquals(preset, AppThemePreset.fromId(preset.id))
    }
    assertEquals(AppThemePreset.ENGINEERING_BLUEPRINT, AppThemePreset.fromId(null, fallbackDark = false))
    assertEquals(AppThemePreset.MIDNIGHT_LAB, AppThemePreset.fromId(null, fallbackDark = true))
  }

  @Test
  fun `theme studio opens from settings portion and switches to floral theme smoothly`() {
    dismissSplashIfVisible()
    composeRule.onNodeWithTag("nav_settings").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("settings_open_themes_btn").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("GateX Theme Studio").assertIsDisplayed()
    composeRule.onNodeWithTag("theme_cat_floral").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("theme_card_sakura_blossom").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Active: Sakura Blossom • 18 Themes").assertIsDisplayed()
  }

  @Test
  fun `workspace full screen mode hides chrome and wire connect plus unwire controls are accessible`() {
    dismissSplashIfVisible()
    composeRule.onNodeWithTag("nav_design").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("ws_fullscreen_btn").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    // Full screen active: exit button & minimal wire/unwire/undo controls shown, bottom nav hidden
    composeRule.onNodeWithTag("ws_exit_fullscreen_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("fs_wire_btn").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("fs_wire_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("fs_unwire_btn").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("fs_undo_wire_btn").assertIsDisplayed()
    // Exit full screen restores bottom nav and workspace toolbar
    composeRule.onNodeWithTag("ws_exit_fullscreen_btn").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("nav_design").assertIsDisplayed()
    composeRule.onNodeWithTag("tool_unwire").assertIsDisplayed()
    composeRule.onNodeWithTag("undo_wire_btn").assertIsDisplayed()
  }
}
