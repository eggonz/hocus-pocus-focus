package com.example.hocuspocusfocus

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
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
    private fun TestScope.newViewModel() =
        FocusViewModel(app, now = { START_TIME + testScheduler.currentTime }, playAlarm = { playedAlarms += it })

    private val playedAlarms = mutableListOf<Uri>()

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
        assertEquals(DEFAULT_DURATION_MILLIS, vm.remainingMillis.value)
        assertEquals(START_TIME + DEFAULT_DURATION_MILLIS, savedEndTime)

        advanceBy(1_000)
        assertEquals(DEFAULT_DURATION_MILLIS - 1_000, vm.remainingMillis.value)
        vm.stop()
    }

    @Test
    fun `session ends by itself when the time is up`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.start()
        // Updates land on whole seconds, so the last second shows until the very end
        advanceBy(DEFAULT_DURATION_MILLIS - 1)
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
        assertEquals(DEFAULT_DURATION_MILLIS - 5_000, second.remainingMillis.value)
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

    @Test
    fun `defaults to 30 minutes`() = runTest(dispatcher) {
        assertEquals(DEFAULT_FOCUS_DURATION_MINUTES, newViewModel().durationMinutes.value)
    }

    @Test
    fun `stepping the duration walks the list and stops at the ends`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.stepDuration(1)
        assertEquals(60, vm.durationMinutes.value)
        vm.stepDuration(-2)
        assertEquals(20, vm.durationMinutes.value)

        vm.stepDuration(-100)
        assertEquals(FOCUS_DURATIONS_MINUTES.first(), vm.durationMinutes.value)
        vm.stepDuration(100)
        assertEquals(FOCUS_DURATIONS_MINUTES.last(), vm.durationMinutes.value)
    }

    @Test
    fun `start uses the selected duration`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.stepDuration(-4)
        vm.start()
        runCurrent()
        assertEquals(5 * 60_000L, vm.remainingMillis.value)
        assertEquals(START_TIME + 5 * 60_000L, savedEndTime)
        vm.stop()
    }

    @Test
    fun `the selected duration is remembered`() = runTest(dispatcher) {
        newViewModel().stepDuration(2)
        assertEquals(90, newViewModel().durationMinutes.value)
    }

    @Test
    fun `an unknown saved duration falls back to the default`() = runTest(dispatcher) {
        prefs.edit { putInt("duration_minutes", 7) }
        assertEquals(DEFAULT_FOCUS_DURATION_MINUTES, newViewModel().durationMinutes.value)
    }

    @Test
    fun `the default alarm sound plays once when the time is up`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.start()
        advanceBy(DEFAULT_DURATION_MILLIS - 1)
        assertEquals(emptyList<Uri>(), playedAlarms)

        advanceBy(1)
        assertEquals(listOf(Settings.System.DEFAULT_ALARM_ALERT_URI), playedAlarms)
    }

    @Test
    fun `stopping early plays no alarm`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.start()
        advanceBy(5_000)
        vm.stop()
        advanceBy(DEFAULT_DURATION_MILLIS)
        assertEquals(emptyList<Uri>(), playedAlarms)
    }

    @Test
    fun `a silent alarm plays nothing`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.setAlarmSound(null)
        vm.start()
        advanceBy(DEFAULT_DURATION_MILLIS)
        assertNull(vm.remainingMillis.value)
        assertEquals(emptyList<Uri>(), playedAlarms)
    }

    @Test
    fun `the selected alarm sound is remembered and played`() = runTest(dispatcher) {
        val sound = Uri.parse("content://media/internal/audio/media/42")
        newViewModel().setAlarmSound(null)
        assertNull(newViewModel().alarmSound.value)

        newViewModel().setAlarmSound(sound)
        val vm = newViewModel()
        assertEquals(sound, vm.alarmSound.value)
        vm.start()
        advanceBy(DEFAULT_DURATION_MILLIS)
        assertEquals(listOf(sound), playedAlarms)
    }

    private companion object {
        const val START_TIME = 1_000_000_000_000L
        const val DEFAULT_DURATION_MILLIS = DEFAULT_FOCUS_DURATION_MINUTES * 60_000L
    }
}
