package com.krystianwsul.checkme.persistencemodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;
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
import java.util.Set;

public class PersistenceManger {
    private static PersistenceManger sInstance;

    private final SQLiteDatabase mSQLiteDatabase;

    final List<CustomTimeRecord> mCustomTimeRecords;

    final List<TaskRecord> mTaskRecords;
    final List<TaskHierarchyRecord> mTaskHierarchyRecords;

    final List<ScheduleRecord> mScheduleRecords;
    final Map<Integer, SingleScheduleRecord> mSingleScheduleRecords;
    final Map<Integer, DailyScheduleRecord> mDailyScheduleRecords;
    final Map<Integer, WeeklyScheduleRecord> mWeeklyScheduleRecords;
    final Map<Integer, MonthlyDayScheduleRecord> mMonthlyDayScheduleRecords;
    final Map<Integer, MonthlyWeekScheduleRecord> mMonthlyWeekScheduleRecords;

    final List<InstanceRecord> mInstanceRecords;

    final List<InstanceShownRecord> mInstanceShownRecords;

    @NonNull
    private final UuidRecord mUuidRecord;

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

        mCustomTimeMaxId = CustomTimeRecord.getMaxId(mSQLiteDatabase);

        mTaskRecords = TaskRecord.getTaskRecords(mSQLiteDatabase);
        Assert.assertTrue(mTaskRecords != null);

        mTaskMaxId = TaskRecord.getMaxId(mSQLiteDatabase);

        if (mTaskRecords.isEmpty())
            mTaskHierarchyRecords = new ArrayList<>();
        else
            mTaskHierarchyRecords = TaskHierarchyRecord.getTaskHierarchyRecords(mSQLiteDatabase);
        Assert.assertTrue(mTaskHierarchyRecords != null);

        mTaskHierarchyMaxId = TaskHierarchyRecord.getMaxId(mSQLiteDatabase);

        mScheduleRecords = ScheduleRecord.getScheduleRecords(mSQLiteDatabase);

        mScheduleMaxId = ScheduleRecord.getMaxId(mSQLiteDatabase);

        mSingleScheduleRecords = Stream.of(SingleScheduleRecord.getSingleScheduleRecords(mSQLiteDatabase))
                    .collect(Collectors.toMap(SingleScheduleRecord::getScheduleId, singleScheduleRecord -> singleScheduleRecord));

        mDailyScheduleRecords = Stream.of(DailyScheduleRecord.getDailyScheduleRecords(mSQLiteDatabase))
                    .collect(Collectors.toMap(DailyScheduleRecord::getScheduleId, dailyScheduleRecord -> dailyScheduleRecord));

        mWeeklyScheduleRecords = Stream.of(WeeklyScheduleRecord.getWeeklyScheduleRecords(mSQLiteDatabase))
                    .collect(Collectors.toMap(WeeklyScheduleRecord::getScheduleId, weeklyScheduleRecord -> weeklyScheduleRecord));

        mMonthlyDayScheduleRecords = Stream.of(MonthlyDayScheduleRecord.getMonthlyDayScheduleRecords(mSQLiteDatabase))
                    .collect(Collectors.toMap(MonthlyDayScheduleRecord::getScheduleId, monthlyDayScheduleRecord -> monthlyDayScheduleRecord));

        mMonthlyWeekScheduleRecords = Stream.of(MonthlyWeekScheduleRecord.getMonthlyWeekScheduleRecords(mSQLiteDatabase))
                    .collect(Collectors.toMap(MonthlyWeekScheduleRecord::getScheduleId, monthlyWeekScheduleRecord -> monthlyWeekScheduleRecord));

        mInstanceRecords = InstanceRecord.getInstanceRecords(mSQLiteDatabase);
        Assert.assertTrue(mInstanceRecords != null);

        mInstanceMaxId = InstanceRecord.getMaxId(mSQLiteDatabase);

        mInstanceShownRecords = InstanceShownRecord.getInstancesShownRecords(mSQLiteDatabase);

        mInstanceShownMaxId = InstanceShownRecord.getMaxId(mSQLiteDatabase);

        mUuidRecord = UuidRecord.getUuidRecord(mSQLiteDatabase);
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
        mInstanceShownRecords = new ArrayList<>();
        mUuidRecord = new UuidRecord(true, UuidRecord.newUuid());

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

    public List<InstanceShownRecord> getInstanceShownRecords() {
        return mInstanceShownRecords;
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

        CustomTimeRecord customTimeRecord = new CustomTimeRecord(false, id, name, sunday.getHour(), sunday.getMinute(), monday.getHour(), monday.getMinute(), tuesday.getHour(), tuesday.getMinute(), wednesday.getHour(), wednesday.getMinute(), thursday.getHour(), thursday.getMinute(), friday.getHour(), friday.getMinute(), saturday.getHour(), saturday.getMinute(), true);
        mCustomTimeRecords.add(customTimeRecord);
        return customTimeRecord;
    }

