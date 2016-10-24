package com.krystianwsul.checkme.persistencemodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.LocalTask;
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
import java.util.Map;

public class PersistenceManger {
    private static PersistenceManger sInstance;

    private final SQLiteDatabase mSQLiteDatabase;

    private final List<CustomTimeRecord> mCustomTimeRecords;

    private final List<TaskRecord> mTaskRecords;
    private final List<TaskHierarchyRecord> mTaskHierarchyRecords;

    private final List<ScheduleRecord> mScheduleRecords;
    private final Map<Integer, SingleScheduleRecord> mSingleScheduleRecords;
    private final Map<Integer, DailyScheduleRecord> mDailyScheduleRecords;
    private final Map<Integer, WeeklyScheduleRecord> mWeeklyScheduleRecords;
    private final Map<Integer, MonthlyDayScheduleRecord> mMonthlyDayScheduleRecords;
    private final Map<Integer, MonthlyWeekScheduleRecord> mMonthlyWeekScheduleRecords;

    private final List<InstanceRecord> mInstanceRecords;

    private final List<InstanceShownRecord> mInstancesShownRecords;

    private int mCustomTimeMaxId;
    private int mTaskMaxId;
    private int mTaskHierarchyMaxId;
    private int mScheduleMaxId;
    private int mInstanceMaxId;
    private int mInstanceShownMaxId;

    @NonNull
    public static synchronized PersistenceManger getInstance(Context context) {
        Assert.assertTrue(context != null);

        if (sInstance == null)
            sInstance = new PersistenceManger(context);
        return sInstance;
    }

    @SuppressLint("UseSparseArrays")
    private PersistenceManger(@NonNull Context context) {
        mSQLiteDatabase = MySQLiteHelper.getDatabase(context);

        mCustomTimeRecords = CustomTimeRecord.getCustomTimeRecords(mSQLiteDatabase);
        Assert.assertTrue(mCustomTimeRecords != null);

        mCustomTimeMaxId = CustomTimeRecord.getMaxId(mSQLiteDatabase);

        mTaskRecords = TaskRecord.getTaskRecords(mSQLiteDatabase);
        Assert.assertTrue(mTaskRecords != null);

        mTaskMaxId = TaskRecord.getMaxId(mSQLiteDatabase);

        List<Integer> taskIds = Stream.of(mTaskRecords)
                .map(TaskRecord::getId)
                .collect(Collectors.toList());

        if (mTaskRecords.isEmpty())
            mTaskHierarchyRecords = new ArrayList<>();
        else
            mTaskHierarchyRecords = TaskHierarchyRecord.getTaskHierarchyRecords(mSQLiteDatabase, taskIds);
        Assert.assertTrue(mTaskHierarchyRecords != null);

        mTaskHierarchyMaxId = TaskHierarchyRecord.getMaxId(mSQLiteDatabase);

        if (mTaskRecords.isEmpty())
            mScheduleRecords = new ArrayList<>();
        else
            mScheduleRecords = ScheduleRecord.getScheduleRecords(mSQLiteDatabase, taskIds);
        Assert.assertTrue(mScheduleRecords != null);

        mScheduleMaxId = ScheduleRecord.getMaxId(mSQLiteDatabase);

        List<Integer> scheduleIds = Stream.of(mScheduleRecords)
                .map(ScheduleRecord::getId)
                .collect(Collectors.toList());

        if (scheduleIds.isEmpty())
            mSingleScheduleRecords = new HashMap<>();
        else
            mSingleScheduleRecords = Stream.of(SingleScheduleRecord.getSingleScheduleRecords(mSQLiteDatabase, scheduleIds))
                    .collect(Collectors.toMap(SingleScheduleRecord::getScheduleId, singleScheduleRecord -> singleScheduleRecord));

        if (scheduleIds.isEmpty())
            mDailyScheduleRecords = new HashMap<>();
        else
            mDailyScheduleRecords = Stream.of(DailyScheduleRecord.getDailyScheduleRecords(mSQLiteDatabase, scheduleIds))
                    .collect(Collectors.toMap(DailyScheduleRecord::getScheduleId, dailyScheduleRecord -> dailyScheduleRecord));

        if (scheduleIds.isEmpty())
            mWeeklyScheduleRecords = new HashMap<>();
        else
            mWeeklyScheduleRecords = Stream.of(WeeklyScheduleRecord.getWeeklyScheduleRecords(mSQLiteDatabase, scheduleIds))
                    .collect(Collectors.toMap(WeeklyScheduleRecord::getScheduleId, weeklyScheduleRecord -> weeklyScheduleRecord));

        if (scheduleIds.isEmpty())
            mMonthlyDayScheduleRecords = new HashMap<>();
        else
            mMonthlyDayScheduleRecords = Stream.of(MonthlyDayScheduleRecord.getMonthlyDayScheduleRecords(mSQLiteDatabase, scheduleIds))
                    .collect(Collectors.toMap(MonthlyDayScheduleRecord::getScheduleId, monthlyDayScheduleRecord -> monthlyDayScheduleRecord));

        if (scheduleIds.isEmpty())
            mMonthlyWeekScheduleRecords = new HashMap<>();
        else
            mMonthlyWeekScheduleRecords = Stream.of(MonthlyWeekScheduleRecord.getMonthlyWeekScheduleRecords(mSQLiteDatabase, scheduleIds))
                    .collect(Collectors.toMap(MonthlyWeekScheduleRecord::getScheduleId, monthlyWeekScheduleRecord -> monthlyWeekScheduleRecord));

        mInstanceRecords = InstanceRecord.getInstanceRecords(mSQLiteDatabase);
        Assert.assertTrue(mInstanceRecords != null);

        mInstanceMaxId = InstanceRecord.getMaxId(mSQLiteDatabase);

        mInstancesShownRecords = InstanceShownRecord.getInstancesShownRecords(mSQLiteDatabase);

        mInstanceShownMaxId = InstanceShownRecord.getMaxId(mSQLiteDatabase);
    }

