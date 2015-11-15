package com.example.krystianwsul.organizatortest.persistencemodel;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Krystian on 10/27/2015.
 */
public class PersistenceManger {
    private static PersistenceManger mInstance;

    private final HashMap<Integer, CustomTimeRecord> mCustomTimeRecords = new HashMap<>();

    private final HashMap<Integer, TaskRecord> mTaskRecords = new HashMap<>();

    private final HashMap<Integer, SingleScheduleRecord> mSingleScheduleRecords = new HashMap<>();
    private final HashMap<Integer, SingleRepetitionRecord> mSingleRepetitionRecords = new HashMap<>();
    private final HashMap<Integer, SingleInstanceRecord> mSingleInstanceRecords = new HashMap<>();

    private final HashMap<Integer, DailyScheduleRecord> mDailyScheduleRecords = new HashMap<>();
    private final HashMap<Integer, DailyScheduleTimeRecord> mDailyScheduleTimeRecords = new HashMap<>();
    private final HashMap<Integer, DailyRepetitionRecord> mDailyRepetitionRecords = new HashMap<>();
    private final HashMap<Integer, DailyInstanceRecord> mDailyInstanceRecords = new HashMap<>();

    private final HashMap<Integer, WeeklyScheduleRecord> mWeeklyScheduleRecords = new HashMap<>();
    private final HashMap<Integer, WeeklyScheduleDayTimeRecord> mWeeklyScheduleDayTimeRecords = new HashMap<>();
    private final HashMap<Integer, WeeklyRepetitionRecord> mWeeklyRepetitionRecords = new HashMap<>();
    private final HashMap<Integer, WeeklyInstanceRecord> mWeeklyInstanceRecords = new HashMap<>();

    private final int mMaxDailyRepetitionId = 0;
    private final int mMaxSingleInstanceId = 0;
    private final int mMaxDailyInstanceId = 0;
    private final int mMaxWeeklyRepetitionId = 0;
    private final int mMaxWeeklyInstanceId = 0;

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

