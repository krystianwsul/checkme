package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(TextUtils::class, android.util.Log::class, Context::class)
class ScheduleEntryTest {

    @Before
    fun setUp() {
        PowerMockito.mockStatic(TextUtils::class.java)
        PowerMockito.mockStatic(Log::class.java)
    }

    // root schedule hint

    @Test
    fun testSingle_2016_9_30() {
        val today = Date(2016, 9, 30) // last friday of the month
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = SingleScheduleEntry(scheduleHint)

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.FRIDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.FRIDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_2016_9_1() {
        val today = Date(2016, 9, 1) // first thursday of the month
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = SingleScheduleEntry(scheduleHint)

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.THURSDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_2016_9_7() {
        val today = Date(2016, 9, 7) // first wednesday of the month
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = SingleScheduleEntry(scheduleHint)

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.WEDNESDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 7)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_2016_9_8() {
        val today = Date(2016, 9, 8) // second thursday of the month
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = SingleScheduleEntry(scheduleHint)

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.THURSDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 8)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_2016_9_28() {
        val today = Date(2016, 9, 28) // fourth wednesday of the month
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = SingleScheduleEntry(scheduleHint)

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.WEDNESDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 28)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 4)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_2016_9_29() {
        val today = Date(2016, 9, 29) // last thursday of the month
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = SingleScheduleEntry(scheduleHint)

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.THURSDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    // group / join schedule hint

    @Test
    fun testSingle_2016_9_29_noon() {
        val hintDate = Date(2016, 9, 29) // last thursday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(hintDate, hourMinute)
        val scheduleEntry = SingleScheduleEntry(scheduleHint)

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == hintDate)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.THURSDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    // single schedule record

    @Test
    fun testSingle_record_2016_9_30_noon() {
        val scheduleDate = Date(2016, 9, 30) // last friday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleEntry = SingleScheduleEntry(CreateTaskViewModel.ScheduleData.Single(scheduleDate, TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null)

        Assert.assertTrue(scheduleDialogData.date == scheduleDate)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.FRIDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.FRIDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_record_2016_9_1_noon() {
        val scheduleDate = Date(2016, 9, 1) // first thursday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleEntry = SingleScheduleEntry(CreateTaskViewModel.ScheduleData.Single(scheduleDate, TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null)

        Assert.assertTrue(scheduleDialogData.date == scheduleDate)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.THURSDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_record_2016_9_7_noon() {
        val scheduleDate = Date(2016, 9, 7) // first wednesday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleEntry = SingleScheduleEntry(CreateTaskViewModel.ScheduleData.Single(scheduleDate, TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null)

        Assert.assertTrue(scheduleDialogData.date == scheduleDate)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.WEDNESDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 7)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_record_2016_9_8_noon() {
        val scheduleDate = Date(2016, 9, 8) // second thursday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleEntry = SingleScheduleEntry(CreateTaskViewModel.ScheduleData.Single(scheduleDate, TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null)

        Assert.assertTrue(scheduleDialogData.date == scheduleDate)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.THURSDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 8)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_record_2016_9_28_noon() {
        val scheduleDate = Date(2016, 9, 28) // fourth wednesday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleEntry = SingleScheduleEntry(CreateTaskViewModel.ScheduleData.Single(scheduleDate, TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null)

        Assert.assertTrue(scheduleDialogData.date == scheduleDate)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.WEDNESDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 28)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 4)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    @Test
    fun testSingle_record_2016_9_29_noon() {
        val scheduleDate = Date(2016, 9, 29) // last thursday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleEntry = SingleScheduleEntry(CreateTaskViewModel.ScheduleData.Single(scheduleDate, TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null)

        Assert.assertTrue(scheduleDialogData.date == scheduleDate)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(DayOfWeek.THURSDAY))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.SINGLE)
    }

    // daily schedule record

    @Test
    fun testDaily_record_2016_9_30_noon() {
        val today = Date(2016, 9, 30) // last friday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(HashSet(Arrays.asList(*DayOfWeek.values())), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == HashSet(Arrays.asList(*DayOfWeek.values())))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.FRIDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testDaily_record_2016_9_1_noon() {
        val today = Date(2016, 9, 1) // first thursday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(HashSet(Arrays.asList(*DayOfWeek.values())), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == HashSet(Arrays.asList(*DayOfWeek.values())))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testDaily_record_2016_9_7_noon() {
        val today = Date(2016, 9, 7) // first wednesday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(HashSet(Arrays.asList(*DayOfWeek.values())), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == HashSet(Arrays.asList(*DayOfWeek.values())))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 7)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testDaily_record_2016_9_8_noon() {
        val today = Date(2016, 9, 8) // second thursday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(HashSet(Arrays.asList(*DayOfWeek.values())), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == HashSet(Arrays.asList(*DayOfWeek.values())))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 8)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testDaily_record_2016_9_28_noon() {
        val today = Date(2016, 9, 28) // fourth wednesday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(HashSet(Arrays.asList(*DayOfWeek.values())), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == HashSet(Arrays.asList(*DayOfWeek.values())))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 28)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 4)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testDaily_record_2016_9_29_noon() {
        val today = Date(2016, 9, 29) // last thursday of the month
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(HashSet(Arrays.asList(*DayOfWeek.values())), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == HashSet(Arrays.asList(*DayOfWeek.values())))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    // weekly schedule record

    @Test
    fun testWeekly_record_2016_9_30_noon() {
        val today = Date(2016, 9, 30) // last friday of the month
        val dayOfWeek = DayOfWeek.FRIDAY
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(setOf(dayOfWeek), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(dayOfWeek))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.FRIDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testWeekly_record_2016_9_1_noon() {
        val today = Date(2016, 9, 1) // first thursday of the month
        val dayOfWeek = DayOfWeek.FRIDAY
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(setOf(dayOfWeek), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(dayOfWeek))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testWeekly_record_2016_9_7_noon() {
        val today = Date(2016, 9, 7) // first wednesday of the month
        val dayOfWeek = DayOfWeek.THURSDAY
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(setOf(dayOfWeek), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(dayOfWeek))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 7)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testWeekly_record_2016_9_8_noon() {
        val today = Date(2016, 9, 8) // second thursday of the month
        val dayOfWeek = DayOfWeek.SATURDAY
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(setOf(dayOfWeek), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(dayOfWeek))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 8)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testWeekly_record_2016_9_28_noon() {
        val today = Date(2016, 9, 28) // fourth wednesday of the month
        val dayOfWeek = DayOfWeek.SUNDAY
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(setOf(dayOfWeek), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(dayOfWeek))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 28)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 4)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.WEDNESDAY)
        Assert.assertTrue(scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }

    @Test
    fun testWeekly_record_2016_9_29_noon() {
        val today = Date(2016, 9, 29) // last thursday of the month
        val dayOfWeek = DayOfWeek.MONDAY
        val hourMinute = HourMinute(12, 0)
        val scheduleHint = CreateTaskActivity.ScheduleHint(today)
        val scheduleEntry = WeeklyScheduleEntry(CreateTaskViewModel.ScheduleData.Weekly(setOf(dayOfWeek), TimePair(hourMinute)))

        val scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint)

        Assert.assertTrue(scheduleDialogData.date == today)
        Assert.assertTrue(scheduleDialogData.daysOfWeek == setOf(dayOfWeek))
        Assert.assertTrue(scheduleDialogData.monthlyDay)
        Assert.assertTrue(scheduleDialogData.monthDayNumber == 2)
        Assert.assertTrue(scheduleDialogData.monthWeekNumber == 1)
        Assert.assertTrue(scheduleDialogData.monthWeekDay == DayOfWeek.THURSDAY)
        Assert.assertTrue(!scheduleDialogData.beginningOfMonth)
        Assert.assertTrue(scheduleDialogData.timePairPersist.hourMinute == hourMinute)
        Assert.assertTrue(scheduleDialogData.timePairPersist.customTimeKey == null)
        Assert.assertTrue(scheduleDialogData.scheduleType === ScheduleType.WEEKLY)
    }
}