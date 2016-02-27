package com.example.krystianwsul.organizator.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "tasks.db";
    public static final int DATABASE_VERSION = 22;

    private static SQLiteDatabase sSQLiteDatabase;

    public static SQLiteDatabase getDatabase(Context context) {
        if (sSQLiteDatabase == null)
            sSQLiteDatabase = new MySQLiteHelper(context.getApplicationContext()).getWritableDatabase();
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
    }
}