    @NonNull
    public TaskRecord createTaskRecord(@NonNull String name, @NonNull ExactTimeStamp startExactTimeStamp, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        int id = ++mTaskMaxId;

        TaskRecord taskRecord = new TaskRecord(false, id, name, startExactTimeStamp.getLong(), null, null, null, null, note);
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

        Integer customTimeId;
        Integer hour;
        Integer minute;

        if (time.getTimePair().mCustomTimeKey != null) {
            Assert.assertTrue(time.getTimePair().mHourMinute == null);

            customTimeId = time.getTimePair().mCustomTimeKey.mLocalCustomTimeId;
            Assert.assertTrue(customTimeId != null);

            hour = null;
            minute = null;
        } else {
            Assert.assertTrue(time.getTimePair().mHourMinute != null);

            customTimeId = null;

            hour = time.getTimePair().mHourMinute.getHour();
            minute = time.getTimePair().mHourMinute.getMinute();
        }

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

        Integer customTimeId;
        Integer hour;
        Integer minute;

        if (time.getTimePair().mCustomTimeKey != null) {
            Assert.assertTrue(time.getTimePair().mHourMinute == null);

            customTimeId = time.getTimePair().mCustomTimeKey.mLocalCustomTimeId;
            Assert.assertTrue(customTimeId != null);

            hour = null;
            minute = null;
        } else {
            Assert.assertTrue(time.getTimePair().mHourMinute != null);

            customTimeId = null;

            hour = time.getTimePair().mHourMinute.getHour();
            minute = time.getTimePair().mHourMinute.getMinute();
        }

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

        Integer customTimeId;
        Integer hour;
        Integer minute;

        if (time.getTimePair().mCustomTimeKey != null) {
            Assert.assertTrue(time.getTimePair().mHourMinute == null);

            customTimeId = time.getTimePair().mCustomTimeKey.mLocalCustomTimeId;
            Assert.assertTrue(customTimeId != null);

            hour = null;
            minute = null;
        } else {
            Assert.assertTrue(time.getTimePair().mHourMinute != null);

            customTimeId = null;

            hour = time.getTimePair().mHourMinute.getHour();
            minute = time.getTimePair().mHourMinute.getMinute();
        }

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

        Integer customTimeId;
        Integer hour;
        Integer minute;

        if (time.getTimePair().mCustomTimeKey != null) {
            Assert.assertTrue(time.getTimePair().mHourMinute == null);

            customTimeId = time.getTimePair().mCustomTimeKey.mLocalCustomTimeId;
            Assert.assertTrue(customTimeId != null);

            hour = null;
            minute = null;
        } else {
            Assert.assertTrue(time.getTimePair().mHourMinute != null);

            customTimeId = null;

            hour = time.getTimePair().mHourMinute.getHour();
            minute = time.getTimePair().mHourMinute.getMinute();
        }

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

        Integer customTimeId;
        Integer hour;
        Integer minute;

        if (time.getTimePair().mCustomTimeKey != null) {
            Assert.assertTrue(time.getTimePair().mHourMinute == null);

            customTimeId = time.getTimePair().mCustomTimeKey.mLocalCustomTimeId;
            Assert.assertTrue(customTimeId != null);

            hour = null;
            minute = null;
        } else {
            Assert.assertTrue(time.getTimePair().mHourMinute != null);

            customTimeId = null;

            hour = time.getTimePair().mHourMinute.getHour();
            minute = time.getTimePair().mHourMinute.getMinute();
        }

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
            Assert.assertTrue(scheduleDateTime.getTime() instanceof LocalCustomTime);

            scheduleCustomTimeId = ((LocalCustomTime) scheduleTime).getId();
        } else {
            Assert.assertTrue(scheduleTime instanceof NormalTime);

            HourMinute hourMinute = ((NormalTime) scheduleTime).getHourMinute();
            scheduleHour = hourMinute.getHour();
            scheduleMinute = hourMinute.getMinute();
        }

        int id = ++mInstanceMaxId;

        InstanceRecord instanceRecord = new InstanceRecord(false, id, localTask.getId(), null, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleCustomTimeId, scheduleHour, scheduleMinute, null, null, null, null, null, null, now.getLong(), false, false);
        mInstanceRecords.add(instanceRecord);
        return instanceRecord;
    }

    public void save(@NonNull Context context) {
        SaveService.Factory.getInstance().startService(context, this);
    }

    SQLiteDatabase getSQLiteDatabase() {
        return mSQLiteDatabase;
    }

    @NonNull
    public InstanceShownRecord createInstanceShownRecord(@NonNull String remoteTaskId, @NonNull Date scheduleDate, @Nullable String remoteCustomTimeId, @Nullable Integer hour, @Nullable Integer minute, @NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        int id = ++mInstanceShownMaxId;

        InstanceShownRecord instanceShownRecord = new InstanceShownRecord(false, id, remoteTaskId, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), remoteCustomTimeId, hour, minute, false, false, projectId);
        mInstanceShownRecords.add(instanceShownRecord);
        return instanceShownRecord;
    }

    public void deleteInstanceShownRecords(@NonNull Set<TaskKey> taskKeys) {
        List<InstanceShownRecord> remove = Stream.of(mInstanceShownRecords)
                .filterNot(instanceShownRecord -> Stream.of(taskKeys).anyMatch(taskKey -> (instanceShownRecord.getProjectId().equals(taskKey.mRemoteProjectId) && instanceShownRecord.getTaskId().equals(taskKey.mRemoteTaskId))))
                .collect(Collectors.toList());

        Stream.of(remove)
                .forEach(InstanceShownRecord::delete);
    }

    @NonNull
    public String getUuid() {
        return mUuidRecord.getUuid();
    }
}