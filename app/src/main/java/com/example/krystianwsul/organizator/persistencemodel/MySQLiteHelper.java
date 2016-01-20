package com.example.krystianwsul.organizator.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import junit.framework.Assert;

import java.util.Calendar;

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "tasks.db";
    public static final int DATABASE_VERSION = 9;

    private static SQLiteDatabase sSQLiteDatabase;

    public synchronized static SQLiteDatabase getDatabase(Context context) {
        Assert.assertTrue(context != null);
        if (sSQLiteDatabase == null)
            sSQLiteDatabase = new MySQLiteHelper(context).getWritableDatabase();
        return sSQLiteDatabase;
    }

    private MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        CustomTimeRecord.onCreate(sqLiteDatabase);

        TaskRecord.onCreate(sqLiteDatabase);
        TaskHierarchyRecord.onCreate(sqLiteDatabase);

        ScheduleRecord.onCreate(sqLiteDatabase);
        DailyScheduleTimeRecord.onCreate(sqLiteDatabase);
        SingleScheduleDateTimeRecord.onCreate(sqLiteDatabase);
        WeeklyScheduleDayOfWeekTimeRecord.onCreate(sqLiteDatabase);

        InstanceRecord.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        CustomTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

        TaskRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        TaskHierarchyRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

        ScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        DailyScheduleTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        SingleScheduleDateTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        WeeklyScheduleDayOfWeekTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

        InstanceRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

        ///

        Calendar calendarToday = Calendar.getInstance();

        Calendar calendarFewDaysAgo = Calendar.getInstance();
        calendarFewDaysAgo.add(Calendar.DATE, -10);

        Calendar calendarYesterday = Calendar.getInstance();
        calendarYesterday.add(Calendar.DATE, -1);

        Calendar calendarNextYear = Calendar.getInstance();
        calendarNextYear.add(Calendar.DATE, 365);

        CustomTimeRecord afterWaking = CustomTimeRecord.createCustomTimeRecord(sqLiteDatabase, "po wstaniu", 9, 0, 6, 0, 6, 0, 6, 0, 6, 0, 6, 0, 9, 0);
        CustomTimeRecord afterWork = CustomTimeRecord.createCustomTimeRecord(sqLiteDatabase, "po pracy", 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0);

        TaskRecord zakupy = TaskRecord.createTaskRecord(sqLiteDatabase, "zakupy", calendarFewDaysAgo.getTimeInMillis(), null);

        TaskRecord halls = TaskRecord.createTaskRecord(sqLiteDatabase, "halls", calendarFewDaysAgo.getTimeInMillis(), null);
        TaskHierarchyRecord zakupyHalls = TaskHierarchyRecord.createTaskHierarchyRecord(sqLiteDatabase, zakupy.getId(), halls.getId(), calendarFewDaysAgo.getTimeInMillis(), null);

        TaskRecord biedronka = TaskRecord.createTaskRecord(sqLiteDatabase, "biedronka", calendarFewDaysAgo.getTimeInMillis(), null);
        TaskHierarchyRecord zakupyBiedronka = TaskHierarchyRecord.createTaskHierarchyRecord(sqLiteDatabase, zakupy.getId(), biedronka.getId(), calendarFewDaysAgo.getTimeInMillis(), null);

        TaskRecord czosnek = TaskRecord.createTaskRecord(sqLiteDatabase, "czosnek", calendarFewDaysAgo.getTimeInMillis(), null);
        TaskHierarchyRecord biedronkaCzosnek = TaskHierarchyRecord.createTaskHierarchyRecord(sqLiteDatabase, biedronka.getId(), czosnek.getId(), calendarFewDaysAgo.getTimeInMillis(), null);
        TaskRecord piersi = TaskRecord.createTaskRecord(sqLiteDatabase, "piersi", calendarFewDaysAgo.getTimeInMillis(), null);
        TaskHierarchyRecord biedronkaPiersi = TaskHierarchyRecord.createTaskHierarchyRecord(sqLiteDatabase, biedronka.getId(), piersi.getId(), calendarFewDaysAgo.getTimeInMillis(), null);

        ScheduleRecord today15 = ScheduleRecord.createScheduleRecord(sqLiteDatabase, zakupy.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        SingleScheduleDateTimeRecord today150 = SingleScheduleDateTimeRecord.createSingleScheduleDateTimeRecord(sqLiteDatabase, today15.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 15, 0);

        TaskRecord rachunek = TaskRecord.createTaskRecord(sqLiteDatabase, "rachunek", calendarFewDaysAgo.getTimeInMillis(), null);

        ScheduleRecord yesterday16 = ScheduleRecord.createScheduleRecord(sqLiteDatabase, rachunek.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        SingleScheduleDateTimeRecord yesterday160 = SingleScheduleDateTimeRecord.createSingleScheduleDateTimeRecord(sqLiteDatabase, yesterday16.getId(), calendarYesterday.get(Calendar.YEAR), calendarYesterday.get(Calendar.MONTH) + 1, calendarYesterday.get(Calendar.DAY_OF_MONTH), null, 16, 0);

        TaskRecord banany = TaskRecord.createTaskRecord(sqLiteDatabase, "banany", calendarFewDaysAgo.getTimeInMillis(), null);

        ScheduleRecord today17 = ScheduleRecord.createScheduleRecord(sqLiteDatabase, banany.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 0);
        SingleScheduleDateTimeRecord today170 = SingleScheduleDateTimeRecord.createSingleScheduleDateTimeRecord(sqLiteDatabase, today17.getId(), calendarToday.get(Calendar.YEAR), calendarToday.get(Calendar.MONTH) + 1, calendarToday.get(Calendar.DAY_OF_MONTH), null, 17, 0);

        TaskRecord iliotibial = TaskRecord.createTaskRecord(sqLiteDatabase, "iliotibial band stretch", calendarFewDaysAgo.getTimeInMillis(), null);

        ScheduleRecord alwaysAfterWakingAfterWork = ScheduleRecord.createScheduleRecord(sqLiteDatabase, iliotibial.getId(), calendarFewDaysAgo.getTimeInMillis(), null, 1);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork0 = DailyScheduleTimeRecord.createDailyScheduleTimeRecord(sqLiteDatabase, alwaysAfterWakingAfterWork.getId(), afterWaking.getId(), null, null);
        DailyScheduleTimeRecord alwaysAfterWakingAfterWork1 = DailyScheduleTimeRecord.createDailyScheduleTimeRecord(sqLiteDatabase, alwaysAfterWakingAfterWork.getId(), afterWork.getId(), null, null);
    }
}
