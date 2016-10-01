package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, android.util.Log.class, Context.class})
public class ScheduleEntryTest {
    @Mock
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.mockStatic(Log.class);
    }

    @SuppressWarnings("EmptyMethod")
    @After
    public void tearDown() throws Exception {

    }

    // root schedule hint

    @Test
    public void testSingle_2016_9_30() {
        Date today = new Date(2016, 9, 30); // last friday of the month
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(scheduleHint);

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.FRIDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.FRIDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_2016_9_1() {
        Date today = new Date(2016, 9, 1); // first thursday of the month
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(scheduleHint);

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_2016_9_7() {
        Date today = new Date(2016, 9, 7); // first wednesday of the month
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(scheduleHint);

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 7);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_2016_9_8() {
        Date today = new Date(2016, 9, 8); // second thursday of the month
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(scheduleHint);

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 8);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_2016_9_28() {
        Date today = new Date(2016, 9, 28); // fourth wednesday of the month
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(scheduleHint);

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 28);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 4);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_2016_9_29() {
        Date today = new Date(2016, 9, 29); // last thursday of the month
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(scheduleHint);

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    // group / join schedule hint

    @Test
    public void testSingle_2016_9_29_noon() {
        Date hintDate = new Date(2016, 9, 29); // last thursday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(hintDate, hourMinute);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(scheduleHint);

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(hintDate));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    // single schedule record

    @Test
    public void testSingle_record_2016_9_30_noon() {
        Date scheduleDate = new Date(2016, 9, 30); // last friday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null);

        Assert.assertTrue(scheduleDialogData.mDate.equals(scheduleDate));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.FRIDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.FRIDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_record_2016_9_1_noon() {
        Date scheduleDate = new Date(2016, 9, 1); // first thursday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null);

        Assert.assertTrue(scheduleDialogData.mDate.equals(scheduleDate));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_record_2016_9_7_noon() {
        Date scheduleDate = new Date(2016, 9, 7); // first wednesday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null);

        Assert.assertTrue(scheduleDialogData.mDate.equals(scheduleDate));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 7);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_record_2016_9_8_noon() {
        Date scheduleDate = new Date(2016, 9, 8); // second thursday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null);

        Assert.assertTrue(scheduleDialogData.mDate.equals(scheduleDate));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 8);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_record_2016_9_28_noon() {
        Date scheduleDate = new Date(2016, 9, 28); // fourth wednesday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null);

        Assert.assertTrue(scheduleDialogData.mDate.equals(scheduleDate));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 28);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 4);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    @Test
    public void testSingle_record_2016_9_29_noon() {
        Date scheduleDate = new Date(2016, 9, 29); // last thursday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        ScheduleEntry scheduleEntry = new SingleScheduleEntry(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(Date.today(), null);

        Assert.assertTrue(scheduleDialogData.mDate.equals(scheduleDate));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);
    }

    // daily schedule record

    @Test
    public void testDaily_record_2016_9_30_noon() {
        Date today = new Date(2016, 9, 30); // last friday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new DailyScheduleEntry(new CreateTaskLoader.DailyScheduleData(new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.FRIDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.FRIDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);
    }

    @Test
    public void testDaily_record_2016_9_1_noon() {
        Date today = new Date(2016, 9, 1); // first thursday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new DailyScheduleEntry(new CreateTaskLoader.DailyScheduleData(new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);
    }

    @Test
    public void testDaily_record_2016_9_7_noon() {
        Date today = new Date(2016, 9, 7); // first wednesday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new DailyScheduleEntry(new CreateTaskLoader.DailyScheduleData(new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 7);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);
    }

    @Test
    public void testDaily_record_2016_9_8_noon() {
        Date today = new Date(2016, 9, 8); // second thursday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new DailyScheduleEntry(new CreateTaskLoader.DailyScheduleData(new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 8);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);
    }

    @Test
    public void testDaily_record_2016_9_28_noon() {
        Date today = new Date(2016, 9, 28); // fourth wednesday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new DailyScheduleEntry(new CreateTaskLoader.DailyScheduleData(new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 28);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 4);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);
    }

    @Test
    public void testDaily_record_2016_9_29_noon() {
        Date today = new Date(2016, 9, 29); // last thursday of the month
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new DailyScheduleEntry(new CreateTaskLoader.DailyScheduleData(new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);
    }

    // weekly schedule record

    @Test
    public void testWeekly_record_2016_9_30_noon() {
        Date today = new Date(2016, 9, 30); // last friday of the month
        DayOfWeek dayOfWeek = DayOfWeek.FRIDAY;
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new WeeklyScheduleEntry(new CreateTaskLoader.WeeklyScheduleData(dayOfWeek, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(dayOfWeek));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.FRIDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY);
    }

    @Test
    public void testWeekly_record_2016_9_1_noon() {
        Date today = new Date(2016, 9, 1); // first thursday of the month
        DayOfWeek dayOfWeek = DayOfWeek.FRIDAY;
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new WeeklyScheduleEntry(new CreateTaskLoader.WeeklyScheduleData(dayOfWeek, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(dayOfWeek));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY);
    }

    @Test
    public void testWeekly_record_2016_9_7_noon() {
        Date today = new Date(2016, 9, 7); // first wednesday of the month
        DayOfWeek dayOfWeek = DayOfWeek.THURSDAY;
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new WeeklyScheduleEntry(new CreateTaskLoader.WeeklyScheduleData(dayOfWeek, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(dayOfWeek));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 7);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY);
    }

    @Test
    public void testWeekly_record_2016_9_8_noon() {
        Date today = new Date(2016, 9, 8); // second thursday of the month
        DayOfWeek dayOfWeek = DayOfWeek.SATURDAY;
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new WeeklyScheduleEntry(new CreateTaskLoader.WeeklyScheduleData(dayOfWeek, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(dayOfWeek));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 8);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY);
    }

    @Test
    public void testWeekly_record_2016_9_28_noon() {
        Date today = new Date(2016, 9, 28); // fourth wednesday of the month
        DayOfWeek dayOfWeek = DayOfWeek.SUNDAY;
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new WeeklyScheduleEntry(new CreateTaskLoader.WeeklyScheduleData(dayOfWeek, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(dayOfWeek));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 28);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 4);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.WEDNESDAY));
        Assert.assertTrue(scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY);
    }

    @Test
    public void testWeekly_record_2016_9_29_noon() {
        Date today = new Date(2016, 9, 29); // last thursday of the month
        DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
        HourMinute hourMinute = new HourMinute(12, 0);
        CreateTaskActivity.ScheduleHint scheduleHint = new CreateTaskActivity.ScheduleHint(today);
        ScheduleEntry scheduleEntry = new WeeklyScheduleEntry(new CreateTaskLoader.WeeklyScheduleData(dayOfWeek, new TimePair(hourMinute)));

        ScheduleDialogFragment.ScheduleDialogData scheduleDialogData = scheduleEntry.getScheduleDialogData(today, scheduleHint);

        Assert.assertTrue(scheduleDialogData.mDate.equals(today));
        Assert.assertTrue(scheduleDialogData.mDayOfWeek.equals(dayOfWeek));
        Assert.assertTrue(scheduleDialogData.mMonthlyDay);
        Assert.assertTrue(scheduleDialogData.mMonthDayNumber == 2);
        Assert.assertTrue(scheduleDialogData.mMonthWeekNumber == 1);
        Assert.assertTrue(scheduleDialogData.mMonthWeekDay.equals(DayOfWeek.THURSDAY));
        Assert.assertTrue(!scheduleDialogData.mBeginningOfMonth);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getHourMinute().equals(hourMinute));
        Assert.assertTrue(scheduleDialogData.mTimePairPersist.getCustomTimeId() == null);
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY);
    }
}