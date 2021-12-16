package com.junwu.androidautomatinguitest

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tiny.mock.Endpoints
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val mockWebServerTestRule = MockWebServerTestRule()

    lateinit var scenario: ActivityScenario<MainActivity>
    lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    fun testLogin() {
        scenario = launchActivity()

        mockWebServerTestRule.enqueue(Endpoints.Post.V1Otp.ExampleSuccess)

        onView(withText(R.string.login_success))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.phoneNumberEditText))
            .check(matches(isDisplayed()))
            .perform(typeText("15648788888"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.continueButton))
            .check(matches(isDisplayed()))
            .perform(click())
        onView(withText(R.string.login_success))
            .check(matches(isDisplayed()))
    }
}