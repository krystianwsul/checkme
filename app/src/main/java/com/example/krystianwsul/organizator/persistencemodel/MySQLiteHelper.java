package com.example.krystianwsul.organizator.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class MySQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 8;

    private static SQLiteDatabase sSQLiteDatabase;

    public static SQLiteDatabase getDatabase(Context applicationContext) {
        if (sSQLiteDatabase == null)
            sSQLiteDatabase = new MySQLiteHelper(applicationContext).getWritableDatabase();
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
        sqLiteDatabase.beginTransaction();

        try
        {
            CustomTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            TaskRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            TaskHierarchyRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            ScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            DailyScheduleTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            SingleScheduleDateTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            WeeklyScheduleDayOfWeekTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            InstanceRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }
}
