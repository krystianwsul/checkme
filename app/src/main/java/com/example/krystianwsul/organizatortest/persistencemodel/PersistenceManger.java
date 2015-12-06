package com.example.krystianwsul.organizatortest.persistencemodel;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.DailySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.WeeklySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

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

    private final HashMap<Integer, SingleScheduleRecord> mSingleScheduleRecords = new HashMap<>();

    private final HashMap<Integer, DailyScheduleRecord> mDailyScheduleRecords = new HashMap<>();
    private final HashMap<Integer, DailyScheduleTimeRecord> mDailyScheduleTimeRecords = new HashMap<>();
    private final HashMap<Integer, DailyRepetitionRecord> mDailyRepetitionRecords = new HashMap<>();

    private final HashMap<Integer, WeeklyScheduleRecord> mWeeklyScheduleRecords = new HashMap<>();
    private final HashMap<Integer, WeeklyScheduleDayOfWeekTimeRecord> mWeeklyScheduleDayOfWeekTimeRecords = new HashMap<>();
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

        TaskRecord zakupy = new TaskRecord(0, null, "zakupy");
        mTaskRecords.put(zakupy.getId(), zakupy);
        TaskRecord halls = new TaskRecord(1, zakupy.getId(), "halls");
        mTaskRecords.put(halls.getId(), halls);
        TaskRecord biedronka = new TaskRecord(2, zakupy.getId(), "biedronka");
        mTaskRecords.put(biedronka.getId(), biedronka);
        TaskRecord czosnek = new TaskRecord(3, biedronka.getId(), "czosnek");
        mTaskRecords.put(czosnek.getId(), czosnek);
        TaskRecord piersi = new TaskRecord(4, biedronka.getId(), "piersi");
        mTaskRecords.put(piersi.getId(), piersi);

        SingleScheduleRecord today15 = new SingleScheduleRecord(zakupy.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 15, 0);
        mSingleScheduleRecords.put(today15.getRootTaskId(), today15);

        TaskRecord rachunek = new TaskRecord(5, null, "rachunek");
        mTaskRecords.put(rachunek.getId(), rachunek);

        SingleScheduleRecord yesterday16 = new SingleScheduleRecord(rachunek.getId(), calendarYesterday.get(Calendar.YEAR), calendarYesterday.get(Calendar.MONTH) + 1, calendarYesterday.get(Calendar.DAY_OF_MONTH), null, 16, 0);
        mSingleScheduleRecords.put(yesterday16.getRootTaskId(), yesterday16);

        TaskRecord banany = new TaskRecord(6, null, "banany");
        mTaskRecords.put(banany.getId(), banany);

        SingleScheduleRecord today17 = new SingleScheduleRecord(banany.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 17, 0);
        mSingleScheduleRecords.put(today17.getRootTaskId(), today17);

        TaskRecord iliotibial = new TaskRecord(7, null, "iliotibial band stretch");
        mTaskRecords.put(iliotibial.getId(), iliotibial);

        DailyScheduleRecord alwaysAfterWakingAfterWork = new DailyScheduleRecord(iliotibial.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mDailyScheduleRecords.put(alwaysAfterWakingAfterWork.getRootTaskId(), alwaysAfterWakingAfterWork);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork0 = new DailyScheduleTimeRecord(1, alwaysAfterWakingAfterWork.getRootTaskId(), afterWaking.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork0.getId(), alwaysAfterWakingAfterWork0);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork1 = new DailyScheduleTimeRecord(2, alwaysAfterWakingAfterWork.getRootTaskId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork1.getId(), alwaysAfterWakingAfterWork1);

        TaskRecord hamstring = new TaskRecord(8, null, "hamstring stretch");
        mTaskRecords.put(hamstring.getId(), hamstring);

        DailyScheduleRecord alwaysAfterWork = new DailyScheduleRecord(hamstring.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mDailyScheduleRecords.put(alwaysAfterWork.getRootTaskId(), alwaysAfterWork);
        DailyScheduleTimeRecord alwaysAfterWork0 = new DailyScheduleTimeRecord(0, alwaysAfterWork.getRootTaskId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWork0.getId(), alwaysAfterWork0);

        TaskRecord piecyk = new TaskRecord(9, null, "piecyk");
        mTaskRecords.put(piecyk.getId(), piecyk);

        SingleScheduleRecord todayAfterWaking = new SingleScheduleRecord(piecyk.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), afterWaking.getId(), null, null);
        mSingleScheduleRecords.put(todayAfterWaking.getRootTaskId(), todayAfterWaking);

        TaskRecord paznokcie = new TaskRecord(10, null, "paznokcie");
        mTaskRecords.put(paznokcie.getId(), paznokcie);

        WeeklyScheduleRecord crazyWeekend = new WeeklyScheduleRecord(paznokcie.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mWeeklyScheduleRecords.put(crazyWeekend.getRootTaskId(), crazyWeekend);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend0 = new WeeklyScheduleDayOfWeekTimeRecord(0, crazyWeekend.getRootTaskId(), DayOfWeek.SATURDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend0.getId(), crazyWeekend0);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend1 = new WeeklyScheduleDayOfWeekTimeRecord(1, crazyWeekend.getRootTaskId(), DayOfWeek.SUNDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend1.getId(), crazyWeekend1);
        WeeklyScheduleDayOfWeekTimeRecord crazyWeekend2 = new WeeklyScheduleDayOfWeekTimeRecord(2, crazyWeekend.getRootTaskId(), DayOfWeek.SUNDAY.ordinal(), null, 17, 0);
        mWeeklyScheduleDayOfWeekTimeRecords.put(crazyWeekend2.getId(), crazyWeekend2);

        TaskRecord task6 = new TaskRecord(11, null, "task 6");
        mTaskRecords.put(task6.getId(), task6);

        DailyScheduleRecord task6schedule = new DailyScheduleRecord(task6.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mDailyScheduleRecords.put(task6schedule.getRootTaskId(), task6schedule);
        DailyScheduleTimeRecord task6schedule0 = new DailyScheduleTimeRecord(3, task6schedule.getRootTaskId(), null, 6, 0);
        mDailyScheduleTimeRecords.put(task6schedule0.getId(), task6schedule0);
    }

    public CustomTimeRecord getCustomTimeRecord(int timeRecordId) {
        return mCustomTimeRecords.get(timeRecordId);
    }

    public Collection<Integer> getCustomTimeIds() {
        return mCustomTimeRecords.keySet();
    }

    public TaskRecord getTaskRecord(int taskId) {
        return mTaskRecords.get(taskId);
    }

    public ArrayList<Integer> getTaskIds(Integer parentTaskId) {
        ArrayList<Integer> taskIds = new ArrayList<>();
        for (TaskRecord taskRecord : mTaskRecords.values()) {
            Integer thisParentTaskId = taskRecord.getParentTaskId();
            if ((thisParentTaskId == null && parentTaskId == null) || (thisParentTaskId != null && parentTaskId != null && thisParentTaskId.equals(parentTaskId)))
                taskIds.add(taskRecord.getId());
        }
        return taskIds;
    }

    public SingleScheduleRecord getSingleScheduleRecord(int rootTaskId) {
        return mSingleScheduleRecords.get(rootTaskId);
    }

    public InstanceRecord getSingleInstanceRecord(int rootTaskId) {
        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.getRootTaskId() != null && instanceRecord.getRootTaskId().equals(rootTaskId))
                return instanceRecord;
        return null;
    }

    public InstanceRecord createSingleInstanceRecord(int id, int taskId, int rootTaskId, Long done) {
        Assert.assertTrue(!mInstanceRecords.containsKey(taskId));

        InstanceRecord instanceRecord = new InstanceRecord(id, taskId, done, rootTaskId, null, null);
        mInstanceRecords.put(instanceRecord.getTaskId(), instanceRecord);
        return instanceRecord;
    }

    public DailyScheduleRecord getDailyScheduleRecord(int rootTaskId) {
        return mDailyScheduleRecords.get(rootTaskId);
    }

    public ArrayList<DailyScheduleTimeRecord> getDailyScheduleTimeRecords(int dailyScheduleTimeId) {
        ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = new ArrayList<>();
        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords.values())
            if (dailyScheduleTimeRecord.getDailyScheduleId() == dailyScheduleTimeId)
                dailyScheduleTimeRecords.add(dailyScheduleTimeRecord);
        return dailyScheduleTimeRecords;
    }

    public ArrayList<DailyRepetitionRecord> getDailyRepetitionRecords(int dailyScheduleTimeId) {
        ArrayList<DailyRepetitionRecord> dailyRepetitionRecords = new ArrayList<>();
        for (DailyRepetitionRecord dailyRepetitionRecord : mDailyRepetitionRecords.values()) {
            if (dailyRepetitionRecord.getDailyScheduleTimeId() == dailyScheduleTimeId) {
                dailyRepetitionRecords.add(dailyRepetitionRecord);
            }
        }
        return dailyRepetitionRecords;
    }

    public int getMaxDailyRepetitionId() {
        return mMaxDailyRepetitionId;
    }

    public InstanceRecord getDailyInstanceRecord(int taskId, int dailyRepetitionId) {
        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.getTaskId() == taskId && instanceRecord.getDailyRepetitionId() != null && instanceRecord.getDailyRepetitionId().equals(dailyRepetitionId))
                return instanceRecord;
        return null;
    }

    public InstanceRecord createDailyInstanceRecord(int id, int taskId, int dailyRepetitionId, Long done) {
        Assert.assertTrue(!mInstanceRecords.containsKey(id));

        InstanceRecord instanceRecord = new InstanceRecord(id, taskId, done, null, dailyRepetitionId, null);
        mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
        return instanceRecord;
    }

    public int getMaxInstanceId() {
        return mMaxInstanceId;
    }

    public WeeklyScheduleRecord getWeeklyScheduleRecord(int weeklyScheduleId) {
        return mWeeklyScheduleRecords.get(weeklyScheduleId);
    }

    public ArrayList<WeeklyScheduleDayOfWeekTimeRecord> getWeeklyScheduleDayOfWeekTimeRecords(int weeklyScheduleId) {
        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : mWeeklyScheduleDayOfWeekTimeRecords.values())
            if (weeklyScheduleDayOfWeekTimeRecord.getWeeklyScheduleId() == weeklyScheduleId)
                weeklyScheduleDayOfWeekTimeRecords.add(weeklyScheduleDayOfWeekTimeRecord);
        return weeklyScheduleDayOfWeekTimeRecords;
    }

    public ArrayList<WeeklyRepetitionRecord> getWeeklyRepetitionRecords(int weeklyScheduleDayOfWeekTimeId) {
        ArrayList<WeeklyRepetitionRecord> weeklyRepetitionRecords = new ArrayList<>();
        for (WeeklyRepetitionRecord weeklyRepetitionRecord : mWeeklyRepetitionRecords.values()) {
            if (weeklyRepetitionRecord.getWeeklyScheduleTimeId() == weeklyScheduleDayOfWeekTimeId) {
                weeklyRepetitionRecords.add(weeklyRepetitionRecord);
            }
        }
        return weeklyRepetitionRecords;
    }

    public int getMaxWeeklyRepetitionId() {
        return mMaxWeeklyRepetitionId;
    }

    public InstanceRecord getWeeklyInstanceRecord(int taskId, int weeklyRepetitionId) {
        for (InstanceRecord instanceRecord : mInstanceRecords.values())
            if (instanceRecord.getTaskId() == taskId && instanceRecord.getWeeklyRepetitionId() != null && instanceRecord.getWeeklyRepetitionId().equals(weeklyRepetitionId))
                return instanceRecord;
        return null;
    }

    public InstanceRecord createWeeklyInstanceRecord(int id, int taskId, int weeklyRepetitionId, Long done) {
        Assert.assertTrue(!mInstanceRecords.containsKey(id));

        InstanceRecord instanceRecord = new InstanceRecord(id, taskId, done, null, null, weeklyRepetitionId);
        mInstanceRecords.put(instanceRecord.getId(), instanceRecord);
        return instanceRecord;
    }

    public TaskRecord createTaskRecord(Task parentTask, String name) {
        Assert.assertTrue(name != null);

        int taskId = Collections.max(mTaskRecords.keySet()) + 1;

        Integer parentId = (parentTask != null ? parentTask.getId() : null);

        TaskRecord taskRecord = new TaskRecord(taskId, parentId, name);
        mTaskRecords.put(taskRecord.getId(), taskRecord);

        return taskRecord;
    }

    public SingleScheduleRecord createSingleScheduleRecord(int rootTaskId, Date date, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(date != null);
        Assert.assertTrue((customTime == null) != (hourMinute == null));

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        SingleScheduleRecord singleScheduleRecord = new SingleScheduleRecord(rootTaskId, date.getYear(), date.getMonth(), date.getDay(), customTimeId, hour, minute);
        mSingleScheduleRecords.put(singleScheduleRecord.getRootTaskId(), singleScheduleRecord);

        return singleScheduleRecord;
    }

    public DailyScheduleRecord createDailyScheduleRecord(int rootTaskId) {
        DailyScheduleRecord dailyScheduleRecord = new DailyScheduleRecord(rootTaskId, TimeStamp.getNow().getLong(), null);
        mDailyScheduleRecords.put(dailyScheduleRecord.getRootTaskId(), dailyScheduleRecord);

        return dailyScheduleRecord;
    }

    public DailyScheduleTimeRecord createDailyScheduleTimeRecord(DailySchedule dailySchedule, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(dailySchedule != null);
        Assert.assertTrue((customTime == null) != (hourMinute == null));

        int dailyScheduleTimeRecordId = Collections.max(mDailyScheduleTimeRecords.keySet()) + 1;

        Integer customTimeId = (customTime != null ? customTime.getId() : null);

        Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
        Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);

        DailyScheduleTimeRecord dailyScheduleTimeRecord = new DailyScheduleTimeRecord(dailyScheduleTimeRecordId, dailySchedule.getRootTaskId(), customTimeId, hour, minute);
        mDailyScheduleTimeRecords.put(dailyScheduleTimeRecord.getId(), dailyScheduleTimeRecord);
        return dailyScheduleTimeRecord;
    }

    public WeeklyScheduleRecord createWeeklyScheduleRecord(int rootTaskId) {
        WeeklyScheduleRecord weeklyScheduleRecord = new WeeklyScheduleRecord(rootTaskId, TimeStamp.getNow().getLong(), null);
        mWeeklyScheduleRecords.put(weeklyScheduleRecord.getRootTaskId(), weeklyScheduleRecord);

        return weeklyScheduleRecord;
    }

    public WeeklyScheduleDayOfWeekTimeRecord createWeeklyScheduleDayOfWeekTimeRecord(WeeklySchedule weeklySchedule, DayOfWeek dayOfWeek, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(weeklySchedule != null);
        Assert.assertTrue(dayOfWeek != null);
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