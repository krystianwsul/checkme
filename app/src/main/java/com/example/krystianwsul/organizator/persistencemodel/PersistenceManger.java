package com.example.krystianwsul.organizator.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DailySchedule;
import com.example.krystianwsul.organizator.domainmodel.SingleSchedule;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.domainmodel.WeeklySchedule;
import com.example.krystianwsul.organizator.utils.ScheduleType;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

public class PersistenceManger {
    private static PersistenceManger sInstance;

    private final SQLiteDatabase mSQLiteDatabase;

    private final TreeMap<Integer, CustomTimeRecord> mCustomTimeRecords;

    private final TreeMap<Integer, TaskRecord> mTaskRecords;
    private final TreeMap<Integer, TaskHierarchyRecord> mTaskHierarchyRecords;

    private final TreeMap<Integer, ScheduleRecord> mScheduleRecords;
    private final TreeMap<Integer, SingleScheduleDateTimeRecord> mSingleScheduleDateTimeRecords;
    private final TreeMap<Integer, DailyScheduleTimeRecord> mDailyScheduleTimeRecords;
    private final TreeMap<Integer, WeeklyScheduleDayOfWeekTimeRecord> mWeeklyScheduleDayOfWeekTimeRecords;

    private final TreeMap<Integer, InstanceRecord> mInstanceRecords;

    private final Context mApplicationContext;

    public static synchronized PersistenceManger getInstance(Context context) {
        Assert.assertTrue(context != null);
        if (sInstance == null)
            sInstance = new PersistenceManger(context);
        return sInstance;
    }

    private PersistenceManger(Context context) {
        Assert.assertTrue(context != null);

        mApplicationContext = context.getApplicationContext();

        mSQLiteDatabase = MySQLiteHelper.getDatabase(mApplicationContext);

        mCustomTimeRecords = new TreeMap<>();
        for (CustomTimeRecord customTimeRecord : CustomTimeRecord.getCustomTimeRecords(mSQLiteDatabase))
            mCustomTimeRecords.put(customTimeRecord.getId(), customTimeRecord);

        mTaskRecords = new TreeMap<>();
        for (TaskRecord taskRecord : TaskRecord.getTaskRecords(mSQLiteDatabase))
            mTaskRecords.put(taskRecord.getId(), taskRecord);

        mTaskHierarchyRecords = new TreeMap<>();
        for (TaskHierarchyRecord taskHierarchyRecord : TaskHierarchyRecord.getTaskHierarchyRecords(mSQLiteDatabase))
            mTaskHierarchyRecords.put(taskHierarchyRecord.getId(), taskHierarchyRecord);

        mScheduleRecords = new TreeMap<>();
        for (ScheduleRecord scheduleRecord : ScheduleRecord.getScheduleRecords(mSQLiteDatabase))
            mScheduleRecords.put(scheduleRecord.getId(), scheduleRecord);

        mSingleScheduleDateTimeRecords = new TreeMap<>();
        for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : SingleScheduleDateTimeRecord.getSingleScheduleDateTimeRecords(mSQLiteDatabase))
            mSingleScheduleDateTimeRecords.put(singleScheduleDateTimeRecord.getScheduleId(), singleScheduleDateTimeRecord);

