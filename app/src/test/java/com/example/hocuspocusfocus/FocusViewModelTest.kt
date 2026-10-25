package com.example.hocuspocusfocus

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric provides the Application and a fresh SharedPreferences for every test
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FocusViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val prefs = app.getSharedPreferences("focus", Context.MODE_PRIVATE)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // The clock follows the dispatcher's virtual time, so delay() and now() stay in step
    private fun TestScope.newViewModel() = FocusViewModel(app) { START_TIME + testScheduler.currentTime }

    private fun TestScope.advanceBy(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private val savedEndTime get() = prefs.getLong("end_time", 0L)

    @Test
    fun `starts idle`() = runTest(dispatcher) {
        assertNull(newViewModel().remainingMillis.value)
    }

    @Test
    fun `start counts down from the full duration and persists the end time`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.start()
        runCurrent()
        assertEquals(FOCUS_DURATION_MILLIS, vm.remainingMillis.value)
        assertEquals(START_TIME + FOCUS_DURATION_MILLIS, savedEndTime)

        advanceBy(1_000)
        assertEquals(FOCUS_DURATION_MILLIS - 1_000, vm.remainingMillis.value)
        vm.stop()
    }

    @Test
    fun `session ends by itself when the time is up`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.start()
        // Updates land on whole seconds, so the last second shows until the very end
        advanceBy(FOCUS_DURATION_MILLIS - 1)
        assertEquals(1_000L, vm.remainingMillis.value)

        advanceBy(1)
        assertNull(vm.remainingMillis.value)
        assertEquals(0L, savedEndTime)
    }

    @Test
    fun `stop ends the session and clears the end time`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.start()
        advanceBy(5_000)
        vm.stop()
        assertNull(vm.remainingMillis.value)
        assertEquals(0L, savedEndTime)

        advanceBy(5_000)
        assertNull(vm.remainingMillis.value)
    }

    @Test
    fun `a recreated view model resumes an ongoing session`() = runTest(dispatcher) {
        val first = newViewModel()
        first.start()
        advanceBy(5_000)

        val second = newViewModel()
        runCurrent()
        assertEquals(FOCUS_DURATION_MILLIS - 5_000, second.remainingMillis.value)
        first.stop()
        second.stop()
    }

    @Test
    fun `an expired saved session is discarded`() = runTest(dispatcher) {
        prefs.edit { putLong("end_time", START_TIME - 1) }
        val vm = newViewModel()
        assertNull(vm.remainingMillis.value)
        assertEquals(0L, savedEndTime)
    }

    private companion object {
        const val START_TIME = 1_000_000_000_000L
    }
}
