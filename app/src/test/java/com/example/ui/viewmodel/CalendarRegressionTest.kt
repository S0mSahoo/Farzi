package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CalendarDayData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CalendarRegressionTest {

  private lateinit var viewModel: FinanceViewModel

  @Before
  fun setUp() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    viewModel = FinanceViewModel(app)
  }

  @Test
  fun testCalendarGridSizeIsAlways42Slots() {
    // Feb 2027 (28 days)
    val feb2027 = viewModel.computeCalendarDays(2027, Calendar.FEBRUARY, emptyList())
    assertEquals(42, feb2027.size)
    val feb2027ActiveDays = feb2027.filterNotNull()
    assertEquals(28, feb2027ActiveDays.size)

    // Feb 2028 Leap Year (29 days)
    val feb2028 = viewModel.computeCalendarDays(2028, Calendar.FEBRUARY, emptyList())
    assertEquals(42, feb2028.size)
    val feb2028ActiveDays = feb2028.filterNotNull()
    assertEquals(29, feb2028ActiveDays.size)

    // April 2027 (30 days)
    val apr2027 = viewModel.computeCalendarDays(2027, Calendar.APRIL, emptyList())
    assertEquals(42, apr2027.size)
    val apr2027ActiveDays = apr2027.filterNotNull()
    assertEquals(30, apr2027ActiveDays.size)

    // October 2026 (31 days)
    val oct2026 = viewModel.computeCalendarDays(2026, Calendar.OCTOBER, emptyList())
    assertEquals(42, oct2026.size)
    val oct2026ActiveDays = oct2026.filterNotNull()
    assertEquals(31, oct2026ActiveDays.size)
  }

  @Test
  fun testMondayFirstWeekdayAlignment() {
    // 1. Month starting on Monday: Feb 2027 (Feb 1, 2027 is Monday)
    val feb2027 = viewModel.computeCalendarDays(2027, Calendar.FEBRUARY, emptyList())
    assertNotNull("First slot must be Day 1 for month starting on Monday", feb2027[0])
    assertEquals(1, feb2027[0]?.dayOfMonth)

    // 2. Month starting on Sunday: Aug 2027 (Aug 1, 2027 is Sunday -> 6 leading blanks, slot 6 is Day 1)
    val aug2027 = viewModel.computeCalendarDays(2027, Calendar.AUGUST, emptyList())
    for (i in 0 until 6) {
      assertNull("Slot $i must be blank for month starting on Sunday", aug2027[i])
    }
    assertNotNull(aug2027[6])
    assertEquals(1, aug2027[6]?.dayOfMonth)

    // 3. Month starting on Saturday: May 2027 (May 1, 2027 is Saturday -> 5 leading blanks, slot 5 is Day 1)
    val may2027 = viewModel.computeCalendarDays(2027, Calendar.MAY, emptyList())
    for (i in 0 until 5) {
      assertNull("Slot $i must be blank for month starting on Saturday", may2027[i])
    }
    assertNotNull(may2027[5])
    assertEquals(1, may2027[5]?.dayOfMonth)

    // 4. Month starting on Thursday: Oct 2026 (Oct 1, 2026 is Thursday -> 3 leading blanks, slot 3 is Day 1)
    val oct2026 = viewModel.computeCalendarDays(2026, Calendar.OCTOBER, emptyList())
    for (i in 0 until 3) {
      assertNull("Slot $i must be blank for month starting on Thursday", oct2026[i])
    }
    assertNotNull(oct2026[3])
    assertEquals(1, oct2026[3]?.dayOfMonth)
  }

  @Test
  fun testNoAdjacentMonthDatesRendered() {
    val feb2027 = viewModel.computeCalendarDays(2027, Calendar.FEBRUARY, emptyList())
    // 28 valid days, 14 trailing blanks
    assertEquals(42, feb2027.size)
    for (i in 28 until 42) {
      assertNull("Trailing slot $i must be null placeholder, not next-month date", feb2027[i])
    }
    // Verify each active day strictly belongs to current month
    feb2027.filterNotNull().forEachIndexed { index, day ->
      assertEquals(index + 1, day.dayOfMonth)
      assertTrue(day.isCurrentMonth)
      assertTrue(day.dateKey.startsWith("2027-02"))
    }
  }

  @Test
  fun testSelectedDateClampingOnMonthTransition() {
    // Set selected date to Jan 31, 2027
    val jan31Cal = Calendar.getInstance().apply {
      set(2027, Calendar.JANUARY, 31, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    viewModel.selectCalendarDate(jan31Cal.timeInMillis)

    // Navigate to Feb 2027 (non-leap year, max 28 days)
    viewModel.setCalendarMonthAndYear(2027, Calendar.FEBRUARY)
    val clampedCal = Calendar.getInstance().apply {
      timeInMillis = viewModel.calendarSelectedDayMillis.value
    }
    assertEquals(2027, clampedCal.get(Calendar.YEAR))
    assertEquals(Calendar.FEBRUARY, clampedCal.get(Calendar.MONTH))
    assertEquals(28, clampedCal.get(Calendar.DAY_OF_MONTH))

    // Navigate to Feb 2028 (leap year, max 29 days)
    viewModel.selectCalendarDate(jan31Cal.timeInMillis)
    viewModel.setCalendarMonthAndYear(2028, Calendar.FEBRUARY)
    val clampedLeapCal = Calendar.getInstance().apply {
      timeInMillis = viewModel.calendarSelectedDayMillis.value
    }
    assertEquals(2028, clampedLeapCal.get(Calendar.YEAR))
    assertEquals(Calendar.FEBRUARY, clampedLeapCal.get(Calendar.MONTH))
    assertEquals(29, clampedLeapCal.get(Calendar.DAY_OF_MONTH))

    // Navigate from March 31 to April (30 days)
    val mar31Cal = Calendar.getInstance().apply {
      set(2027, Calendar.MARCH, 31, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    viewModel.selectCalendarDate(mar31Cal.timeInMillis)
    viewModel.setCalendarMonthAndYear(2027, Calendar.APRIL)
    val clampedAprCal = Calendar.getInstance().apply {
      timeInMillis = viewModel.calendarSelectedDayMillis.value
    }
    assertEquals(2027, clampedAprCal.get(Calendar.YEAR))
    assertEquals(Calendar.APRIL, clampedAprCal.get(Calendar.MONTH))
    assertEquals(30, clampedAprCal.get(Calendar.DAY_OF_MONTH))

    // Mid-month day should not be modified
    val mar15Cal = Calendar.getInstance().apply {
      set(2027, Calendar.MARCH, 15, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    viewModel.selectCalendarDate(mar15Cal.timeInMillis)
    viewModel.setCalendarMonthAndYear(2027, Calendar.APRIL)
    val clampedMar15Cal = Calendar.getInstance().apply {
      timeInMillis = viewModel.calendarSelectedDayMillis.value
    }
    assertEquals(15, clampedMar15Cal.get(Calendar.DAY_OF_MONTH))
  }

  @Test
  fun testNavigationAcrossYearBoundary() {
    // Start at Dec 2026
    viewModel.setCalendarMonthAndYear(2026, Calendar.DECEMBER)
    assertEquals(2026, viewModel.calendarMonth.value.get(Calendar.YEAR))
    assertEquals(Calendar.DECEMBER, viewModel.calendarMonth.value.get(Calendar.MONTH))

    // Next month -> Jan 2027
    viewModel.nextCalendarMonth()
    assertEquals(2027, viewModel.calendarMonth.value.get(Calendar.YEAR))
    assertEquals(Calendar.JANUARY, viewModel.calendarMonth.value.get(Calendar.MONTH))

    // Previous month -> Dec 2026
    viewModel.previousCalendarMonth()
    assertEquals(2026, viewModel.calendarMonth.value.get(Calendar.YEAR))
    assertEquals(Calendar.DECEMBER, viewModel.calendarMonth.value.get(Calendar.MONTH))
  }

  @Test
  fun testJanuary31ToFebruary2027() {
    val jan31Cal = Calendar.getInstance().apply {
      set(2027, Calendar.JANUARY, 31, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    viewModel.selectCalendarDate(jan31Cal.timeInMillis)
    viewModel.setCalendarMonthAndYear(2027, Calendar.FEBRUARY)

    assertEquals(Calendar.FEBRUARY, viewModel.selectedCalendar.value.get(Calendar.MONTH))
    assertEquals(2027, viewModel.selectedCalendar.value.get(Calendar.YEAR))
    
    val selectedCal = Calendar.getInstance().apply {
      timeInMillis = viewModel.calendarSelectedDayMillis.value
    }
    assertEquals(28, selectedCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(selectedCal.get(Calendar.DAY_OF_MONTH), viewModel.selectedCalendar.value.get(Calendar.DAY_OF_MONTH))
  }

  @Test
  fun testMarch31ToApril2027() {
    val mar31Cal = Calendar.getInstance().apply {
      set(2027, Calendar.MARCH, 31, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    viewModel.selectCalendarDate(mar31Cal.timeInMillis)
    viewModel.nextCalendarMonth()
    
    assertEquals(Calendar.APRIL, viewModel.selectedCalendar.value.get(Calendar.MONTH))
    assertEquals(30, viewModel.selectedCalendar.value.get(Calendar.DAY_OF_MONTH))
  }

  @Test
  fun testLeapYearFebruary() {
    val jan31Cal = Calendar.getInstance().apply {
      set(2028, Calendar.JANUARY, 31, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    viewModel.selectCalendarDate(jan31Cal.timeInMillis)
    viewModel.nextCalendarMonth()
    
    assertEquals(Calendar.FEBRUARY, viewModel.selectedCalendar.value.get(Calendar.MONTH))
    assertEquals(29, viewModel.selectedCalendar.value.get(Calendar.DAY_OF_MONTH))
  }

  @Test
  fun testReverseNavigationMarchToFebruary() {
    val mar31Cal = Calendar.getInstance().apply {
      set(2027, Calendar.MARCH, 31, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    viewModel.selectCalendarDate(mar31Cal.timeInMillis)
    viewModel.previousCalendarMonth()
    
    assertEquals(Calendar.FEBRUARY, viewModel.selectedCalendar.value.get(Calendar.MONTH))
    assertEquals(28, viewModel.selectedCalendar.value.get(Calendar.DAY_OF_MONTH))
  }
}
