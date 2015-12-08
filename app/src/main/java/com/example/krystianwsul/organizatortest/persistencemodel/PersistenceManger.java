package com.example.krystianwsul.organizatortest.persistencemodel;

import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.DailySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.WeeklySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.WeeklyScheduleDayOfWeekTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
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

    private final HashMap<Integer, ScheduleRecord> mScheduleRecords = new HashMap<>();

    private final HashMap<Integer, SingleScheduleDateTimeRecord> mSingleScheduleDateTimeRecords = new HashMap<>();
    private final HashMap<Integer, DailyScheduleTimeRecord> mDailyScheduleTimeRecords = new HashMap<>();
    private final HashMap<Integer, WeeklyScheduleDayOfWeekTimeRecord> mWeeklyScheduleDayOfWeekTimeRecords = new HashMap<>();

    private final HashMap<Integer, DailyRepetitionRecord> mDailyRepetitionRecords = new HashMap<>();
    private final HashMap<Integer, WeeklyRepetitionRecord> mWeeklyRepetitionRecords = new HashMap<>();

    private final HashMap<Integer, InstanceRecord> mInstanceRecords = new HashMap<>();

    private final int mMaxDailyRepetitionId = 0;
    private final int mMaxWeeklyRepetitionId = 0;

    private final int mMaxInstanceId = 0;

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

        TaskRecord zakupy = new TaskRecord(0, null, "zakupy", 0, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(zakupy.getId(), zakupy);
        TaskRecord halls = new TaskRecord(1, zakupy.getId(), "halls", 0, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(halls.getId(), halls);
        TaskRecord biedronka = new TaskRecord(2, zakupy.getId(), "biedronka", 1, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(biedronka.getId(), biedronka);
        TaskRecord czosnek = new TaskRecord(3, biedronka.getId(), "czosnek", 1, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(czosnek.getId(), czosnek);
        TaskRecord piersi = new TaskRecord(4, biedronka.getId(), "piersi", 2, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(piersi.getId(), piersi);

        ScheduleRecord today15 = new ScheduleRecord(0, zakupy.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(today15.getRootTaskId(), today15);
        SingleScheduleDateTimeRecord today150 = new SingleScheduleDateTimeRecord(today15.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 15, 0);
        mSingleScheduleDateTimeRecords.put(today150.getScheduleId(), today150);

        TaskRecord rachunek = new TaskRecord(5, null, "rachunek", 1, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(rachunek.getId(), rachunek);

        ScheduleRecord yesterday16 = new ScheduleRecord(1, rachunek.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(yesterday16.getId(), yesterday16);
        SingleScheduleDateTimeRecord yesterday160 = new SingleScheduleDateTimeRecord(yesterday16.getId(), calendarYesterday.get(Calendar.YEAR), calendarYesterday.get(Calendar.MONTH) + 1, calendarYesterday.get(Calendar.DAY_OF_MONTH), null, 16, 0);
        mSingleScheduleDateTimeRecords.put(yesterday160.getScheduleId(), yesterday160);

        TaskRecord banany = new TaskRecord(6, null, "banany", 2, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(banany.getId(), banany);

        ScheduleRecord today17 = new ScheduleRecord(2, banany.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(today17.getId(), today17);
        SingleScheduleDateTimeRecord today170 = new SingleScheduleDateTimeRecord(today17.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 17, 0);
        mSingleScheduleDateTimeRecords.put(today170.getScheduleId(), today170);

        TaskRecord iliotibial = new TaskRecord(7, null, "iliotibial band stretch", 3, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(iliotibial.getId(), iliotibial);

        ScheduleRecord alwaysAfterWakingAfterWork = new ScheduleRecord(3, iliotibial.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 1);
        mScheduleRecords.put(alwaysAfterWakingAfterWork.getId(), alwaysAfterWakingAfterWork);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork0 = new DailyScheduleTimeRecord(1, alwaysAfterWakingAfterWork.getId(), afterWaking.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork0.getId(), alwaysAfterWakingAfterWork0);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork1 = new DailyScheduleTimeRecord(2, alwaysAfterWakingAfterWork.getId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork1.getId(), alwaysAfterWakingAfterWork1);

        TaskRecord hamstring = new TaskRecord(8, null, "hamstring stretch", 4, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(hamstring.getId(), hamstring);

        ScheduleRecord alwaysAfterWork = new ScheduleRecord(4, hamstring.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 1);
        mScheduleRecords.put(alwaysAfterWork.getId(), alwaysAfterWork);
        DailyScheduleTimeRecord alwaysAfterWork0 = new DailyScheduleTimeRecord(0, alwaysAfterWork.getId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWork0.getId(), alwaysAfterWork0);

        TaskRecord piecyk = new TaskRecord(9, null, "piecyk", 5, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(piecyk.getId(), piecyk);

        ScheduleRecord todayAfterWaking = new ScheduleRecord(5, piecyk.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        mScheduleRecords.put(todayAfterWaking.getId(), todayAfterWaking);
        SingleScheduleDateTimeRecord todayAfterWaking0 = new SingleScheduleDateTimeRecord(todayAfterWaking.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), afterWaking.getId(), null, null);
        mSingleScheduleDateTimeRecords.put(todayAfterWaking0.getScheduleId(), todayAfterWaking0);

        TaskRecord paznokcie = new TaskRecord(10, null, "paznokcie", 6, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(paznokcie.getId(), paznokcie);

        ScheduleRecord crazyWeekend = new ScheduleRecord(6, paznokcie.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 2);
        mScheduleRecords.put(crazyWeekend.getId(), crazyWeekend);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend0 = new WeeklyScheduleDayOfWeekTimeRecord(0, crazyWeekend.getId(), DayOfWeek.SATURDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend0.getId(), crazyWeekend0);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend1 = new WeeklyScheduleDayOfWeekTimeRecord(1, crazyWeekend.getId(), DayOfWeek.SUNDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend1.getId(), crazyWeekend1);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend2 = new WeeklyScheduleDayOfWeekTimeRecord(2, crazyWeekend.getId(), DayOfWeek.SUNDAY.ordinal(), null, 17, 0);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend2.getId(), crazyWeekend2);

        TaskRecord task6 = new TaskRecord(11, null, "task 6", 7, calendarFewDaysAgo.getTimeInMillis(), null);
        mTaskRecords.put(task6.getId(), task6);

        ScheduleRecord task6schedule = new ScheduleRecord(7, task6.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 1);
        mScheduleRecords.put(task6schedule.getId(), task6schedule);
        DailyScheduleTimeRecord task6schedule0 = new DailyScheduleTimeRecord(3, task6schedule.getId(), null, 6, 0);
        mDailyScheduleTimeRecords.put(task6schedule0.getId(), task6schedule0);
    }

    public Collection<CustomTimeRecord> getCustomTimeRecords() {
        return mCustomTimeRecords.values();
    }

    public ArrayList<TaskRecord> getTaskRecords(Task parentTask) {
        ArrayList<TaskRecord> taskRecords = new ArrayList<>();
        for (TaskRecord taskRecord : mTaskRecords.values()) {
            Integer thisParentTaskId = taskRecord.getParentTaskId();
            if ((thisParentTaskId == null && parentTask == null) || (thisParentTaskId != null && parentTask != null && thisParentTaskId.equals(parentTask.getId())))
                taskRecords.add(taskRecord);
        }
        return taskRecords;
    }

    public ArrayList<ScheduleRecord> getScheduleRecords(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        ArrayList<ScheduleRecord> scheduleRecords = new ArrayList<>();
        for (ScheduleRecord scheduleRecord : mScheduleRecords.values())
            if (scheduleRecord.getRootTaskId() == rootTask.getId())
                scheduleRecords.add(scheduleRecord);
        return scheduleRecords;
    }

    public SingleScheduleDateTimeRecord getSingleScheduleDateTimeRecord(SingleSchedule singleSchedule) {
        Assert.assertTrue(singleSchedule != null);
        return mSingleScheduleDateTimeRecords.get(singleSchedule.getId());
    }

    public InstanceRecord getSingleInstanceRecord(Task task) {
        Assert.assertTrue(task != null);

        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.getRootTaskId() != null && instanceRecord.getRootTaskId().equals(task.getId()))
                return instanceRecord;
        return null;
    }

    public InstanceRecord createSingleInstanceRecord(int id, Task task, RootTask rootTask, Long done) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(rootTask != null);

        Assert.assertTrue(!mInstanceRecords.containsKey(task.getId()));

        InstanceRecord instanceRecord = new InstanceRecord(id, task.getId(), done, rootTask.getId(), null, null);
        mInstanceRecords.put(instanceRecord.getTaskId(), instanceRecord);
        return instanceRecord;
    }

    public ArrayList<DailyScheduleTimeRecord> getDailyScheduleTimeRecords(DailySchedule dailySchedule) {
        Assert.assertTrue(dailySchedule != null);

        ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = new ArrayList<>();
        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords.values())
            if (dailyScheduleTimeRecord.getScheduleId() == dailySchedule.getId())
                dailyScheduleTimeRecords.add(dailyScheduleTimeRecord);
        return dailyScheduleTimeRecords;
    }

    public ArrayList<DailyRepetitionRecord> getDailyRepetitionRecords(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);

        ArrayList<DailyRepetitionRecord> dailyRepetitionRecords = new ArrayList<>();
        for (DailyRepetitionRecord dailyRepetitionRecord : mDailyRepetitionRecords.values()) {
            if (dailyRepetitionRecord.getDailyScheduleTimeId() == dailyScheduleTime.getId()) {
                dailyRepetitionRecords.add(dailyRepetitionRecord);
            }
        }
        return dailyRepetitionRecords;
    }

    public int getMaxDailyRepetitionId() {
        return mMaxDailyRepetitionId;
    }

    public InstanceRecord getDailyInstanceRecord(Task task, DailyRepetition dailyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);

        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.getTaskId() == task.getId() && instanceRecord.getDailyRepetitionId() != null && instanceRecord.getDailyRepetitionId().equals(dailyRepetition.getId()))
                return instanceRecord;
        return null;
    }

    public InstanceRecord createDailyInstanceRecord(int id, Task task, DailyRepetition dailyRepetition, Long done) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);

        Assert.assertTrue(!mInstanceRecords.containsKey(id));

        InstanceRecord instanceRecord = new InstanceRecord(id, task.getId(), done, null, dailyRepetition.getId(), null);
        mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
        return instanceRecord;
    }

    public int getMaxInstanceId() {
        return mMaxInstanceId;
    }

    public ArrayList<WeeklyScheduleDayOfWeekTimeRecord> getWeeklyScheduleDayOfWeekTimeRecords(WeeklySchedule weeklySchedule) {
        Assert.assertTrue(weeklySchedule != null);

        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords.values())
            if (weeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleId() == weeklySchedule.getId())
                weeklyScheduleDayOfWeekTimeRecords.add(weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecords;
    }

    public ArrayList<WeeklyRepetitionRecord> getWeeklyRepetitionRecords(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);

        ArrayList<WeeklyRepetitionRecord> weeklyRepetitionRecords = new ArrayList<>();
        for (WeeklyRepetitionRecord weeklyRepetitionRecord : mWeeklyRepetitionRecords.values()) {
            if (weeklyRepetitionRecord.getWeeklyScheduleTimeId() == weeklyScheduleDayOfWeekTime.getId()) {
                weeklyRepetitionRecords.add(weeklyRepetitionRecord);
            }
        }
        return weeklyRepetitionRecords;
    }

    public int getMaxWeeklyRepetitionId() {
        return mMaxWeeklyRepetitionId;
    }

    public InstanceRecord getWeeklyInstanceRecord(Task task, WeeklyRepetition weeklyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);

        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.getTaskId() == task.getId() && instanceRecord.getWeeklyRepetitionId() != null && instanceRecord.getWeeklyRepetitionId().equals(weeklyRepetition.getId()))
                return instanceRecord;
        return null;
    }

    public InstanceRecord createWeeklyInstanceRecord(int id, Task task, WeeklyRepetition weeklyRepetition, Long done) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);

        Assert.assertTrue(!mInstanceRecords.containsKey(id));

        InstanceRecord instanceRecord = new InstanceRecord(id, task.getId(), done, null, null, weeklyRepetition.getId());
        mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
        return instanceRecord;
    }

    public TaskRecord createTaskRecord(Task parentTask, String name, int ordinal) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        int taskId = Collections.max(mTaskRecords.keySet()) + 1;

        Integer parentId = (parentTask != null ? parentTask.getId() : null);

        TaskRecord taskRecord = new TaskRecord(taskId, parentId, name, ordinal, TimeStamp.getNow().getLong(), null);
        mTaskRecords.put(taskRecord.getId(), taskRecord);

        return taskRecord;
    }

    public ScheduleRecord createScheduleRecord(RootTask rootTask, Schedule.ScheduleType scheduleType) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(scheduleType != null);

        int id = Collections.max(mScheduleRecords.keySet()) + 1;

        ScheduleRecord scheduleRecord = new ScheduleRecord(id, rootTask.getId(), TimeStamp.getNow().getLong(), null, scheduleType.ordinal());
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
}