        DailyScheduleRecord alwaysAfterWakingAfterWork = new DailyScheduleRecord(0, iliotibial.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mDailyScheduleRecords.put(alwaysAfterWakingAfterWork.getId(), alwaysAfterWakingAfterWork);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork0 = new DailyScheduleTimeRecord(1, alwaysAfterWakingAfterWork.getId(), afterWaking.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork0.getId(), alwaysAfterWakingAfterWork0);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork1 = new DailyScheduleTimeRecord(2, alwaysAfterWakingAfterWork.getId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWakingAfterWork1.getId(), alwaysAfterWakingAfterWork1);

        TaskRecord hamstring = new TaskRecord(8, null, "hamstring stretch");
        mTaskRecords.put(hamstring.getId(), hamstring);

        DailyScheduleRecord alwaysAfterWork = new DailyScheduleRecord(1, hamstring.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mDailyScheduleRecords.put(alwaysAfterWork.getId(), alwaysAfterWork);
        DailyScheduleTimeRecord alwaysAfterWork0 = new DailyScheduleTimeRecord(0, alwaysAfterWork.getId(), afterWork.getId(), null, null);
        mDailyScheduleTimeRecords.put(alwaysAfterWork0.getId(), alwaysAfterWork0);

        TaskRecord piecyk = new TaskRecord(9, null, "piecyk");
        mTaskRecords.put(piecyk.getId(), piecyk);

        SingleScheduleRecord todayAfterWaking = new SingleScheduleRecord(piecyk.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), afterWaking.getId(), null, null);
        mSingleScheduleRecords.put(todayAfterWaking.getRootTaskId(), todayAfterWaking);

        TaskRecord paznokcie = new TaskRecord(10, null, "paznokcie");
        mTaskRecords.put(paznokcie.getId(), paznokcie);

        WeeklyScheduleRecord crazyWeekend = new WeeklyScheduleRecord(0, paznokcie.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mWeeklyScheduleRecords.put(crazyWeekend.getId(), crazyWeekend);
        WeeklyScheduleDayTimeRecord crazyWeekend0 = new WeeklyScheduleDayTimeRecord(0, crazyWeekend.getId(), DayOfWeek.SATURDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayTimeRecords.put(crazyWeekend0.getId(), crazyWeekend0);
        WeeklyScheduleDayTimeRecord crazyWeekend1 = new WeeklyScheduleDayTimeRecord(1, crazyWeekend.getId(), DayOfWeek.SUNDAY.ordinal(), afterWaking.getId(), null, null);
        mWeeklyScheduleDayTimeRecords.put(crazyWeekend1.getId(), crazyWeekend1);
        WeeklyScheduleDayTimeRecord crazyWeekend2 = new WeeklyScheduleDayTimeRecord(2, crazyWeekend.getId(), DayOfWeek.SUNDAY.ordinal(), null, 17, 0);
        mWeeklyScheduleDayTimeRecords.put(crazyWeekend2.getId(), crazyWeekend2);

        TaskRecord task6 = new TaskRecord(11, null, "task 6");
        mTaskRecords.put(task6.getId(), task6);

        DailyScheduleRecord task6schedule = new DailyScheduleRecord(2, task6.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        mDailyScheduleRecords.put(task6schedule.getId(), task6schedule);
        DailyScheduleTimeRecord task6schedule0 = new DailyScheduleTimeRecord(3, task6schedule.getId(), null, 6, 0);
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
        ArrayList<Integer> taskIds = new ArrayList();
        for (TaskRecord taskRecord : mTaskRecords.values())
            if (taskRecord.getParentTaskId() == parentTaskId)
                taskIds.add(taskRecord.getId());
        return taskIds;
    }

    public SingleScheduleRecord getSingleScheduleRecord(int rootTaskId) {
        return mSingleScheduleRecords.get(rootTaskId);
    }

    public SingleRepetitionRecord getSingleRepetitionRecord(int rootTaskId) {
        return mSingleRepetitionRecords.get(rootTaskId);
    }

    public SingleInstanceRecord getSingleInstanceRecord(int rootTaskId) {
        return mSingleInstanceRecords.get(rootTaskId);
    }

    public int getMaxSingleInstanceId() {
        return mMaxSingleInstanceId;
    }

    public DailyScheduleRecord getDailyScheduleRecord(int dailyScheduleId) {
        return mDailyScheduleRecords.get(dailyScheduleId);
    }

    public ArrayList<Integer> getDailyScheduleIds(int rootTaskId) {
        ArrayList<Integer> dailyScheduleIds = new ArrayList<>();
        for (DailyScheduleRecord dailyScheduleRecord : mDailyScheduleRecords.values())
            if (dailyScheduleRecord.getRootTaskId() == rootTaskId)
                dailyScheduleIds.add(dailyScheduleRecord.getId());
        return dailyScheduleIds;
    }

    public DailyScheduleTimeRecord getDailyScheduleTimeRecord(int dailyScheduleTimeId) {
        return mDailyScheduleTimeRecords.get(dailyScheduleTimeId);
    }

    public ArrayList<Integer> getDailyScheduleTimeIds(int dailyScheduleTimeId) {
        ArrayList<Integer> dailyScheduleTimeIds = new ArrayList<>();
        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : mDailyScheduleTimeRecords.values())
            if (dailyScheduleTimeRecord.getDailyScheduleId() == dailyScheduleTimeId)
                dailyScheduleTimeIds.add(dailyScheduleTimeRecord.getId());
        return dailyScheduleTimeIds;
    }

    public DailyRepetitionRecord getDailyRepetitionRecord(int dailyRepetitionId) {
        return mDailyRepetitionRecords.get(dailyRepetitionId);
    }

    public DailyRepetitionRecord getDailyRepetitionRecord(int dailyScheduleTimeId, Date scheduleDate) {
        for (DailyRepetitionRecord dailyRepetitionRecord : mDailyRepetitionRecords.values()) {
            if (dailyRepetitionRecord.getDailyScheduleTimeId() == dailyScheduleTimeId && dailyRepetitionRecord.getScheduleYear() == scheduleDate.getYear() && dailyRepetitionRecord.getScheduleMonth() == scheduleDate.getMonth() && dailyRepetitionRecord.getScheduleDay() == scheduleDate.getDay()) {
                return dailyRepetitionRecord;
            }
        }
        return null;
    }

    public int getMaxDailyRepetitionId() {
        return mMaxDailyRepetitionId;
    }

    public DailyInstanceRecord getDailyInstanceRecord(int dailyInstanceId) {
        return mDailyInstanceRecords.get(dailyInstanceId);
    }

    public DailyInstanceRecord getDailyInstanceRecord(int taskId, int dailyRepetitionId) {
        for (DailyInstanceRecord dailyInstanceRecord : mDailyInstanceRecords.values()) {
            if (dailyInstanceRecord.getTaskId() == taskId && dailyInstanceRecord.getDailyRepetitionId() == dailyRepetitionId) {
                return dailyInstanceRecord;
            }
        }
        return null;
    }

    public int getMaxDailyInstanceId() {
        return mMaxDailyInstanceId;
    }

    public WeeklyScheduleRecord getWeeklyScheduleRecord(int weeklyScheduleId) {
        return mWeeklyScheduleRecords.get(weeklyScheduleId);
    }

    public ArrayList<Integer> getWeeklyScheduleIds(int rootTaskId) {
        ArrayList<Integer> weeklyScheduleIds = new ArrayList<>();
        for (WeeklyScheduleRecord weeklyScheduleRecord : mWeeklyScheduleRecords.values())
            if (weeklyScheduleRecord.getRootTaskId() == rootTaskId)
                weeklyScheduleIds.add(weeklyScheduleRecord.getId());
        return weeklyScheduleIds;
    }

    public ArrayList<Integer> getWeeklyScheduleDayTimeIds(int weeklyScheduleId) {
        ArrayList<Integer> weeklyScheduleDayTimeIds = new ArrayList<>();
        for (WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord : mWeeklyScheduleDayTimeRecords.values())
            if (weeklyScheduleDayTimeRecord.getWeeklyScheduleId() == weeklyScheduleId)
                weeklyScheduleDayTimeIds.add(weeklyScheduleDayTimeRecord.getId());
        return weeklyScheduleDayTimeIds;
    }

    public WeeklyScheduleDayTimeRecord getWeeklyScheduleDayTimeRecord(int weeklyScheduleDayTimeId) {
        return mWeeklyScheduleDayTimeRecords.get(weeklyScheduleDayTimeId);
    }

    public WeeklyRepetitionRecord getWeeklyRepetitionRecord(int weeklyRepetitionId) {
        return mWeeklyRepetitionRecords.get(weeklyRepetitionId);
    }

    public WeeklyRepetitionRecord getWeeklyRepetitionRecord(int weeklyScheduleDayTimeId, Date scheduleDate) {
        for (WeeklyRepetitionRecord weeklyRepetitionRecord : mWeeklyRepetitionRecords.values()) {
            if (weeklyRepetitionRecord.getWeeklyScheduleTimeId() == weeklyScheduleDayTimeId && weeklyRepetitionRecord.getScheduleYear() == scheduleDate.getYear() && weeklyRepetitionRecord.getScheduleMonth() == scheduleDate.getMonth() && weeklyRepetitionRecord.getScheduleDay() == scheduleDate.getDay()) {
                return weeklyRepetitionRecord;
            }
        }
        return null;
    }

    public int getMaxWeeklyRepetitionId() {
        return mMaxWeeklyRepetitionId;
    }

    public WeeklyInstanceRecord getWeeklyInstanceRecord(int weeklyInstanceId) {
        return mWeeklyInstanceRecords.get(weeklyInstanceId);
    }

    public WeeklyInstanceRecord getWeeklyInstanceRecord(int taskId, int weeklyRepetitionId) {
        for (WeeklyInstanceRecord weeklyInstanceRecord : mWeeklyInstanceRecords.values()) {
            if (weeklyInstanceRecord.getTaskId() == taskId && weeklyInstanceRecord.getWeeklyRepetitionId() == weeklyRepetitionId) {
                return weeklyInstanceRecord;
            }
        }
        return null;
    }

    public int getMaxWeeklyInstanceId() {
        return mMaxWeeklyInstanceId;
    }
}