        mDailyScheduleTimeRecords = new TreeMap<>();
        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : DailyScheduleTimeRecord.getDailyScheduleTimeRecords(mSQLiteDatabase))
            mDailyScheduleTimeRecords.put(dailyScheduleTimeRecord.getId(), dailyScheduleTimeRecord);

        mWeeklyScheduleDayOfWeekTimeRecords = new TreeMap<>();
        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : WeeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleDayOfWeekTimeRecords(mSQLiteDatabase))
            mWeeklyScheduleDayOfWeekTimeRecords.put(weeklyScheduleDayOfWeekTimeRecord.getId(), weeklyScheduleDayOfWeekTimeRecord);

        mInstanceRecords = new TreeMap<>();
        for (InstanceRecord instanceRecord : InstanceRecord.getInstanceRecords(mSQLiteDatabase))
            mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
    }

    public synchronized void reset() {
        sInstance = null;
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

    private int getNextId(TreeMap<Integer, ? extends Record> hashMap) {
        if (hashMap.isEmpty())
            return 1;
        else
            return Collections.max(hashMap.keySet()) + 1;
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

        int id = getNextId(mCustomTimeRecords);

        CustomTimeRecord customTimeRecord = new CustomTimeRecord(false, id, name, sunday.getHour(), sunday.getMinute(), monday.getHour(), monday.getMinute(), tuesday.getHour(), tuesday.getMinute(), wednesday.getHour(), wednesday.getMinute(), thursday.getHour(), thursday.getMinute(), friday.getHour(), friday.getMinute(), saturday.getHour(), saturday.getMinute(), true);
        mCustomTimeRecords.put(customTimeRecord.getId(), customTimeRecord);
        return customTimeRecord;
    }

    public TaskRecord createTaskRecord(String name, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startExactTimeStamp != null);

        int id = getNextId(mTaskRecords);

        TaskRecord taskRecord = new TaskRecord(false, id, name, startExactTimeStamp.getLong(), null, true, null);
        mTaskRecords.put(taskRecord.getId(), taskRecord);

        return taskRecord;
    }

    public TaskHierarchyRecord createTaskHierarchyRecord(Task parentTask, Task childTask, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(startExactTimeStamp));
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(startExactTimeStamp));

        int id = getNextId(mTaskHierarchyRecords);

        TaskHierarchyRecord taskHierarchyRecord = new TaskHierarchyRecord(false, id, parentTask.getId(), childTask.getId(), startExactTimeStamp.getLong(), null);
        mTaskHierarchyRecords.put(taskHierarchyRecord.getId(), taskHierarchyRecord);
        return taskHierarchyRecord;
    }

    public ScheduleRecord createScheduleRecord(Task rootTask, ScheduleType scheduleType, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(scheduleType != null);
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(rootTask.current(startExactTimeStamp));

        int id = getNextId(mScheduleRecords);

        ScheduleRecord scheduleRecord = new ScheduleRecord(false, id, rootTask.getId(), startExactTimeStamp.getLong(), null, scheduleType.ordinal());
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

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = new SingleScheduleDateTimeRecord(false, singleSchedule.getId(), date.getYear(), date.getMonth(), date.getDay(), customTimeId, hour, minute);
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

        int id = getNextId(mDailyScheduleTimeRecords);

        DailyScheduleTimeRecord dailyScheduleTimeRecord = new DailyScheduleTimeRecord(false, id, dailySchedule.getId(), customTimeId, hour, minute);
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

        int id = getNextId(mWeeklyScheduleDayOfWeekTimeRecords);

        WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = new WeeklyScheduleDayOfWeekTimeRecord(false, id, weeklySchedule.getId(), dayOfWeek.ordinal(), customTimeId, hour, minute);
        mWeeklyScheduleDayOfWeekTimeRecords.put(weeklyScheduleDayOfWeekTimeRecord.getId(), weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecord;
    }

    public InstanceRecord createInstanceRecord(Task task, DateTime scheduleDateTime, ExactTimeStamp now) {
        Assert.assertTrue(task != null);

        ExactTimeStamp taskEndExactTimeStamp = task.getEndExactTimeStamp();
        Assert.assertTrue((taskEndExactTimeStamp == null || taskEndExactTimeStamp.compareTo(scheduleDateTime.getTimeStamp().toExactTimeStamp()) > 0));

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

        int id = getNextId(mInstanceRecords);

        InstanceRecord instanceRecord = new InstanceRecord(false, id, task.getId(), null, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleCustomTimeId, scheduleHour, scheduleMinute, null, null, null, null, null, null, now.getLong(), false, false, true);
        mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
        return instanceRecord;
    }

    public void save() {
        // insert

        ArrayList<InsertCommand> insertCommands = new ArrayList<>();

        for (CustomTimeRecord customTimeRecord : mCustomTimeRecords.values())
            if (customTimeRecord.needsInsert())
                insertCommands.add(customTimeRecord.getInsertCommand());

        for (TaskRecord taskRecord : mTaskRecords.values())
            if (taskRecord.needsInsert())
                insertCommands.add(taskRecord.getInsertCommand());

        for (TaskHierarchyRecord taskHierarchyRecord : mTaskHierarchyRecords.values())
            if (taskHierarchyRecord.needsInsert())
                insertCommands.add(taskHierarchyRecord.getInsertCommand());

        for (ScheduleRecord scheduleRecord : mScheduleRecords.values())
            if (scheduleRecord.needsInsert())
                insertCommands.add(scheduleRecord.getInsertCommand());

        for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : mSingleScheduleDateTimeRecords.values())
            if (singleScheduleDateTimeRecord.needsInsert())
                insertCommands.add(singleScheduleDateTimeRecord.getInsertCommand());

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords.values())
            if (dailyScheduleTimeRecord.needsInsert())
                insertCommands.add(dailyScheduleTimeRecord.getInsertCommand());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords.values())
            if (weeklyScheduleDayOfWeekTimeRecord.needsInsert())
                insertCommands.add(weeklyScheduleDayOfWeekTimeRecord.getInsertCommand());

        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.needsInsert())
                insertCommands.add(instanceRecord.getInsertCommand());

        // update

        ArrayList<UpdateCommand> updateCommands = new ArrayList<>();

        for (CustomTimeRecord customTimeRecord : mCustomTimeRecords.values())
            if (customTimeRecord.needsUpdate())
                updateCommands.add(customTimeRecord.getUpdateCommand());

        for (TaskRecord taskRecord : mTaskRecords.values())
            if (taskRecord.needsUpdate())
                updateCommands.add(taskRecord.getUpdateCommand());

        for (TaskHierarchyRecord taskHierarchyRecord : mTaskHierarchyRecords.values())
            if (taskHierarchyRecord.needsUpdate())
                updateCommands.add(taskHierarchyRecord.getUpdateCommand());

        for (ScheduleRecord scheduleRecord : mScheduleRecords.values())
            if (scheduleRecord.needsUpdate())
                updateCommands.add(scheduleRecord.getUpdateCommand());

        for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : mSingleScheduleDateTimeRecords.values())
            if (singleScheduleDateTimeRecord.needsUpdate())
                updateCommands.add(singleScheduleDateTimeRecord.getUpdateCommand());

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords.values())
            if (dailyScheduleTimeRecord.needsUpdate())
                updateCommands.add(dailyScheduleTimeRecord.getUpdateCommand());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords.values())
            if (weeklyScheduleDayOfWeekTimeRecord.needsUpdate())
                updateCommands.add(weeklyScheduleDayOfWeekTimeRecord.getUpdateCommand());

        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.needsUpdate())
                updateCommands.add(instanceRecord.getUpdateCommand());

        SaveService.startService(mApplicationContext, insertCommands, updateCommands);
    }

    SQLiteDatabase getSQLiteDatabase() {
        return mSQLiteDatabase;
    }
}