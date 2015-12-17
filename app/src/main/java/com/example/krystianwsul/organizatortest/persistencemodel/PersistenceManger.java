package com.example.krystianwsul.organizatortest.persistencemodel;

import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.DailySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.WeeklySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class PersistenceManger {
    private static PersistenceManger mInstance;

    private final HashMap<Integer, CustomTimeRecord> mCustomTimeRecords = new HashMap<>();

    private final HashMap<Integer, TaskRecord> mTaskRecords = new HashMap<>();

    private final HashMap<Integer, TaskHierarchyRecord> mTaskHierarchyRecords = new HashMap<>();

    private final HashMap<Integer, ScheduleRecord> mScheduleRecords = new HashMap<>();

    private final HashMap<Integer, SingleScheduleDateTimeRecord> mSingleScheduleDateTimeRecords = new HashMap<>();
    private final HashMap<Integer, DailyScheduleTimeRecord> mDailyScheduleTimeRecords = new HashMap<>();
    private final HashMap<Integer, WeeklyScheduleDayOfWeekTimeRecord> mWeeklyScheduleDayOfWeekTimeRecords = new HashMap<>();

    private final HashMap<Integer, InstanceRecord> mInstanceRecords = new HashMap<>();

    private final int mMaxDailyRepetitionId = 0;
    private final int mMaxWeeklyRepetitionId = 0;

    public static PersistenceManger getInstance() {
        if (mInstance == null)
            mInstance = new PersistenceManger();
        return mInstance;
    }

    private PersistenceManger() {
        Calendar calendarToday = Calendar.getInstance();

        Calendar calendarFewDaysAgo = Calendar.getInstance();
        calendarFewDaysAgo.add(Calendar.DATE, -10);

        Calendar calendarYesterday = Calendar.getInstance();
        calendarYesterday.add(Calendar.DATE, -1);

        Calendar calendarNextYear = Calendar.getInstance();
        calendarNextYear.add(Calendar.DATE, 365);

        CustomTimeRecord afterWaking = new CustomTimeRecord(0, "po wstaniu", 9, 0, 6, 0, 6, 0, 6, 0, 6, 0, 6, 0, 9, 0);
        mCustomTimeRecords.put(afterWaking.getId(), afterWaking);
        CustomTimeRecord afterWork = new CustomTimeRecord(1, "po pracy", 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0);
        mCustomTimeRecords.put(afterWork.getId(), afterWork);

        TaskRecord zakupy = new TaskRecord(0, "zakupy", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(zakupy.getId(), zakupy);

        TaskRecord halls = new TaskRecord(1, "halls", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(halls.getId(), halls);
        TaskHierarchyRecord zakupyHalls = new TaskHierarchyRecord(0, zakupy.getId(), halls.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskHierarchyRecords.put(zakupyHalls.getId(), zakupyHalls);

        TaskRecord biedronka = new TaskRecord(2, "biedronka", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(biedronka.getId(), biedronka);
        TaskHierarchyRecord zakupyBiedronka = new TaskHierarchyRecord(1, zakupy.getId(), biedronka.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskHierarchyRecords.put(zakupyBiedronka.getId(), zakupyBiedronka);


        TaskRecord czosnek = new TaskRecord(3, "czosnek", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(czosnek.getId(), czosnek);
        TaskHierarchyRecord biedronkaCzosnek = new TaskHierarchyRecord(2, biedronka.getId(), czosnek.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskHierarchyRecords.put(biedronkaCzosnek.getId(), biedronkaCzosnek);

        TaskRecord piersi = new TaskRecord(4, "piersi", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(piersi.getId(), piersi);
        TaskHierarchyRecord biedronkaPiersi = new TaskHierarchyRecord(3, biedronka.getId(), piersi.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskHierarchyRecords.put(biedronkaPiersi.getId(), biedronkaPiersi);

        ScheduleRecord today15 = new ScheduleRecord(0, zakupy.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(today15.getRootTaskId(), today15);
        SingleScheduleDateTimeRecord today150 = new SingleScheduleDateTimeRecord(today15.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 15, 0);
        mSingleScheduleDateTimeRecords.put(today150.getScheduleId(), today150);

        TaskRecord rachunek = new TaskRecord(5, "rachunek", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(rachunek.getId(), rachunek);

        ScheduleRecord yesterday16 = new ScheduleRecord(1, rachunek.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(yesterday16.getId(), yesterday16);
        SingleScheduleDateTimeRecord yesterday160 = new SingleScheduleDateTimeRecord(yesterday16.getId(), calendarYesterday.get(Calendar.YEAR), calendarYesterday.get(Calendar.MONTH) + 1, calendarYesterday.get(Calendar.DAY_OF_MONTH), null, 16, 0);
        mSingleScheduleDateTimeRecords.put(yesterday160.getScheduleId(), yesterday160);

        TaskRecord banany = new TaskRecord(6, "banany", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(banany.getId(), banany);

        ScheduleRecord today17 = new ScheduleRecord(2, banany.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(today17.getId(), today17);
        SingleScheduleDateTimeRecord today170 = new SingleScheduleDateTimeRecord(today17.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 17, 0);
        mSingleScheduleDateTimeRecords.put(today170.getScheduleId(), today170);

        TaskRecord iliotibial = new TaskRecord(7, "iliotibial band stretch", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(iliotibial.getId(), iliotibial);

        ScheduleRecord alwaysAfterWakingAfterWork = new ScheduleRecord(3, iliotibial.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 1);
        mScheduleRecords.put(alwaysAfterWakingAfterWork.getId(), alwaysAfterWakingAfterWork);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork0 = new DailyScheduleTimeRecord(1, alwaysAfterWakingAfterWork.getId(), afterWaking.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork0.getId(), alwaysAfterWakingAfterWork0);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork1 = new DailyScheduleTimeRecord(2, alwaysAfterWakingAfterWork.getId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork1.getId(), alwaysAfterWakingAfterWork1);

        TaskRecord hamstring = new TaskRecord(8, "hamstring stretch", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(hamstring.getId(), hamstring);

        ScheduleRecord alwaysAfterWork = new ScheduleRecord(4, hamstring.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 1);
        mScheduleRecords.put(alwaysAfterWork.getId(), alwaysAfterWork);
        DailyScheduleTimeRecord alwaysAfterWork0 = new DailyScheduleTimeRecord(0, alwaysAfterWork.getId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWork0.getId(), alwaysAfterWork0);

        TaskRecord piecyk = new TaskRecord(9, "piecyk", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(piecyk.getId(), piecyk);

        ScheduleRecord todayAfterWaking = new ScheduleRecord(5, piecyk.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(todayAfterWaking.getId(), todayAfterWaking);
        SingleScheduleDateTimeRecord todayAfterWaking0 = new SingleScheduleDateTimeRecord(todayAfterWaking.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), afterWaking.getId(), null, null);
        mSingleScheduleDateTimeRecords.put(todayAfterWaking0.getScheduleId(), todayAfterWaking0);

        TaskRecord paznokcie = new TaskRecord(10, "paznokcie", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(paznokcie.getId(), paznokcie);

        ScheduleRecord crazyWeekend = new ScheduleRecord(6, paznokcie.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 2);
        mScheduleRecords.put(crazyWeekend.getId(), crazyWeekend);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend0 = new WeeklyScheduleDayOfWeekTimeRecord(0, crazyWeekend.getId(), DayOfWeek.SATURDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend0.getId(), crazyWeekend0);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend1 = new WeeklyScheduleDayOfWeekTimeRecord(1, crazyWeekend.getId(), DayOfWeek.SUNDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend1.getId(), crazyWeekend1);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend2 = new WeeklyScheduleDayOfWeekTimeRecord(2, crazyWeekend.getId(), DayOfWeek.SUNDAY.ordinal(), null, 17, 0);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend2.getId(), crazyWeekend2);

        TaskRecord task6 = new TaskRecord(11, "task 6", calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(task6.getId(), task6);

        ScheduleRecord task6schedule = new ScheduleRecord(7, task6.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 1);
        mScheduleRecords.put(task6schedule.getId(), task6schedule);
        DailyScheduleTimeRecord task6schedule0 = new DailyScheduleTimeRecord(3, task6schedule.getId(), null, 6, 0);
        mDailyScheduleTimeRecords.put(task6schedule0.getId(), task6schedule0);
    }

    public Collection<CustomTimeRecord> getCustomTimeRecords() {
        return mCustomTimeRecords.values();
    }

    public Collection<TaskRecord> getTaskRecords() {
        return mTaskRecords.values();
    }

    public Collection<TaskHierarchyRecord> getTaskHierarchyRecords() {
        return mTaskHierarchyRecords.values();
    }

    public ArrayList<ScheduleRecord> getScheduleRecords(Task task) {
        Assert.assertTrue(task != null);

        ArrayList<ScheduleRecord> scheduleRecords = new ArrayList<>();
        for (ScheduleRecord scheduleRecord : mScheduleRecords.values())
            if (scheduleRecord.getRootTaskId() == task.getId())
                scheduleRecords.add(scheduleRecord);
        return scheduleRecords;
    }

    public SingleScheduleDateTimeRecord getSingleScheduleDateTimeRecord(SingleSchedule singleSchedule) {
        Assert.assertTrue(singleSchedule != null);
        return mSingleScheduleDateTimeRecords.get(singleSchedule.getId());
    }

    public ArrayList<DailyScheduleTimeRecord> getDailyScheduleTimeRecords(DailySchedule dailySchedule) {
        Assert.assertTrue(dailySchedule != null);

        ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = new ArrayList<>();
        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords.values())
            if (dailyScheduleTimeRecord.getScheduleId() == dailySchedule.getId())
                dailyScheduleTimeRecords.add(dailyScheduleTimeRecord);
        return dailyScheduleTimeRecords;
    }

    public int getMaxDailyRepetitionId() {
        return mMaxDailyRepetitionId;
    }

    public ArrayList<WeeklyScheduleDayOfWeekTimeRecord> getWeeklyScheduleDayOfWeekTimeRecords(WeeklySchedule weeklySchedule) {
        Assert.assertTrue(weeklySchedule != null);

        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords.values())
            if (weeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleId() == weeklySchedule.getId())
                weeklyScheduleDayOfWeekTimeRecords.add(weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecords;
    }

    public int getMaxWeeklyRepetitionId() {
        return mMaxWeeklyRepetitionId;
    }

    public TaskRecord createTaskRecord(String name, TimeStamp startTimeStamp) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startTimeStamp != null);

        int taskId = Collections.max(mTaskRecords.keySet()) + 1;

        TaskRecord taskRecord = new TaskRecord(taskId, name, startTimeStamp.getLong(), null);
        mTaskRecords.put(taskRecord.getId(), taskRecord);

        return taskRecord;
    }

    public TaskHierarchyRecord createTaskHierarchyRecord(Task parentTask, Task childTask, TimeStamp startTimeStamp) {
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(startTimeStamp));
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(startTimeStamp));

        int taskHierarchyId = Collections.max(mTaskHierarchyRecords.keySet()) + 1;

        TaskHierarchyRecord taskHierarchyRecord = new TaskHierarchyRecord(taskHierarchyId, parentTask.getId(), childTask.getId(), startTimeStamp.getLong(), null);
        mTaskHierarchyRecords.put(taskHierarchyRecord.getId(), taskHierarchyRecord);
        return taskHierarchyRecord;
    }

    public ScheduleRecord createScheduleRecord(Task rootTask, Schedule.ScheduleType scheduleType, TimeStamp startTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(scheduleType != null);
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(rootTask.current(startTimeStamp));

        int id = Collections.max(mScheduleRecords.keySet()) + 1;

        ScheduleRecord scheduleRecord = new ScheduleRecord(id, rootTask.getId(), startTimeStamp.getLong(), null, scheduleType.ordinal());
        mScheduleRecords.put(scheduleRecord.getId(), scheduleRecord);

        return scheduleRecord;
    }

    public SingleScheduleDateTimeRecord createSingleScheduleDateTimeRecord(SingleSchedule singleSchedule, Date date, Time time) {
        Assert.assertTrue(singleSchedule != null);
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = new SingleScheduleDateTimeRecord(singleSchedule.getId(), date.getYear(), date.getMonth(), date.getDay(), customTimeId, hour, minute);
        mSingleScheduleDateTimeRecords.put(singleScheduleDateTimeRecord.getScheduleId(), singleScheduleDateTimeRecord);

        return singleScheduleDateTimeRecord;
    }

    public DailyScheduleTimeRecord createDailyScheduleTimeRecord(DailySchedule dailySchedule, Time time) {
        Assert.assertTrue(dailySchedule != null);
        Assert.assertTrue(time != null);

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        int dailyScheduleTimeRecordId = Collections.max(mDailyScheduleTimeRecords.keySet()) + 1;

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        DailyScheduleTimeRecord dailyScheduleTimeRecord = new DailyScheduleTimeRecord(dailyScheduleTimeRecordId, dailySchedule.getRootTaskId(), customTimeId, hour, minute);
        mDailyScheduleTimeRecords.put(dailyScheduleTimeRecord.getId(), dailyScheduleTimeRecord);
        return dailyScheduleTimeRecord;
    }

    public WeeklyScheduleDayOfWeekTimeRecord createWeeklyScheduleDayOfWeekTimeRecord(WeeklySchedule weeklySchedule, DayOfWeek dayOfWeek, Time time) {
        Assert.assertTrue(weeklySchedule != null);
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue(time != null);

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        int weeklyScheduleDayOfWeekTimeRecordId = Collections.max(mWeeklyScheduleDayOfWeekTimeRecords.keySet()) + 1;

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = new WeeklyScheduleDayOfWeekTimeRecord(weeklyScheduleDayOfWeekTimeRecordId, weeklySchedule.getRootTaskId(), dayOfWeek.ordinal(), customTimeId, hour, minute);
        mWeeklyScheduleDayOfWeekTimeRecords.put(weeklyScheduleDayOfWeekTimeRecord.getId(), weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecord;
    }

    public Collection<InstanceRecord> getInstanceRecords() {
        return mInstanceRecords.values();
    }

    public InstanceRecord createInstanceRecord(Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp()));

        Date scheduleDate = scheduleDateTime.getDate();
        Time scheduleTime = scheduleDateTime.getTime();

        Integer scheduleCustomTimeId = null;
        Integer scheduleHour = null;
        Integer scheduleMinute = null;
        if (scheduleDateTime.getTime() instanceof CustomTime) {
            scheduleCustomTimeId = ((CustomTime) scheduleTime).getId();
        } else {
            Assert.assertTrue(scheduleTime instanceof NormalTime);

            HourMinute hourMinute = ((NormalTime) scheduleTime).getHourMinute();
            scheduleHour = hourMinute.getHour();
            scheduleMinute = hourMinute.getMinute();
        }

        int id = getNextInstanceId();

        InstanceRecord instanceRecord = new InstanceRecord(id, task.getId(), null, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleCustomTimeId, scheduleHour, scheduleMinute, null, null, null, null, null, null, TimeStamp.getNow().getLong());
        mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
        return instanceRecord;
    }

    private int getNextInstanceId() {
        if (mInstanceRecords.isEmpty())
            return 0;
        else
            return Collections.max(mInstanceRecords.keySet()) + 1;
    }
}