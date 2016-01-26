package com.example.krystianwsul.organizator.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DailySchedule;
import com.example.krystianwsul.organizator.domainmodel.Schedule;
import com.example.krystianwsul.organizator.domainmodel.SingleSchedule;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.domainmodel.WeeklySchedule;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class PersistenceManger {
    private final SQLiteDatabase mSQLiteDatabase;

    private final HashMap<Integer, CustomTimeRecord> mCustomTimeRecords;

    private final HashMap<Integer, TaskRecord> mTaskRecords;
    private final HashMap<Integer, TaskHierarchyRecord> mTaskHierarchyRecords;

    private final HashMap<Integer, ScheduleRecord> mScheduleRecords;
    private final HashMap<Integer, SingleScheduleDateTimeRecord> mSingleScheduleDateTimeRecords;
    private final HashMap<Integer, DailyScheduleTimeRecord> mDailyScheduleTimeRecords;
    private final HashMap<Integer, WeeklyScheduleDayOfWeekTimeRecord> mWeeklyScheduleDayOfWeekTimeRecords;

    private final HashMap<Integer, InstanceRecord> mInstanceRecords;

    public PersistenceManger(Context context) {
        Assert.assertTrue(context != null);

        mSQLiteDatabase = MySQLiteHelper.getDatabase(context);

        mCustomTimeRecords = new HashMap<>();
        for (CustomTimeRecord customTimeRecord : CustomTimeRecord.getCustomTimeRecords(mSQLiteDatabase))
            mCustomTimeRecords.put(customTimeRecord.getId(), customTimeRecord);

        mTaskRecords = new HashMap<>();
        for (TaskRecord taskRecord : TaskRecord.getTaskRecords(mSQLiteDatabase))
            mTaskRecords.put(taskRecord.getId(), taskRecord);

        mTaskHierarchyRecords = new HashMap<>();
        for (TaskHierarchyRecord taskHierarchyRecord : TaskHierarchyRecord.getTaskHierarchyRecords(mSQLiteDatabase))
            mTaskHierarchyRecords.put(taskHierarchyRecord.getId(), taskHierarchyRecord);

        mScheduleRecords = new HashMap<>();
        for (ScheduleRecord scheduleRecord : ScheduleRecord.getScheduleRecords(mSQLiteDatabase))
            mScheduleRecords.put(scheduleRecord.getId(), scheduleRecord);

        mSingleScheduleDateTimeRecords = new HashMap<>();
        for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : SingleScheduleDateTimeRecord.getSingleScheduleDateTimeRecords(mSQLiteDatabase))
            mSingleScheduleDateTimeRecords.put(singleScheduleDateTimeRecord.getScheduleId(), singleScheduleDateTimeRecord);

        mDailyScheduleTimeRecords = new HashMap<>();
        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : DailyScheduleTimeRecord.getDailyScheduleTimeRecords(mSQLiteDatabase))
            mDailyScheduleTimeRecords.put(dailyScheduleTimeRecord.getId(), dailyScheduleTimeRecord);

        mWeeklyScheduleDayOfWeekTimeRecords = new HashMap<>();
        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : WeeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleDayOfWeekTimeRecords(mSQLiteDatabase))
            mWeeklyScheduleDayOfWeekTimeRecords.put(weeklyScheduleDayOfWeekTimeRecord.getId(), weeklyScheduleDayOfWeekTimeRecord);

        mInstanceRecords = new HashMap<>();
        for (InstanceRecord instanceRecord : InstanceRecord.getInstanceRecords(mSQLiteDatabase))
            mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
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

    public ArrayList<WeeklyScheduleDayOfWeekTimeRecord> getWeeklyScheduleDayOfWeekTimeRecords(WeeklySchedule weeklySchedule) {
        Assert.assertTrue(weeklySchedule != null);

        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords.values())
            if (weeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleId() == weeklySchedule.getId())
                weeklyScheduleDayOfWeekTimeRecords.add(weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecords;
    }

    public Collection<InstanceRecord> getInstanceRecords() {
        return mInstanceRecords.values();
    }

    public TaskRecord createTaskRecord(String name, TimeStamp startTimeStamp) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startTimeStamp != null);

        TaskRecord taskRecord = TaskRecord.createTaskRecord(mSQLiteDatabase, name, startTimeStamp.getLong(), null);
        mTaskRecords.put(taskRecord.getId(), taskRecord);

        return taskRecord;
    }

    public TaskHierarchyRecord createTaskHierarchyRecord(Task parentTask, Task childTask, TimeStamp startTimeStamp) {
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(startTimeStamp));
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(startTimeStamp));

        TaskHierarchyRecord taskHierarchyRecord = TaskHierarchyRecord.createTaskHierarchyRecord(mSQLiteDatabase, parentTask.getId(), childTask.getId(), startTimeStamp.getLong(), null);
        mTaskHierarchyRecords.put(taskHierarchyRecord.getId(), taskHierarchyRecord);
        return taskHierarchyRecord;
    }

    public ScheduleRecord createScheduleRecord(Task rootTask, Schedule.ScheduleType scheduleType, TimeStamp startTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(scheduleType != null);
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(rootTask.current(startTimeStamp));

        ScheduleRecord scheduleRecord = ScheduleRecord.createScheduleRecord(mSQLiteDatabase, rootTask.getId(), startTimeStamp.getLong(), null, scheduleType.ordinal());
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

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = SingleScheduleDateTimeRecord.createSingleScheduleDateTimeRecord(mSQLiteDatabase, singleSchedule.getId(), date.getYear(), date.getMonth(), date.getDay(), customTimeId, hour, minute);
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

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        DailyScheduleTimeRecord dailyScheduleTimeRecord = DailyScheduleTimeRecord.createDailyScheduleTimeRecord(mSQLiteDatabase, dailySchedule.getId(), customTimeId, hour, minute);
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

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = WeeklyScheduleDayOfWeekTimeRecord.createWeeklyScheduleDayOfWeekTimeRecord(mSQLiteDatabase, weeklySchedule.getId(), dayOfWeek.ordinal(), customTimeId, hour, minute);
        mWeeklyScheduleDayOfWeekTimeRecords.put(weeklyScheduleDayOfWeekTimeRecord.getId(), weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecord;
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

        TimeStamp now = TimeStamp.getNow();
        TimeStamp scheduleTimeStamp = scheduleDateTime.getTimeStamp();
        TimeStamp hierarchy = (now.compareTo(scheduleTimeStamp) < 0 ? now : scheduleTimeStamp);

        InstanceRecord instanceRecord = InstanceRecord.createInstanceRecord(mSQLiteDatabase, task.getId(), null, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleCustomTimeId, scheduleHour, scheduleMinute, null, null, null, null, null, null, hierarchy.getLong(), false, false);
        mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
        return instanceRecord;
    }

    public CustomTimeRecord createCustomTimeRecord(String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(hourMinutes != null);

        Assert.assertTrue(hourMinutes.get(DayOfWeek.SUNDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.MONDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.TUESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.WEDNESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.THURSDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.FRIDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.SATURDAY) != null);

        HourMinute sunday = hourMinutes.get(DayOfWeek.SUNDAY);
        HourMinute monday = hourMinutes.get(DayOfWeek.MONDAY);
        HourMinute tuesday = hourMinutes.get(DayOfWeek.TUESDAY);
        HourMinute wednesday = hourMinutes.get(DayOfWeek.WEDNESDAY);
        HourMinute thursday = hourMinutes.get(DayOfWeek.THURSDAY);
        HourMinute friday = hourMinutes.get(DayOfWeek.FRIDAY);
        HourMinute saturday = hourMinutes.get(DayOfWeek.SATURDAY);

        CustomTimeRecord customTimeRecord = CustomTimeRecord.createCustomTimeRecord(mSQLiteDatabase, name, sunday.getHour(), sunday.getMinute(), monday.getHour(), monday.getMinute(), tuesday.getHour(), tuesday.getMinute(), wednesday.getHour(), wednesday.getMinute(), thursday.getHour(), thursday.getMinute(), friday.getHour(), friday.getMinute(), saturday.getHour(), saturday.getMinute());
        mCustomTimeRecords.put(customTimeRecord.getId(), customTimeRecord);
        return customTimeRecord;
    }

    public void save() {
        for (CustomTimeRecord customTimeRecord : mCustomTimeRecords.values())
            customTimeRecord.update(mSQLiteDatabase);

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords.values())
            dailyScheduleTimeRecord.update(mSQLiteDatabase);

        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            instanceRecord.update(mSQLiteDatabase);

        for (ScheduleRecord scheduleRecord : mScheduleRecords.values())
            scheduleRecord.update(mSQLiteDatabase);

        for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : mSingleScheduleDateTimeRecords.values())
            singleScheduleDateTimeRecord.update(mSQLiteDatabase);

        for (TaskHierarchyRecord taskHierarchyRecord : mTaskHierarchyRecords.values())
            taskHierarchyRecord.update(mSQLiteDatabase);

        for (TaskRecord taskRecord : mTaskRecords.values())
            taskRecord.update(mSQLiteDatabase);

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords.values())
            weeklyScheduleDayOfWeekTimeRecord.update(mSQLiteDatabase);
    }
}