    @SuppressLint("UseSparseArrays")
    public PersistenceManger() {
        mSQLiteDatabase = null;
        mCustomTimeRecords = new ArrayList<>();
        mTaskRecords = new ArrayList<>();
        mTaskHierarchyRecords = new ArrayList<>();
        mScheduleRecords = new ArrayList<>();
        mSingleScheduleRecords = new HashMap<>();
        mDailyScheduleRecords = new HashMap<>();
        mWeeklyScheduleRecords = new HashMap<>();
        mMonthlyDayScheduleRecords = new HashMap<>();
        mMonthlyWeekScheduleRecords = new HashMap<>();
        mInstanceRecords = new ArrayList<>();
        mInstancesShownRecords = new ArrayList<>();

        mCustomTimeMaxId = 0;
        mTaskMaxId = 0;
        mTaskHierarchyMaxId = 0;
        mScheduleMaxId = 0;
        mInstanceMaxId = 0;
        mInstanceShownMaxId = 0;
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

    public List<ScheduleRecord> getScheduleRecords(int localTaskId) {
        return Stream.of(mScheduleRecords)
                .filter(scheduleRecord -> scheduleRecord.getRootTaskId() == localTaskId)
                .collect(Collectors.toList());
    }

    @Nullable
    public SingleScheduleRecord getSingleScheduleRecord(int scheduleId) {
        return mSingleScheduleRecords.get(scheduleId);
    }

    @Nullable
    public DailyScheduleRecord getDailyScheduleRecord(int scheduleId) {
        return mDailyScheduleRecords.get(scheduleId);
    }

    @Nullable
    public WeeklyScheduleRecord getWeeklyScheduleRecord(int scheduleId) {
        return mWeeklyScheduleRecords.get(scheduleId);
    }

    @Nullable
    public MonthlyDayScheduleRecord getMonthlyDayScheduleRecord(int scheduleId) {
        return mMonthlyDayScheduleRecords.get(scheduleId);
    }

    @Nullable
    public MonthlyWeekScheduleRecord getMonthlyWeekScheduleRecord(int scheduleId) {
        return mMonthlyWeekScheduleRecords.get(scheduleId);
    }

    public Collection<InstanceRecord> getInstanceRecords() {
        return mInstanceRecords;
    }

    public List<InstanceShownRecord> getInstancesShownRecords() {
        return mInstancesShownRecords;
    }

    public CustomTimeRecord createCustomTimeRecord(String name, Map<DayOfWeek, HourMinute> hourMinutes) {
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

        CustomTimeRecord customTimeRecord = new CustomTimeRecord(false, id, name, sunday.getHour(), sunday.getMinute(), monday.getHour(), monday.getMinute(), tuesday.getHour(), tuesday.getMinute(), wednesday.getHour(), wednesday.getMinute(), thursday.getHour(), thursday.getMinute(), friday.getHour(), friday.getMinute(), saturday.getHour(), saturday.getMinute(), true, true);
        mCustomTimeRecords.add(customTimeRecord);
        return customTimeRecord;
    }

    @NonNull
    public TaskRecord createTaskRecord(@NonNull String name, @NonNull ExactTimeStamp startExactTimeStamp, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        int id = ++mTaskMaxId;

        TaskRecord taskRecord = new TaskRecord(false, id, name, startExactTimeStamp.getLong(), null, true, null, null, null, note);
        mTaskRecords.add(taskRecord);

        return taskRecord;
    }

    public TaskHierarchyRecord createTaskHierarchyRecord(@NonNull LocalTask parentLocalTask, @NonNull LocalTask childLocalTask, @NonNull ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(parentLocalTask.current(startExactTimeStamp));
        Assert.assertTrue(childLocalTask.current(startExactTimeStamp));

        int id = ++mTaskHierarchyMaxId;

        TaskHierarchyRecord taskHierarchyRecord = new TaskHierarchyRecord(false, id, parentLocalTask.getId(), childLocalTask.getId(), startExactTimeStamp.getLong(), null);
        mTaskHierarchyRecords.add(taskHierarchyRecord);
        return taskHierarchyRecord;
    }

    @NonNull
    public ScheduleRecord createScheduleRecord(@NonNull LocalTask rootLocalTask, @NonNull ScheduleType scheduleType, @NonNull ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(rootLocalTask.current(startExactTimeStamp));

        int id = ++mScheduleMaxId;

        ScheduleRecord scheduleRecord = new ScheduleRecord(false, id, rootLocalTask.getId(), startExactTimeStamp.getLong(), null, scheduleType.ordinal());
        mScheduleRecords.add(scheduleRecord);

        return scheduleRecord;
    }

    @NonNull
    public SingleScheduleRecord createSingleScheduleRecord(int scheduleId, @NonNull Date date, @NonNull Time time) {
        Assert.assertTrue(!mSingleScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mDailyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mWeeklyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyDayScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyWeekScheduleRecords.containsKey(scheduleId));

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        SingleScheduleRecord singleScheduleRecord = new SingleScheduleRecord(false, scheduleId, date.getYear(), date.getMonth(), date.getDay(), customTimeId, hour, minute);
        mSingleScheduleRecords.put(singleScheduleRecord.getScheduleId(), singleScheduleRecord);

        return singleScheduleRecord;
    }

    @NonNull
    public DailyScheduleRecord createDailyScheduleRecord(int scheduleId, @NonNull Time time) {
        Assert.assertTrue(!mSingleScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mDailyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mWeeklyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyDayScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyWeekScheduleRecords.containsKey(scheduleId));

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        DailyScheduleRecord dailyScheduleRecord = new DailyScheduleRecord(false, scheduleId, customTimeId, hour, minute);
        mDailyScheduleRecords.put(scheduleId, dailyScheduleRecord);

        return dailyScheduleRecord;
    }

    @NonNull
    public WeeklyScheduleRecord createWeeklyScheduleRecord(int scheduleId, @NonNull DayOfWeek dayOfWeek, @NonNull Time time) {
        Assert.assertTrue(!mSingleScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mDailyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mWeeklyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyDayScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyWeekScheduleRecords.containsKey(scheduleId));

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        WeeklyScheduleRecord weeklyScheduleRecord = new WeeklyScheduleRecord(false, scheduleId, dayOfWeek.ordinal(), customTimeId, hour, minute);
        mWeeklyScheduleRecords.put(scheduleId, weeklyScheduleRecord);

        return weeklyScheduleRecord;
    }

    @NonNull
    public MonthlyDayScheduleRecord createMonthlyDayScheduleRecord(int scheduleId, int dayOfMonth, boolean beginningOfMonth, @NonNull Time time) {
        Assert.assertTrue(!mSingleScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mDailyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mWeeklyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyDayScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyWeekScheduleRecords.containsKey(scheduleId));

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        MonthlyDayScheduleRecord monthlyDayScheduleRecord = new MonthlyDayScheduleRecord(false, scheduleId, dayOfMonth, beginningOfMonth, customTimeId, hour, minute);
        mMonthlyDayScheduleRecords.put(scheduleId, monthlyDayScheduleRecord);

        return monthlyDayScheduleRecord;
    }

    @NonNull
    public MonthlyWeekScheduleRecord createMonthlyWeekScheduleRecord(int scheduleId, int dayOfMonth, @NonNull DayOfWeek dayOfWeek, boolean beginningOfMonth, @NonNull Time time) {
        Assert.assertTrue(!mSingleScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mDailyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mWeeklyScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyDayScheduleRecords.containsKey(scheduleId));
        Assert.assertTrue(!mMonthlyWeekScheduleRecords.containsKey(scheduleId));

        Pair<CustomTime, HourMinute> pair = time.getPair();

        CustomTime customTime = pair.first;
        HourMinute hourMinute = pair.second;

        Assert.assertTrue((customTime == null) != (hourMinute == null));

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        MonthlyWeekScheduleRecord monthlyWeekScheduleRecord = new MonthlyWeekScheduleRecord(false, scheduleId, dayOfMonth, dayOfWeek.ordinal(), beginningOfMonth, customTimeId, hour, minute);
        mMonthlyWeekScheduleRecords.put(scheduleId, monthlyWeekScheduleRecord);

        return monthlyWeekScheduleRecord;
    }

    @NonNull
    public InstanceRecord createInstanceRecord(@NonNull LocalTask localTask, @NonNull DateTime scheduleDateTime, @NonNull ExactTimeStamp now) {
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

        InstanceRecord instanceRecord = new InstanceRecord(false, id, localTask.getId(), null, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleCustomTimeId, scheduleHour, scheduleMinute, null, null, null, null, null, null, now.getLong(), false, false, true);
        mInstanceRecords.add(instanceRecord);
        return instanceRecord;
    }

    public void save(@NonNull Context context) {
        // save

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

        for (SingleScheduleRecord singleScheduleRecord : mSingleScheduleRecords.values())
            if (singleScheduleRecord.needsInsert())
                insertCommands.add(singleScheduleRecord.getInsertCommand());

        for (DailyScheduleRecord dailyScheduleRecord : mDailyScheduleRecords.values())
            if (dailyScheduleRecord.needsInsert())
                insertCommands.add(dailyScheduleRecord.getInsertCommand());

        for (WeeklyScheduleRecord weeklyScheduleRecord : mWeeklyScheduleRecords.values())
            if (weeklyScheduleRecord.needsInsert())
                insertCommands.add(weeklyScheduleRecord.getInsertCommand());

        for (MonthlyDayScheduleRecord monthlyDayScheduleRecord : mMonthlyDayScheduleRecords.values())
            if (monthlyDayScheduleRecord.needsInsert())
                insertCommands.add(monthlyDayScheduleRecord.getInsertCommand());

        for (MonthlyWeekScheduleRecord monthlyWeekScheduleRecord : mMonthlyWeekScheduleRecords.values())
            if (monthlyWeekScheduleRecord.needsInsert())
                insertCommands.add(monthlyWeekScheduleRecord.getInsertCommand());

        for (InstanceRecord instanceRecord : mInstanceRecords)
            if (instanceRecord.needsInsert())
                insertCommands.add(instanceRecord.getInsertCommand());

        for (InstanceShownRecord instanceShownRecord : mInstancesShownRecords)
            if (instanceShownRecord.needsInsert())
                insertCommands.add(instanceShownRecord.getInsertCommand());

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

        for (SingleScheduleRecord singleScheduleRecord : mSingleScheduleRecords.values())
            if (singleScheduleRecord.needsUpdate())
                updateCommands.add(singleScheduleRecord.getUpdateCommand());

        for (DailyScheduleRecord dailyScheduleRecord : mDailyScheduleRecords.values())
            if (dailyScheduleRecord.needsUpdate())
                updateCommands.add(dailyScheduleRecord.getUpdateCommand());

        for (WeeklyScheduleRecord weeklyScheduleRecord : mWeeklyScheduleRecords.values())
            if (weeklyScheduleRecord.needsUpdate())
                updateCommands.add(weeklyScheduleRecord.getUpdateCommand());

        for (MonthlyDayScheduleRecord monthlyDayScheduleRecord : mMonthlyDayScheduleRecords.values())
            if (monthlyDayScheduleRecord.needsUpdate())
                updateCommands.add(monthlyDayScheduleRecord.getUpdateCommand());

        for (MonthlyWeekScheduleRecord monthlyWeekScheduleRecord : mMonthlyWeekScheduleRecords.values())
            if (monthlyWeekScheduleRecord.needsUpdate())
                updateCommands.add(monthlyWeekScheduleRecord.getUpdateCommand());

        for (InstanceRecord instanceRecord : mInstanceRecords)
            if (instanceRecord.needsUpdate())
                updateCommands.add(instanceRecord.getUpdateCommand());

        for (InstanceShownRecord instanceShownRecord : mInstancesShownRecords)
            if (instanceShownRecord.needsUpdate())
                updateCommands.add(instanceShownRecord.getUpdateCommand());

        ArrayList<DeleteCommand> deleteCommands = new ArrayList<>();

        deleteCommands.addAll(delete(mCustomTimeRecords));
        deleteCommands.addAll(delete(mTaskRecords));
        deleteCommands.addAll(delete(mTaskHierarchyRecords));
        deleteCommands.addAll(delete(mScheduleRecords));
        deleteCommands.addAll(delete(mSingleScheduleRecords));
        deleteCommands.addAll(delete(mDailyScheduleRecords));
        deleteCommands.addAll(delete(mWeeklyScheduleRecords));
        deleteCommands.addAll(delete(mMonthlyDayScheduleRecords));
        deleteCommands.addAll(delete(mMonthlyWeekScheduleRecords));
        deleteCommands.addAll(delete(mInstanceRecords));
        deleteCommands.addAll(delete(mInstancesShownRecords));

        SaveService.startService(context, insertCommands, updateCommands, deleteCommands);
    }

    SQLiteDatabase getSQLiteDatabase() {
        return mSQLiteDatabase;
    }

    @NonNull
    private static List<DeleteCommand> delete(@NonNull Collection<? extends Record> collection) {
        List<Record> deleted = Stream.of(collection)
                .filter(Record::needsDelete)
                .collect(Collectors.toList());

        //noinspection SuspiciousMethodCalls
        collection.removeAll(deleted);

        return Stream.of(deleted)
                .map(Record::getDeleteCommand)
                .collect(Collectors.toList());
    }

    @NonNull
    private static List<DeleteCommand> delete(@NonNull Map<Integer, ? extends Record> map) {
        Map<Integer, Record> deleted = Stream.of(map)
                .filter(pair -> pair.getValue().needsDelete())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Stream.of(deleted.keySet())
                .forEach(map::remove);

        return Stream.of(deleted.values())
                .map(Record::getDeleteCommand)
                .collect(Collectors.toList());
    }

    @NonNull
    public InstanceShownRecord createInstanceShownRecord(@NonNull String remoteTaskId, @NonNull DateTime scheduleDateTime) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteTaskId));

        int id = ++mInstanceShownMaxId;

        HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().mHourMinute;
        Integer hour = (hourMinute == null ? null : hourMinute.getHour());
        Integer minute = (hourMinute == null ? null : hourMinute.getMinute());
        InstanceShownRecord instanceShownRecord = new InstanceShownRecord(false, id, remoteTaskId, scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), scheduleDateTime.getTime().getTimePair().mCustomTimeId, hour, minute, false, false);
        mInstancesShownRecords.add(instanceShownRecord);
        return instanceShownRecord;
    }
}