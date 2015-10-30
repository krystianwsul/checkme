package com.example.krystianwsul.organizatortest.persistencemodel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Krystian on 10/27/2015.
 */
public class PersistenceManger {
    private static PersistenceManger mInstance;

    private HashMap<Integer, TimeRecord> mTimeRecords = new HashMap<>();
    private HashMap<Integer, SingleScheduleRecord> mSingleScheduleRecords = new HashMap<>();
    private HashMap<Integer, WeeklyScheduleRecord> mWeeklyScheduleRecords = new HashMap<>();
    private HashMap<Integer, WeeklyScheduleTimeRecord> mWeeklyScheduleTimeRecords = new HashMap<>();
    private HashMap<Integer, TaskRecord> mTaskRecords = new HashMap<>();

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

        TimeRecord afterWaking = new TimeRecord(0, "po wstaniu", 9, 0, 6, 0, 6, 0, 6, 0, 6, 0, 6, 0, 9, 0);
        mTimeRecords.put(afterWaking.getId(), afterWaking);
        TimeRecord afterWork = new TimeRecord(1, "po pracy", null, null, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0);
        mTimeRecords.put(afterWork.getId(), afterWork);

        SingleScheduleRecord todayAfterWaking = new SingleScheduleRecord(0, calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), afterWaking.getId(), null, null);
        mSingleScheduleRecords.put(todayAfterWaking.getId(), todayAfterWaking);

        SingleScheduleRecord today15 = new SingleScheduleRecord(1, calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 15, 0);
        mSingleScheduleRecords.put(today15.getId(), today15);
        SingleScheduleRecord yesterday16 = new SingleScheduleRecord(2, calendarYesterday.get(Calendar.YEAR), calendarYesterday.get(Calendar.MONTH) + 1, calendarYesterday.get(Calendar.DAY_OF_MONTH), null, 16, 0);
        mSingleScheduleRecords.put(yesterday16.getId(), yesterday16);

        WeeklyScheduleRecord alwaysAfterWork = new WeeklyScheduleRecord(0, calendarFewDaysAgo.getTimeInMillis(), null);
        mWeeklyScheduleRecords.put(alwaysAfterWork.getId(), alwaysAfterWork);
        WeeklyScheduleTimeRecord alwaysAfterWork0 = new WeeklyScheduleTimeRecord(0, alwaysAfterWork.getId(), afterWork.getId(), null, null);
        mWeeklyScheduleTimeRecords.put(alwaysAfterWork0.getId(), alwaysAfterWork0);

        WeeklyScheduleRecord alwaysAfterWakingAfterWork = new WeeklyScheduleRecord(1, calendarFewDaysAgo.getTimeInMillis(), null);
        mWeeklyScheduleRecords.put(alwaysAfterWakingAfterWork.getId(), alwaysAfterWakingAfterWork);
        WeeklyScheduleTimeRecord alwaysAfterWakingAfterWork0 = new WeeklyScheduleTimeRecord(1, alwaysAfterWakingAfterWork.getId(), afterWaking.getId(), null, null);
        mWeeklyScheduleTimeRecords.put(alwaysAfterWakingAfterWork0.getId(), alwaysAfterWakingAfterWork0);
        WeeklyScheduleTimeRecord alwaysAfterWakingAfterWork1 = new WeeklyScheduleTimeRecord(2, alwaysAfterWakingAfterWork.getId(), afterWork.getId(), null, null);
        mWeeklyScheduleTimeRecords.put(alwaysAfterWakingAfterWork1.getId(), alwaysAfterWakingAfterWork1);

        TaskRecord zakupy = new TaskRecord(0, null, "zakupy", today15.getId(), null);
        mTaskRecords.put(zakupy.getId(), zakupy);
        TaskRecord halls = new TaskRecord(1, zakupy.getId(), "halls", null, null);
        mTaskRecords.put(halls.getId(), halls);
        TaskRecord biedronka = new TaskRecord(2, zakupy.getId(), "biedronka", null, null);
        mTaskRecords.put(biedronka.getId(), biedronka);
        TaskRecord czosnek = new TaskRecord(3, biedronka.getId(), "czosnek", null, null);
        mTaskRecords.put(czosnek.getId(), czosnek);
        TaskRecord piersi = new TaskRecord(4, biedronka.getId(), "piersi", null, null);
        mTaskRecords.put(piersi.getId(), piersi);

        TaskRecord rachunek = new TaskRecord(5, null, "rachunek", yesterday16.getId(), null);
        mTaskRecords.put(rachunek.getId(), rachunek);

        TaskRecord banany = new TaskRecord(6, null, "banany", today15.getId(), null);
        mTaskRecords.put(banany.getId(), banany);

        TaskRecord iliotibial = new TaskRecord(7, null, "iliotibial band stretch", null, alwaysAfterWakingAfterWork.getId());
        mTaskRecords.put(iliotibial.getId(), iliotibial);

        TaskRecord hamstring = new TaskRecord(8, null, "hamstring stretch", null, alwaysAfterWork.getId());
        mTaskRecords.put(hamstring.getId(), hamstring);
    }

    public SingleScheduleRecord getSingleScheduleRecord(int singleScheduleId) {
        return mSingleScheduleRecords.get(singleScheduleId);
    }

    public TimeRecord getTimeRecord(int timeRecordId) {
        return mTimeRecords.get(timeRecordId);
    }

    public WeeklyScheduleRecord getWeeklyScheduleRecord(int weeklyScheduleId) {
        return mWeeklyScheduleRecords.get(weeklyScheduleId);
    }

    public WeeklyScheduleTimeRecord getWeeklyScheduleTimeRecord(int weeklyScheduleTimeId) {
        return mWeeklyScheduleTimeRecords.get(weeklyScheduleTimeId);
    }

    public ArrayList<Integer> getWeeklyScheduleTimeIds(int weeklyScheduleTimeId) {
        ArrayList<Integer> weeklyScheduleTimeIds = new ArrayList<>();
        for (WeeklyScheduleTimeRecord weeklyScheduleTimeRecord : mWeeklyScheduleTimeRecords.values())
            if (weeklyScheduleTimeRecord.getWeeklyScheduleId() == weeklyScheduleTimeId)
                weeklyScheduleTimeIds.add(weeklyScheduleTimeRecord.getId());
        return weeklyScheduleTimeIds;
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
}
