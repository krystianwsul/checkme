package com.krystianwsul.checkme.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.DailySchedule;
import com.krystianwsul.checkme.domainmodel.SingleSchedule;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.domainmodel.WeeklySchedule;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class PersistenceManger {
    private static PersistenceManger sInstance;

    private final SQLiteDatabase mSQLiteDatabase;

    private final ArrayList<CustomTimeRecord> mCustomTimeRecords;

    private final ArrayList<TaskRecord> mTaskRecords;
    private final ArrayList<TaskHierarchyRecord> mTaskHierarchyRecords;

    private final ArrayList<ScheduleRecord> mScheduleRecords;
    private final HashMap<Integer, SingleScheduleDateTimeRecord> mSingleScheduleDateTimeRecords;
    private final ArrayList<DailyScheduleTimeRecord> mDailyScheduleTimeRecords;
    private final ArrayList<WeeklyScheduleDayOfWeekTimeRecord> mWeeklyScheduleDayOfWeekTimeRecords;

    private final ArrayList<InstanceRecord> mInstanceRecords;

    private final Context mApplicationContext;

    private int mCustomTimeMaxId;
    private int mTaskMaxId;
    private int mTaskHierarchyMaxId;
    private int mScheduleMaxId;
    private int mDailyScheduleTimeMaxId;
    private int mWeeklyScheduleDayOfWeekTimeMaxId;
    private int mInstanceMaxId;

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

        mCustomTimeRecords = new ArrayList<>();
        for (CustomTimeRecord customTimeRecord : CustomTimeRecord.getCustomTimeRecords(mSQLiteDatabase))
            mCustomTimeRecords.add(customTimeRecord);

        mCustomTimeMaxId = CustomTimeRecord.getMaxId(mSQLiteDatabase);

        mTaskRecords = new ArrayList<>();
        for (TaskRecord taskRecord : TaskRecord.getTaskRecords(mSQLiteDatabase))
            mTaskRecords.add(taskRecord);

        mTaskMaxId = TaskRecord.getMaxId(mSQLiteDatabase);

        List<Integer> taskIds = Stream.of(mTaskRecords)
                .map(TaskRecord::getId)
                .collect(Collectors.toList());

        mTaskHierarchyRecords = new ArrayList<>();
        if (!mTaskRecords.isEmpty())
            for (TaskHierarchyRecord taskHierarchyRecord : TaskHierarchyRecord.getTaskHierarchyRecords(mSQLiteDatabase, taskIds))
                mTaskHierarchyRecords.add(taskHierarchyRecord);

        mTaskHierarchyMaxId = TaskHierarchyRecord.getMaxId(mSQLiteDatabase);

        mScheduleRecords = new ArrayList<>();
        if (!mTaskRecords.isEmpty())
            for (ScheduleRecord scheduleRecord : ScheduleRecord.getScheduleRecords(mSQLiteDatabase, taskIds))
                mScheduleRecords.add(scheduleRecord);

        mScheduleMaxId = ScheduleRecord.getMaxId(mSQLiteDatabase);

        List<Integer> scheduleIds = Stream.of(mScheduleRecords)
                .map(ScheduleRecord::getId)
                .collect(Collectors.toList());

        mSingleScheduleDateTimeRecords = new HashMap<>();
        if (!scheduleIds.isEmpty())
            for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : SingleScheduleDateTimeRecord.getSingleScheduleDateTimeRecords(mSQLiteDatabase, scheduleIds))
                mSingleScheduleDateTimeRecords.put(singleScheduleDateTimeRecord.getScheduleId(), singleScheduleDateTimeRecord);

        mDailyScheduleTimeRecords = new ArrayList<>();
        if (!scheduleIds.isEmpty())
            for (DailyScheduleTimeRecord dailyScheduleTimeRecord : DailyScheduleTimeRecord.getDailyScheduleTimeRecords(mSQLiteDatabase, scheduleIds))
                mDailyScheduleTimeRecords.add(dailyScheduleTimeRecord);

        mDailyScheduleTimeMaxId = DailyScheduleTimeRecord.getMaxId(mSQLiteDatabase);

        mWeeklyScheduleDayOfWeekTimeRecords = new ArrayList<>();
        if (!scheduleIds.isEmpty())
            for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : WeeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleDayOfWeekTimeRecords(mSQLiteDatabase, scheduleIds))
                mWeeklyScheduleDayOfWeekTimeRecords.add(weeklyScheduleDayOfWeekTimeRecord);

        mWeeklyScheduleDayOfWeekTimeMaxId = WeeklyScheduleDayOfWeekTimeRecord.getMaxId(mSQLiteDatabase);

        mInstanceRecords = new ArrayList<>();
        for (InstanceRecord instanceRecord : InstanceRecord.getInstanceRecords(mSQLiteDatabase))
            mInstanceRecords.add(instanceRecord);

        mInstanceMaxId = InstanceRecord.getMaxId(mSQLiteDatabase);
    }

    public synchronized void reset() {
        sInstance = null;
    }

    public Collection<CustomTimeRecord> getCustomTimeRecords() {
        return mCustomTimeRecords;
    }

    public Collection<TaskRecord> getTaskRecords() {
        return mTaskRecords;
    }

    public Collection<TaskHierarchyRecord> getTaskHierarchyRecords() {
        return mTaskHierarchyRecords;
    }

    public ArrayList<ScheduleRecord> getScheduleRecords(Task task) {
        Assert.assertTrue(task != null);

        ArrayList<ScheduleRecord> scheduleRecords = new ArrayList<>();
        for (ScheduleRecord scheduleRecord : mScheduleRecords)
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
        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords)
            if (dailyScheduleTimeRecord.getScheduleId() == dailySchedule.getId())
                dailyScheduleTimeRecords.add(dailyScheduleTimeRecord);
        return dailyScheduleTimeRecords;
    }

    public ArrayList<WeeklyScheduleDayOfWeekTimeRecord> getWeeklyScheduleDayOfWeekTimeRecords(WeeklySchedule weeklySchedule) {
        Assert.assertTrue(weeklySchedule != null);

        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords)
            if (weeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleId() == weeklySchedule.getId())
                weeklyScheduleDayOfWeekTimeRecords.add(weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecords;
    }

    public Collection<InstanceRecord> getInstanceRecords() {
        return mInstanceRecords;
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

        int id = ++mCustomTimeMaxId;

        CustomTimeRecord customTimeRecord = new CustomTimeRecord(false, id, name, sunday.getHour(), sunday.getMinute(), monday.getHour(), monday.getMinute(), tuesday.getHour(), tuesday.getMinute(), wednesday.getHour(), wednesday.getMinute(), thursday.getHour(), thursday.getMinute(), friday.getHour(), friday.getMinute(), saturday.getHour(), saturday.getMinute(), true);
        mCustomTimeRecords.add(customTimeRecord);
        return customTimeRecord;
    }

    public TaskRecord createTaskRecord(String name, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startExactTimeStamp != null);

        int id = ++mTaskMaxId;

        TaskRecord taskRecord = new TaskRecord(false, id, name, startExactTimeStamp.getLong(), null, true, null);
        mTaskRecords.add(taskRecord);

        return taskRecord;
    }

    public TaskHierarchyRecord createTaskHierarchyRecord(Task parentTask, Task childTask, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(startExactTimeStamp));
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(startExactTimeStamp));

        int id = ++mTaskHierarchyMaxId;

        TaskHierarchyRecord taskHierarchyRecord = new TaskHierarchyRecord(false, id, parentTask.getId(), childTask.getId(), startExactTimeStamp.getLong(), null);
        mTaskHierarchyRecords.add(taskHierarchyRecord);
        return taskHierarchyRecord;
    }

    public ScheduleRecord createScheduleRecord(Task rootTask, ScheduleType scheduleType, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(scheduleType != null);
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(rootTask.current(startExactTimeStamp));

        int id = ++mScheduleMaxId;

        ScheduleRecord scheduleRecord = new ScheduleRecord(false, id, rootTask.getId(), startExactTimeStamp.getLong(), null, scheduleType.ordinal());
        mScheduleRecords.add(scheduleRecord);

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

        int id = ++mDailyScheduleTimeMaxId;

        DailyScheduleTimeRecord dailyScheduleTimeRecord = new DailyScheduleTimeRecord(false, id, dailySchedule.getId(), customTimeId, hour, minute);
        mDailyScheduleTimeRecords.add(dailyScheduleTimeRecord);
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

        int id = ++mWeeklyScheduleDayOfWeekTimeMaxId;

        WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = new WeeklyScheduleDayOfWeekTimeRecord(false, id, weeklySchedule.getId(), dayOfWeek.ordinal(), customTimeId, hour, minute);
        mWeeklyScheduleDayOfWeekTimeRecords.add(weeklyScheduleDayOfWeekTimeRecord);
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

        int id = ++mInstanceMaxId;

        InstanceRecord instanceRecord = new InstanceRecord(false, id, task.getId(), null, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleCustomTimeId, scheduleHour, scheduleMinute, null, null, null, null, null, null, now.getLong(), false, false, true);
        mInstanceRecords.add(instanceRecord);
        return instanceRecord;
    }

    public void save() {
        // insert

        ArrayList<InsertCommand> insertCommands = new ArrayList<>();

        for (CustomTimeRecord customTimeRecord : mCustomTimeRecords)
            if (customTimeRecord.needsInsert())
                insertCommands.add(customTimeRecord.getInsertCommand());

        for (TaskRecord taskRecord : mTaskRecords)
            if (taskRecord.needsInsert())
                insertCommands.add(taskRecord.getInsertCommand());

        for (TaskHierarchyRecord taskHierarchyRecord : mTaskHierarchyRecords)
            if (taskHierarchyRecord.needsInsert())
                insertCommands.add(taskHierarchyRecord.getInsertCommand());

        for (ScheduleRecord scheduleRecord : mScheduleRecords)
            if (scheduleRecord.needsInsert())
                insertCommands.add(scheduleRecord.getInsertCommand());

        for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : mSingleScheduleDateTimeRecords.values())
            if (singleScheduleDateTimeRecord.needsInsert())
                insertCommands.add(singleScheduleDateTimeRecord.getInsertCommand());

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords)
            if (dailyScheduleTimeRecord.needsInsert())
                insertCommands.add(dailyScheduleTimeRecord.getInsertCommand());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords)
            if (weeklyScheduleDayOfWeekTimeRecord.needsInsert())
                insertCommands.add(weeklyScheduleDayOfWeekTimeRecord.getInsertCommand());

        for (InstanceRecord instanceRecord : mInstanceRecords)
            if (instanceRecord.needsInsert())
                insertCommands.add(instanceRecord.getInsertCommand());

        // update

        ArrayList<UpdateCommand> updateCommands = new ArrayList<>();

        for (CustomTimeRecord customTimeRecord : mCustomTimeRecords)
            if (customTimeRecord.needsUpdate())
                updateCommands.add(customTimeRecord.getUpdateCommand());

        for (TaskRecord taskRecord : mTaskRecords)
            if (taskRecord.needsUpdate())
                updateCommands.add(taskRecord.getUpdateCommand());

        for (TaskHierarchyRecord taskHierarchyRecord : mTaskHierarchyRecords)
            if (taskHierarchyRecord.needsUpdate())
                updateCommands.add(taskHierarchyRecord.getUpdateCommand());

        for (ScheduleRecord scheduleRecord : mScheduleRecords)
            if (scheduleRecord.needsUpdate())
                updateCommands.add(scheduleRecord.getUpdateCommand());

        for (SingleScheduleDateTimeRecord singleScheduleDateTimeRecord : mSingleScheduleDateTimeRecords.values())
            if (singleScheduleDateTimeRecord.needsUpdate())
                updateCommands.add(singleScheduleDateTimeRecord.getUpdateCommand());

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords)
            if (dailyScheduleTimeRecord.needsUpdate())
                updateCommands.add(dailyScheduleTimeRecord.getUpdateCommand());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords)
            if (weeklyScheduleDayOfWeekTimeRecord.needsUpdate())
                updateCommands.add(weeklyScheduleDayOfWeekTimeRecord.getUpdateCommand());

        for (InstanceRecord instanceRecord : mInstanceRecords)
            if (instanceRecord.needsUpdate())
                updateCommands.add(instanceRecord.getUpdateCommand());

        SaveService.startService(mApplicationContext, insertCommands, updateCommands);
    }

    SQLiteDatabase getSQLiteDatabase() {
        return mSQLiteDatabase;
    }
}