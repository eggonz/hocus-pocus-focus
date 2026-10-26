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
    fun `a duration saved in seconds by an older version is kept`() = runTest(dispatcher) {
        prefs.edit { putInt("duration_seconds", 90 * 60) }
        assertEquals(90, newViewModel().durationMinutes.value)
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
        assertEquals(listOf(Settings.System.DEFAULT_NOTIFICATION_URI), playedAlarms)
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

    @Test
    fun `always using the app guard is off by default and remembered`() = runTest(dispatcher) {
        assertEquals(false, newViewModel().alwaysUseAppGuard.value)
        newViewModel().setAlwaysUseAppGuard(true)
        assertEquals(true, newViewModel().alwaysUseAppGuard.value)
    }

    @Test
    fun `starts with the one hour Morning routine`() = runTest(dispatcher) {
        assertEquals(listOf(Routine(0, RoutineIcon.SUN, "Morning", 60)), newViewModel().routines.value)
    }

    @Test
    fun `an edited routine is remembered`() = runTest(dispatcher) {
        val edited = Routine(0, RoutineIcon.BOOK, "Reading", 30)
        newViewModel().updateRoutine(edited)
        assertEquals(listOf(edited), newViewModel().routines.value)
    }

    @Test
    fun `an added routine gets a new id and is remembered`() = runTest(dispatcher) {
        val added = newViewModel().addRoutine()
        assertEquals(Routine(1, RoutineIcon.SUN, "", 60), added)
        assertEquals(listOf(Routine(0, RoutineIcon.SUN, "Morning", 60), added), newViewModel().routines.value)
    }

    @Test
    fun `a deleted routine stays deleted`() = runTest(dispatcher) {
        val vm = newViewModel()
        val added = vm.addRoutine()
        vm.deleteRoutine(0)
        assertEquals(listOf(added), newViewModel().routines.value)
    }

    @Test
    fun `a routine's allowed apps are remembered in order`() = runTest(dispatcher) {
        val edited = Routine(0, RoutineIcon.SUN, "Morning", 60, listOf("com.example.b", "com.example.a"))
        newViewModel().updateRoutine(edited)
        assertEquals(listOf(edited), newViewModel().routines.value)
    }

    @Test
    fun `a routine saved before allowed apps existed has none`() = runTest(dispatcher) {
        prefs.edit {
            putString("routines", """[{"id":0,"icon":"MOON","title":"Night","duration_minutes":30}]""")
        }
        assertEquals(listOf(Routine(0, RoutineIcon.MOON, "Night", 30)), newViewModel().routines.value)
    }

    @Test
    fun `a routine counts down its own duration and is kept as the session routine`() = runTest(dispatcher) {
        val routine = Routine(0, RoutineIcon.SUN, "Morning", 5, listOf("com.example.a"))
        val vm = newViewModel()
        vm.startRoutine(routine)
        runCurrent()
        assertEquals(5 * 60_000L, vm.remainingMillis.value)
        assertEquals(routine, vm.sessionRoutine.value)

        advanceBy(5 * 60_000L)
        assertNull(vm.remainingMillis.value)
        assertNull(vm.sessionRoutine.value)
    }

    @Test
    fun `stopping a routine clears the session routine`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.startRoutine(vm.routines.value.first())
        advanceBy(1_000)
        vm.stop()
        assertNull(vm.sessionRoutine.value)
        assertNull(newViewModel().sessionRoutine.value)
    }

    @Test
    fun `a plain session has no routine`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.startRoutine(vm.routines.value.first())
        vm.stop()
        vm.start()
        runCurrent()
        assertNull(vm.sessionRoutine.value)
        assertNull(newViewModel().sessionRoutine.value)
        vm.stop()
    }

    @Test
    fun `a recreated view model resumes an ongoing routine`() = runTest(dispatcher) {
        val first = newViewModel()
        val routine = first.routines.value.first()
        first.startRoutine(routine)
        advanceBy(5_000)

        val second = newViewModel()
        runCurrent()
        assertEquals(routine, second.sessionRoutine.value)
        assertEquals(routine.durationMinutes * 60_000L - 5_000, second.remainingMillis.value)
        first.stop()
        second.stop()
    }

    private fun TestScope.savedAllowedApps() =
        FocusViewModel.savedSessionAllowedApps(app, START_TIME + testScheduler.currentTime)

    @Test
    fun `the app guard sees a routine session's allowed apps until it ends`() = runTest(dispatcher) {
        val routine = Routine(0, RoutineIcon.SUN, "Morning", 5, listOf("com.example.a"))
        val vm = newViewModel()
        vm.updateRoutine(routine)
        assertNull(savedAllowedApps())

        vm.startRoutine(routine)
        assertEquals(listOf("com.example.a"), savedAllowedApps())
        advanceBy(5 * 60_000L)
        assertNull(savedAllowedApps())
    }

    @Test
    fun `the app guard sees no allowed apps in a plain session`() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.updateRoutine(Routine(0, RoutineIcon.SUN, "Morning", 5, listOf("com.example.a")))
        vm.start()
        assertEquals(emptyList<String>(), savedAllowedApps())
        vm.stop()
        assertNull(savedAllowedApps())
    }

    private companion object {
        const val START_TIME = 1_000_000_000_000L
        const val DEFAULT_DURATION_MILLIS = DEFAULT_FOCUS_DURATION_MINUTES * 60_000L
    }
}
